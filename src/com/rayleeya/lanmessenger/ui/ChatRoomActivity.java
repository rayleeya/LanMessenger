package com.rayleeya.lanmessenger.ui;

import java.net.SocketAddress;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;
import android.view.View;

import com.rayleeya.lanmessenger.BuildConfig;
import com.rayleeya.lanmessenger.R;
import com.rayleeya.lanmessenger.model.User;
import com.rayleeya.lanmessenger.service.ILanMessenger;
import com.rayleeya.lanmessenger.service.LanMessengerService;
import com.rayleeya.lanmessenger.service.LanMessengerService.OnEventListener;
import com.rayleeya.lanmessenger.util.Events;
import com.rayleeya.lanmessenger.util.Utils;

public class ChatRoomActivity extends BaseActivity implements View.OnClickListener {

	private static final String TAG = "ChatRoomActivity";
	private static final boolean DEBUG = BuildConfig.DEBUG;
	
	private Button bSpeak;
	private ILanMessenger mMsger;
	private User mUser;
	
	private ChatRoomListener mListener;
	private boolean hasListener;
	private class ChatRoomListener extends DataSetObserver implements OnEventListener {
		@Override
		public void onError(int errno, String msg) {
			//TODO: handle errors
		}
		
		@Override
		public void onMessage(int msgno, int arg1, int arg2, Object obj) {
			switch (msgno) {
				case Events.MSG_RECV_VOICE_RES :
					if (arg1 == 1) { //accept
						if (!(obj instanceof SocketAddress)) 
							throw new IllegalArgumentException("Arg obj should be a SocketAddress");
						mMsger.startVoiceMsg((SocketAddress)obj);
					} else {
						
					}
					break;
					
			}
		}

		@Override
		public void onChanged() {
		}

		@Override
		public void onInvalidated() {
		}
	}
	
	private ServiceConnection servConn = new ServiceConnection() {
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mMsger = (ILanMessenger) service;
			if (mMsger == null) {
				String error = "There is an error, DatagramSocket created failed, check the log for details.";
				if (DEBUG) Log.v(TAG, error);
				Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
				finish(); //TODO: handle errors more gracefully
			} else if (mMsger.hasError()) {
				int code = mMsger.getError();
				if (code == Events.ERR_SO_ADDR_ALREADY_IN_USE) {
					code = R.string.err_so_addr_already_in_use;
				} else if (code == Events.ERR_SO) {
					code = R.string.err_so;
				}
				Toast.makeText(getApplicationContext(), code, Toast.LENGTH_LONG).show();
				finish();
		    } else {
		    	if (attachUser()) {
		    		registerListeners();
		    		mMsger.sendVoiceRequest(mUser);
		    	} else {
		    		Toast.makeText(getApplicationContext(), R.string.err_attach_user_fail, Toast.LENGTH_LONG).show();
					finish();
		    	}
			}
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			unregisterListeners();
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat_room);
		
		bSpeak = (Button) findViewById(R.id.btn_speak);
		bSpeak.setText("Button");
		bSpeak.setOnClickListener(this);
		
		mListener = new ChatRoomListener();
		
		Intent service = new Intent(this, LanMessengerService.class);
		bindService(service, servConn, BIND_AUTO_CREATE);
	}

	protected boolean attachUser() {
		Intent i = getIntent();
		int gid = i.getIntExtra(Utils.EXTRA_GROUP_ID, -1);
		int uid = i.getIntExtra(Utils.EXTRA_USER_ID, -1);
		mUser = mMsger.getUser(gid, uid);
		return mUser != null;
	}

	@Override
	protected void onStart() {
		super.onStart();
		registerListeners();
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
		unregisterListeners(); //onServiceDisconnected may not be invoked.
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(servConn);
	}

	//-------- implement methods --------
	@Override
	public void onClick(View v) {
		if (bSpeak == v) {
			onSpeakClick();
		}
	}

	private void onSpeakClick() {
		
	}
	
	//----------------------------------
	private void registerListeners() {
		if (!hasListener && mMsger != null) {
			mMsger.registerDataSetObserver(mListener);
			mMsger.regitsterOnEventListener(mListener);
			hasListener = true;
		}
	}
	
	private void unregisterListeners() {
	}

}
