package com.rayleeya.lanmessenger.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import com.rayleeya.lanmessenger.ipmsg.IpMsg;
import com.rayleeya.lanmessenger.ipmsg.IpMsgConst;
import com.rayleeya.lanmessenger.model.Group;
import com.rayleeya.lanmessenger.model.User;
import com.rayleeya.lanmessenger.net.PacketHelper;
import com.rayleeya.lanmessenger.net.UdpManager;
import com.rayleeya.lanmessenger.util.Errors;
import com.rayleeya.lanmessenger.BuildConfig;

import android.os.Process;
import android.util.Log;

public class MsgReceiverThread extends Thread {

	private static final String TAG = "MsgReceiverThread";
	private static final boolean DEBUG = BuildConfig.DEBUG;
	
	
	private int mPriority;
	private boolean mRunning;
	private LanMessengerManager mMsgerManager;
	private UdpManager mUdpManager;
	
	public MsgReceiverThread() {
		this(null);
	}
	
	public MsgReceiverThread(String name) {
		super(name == null ? TAG : name);
		mPriority = Process.THREAD_PRIORITY_DEFAULT;
		init();
	}
	
	public MsgReceiverThread(String name, int priority) {
		super(name == null ? TAG : name);
		mPriority = priority;
		init();
	}
	
	private void init() {
		mRunning = true;
		Process.setThreadPriority(mPriority);
	}
	
	public void setLanMessengerManager(LanMessengerManager mgr) {
		mMsgerManager = mgr;
	}
	
	public void setUdpManager(UdpManager mgr) {
		mUdpManager = mgr;
	}
	
	@Override
	public void run() {
		if (mUdpManager == null) 
			throw new NullPointerException("You must set the UdpManager.");
		
		UdpManager um = mUdpManager;
		DatagramSocket so = null;
		try {
			so = um.openSocket();
		} catch (SocketException e1) {
			e1.printStackTrace();
			mMsgerManager.handleError(Errors.ERR_SO);
			return;
		}
		
		LanMessengerManager msgerManager = mMsgerManager;
		DatagramPacket pack = um.createRecvDatagramPacket();
		while (mRunning) {
			try {
				so.receive(pack);
			} catch (IOException e) {
				e.printStackTrace();
				mRunning = false;
				um.closeSocket();
				if (DEBUG) Log.e(TAG, "Receive UDP datagram packet failed. Exit!");
				break;
			}
			
			if (!mRunning) return;
			
			if (pack.getLength() == 0) {
				if (DEBUG) Log.e(TAG, "Receive UDP datagram packet, bug no data.");
				continue;
			}
			
			String msg = null;
			try {
				msg = PacketHelper.parsePacket(pack);
			} catch (UnsupportedEncodingException e) {
				if (DEBUG) Log.e(TAG, "Receive UDP datagram packet failed, unsupported encoding : " 
						+ PacketHelper.mCharsetName);
			} finally {
				if (msg == null) continue;
			}
			
			IpMsg im = IpMsg.createFromString(msg);
			if (DEBUG) Log.d(TAG, "Receive UDP data [" + im.toString() + "]");
			
			switch (im.getCommand().getCmd()) {
				case IpMsgConst.IPMSG_BR_ENTRY : 
					addUser(pack, im);
					if (!isSelf(pack)) {
						msgerManager.sendAnsEntry(pack.getSocketAddress());
					}
					break;
				
				case IpMsgConst.IPMSG_BR_EXIT :
					if (!isSelf(pack)) {
						removeUser(pack, im);
					} else {
						mRunning = false;
					}
					break;
				
				case IpMsgConst.IPMSG_ANSENTRY :
					addUser(pack, im);
					break;
					
				case IpMsgConst.IPMSG_SENDMSG :
					
					break;
					
				case IpMsgConst.IPMSG_VOICE_REQ :
					msgerManager.receiveVoiceRequest(pack.getSocketAddress());
					break;
				
				case IpMsgConst.IPMSG_VOICE_RES :
					int ret = 0;
					String additional = im.getAdditionalSection();
					if (additional != null) {
						String[] sections = additional.split(IpMsgConst.IPMSG_ADITIONAL_SPLITER);
						if (sections != null && sections.length == 1) {
							try {
								ret = Integer.valueOf(sections[0]);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
					msgerManager.receiveVoiceResponse(pack.getSocketAddress(), ret);
					break;
			}
			
		}
	}
	
	private boolean isSelf(DatagramPacket pack) {
		return pack.getAddress().getHostAddress().equals(mUdpManager.getLocalAddress());
	}
	
	public boolean isRunning() {
		return mRunning;
	}
	
	public void quit() {
		mRunning = false;
		interrupt();
		mUdpManager.closeSocket();
	}
	
	private class UserInfo {
		User user;
		int groupType;
		String groupName;
	}
	
	private void addUser(DatagramPacket pack, IpMsg msg) {
		UserInfo ui = createUserFromPacketAndMsg(pack, msg);
		mMsgerManager.receiveAddUser(ui.user, ui.groupType, ui.groupName);
	}
	
	private void removeUser(DatagramPacket pack, IpMsg msg) {
		UserInfo ui = createUserFromPacketAndMsg(pack, msg);
		mMsgerManager.receiveRemoveUser(ui.user);
	}
	
	private UserInfo createUserFromPacketAndMsg(DatagramPacket pack, IpMsg msg) {
		String groupName = null;
		int groupType = Group.GROUP_UNSPECIFIED;
		User user = new User();
		user.setUsername(msg.getSenderName());
		user.setHostname(msg.getSenderHost());
		user.setIp(pack.getAddress().getHostAddress());
		user.setPort(pack.getPort());
		
		String aditional = msg.getAdditionalSection();
		boolean setNickname = false;
		boolean setGroup = false;
		if (aditional != null) {
			String[] parts = aditional.split(IpMsgConst.IPMSG_ADITIONAL_SPLITER); 
			if (parts != null) {
				if (parts.length >= 1) {
					user.setNickname(parts[0]);
					setNickname = true;
				}
				if (parts.length >= 2) {
					groupName = parts[1]; 
					groupType = Group.GROUP_SPECIFIED;
					setGroup = true;
				}
			}
		}

		if (!setNickname) {
			user.setNickname(msg.getSenderName());
		}
		
		if (!setGroup) {
			if (mUdpManager.getLocalAddress().equals(user.getIp())) {
				groupType = Group.GROUP_MYSELF;
			} else {
				groupType = Group.GROUP_UNSPECIFIED;
			}
		}
		
		UserInfo ui = new UserInfo();
		ui.user = user;
		ui.groupType = groupType;
		ui.groupName = groupName;
		return ui;
	}
}
