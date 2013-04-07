package com.rayleeya.lanmessenger.service;

import java.net.SocketAddress;
import java.util.List;

import com.rayleeya.lanmessenger.model.Group;
import com.rayleeya.lanmessenger.model.User;
import com.rayleeya.lanmessenger.service.LanMessengerManager.OnErrorListener;

import android.database.DataSetObserver;

public interface ILanMessenger {

	//-------------------------------------------------------
	void sendEntry();
	
	void sendExit();
	
	void sendAnsEntry(SocketAddress socketAddress);
	
	//-------------------------------------------------------
	void regitsterOnErrorListener(OnErrorListener listener);
	
	void unregitsterOnErrorListener(OnErrorListener listener);
	
	void registerDataSetObserver(DataSetObserver observer);
	
	void unregisterDataSetObserver(DataSetObserver observer);
	
	//-------------------------------------------------------
	List<Group> getGroupsAndUsers();
	
	User getUser(int gid, int uid);
	
	boolean hasError();
	
	int getError();
	
	void setError(int error);

	void sendVoiceRequest(User user);

	void sendVoiceResponse(SocketAddress sockAddress, boolean accept);
}
