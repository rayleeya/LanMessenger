package com.rayleeya.lanmessenger.util;

public interface Events {

	int NO_ERROR = 0;
	int ERR_SO = 1;
	int ERR_SO_ADDR_ALREADY_IN_USE = ERR_SO | 1<<8;
	int ERR_ATTACH_USER_FAIL = 2;
	
	
	
	int NO_MSG = 100;
	int MSG_RECV_VOICE_RES = 101;
}
