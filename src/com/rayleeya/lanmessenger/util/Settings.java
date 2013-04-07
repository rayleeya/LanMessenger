package com.rayleeya.lanmessenger.util;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {

	public static final String SETTING_FILE_NAME = "settings";
	public static final String PREF_NICKNAME = "nickname";
	public static final String PREF_GROUPNAME = "groupname";
	
	public static String getString(Context cxt, String name, String defVal) {
		SharedPreferences sp = getSharedPreferences(cxt);
		return sp.getString(name, defVal);
	}
	
	public static boolean setString(Context cxt, String name, String value) {
		SharedPreferences sp = getSharedPreferences(cxt);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString(name, value);
		return editor.commit();
	}
	
	public static SharedPreferences getSharedPreferences(Context cxt) {
		return cxt.getSharedPreferences(Settings.SETTING_FILE_NAME, Context.MODE_PRIVATE);
	}
}
