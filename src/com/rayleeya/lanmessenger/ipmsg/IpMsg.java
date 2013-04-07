package com.rayleeya.lanmessenger.ipmsg;

import com.rayleeya.lanmessenger.model.User;

public class IpMsg {

	public volatile static long mPacketNo = 0;
	
	String  version;
	long    packetNo;
	String  senderName;
	String  senderHost;
	Command command;
	String  additionalSection;
	long    timestamp;
	
	public class Command {
		int cmd;
		int option;
		
		public Command(int command) {
			cmd = command & 0xFF;
			option = command & 0xFFFFFF00;
		}

		public int getCmd() {
			return cmd;
		}

		public void setCmd(int cmd) {
			this.cmd = cmd;
		}

		public int getOption() {
			return option;
		}

		public void setOption(int option) {
			this.option = option;
		}

		@Override
		public String toString() {
			return (cmd | option) + "";
		}
		
	}
	
	IpMsg() {}
	
	IpMsg(String ver, long packNo, String name, String host, int cmd, String section) {
		version = ver;
		packetNo = packNo;
		senderName = name;
		senderHost = host;
		command = new Command(cmd);
		additionalSection = section;
		timestamp = System.currentTimeMillis();
	}
	
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public long getPacketNo() {
		return packetNo;
	}

	public void setPacketNo(long packetNo) {
		this.packetNo = packetNo;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}

	public String getSenderHost() {
		return senderHost;
	}

	public void setSenderHost(String senderHost) {
		this.senderHost = senderHost;
	}

	public Command getCommand() {
		return command;
	}

	public void setCommand(Command command) {
		this.command = command;
	}
	
	public void setCommand(int command) {
		this.command = new Command(command);
	}

	public String getAdditionalSection() {
		return additionalSection;
	}

	public void setAdditionalSection(String additionalSection) {
		this.additionalSection = additionalSection;
	}
	
	public void appendAdditionalSection(String append) {
		additionalSection += IpMsgConst.IPMSG_ADITIONAL_SPLITER + append;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	//-------- Utils --------
	private static long nextPacketNo() {
		if (mPacketNo == Long.MAX_VALUE) mPacketNo = 1;
		else mPacketNo++;
		return mPacketNo;
	}
	
	public static IpMsg createFromString(String msg) {
		if (msg == null || "".equals(msg)) 
			throw new NullPointerException("The IpMsg string must not be blank.");
		String[] args = msg.split(IpMsgConst.IPMSG_PART_SPLITER);
		if (args == null || args.length == 0)
			throw new IllegalArgumentException("The IpMsg string msg is empty.");
		
		IpMsg im = new IpMsg();
		try {
			im.version = args[0];
			im.packetNo = Long.parseLong(args[1]);
			im.senderName = args[2];
			im.senderHost = args[3];
			im.setCommand(Integer.parseInt(args[4]));
			
			if(args.length >= 6){
				im.additionalSection = args[5];
				for(int i = 6; i < args.length; i++){
					im.additionalSection += IpMsgConst.IPMSG_ADITIONAL_SPLITER + args[i];
				}
			} else{
				im.additionalSection = "";
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("IpMsg string is invalid : " + msg, e);
		}
		return im;
	}
	
	public static IpMsg createFromUserAndCmd(User user, int cmd) {
		IpMsg im = new IpMsg();
		im.setVersion(IpMsgConst.IPMSG_VERSION);
		im.setPacketNo(nextPacketNo());
		im.setCommand(cmd);
		
		String additional = "";
		if (user != null) {
			im.setSenderName(user.getUsername());
			im.setSenderHost(user.getHostname());
			additional = user.getNickname() + IpMsgConst.IPMSG_ADITIONAL_SPLITER 
					   + user.getGroup().getName();
		} else {
			im.setSenderName("");
			im.setSenderHost("");
		}
		im.setAdditionalSection(additional);
		
		return im;
	}
	
	@Override
	public String toString() {
		String spliter = IpMsgConst.IPMSG_PART_SPLITER;
		StringBuilder sb = new StringBuilder();
		sb.append(version)   .append(spliter)
		  .append(packetNo)  .append(spliter)
		  .append(senderName).append(spliter)
		  .append(senderHost).append(spliter)
		  .append(command)   .append(spliter)
		  .append(additionalSection);
		return sb.toString();
	}
}
