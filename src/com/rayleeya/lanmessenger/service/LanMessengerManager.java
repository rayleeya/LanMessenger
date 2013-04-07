package com.rayleeya.lanmessenger.service;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import com.rayleeya.lanmessenger.model.Group;
import com.rayleeya.lanmessenger.model.User;
import com.rayleeya.lanmessenger.util.Errors;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;

public class LanMessengerManager extends Binder implements ILanMessenger {
	
	private static final String TAG = "LanMessengerManager";
	
	private MsgSenderThread mSender;
	private MsgReceiverThread mReceiver;
	private H mH;
	
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
					mMsgHandler.receiveVoidRequest((SocketAddress)msg.obj);
					break;
					
				case MSG_RECEIVE_VOICE_RES :
					//TODO: work is here.
//					mMsgHandler.receiveVoidResponse((SocketAddress)msg.obj, msg.arg1);
					break;
			}
		}
	}
	
	public LanMessengerManager(MsgSenderThread sender, MsgReceiverThread receiver) {
		mSender = sender;
		mReceiver = receiver;
		mH = new H();
	}
	
	//-------- Error Observerable/Observers --------
	public interface OnErrorListener {
		public void onError(int errno, String msg);
	}
	
	private final ArrayList<OnErrorListener> mErrorListeners = new ArrayList<OnErrorListener>();
	private int errorcode = Errors.NO_ERROR;
	
	public void regitsterOnErrorListener(OnErrorListener listener) {
		mErrorListeners.add(listener);
	}
	
	public void unregitsterOnErrorListener(OnErrorListener listener) {
		mErrorListeners.remove(listener);
	}
	
	public void notifyError(int errno) {
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
    
    public void notifyDataSetInvalidated() {
        mDataSetObservable.notifyInvalidated();
    }
    
    public void notifyDataSetChanged() {
        mDataSetObservable.notifyChanged();
    }
    
    public void handleDataSetInvalidated() {
    	mH.removeMessages(H.MSG_HANDLE_DATASET_INVALIDATED);
		mH.sendEmptyMessageDelayed(H.MSG_HANDLE_DATASET_INVALIDATED, H.DATASET_REFRESH_DELAY);
    }
    
    public void handleDataSetChanged() {
    	mH.removeMessages(H.MSG_HANDLE_DATASET_CHANGED);
		mH.sendEmptyMessageDelayed(H.MSG_HANDLE_DATASET_CHANGED, H.DATASET_REFRESH_DELAY);
    }
	
    //-------- MsgHandlerCallback --------
    public interface MsgHandler {
    	public boolean addUser(User user, int groupType, String groupName);
    	public boolean removeUser(User user);
    	public List<Group> getGroupsAndUsers();
    	public User getUser(int gid, int uid);
		public User getSelfUser();
		public void receiveVoidRequest(SocketAddress socketAddress);
    }
    
    private MsgHandler mMsgHandler;
    
    public void setMsgHandler(MsgHandler handler) {
    	mMsgHandler = handler;
    }

    public MsgHandler getMsgHandler() {
    	return mMsgHandler;
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
		boolean ret = mMsgHandler.addUser(user, groupType, groupName);
		handleDataSetChanged();
		return ret;
	}
	
	public boolean receiveRemoveUser(User user) {
		boolean ret = mMsgHandler.removeUser(user);
		handleDataSetChanged();
		return ret;
	}
	
	public boolean receiveVoiceRequest(SocketAddress socketAddress) {
		Message msg = mH.obtainMessage(H.MSG_RECEIVE_VOICE_REQ, socketAddress);
		mH.sendMessage(msg);
		return true;
	}
	
	public boolean receiveVoiceResponse(SocketAddress socketAddress, int accept) {
		Message msg = mH.obtainMessage(H.MSG_RECEIVE_VOICE_RES, accept, 0, socketAddress);
		mH.sendMessage(msg);
		return true;
	}
	
	public User getSelfUser() {
		return mMsgHandler.getSelfUser();
	}

	public List<Group> getGroupsAndUsers() {
		return mMsgHandler.getGroupsAndUsers();
	}
	
	@Override
	public User getUser(int gid, int uid) {
		return mMsgHandler.getUser(gid, uid);
	}
	
	public void quit() {
		mSender.quit();
		mReceiver.quit();
	}
}
