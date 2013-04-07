package com.rayleeya.lanmessenger.model;

public class User {

	String nickname;
	Group  group;
	String username;
	String hostname;
	String ip;
	int    port;
	String mac;
	boolean online;
	private boolean selected;
	
	public User() {
	}
	
	public User(String nickname, String username, String hostname, String ip, String mac) {
		this.nickname   = nickname;
		this.username   = username;
		this.hostname   = hostname;
		this.ip     	= ip;
		this.mac    	= mac;
		this.online 	= true;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		this.online = online;
	}

	public void removeFromGroup() {
		if (group != null) {
			group.removeUser(this);
		}
	}
	
	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
	@Override
	protected User clone() throws CloneNotSupportedException {
		User user = new User();
		user.nickname = nickname;
		user.username = username;
		user.hostname = hostname;
		user.ip 	  = ip;
		user.port 	  = port;
		user.mac 	  = mac;
		user.online   = online;
		user.selected = selected;
		return user;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("User [")
		  .append("group=").append(group != null ? group.name : "(Unknown)")
		  .append(" nickname=").append(nickname)
		  .append(" username=").append(username)
		  .append(" hostname=").append(hostname)
		  .append(" ip=")      .append(ip)
		  .append(" port=")    .append(port)
		  .append(" mac=")     .append(mac)
		  .append(" online=")  .append(online)
		  .append(" selected=").append(selected)
		  .append("]");
		return sb.toString();
	}
	
}
