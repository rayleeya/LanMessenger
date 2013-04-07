package com.rayleeya.lanmessenger.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Group {

	public static final int GROUP_MYSELF = 1;
	public static final int GROUP_SPECIFIED = 2;
	public static final int GROUP_UNSPECIFIED = 3;
	
	int type;
	String name;
	ArrayList<User> users;
	private boolean selected;
	
	public Group(int type, String name) {
		this.type = type;
		this.name = name == null ? "Unknown" : name;
		users = new ArrayList<User>();
	}
	
	public boolean addUser(User user) {
		user.setGroup(this);
		return users.add(user);
	}
	
	public boolean removeUser(User user) {
		user.setGroup(null);
		return users.remove(user);
	}
	
	public int indexOf(User user) {
		return users.indexOf(user);
	}
	
	public String getName() {
		return name;
	}
	
	public List<User> getUsers() {
		return users;
	}

	public User getUser(int index) {
		return users.get(index);
	}
	
	public int getType() {
		return type;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Group [")
		  .append("name=").append(name)
		  .append(" type=").append(type)
		  .append(" selected=").append(selected)
		  .append("], Users : \n")
		  .append(Arrays.toString(users.toArray()));
		return super.toString();
	}

	@Override
	public Group clone() throws CloneNotSupportedException {
		Group group = new Group(type, name);
		group.selected = selected;
		for (User u : users) {
			group.addUser(u.clone());
		}
		return group;
	}
	
}
