package com.rayleeya.lanmessenger.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import android.os.Build;
import android.util.Log;

import com.rayleeya.lanmessenger.BuildConfig;
import com.rayleeya.lanmessenger.ipmsg.IpMsgConst;

public class UdpManager {

	private static final String TAG = "UdpManager";
	private static final boolean DEBUG = BuildConfig.DEBUG;
	
	public static final int UPD_DEFAULT_BUFFER_SIZE = 8192; //8k is good size for audio data
	public static final int UDP_RECV_BUFFER_SIZE = UPD_DEFAULT_BUFFER_SIZE; 
	public static final int UDP_SEND_BUFFER_SIZE = UPD_DEFAULT_BUFFER_SIZE;
	
	private static UdpManager mInstance;
	private DatagramSocket mSocket;
	private DatagramPacket mRecvPacket;
	private DatagramPacket mSendPacket;
	private String mLocalAddress;
	private String mUsername;
	private String mHostname;
	
	private UdpManager() throws SocketException {
		openSocket();
	}
	
	public static UdpManager getInstance() throws SocketException {
		if (mInstance == null) {
			synchronized (UdpManager.class) {
				if (mInstance == null) {
					mInstance = new UdpManager();
				}
			}
		}
		return mInstance;
	}
	
	public DatagramSocket openSocket() throws SocketException {
		if (mSocket == null) {
			mSocket = new DatagramSocket(IpMsgConst.getPort());
		}
		return mSocket;
	}
	
	public void closeSocket() {
		if (mSocket != null) {
			mSocket.close();
			mSocket = null;
		}
	}
	
	public String getLocalAddress() {
		if (mLocalAddress == null) {
			try {
				Enumeration<NetworkInterface> enums = NetworkInterface.getNetworkInterfaces();
				while (enums.hasMoreElements()) {
					NetworkInterface ni = enums.nextElement();
					Enumeration<InetAddress> addrs = ni.getInetAddresses();
					while (addrs.hasMoreElements()) {
						InetAddress ia = addrs.nextElement();
						if (!ia.isLoopbackAddress()) {
							mLocalAddress = ia.getHostAddress();
							return mLocalAddress;
						}
					}
				}
			} catch (SocketException e) {
				mLocalAddress = "";
				e.printStackTrace();
			}
		}
		return mLocalAddress;
	}
	
	public String getUsername() {
		if (mUsername == null) {
			mUsername = Build.BRAND + "_" + Build.MODEL;
		}
		return mUsername;
	}
	
	public String getHostname() {
		if (mHostname == null) {
			try {
				Enumeration<NetworkInterface> enums = NetworkInterface.getNetworkInterfaces();
				while (enums.hasMoreElements()) {
					NetworkInterface ni = enums.nextElement();
					Enumeration<InetAddress> addrs = ni.getInetAddresses();
					while (addrs.hasMoreElements()) {
						InetAddress ia = addrs.nextElement();
						if (!ia.isLoopbackAddress()) {
							mHostname = ia.getHostName();
							return mHostname;
						}
					}
				}
			} catch (SocketException e) {
				mHostname = "";
				e.printStackTrace();
			}
		}
		return mHostname;
	}
	
	public DatagramPacket createRecvDatagramPacket() {
		if (mRecvPacket == null) {
			byte[] buffer = new byte[UDP_RECV_BUFFER_SIZE];
			mRecvPacket = new DatagramPacket(buffer, UDP_RECV_BUFFER_SIZE);
		}
		return mRecvPacket;
	}
	
	public DatagramPacket createSendDatagramPacket() {
		if (mSendPacket == null) {
			byte[] buffer = new byte[UDP_RECV_BUFFER_SIZE];
			mSendPacket = new DatagramPacket(buffer, UDP_RECV_BUFFER_SIZE);
		}
		return mSendPacket;
	}

	public boolean sendUdpData(String msg, String charset, SocketAddress sockAddr) {
		InetSocketAddress inetAddr = (InetSocketAddress) sockAddr;
		return sendUdpData(msg, charset, inetAddr.getAddress(), inetAddr.getPort());
	}
	
	public boolean sendUdpData(String msg, String charset, InetAddress addr, int port) {
		if (DEBUG) Log.d(TAG, "Send UDP data [" + msg + " | " + charset + " | " + addr.getHostAddress() 
				+ " | " + addr.getHostName() + " | " + port + "]");
		try {
			DatagramSocket so = openSocket();
			DatagramPacket pack = createSendDatagramPacket();
			
			byte[] data = msg.getBytes(charset);
			pack.setData(data, 0, data.length);
			pack.setAddress(addr);
			pack.setPort(port);
			
			so.send(pack);
			return true;
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public SocketAddress getBroadcastAddress() {
		try {
			InetAddress addr = InetAddress.getByName(IpMsgConst.IPMSG_BROADCAST_ADDRESS);
			return new InetSocketAddress(addr, IpMsgConst.getPort());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
}
