package com.rayleeya.lanmessenger.net;

import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;

public class PacketHelper {

	public static final String DEFAULT_CHARSET_NAME = "GBK";
	public static String mCharsetName = DEFAULT_CHARSET_NAME;
	
	public static String parsePacket(DatagramPacket pack) 
			throws UnsupportedEncodingException {
		return parsePacket(pack.getData(), pack.getOffset(), pack.getLength());
	}
	
	public static String parsePacket(byte[] data, int offset, int length) 
									throws UnsupportedEncodingException {
		return parsePacket(data, offset, length, mCharsetName);
	}
	
	public static String parsePacket(byte[] data, int offset, int length, String charsetName) 
									throws UnsupportedEncodingException {
		return new String(data, offset, length, charsetName);
	}
}
