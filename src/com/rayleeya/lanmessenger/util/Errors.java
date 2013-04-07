package com.rayleeya.lanmessenger.util;

public interface Errors {

	int NO_ERROR = 0;
	int ERR_SO = 1;
	int ERR_SO_ADDR_ALREADY_IN_USE = ERR_SO | 1<<8;
	
	int ERR_ATTACH_USER_FAIL = 2;
}
