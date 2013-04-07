package com.rayleeya.lanmessenger.service;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.rayleeya.lanmessenger.BuildConfig;
import com.rayleeya.lanmessenger.R;
import com.rayleeya.lanmessenger.ipmsg.IpMsgConst;
import com.rayleeya.lanmessenger.model.Group;
import com.rayleeya.lanmessenger.model.User;
import com.rayleeya.lanmessenger.net.UdpManager;
import com.rayleeya.lanmessenger.ui.ChatRoomActivity;
import com.rayleeya.lanmessenger.util.Errors;
import com.rayleeya.lanmessenger.util.Settings;
import com.rayleeya.lanmessenger.util.Utils;

public class LanMessengerService extends Service {

	private static final String TAG = "LanMessengerService";
	private static final boolean DEBUG = BuildConfig.DEBUG;
	
	private LanMessengerManager mMsgerManager;
	private HashMap<String, Group> mGroups;
	private HashMap<String, User> mUsers;
	User mSelf;
	
	public LanMessengerService() {
		super();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		if (DEBUG) Log.v(TAG, "onCreate()");
		
		int error = Errors.NO_ERROR;
		UdpManager udpManager = null;
		try {
			udpManager = UdpManager.getInstance();
		} catch (BindException e) {
			e.printStackTrace();
			error = Errors.ERR_SO_ADDR_ALREADY_IN_USE;
		} catch (SocketException e) {
			e.printStackTrace();
			error = Errors.ERR_SO;
		}
		
		mGroups = new HashMap<String, Group>();
		mUsers = new HashMap<String, User>();
		
		MsgReceiverThread receiver = new MsgReceiverThread();
		MsgSenderThread sender = new MsgSenderThread();
		mMsgerManager = new LanMessengerManager(sender, receiver);
		
		User self = createSelf(udpManager);
		mUsers.put(self.getIp(), self);
		mGroups.put(self.getGroup().getName(), self.getGroup());

		receiver.setLanMessengerManager(mMsgerManager);
		receiver.setUdpManager(udpManager);
		receiver.start();
		
		sender.setLanMessengerManager(mMsgerManager);
		sender.setUdpManager(udpManager);
		sender.start();
		
		mMsgerManager.setError(error);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mMsgerManager;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (DEBUG) Log.v(TAG, "onDestroy()");
		try {
			mMsgerManager.sendExit();
			mMsgerManager.quit();
		} catch (Exception e) {
			//just ignore
		}
		mGroups.clear();
		mUsers.clear();
	}

	//-------- Manage Users / Groups --------
	public User createSelf(UdpManager um) {
		if (mSelf == null) {
			String nickname = Settings.getString(this, Settings.PREF_NICKNAME, um.getUsername());
			String groupname = Settings.getString(this, Settings.PREF_GROUPNAME, getString(R.string.def_groupname));
			
			User self = new User();
			self.setNickname(nickname);
			self.setUsername(um.getUsername());
			self.setHostname(um.getHostname());
			self.setIp(um.getLocalAddress());
			self.setPort(IpMsgConst.getPort());

			Group group = new Group(Group.GROUP_MYSELF, groupname);
			group.addUser(self);
			
			mSelf = self;
		}
		return mSelf;
	}
	
	
	//-------- Error Observerable/Observers --------
	public interface OnErrorListener {
		public void onError(int errno, String msg);
	}
			
	//------------------------------------------------------------------------------------------
	//------------------------ The Binder Server : LanMessengerManager -------------------------
	//------------------------------------------------------------------------------------------
	
	public class LanMessengerManager extends Binder implements ILanMessenger {
		
		private static final String TAG = "LanMessengerManager";
		
		private MsgSenderThread mSender;
		private MsgReceiverThread mReceiver;
		private H mH;
		
		private Comparator<User> mUserComparator = new Comparator<User>() {
			@Override
			public int compare(User u1, User u2) {
				if (u1 == u2) return 0;
				return u1.getNickname().compareTo(u2.getNickname());
			}
		};
		
		private Comparator<Group> mGroupComparator = new Comparator<Group>() {
			@Override
			public int compare(Group g1, Group g2) {
				if (g1 == g2) return 0;
				return g1.getName().compareTo(g2.getName());
			}
		};
		
		private class H extends Handler {

			private static final int DATASET_REFRESH_DELAY = 200;
			
			private static final int MSG_HANDLE_ERROR = 1;
			private static final int MSG_HANDLE_DATASET_CHANGED = 2;
			private static final int MSG_HANDLE_DATASET_INVALIDATED = 3;
			private static final int MSG_RECEIVE_VOICE_REQ = 4;
			private static final int MSG_RECEIVE_VOICE_RES = 5;
			
			@Override
			public void handleMessage(Message msg) {
				int what = msg.what;
				switch (what) {
					case MSG_HANDLE_ERROR : 
						notifyError(msg.arg1);
						break;
						
					case MSG_HANDLE_DATASET_CHANGED :
						notifyDataSetChanged();
						break;
						
					case MSG_HANDLE_DATASET_INVALIDATED :
						notifyDataSetInvalidated();
						break;
						
					case MSG_RECEIVE_VOICE_REQ :
						execReceiveVoidRequest((SocketAddress)msg.obj);
						break;
						
					case MSG_RECEIVE_VOICE_RES :
						//TODO: work is here.
//						receiveVoidResponse((SocketAddress)msg.obj, msg.arg1);
						break;
				}
			}
		}
		
		public LanMessengerManager(MsgSenderThread sender, MsgReceiverThread receiver) {
			mSender = sender;
			mReceiver = receiver;
			mH = new H();
		}
		
		private final ArrayList<OnErrorListener> mErrorListeners = new ArrayList<OnErrorListener>();
		private int errorcode = Errors.NO_ERROR;
		
		public void regitsterOnErrorListener(OnErrorListener listener) {
			mErrorListeners.add(listener);
		}
		
		public void unregitsterOnErrorListener(OnErrorListener listener) {
			mErrorListeners.remove(listener);
		}
		
		private void notifyError(int errno) {
			String msg = ""; //Unknown error
			switch (errno) {
				case Errors.ERR_SO : 
				//TODO: More detail error handlers
				break;
			}
			
			for (OnErrorListener l : mErrorListeners) {
				l.onError(errno, msg);
			}
		}
		
		public void handleError(int errno) {
			Message msg = mH.obtainMessage(H.MSG_HANDLE_ERROR, errno, 0);
			mH.sendMessage(msg);
		}
		
		public int getError() {
			return errorcode;
		}
		
		public void setError(int err) {
			errorcode = err;
		}
		
		public boolean hasError() {
			return errorcode != Errors.NO_ERROR;
		}
		//-------- Data Set Observerable/Observers --------
		private final DataSetObservable mDataSetObservable = new DataSetObservable();
	    
	    public void registerDataSetObserver(DataSetObserver observer) {
	        mDataSetObservable.registerObserver(observer);
	    }

	    public void unregisterDataSetObserver(DataSetObserver observer) {
	        mDataSetObservable.unregisterObserver(observer);
	    }
	    
	    private void notifyDataSetInvalidated() {
	        mDataSetObservable.notifyInvalidated();
	    }
	    
	    private void notifyDataSetChanged() {
	        mDataSetObservable.notifyChanged();
	    }
	    
	    private void handleDataSetInvalidated() {
	    	mH.removeMessages(H.MSG_HANDLE_DATASET_INVALIDATED);
			mH.sendEmptyMessageDelayed(H.MSG_HANDLE_DATASET_INVALIDATED, H.DATASET_REFRESH_DELAY);
	    }
	    
	    private void handleDataSetChanged() {
	    	mH.removeMessages(H.MSG_HANDLE_DATASET_CHANGED);
			mH.sendEmptyMessageDelayed(H.MSG_HANDLE_DATASET_CHANGED, H.DATASET_REFRESH_DELAY);
	    }
	    
		//-------- SendXxx methods --------
	    //-------- Run in the main thread --------
		@Override
		public void sendEntry() {
			mSender.sendEntry();
		}

		@Override
		public void sendExit() {
			mSender.sendExit();
		}

		@Override
		public void sendAnsEntry(SocketAddress socketAddress) {
			mSender.sendAnsEntry(socketAddress);
		}
		
		@Override
		public void sendVoiceRequest(User user) {
			if (user != null) {
				SocketAddress sa = new InetSocketAddress(user.getIp(), user.getPort());
				mSender.sendVoiceRequest(sa);
			}
		}
		
		@Override
		public void sendVoiceResponse(SocketAddress sockAddress, boolean accept) {
			mSender.sendVoiceResponse(sockAddress, accept);
		}
		
		//-------- ReceivXxx methods --------
		//-------- Run in the thread of MsgReceiverThread --------
		public boolean receiveAddUser(User user, int groupType, String groupName) {
			boolean ret = false;
			synchronized (mUsers) {
				if (user != null) {
					User u = mUsers.put(user.getIp(), user);
					Group og = null;
					if (u != null) {
						og = u.getGroup();
						u.removeFromGroup();
					}
					
					Group g = mGroups.get(groupName);
					if (g == null) {
						g = new Group(groupType, groupName);
						mGroups.put(groupName, g);
					}
					g.addUser(user);
	
					if (user.getIp().equals(mSelf.getIp())) {
						mSelf = user;
					}
					
					if (og != null && og.getUsers().isEmpty()) mGroups.remove(og);
					
					ret = true;
				}
			}
			handleDataSetChanged();
			return ret;
		}
		
		public boolean receiveRemoveUser(User user) {
			boolean ret = false;
			synchronized (mUsers) {
				if (user != null) {
					User u = mUsers.remove(user.getIp());
					if (u != null) {
						Group g = u.getGroup();
						u.removeFromGroup();
						if (g != null && g.getUsers().isEmpty()) {
							mGroups.remove(g.getName());
						}
					}
					ret = true;
				}
			}
			handleDataSetChanged();
			return ret;
		}
		
		public boolean receiveVoiceRequest(SocketAddress socketAddress) {
			Message msg = mH.obtainMessage(H.MSG_RECEIVE_VOICE_REQ, socketAddress);
			mH.sendMessage(msg);
			return true;
		}
		
		private void execReceiveVoidRequest(final SocketAddress socketAddress) {
			final Context cxt = LanMessengerService.this.getApplicationContext();
			AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
			builder.setMessage(R.string.msg_voice_request);
			builder.setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent i = new Intent(cxt, ChatRoomActivity.class);
					i.putExtra(Utils.EXTRA_ADDRESS, socketAddress);
					cxt.startActivity(i);
					mMsgerManager.sendVoiceResponse(socketAddress, true);
				}
			});
			builder.setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mMsgerManager.sendVoiceResponse(socketAddress, false);
				}
			});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					mMsgerManager.sendVoiceResponse(socketAddress, false);
				}
			});
			
			builder.show();
		}
		
		public boolean receiveVoiceResponse(SocketAddress socketAddress, int accept) {
			Message msg = mH.obtainMessage(H.MSG_RECEIVE_VOICE_RES, accept, 0, socketAddress);
			mH.sendMessage(msg);
			return true;
		}
		
		public User getSelfUser() {
			return mSelf;
		}

		public List<Group> getGroupsAndUsers() {
			ArrayList<Group> groups = new ArrayList<Group>();
			if (mGroups == null || mGroups.isEmpty()) return new ArrayList<Group>();
				
			synchronized(mUsers) {
				Iterator<Group> it = mGroups.values().iterator();
				while (it.hasNext()) {
					Group g = it.next();
					try {
						if (g == null || g.getUsers().size() == 0) {
							it.remove();
						} else {
							Group tg = g.clone();
							groups.add(tg);
						}
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
					}
				}
			}
			
			for (Group g : groups) {
				Collections.sort(g.getUsers(), mUserComparator);
			}
			Collections.sort(groups, mGroupComparator);
			return groups;
		}
		
		@Override
		public User getUser(int gid, int uid) {
			User user = null;
			try {
				user = mGroups.get(gid).getUser(uid);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return user;
		}
		
		public void quit() {
			mSender.quit();
			mReceiver.quit();
		}

	}
}
