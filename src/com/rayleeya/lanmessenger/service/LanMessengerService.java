package com.rayleeya.lanmessenger.service;

import java.net.BindException;
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
import android.os.IBinder;
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

	private class MyMsgHandler implements LanMessengerManager.MsgHandler {
		@Override
		public boolean addUser(User user, int groupType, String groupName) {
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
					
					return true;
				}
				return false;
			}
		}

		@Override
		public boolean removeUser(User user) {
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
					return true;
				}
				return false;
			}
		}
		
		@Override
		public User getSelfUser() {
			return mSelf;
		}
		
		@Override
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
		
		@Override
		public void receiveVoidRequest(final SocketAddress socketAddress) {
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
		
		mMsgerManager.setMsgHandler(new MyMsgHandler());
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
	
}
