package com.rayleeya.lanmessenger.service;

import java.net.SocketAddress;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.rayleeya.lanmessenger.ipmsg.IpMsg;
import com.rayleeya.lanmessenger.ipmsg.IpMsgConst;
import com.rayleeya.lanmessenger.model.User;
import com.rayleeya.lanmessenger.net.PacketHelper;
import com.rayleeya.lanmessenger.net.UdpManager;

public class MsgSenderThread extends HandlerThread {

	private static final String TAG = "MsgSenderThread";
	
	private H mH;
	private LanMessengerManager mMsgerManager;
	private UdpManager mUdpManager;
	
	private class H extends Handler {
		
		public H(Looper looper) {
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg) {
			
			SocketAddress sockAddr = null;
			User self = mMsgerManager.getSelfUser();
			IpMsg im = null;
			int what = msg.what;
			
			switch (what) {
				case IpMsgConst.IPMSG_BR_ENTRY : 
					sockAddr = mUdpManager.getBroadcastAddress();
					im = IpMsg.createFromUserAndCmd(self, IpMsgConst.IPMSG_BR_ENTRY);
					mUdpManager.sendUdpData(im.toString(), PacketHelper.mCharsetName, sockAddr);
					break;
				
				case IpMsgConst.IPMSG_BR_EXIT : 
					sockAddr = mUdpManager.getBroadcastAddress();
					im = IpMsg.createFromUserAndCmd(self, IpMsgConst.IPMSG_BR_EXIT);
					mUdpManager.sendUdpData(im.toString(), PacketHelper.mCharsetName, sockAddr);
					break;
					
				case IpMsgConst.IPMSG_ANSENTRY : 
					sockAddr = (SocketAddress) msg.obj;
					im = IpMsg.createFromUserAndCmd(self, IpMsgConst.IPMSG_ANSENTRY);
					mUdpManager.sendUdpData(im.toString(), PacketHelper.mCharsetName, sockAddr);
					break;
					
				case IpMsgConst.IPMSG_SENDMSG : 
					
					break;	
					
				case IpMsgConst.IPMSG_VOICE_REQ :
					sockAddr = (SocketAddress) msg.obj;
					im = IpMsg.createFromUserAndCmd(self, IpMsgConst.IPMSG_VOICE_REQ);
					mUdpManager.sendUdpData(im.toString(), PacketHelper.mCharsetName, sockAddr);
					break;
					
				case IpMsgConst.IPMSG_VOICE_RES :
					sockAddr = (SocketAddress) msg.obj;
					int accept = msg.arg1;
					im = IpMsg.createFromUserAndCmd(self, IpMsgConst.IPMSG_VOICE_RES);
					im.appendAdditionalSection(accept + "");
					mUdpManager.sendUdpData(im.toString(), PacketHelper.mCharsetName, sockAddr);
					break;
			}
		}
	}
	
	public MsgSenderThread() {
		this(null);
	}
	
	public MsgSenderThread(String name) {
		super(name == null ? TAG : name);
		init();
	}
	
	public MsgSenderThread(String name, int priority) {
		super(name == null ? TAG : name, priority);
		init();
	}

	private void init() {
		super.start();
	}

	public void setLanMessengerManager(LanMessengerManager mgr) {
		mMsgerManager = mgr;
	}
	
	public void setUdpManager(UdpManager mgr) {
		mUdpManager = mgr;
	}
	
	public void start() {
		if (mUdpManager == null) throw new NullPointerException("You must set the UdpManager.");
		mH = new H(getLooper());
	}
	
	
	//-------- sendXxx methods --------
	void send(int cmd) {
		mH.sendEmptyMessage(cmd);
	}
	
	void send(int cmd, Object msg) {
		Message m = mH.obtainMessage(cmd, msg);
		mH.sendMessage(m);
	}
	
	void send(int cmd, int arg1, int arg2, Object msg) {
		Message m = mH.obtainMessage(cmd, arg1, arg2, msg);
		mH.sendMessage(m);
	}
	
	public void sendEntry() {
		send(IpMsgConst.IPMSG_BR_ENTRY);
	}
	
	public void sendExit() {
		send(IpMsgConst.IPMSG_BR_EXIT);
	}
	
	public void sendAnsEntry(SocketAddress socketAddress) {
		send(IpMsgConst.IPMSG_ANSENTRY, socketAddress);
	}

	public void sendVoiceRequest(SocketAddress socketAddress) {
		send(IpMsgConst.IPMSG_VOICE_REQ, socketAddress);
	}

	public void sendVoiceResponse(SocketAddress socketAddress, boolean accept) {
		send(IpMsgConst.IPMSG_VOICE_RES, (accept ? 1 : 0), 0, socketAddress);
	}
}
