package com.rayleeya.lanmessenger.ui;

import com.rayleeya.lanmessenger.BuildConfig;

import android.app.Activity;
import android.os.Bundle;

public class BaseActivity extends Activity {

	private static final String TAG = "BaseActivity";
	private static final boolean DEBUG = BuildConfig.DEBUG;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onStart() {
		
		super.onStart();
	}

	@Override
	protected void onResume() {
		
		super.onResume();
	}

	@Override
	protected void onPause() {
		
		super.onPause();
	}

	@Override
	protected void onStop() {
		
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		
		super.onDestroy();
	}

}
