package com.rayleeya.lanmessenger.ui;

import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.rayleeya.lanmessenger.BuildConfig;
import com.rayleeya.lanmessenger.R;
import com.rayleeya.lanmessenger.model.Group;
import com.rayleeya.lanmessenger.service.ILanMessenger;
import com.rayleeya.lanmessenger.service.LanMessengerService.OnErrorListener;
import com.rayleeya.lanmessenger.service.LanMessengerService;
import com.rayleeya.lanmessenger.util.Errors;

public class LanMessengerActivity extends Activity {

	private static final String TAG = "LanMessengerActivity";
	private static final boolean DEBUG = BuildConfig.DEBUG;
	
	private ExpandableListView mList;
	private UserExpandableListAdapter mAdapter;
	private ILanMessenger mMsger;
	private ContentUpdater mContentUpdater;
	
	private H mHandler;
	private class H extends Handler {
		private static final int MSG_DATASET_CHANGED = 1;
		private static final int MSG_DATASET_INVALIDATED = 2;
		
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_DATASET_CHANGED :
					mAdapter.notifyDataSetChanged();
					break;
				case MSG_DATASET_INVALIDATED :
					mAdapter.notifyDataSetInvalidated();
					break;
			}
		}
	}
	
	private LanMessengerListener mListener;
	private class LanMessengerListener extends DataSetObserver implements OnErrorListener {
		@Override
		public void onError(int errno, String msg) {
			//TODO: handle errors
		}

		@Override
		public void onChanged() {
			if (!mContentUpdater.hasMessages(H.MSG_DATASET_CHANGED)) {
				mContentUpdater.sendEmptyMessage(H.MSG_DATASET_CHANGED);
			}
		}

		@Override
		public void onInvalidated() {
			if (!mContentUpdater.hasMessages(H.MSG_DATASET_INVALIDATED)) {
				mContentUpdater.sendEmptyMessage(H.MSG_DATASET_INVALIDATED);
			}
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
				if (code == Errors.ERR_SO_ADDR_ALREADY_IN_USE) {
					code = R.string.err_so_addr_already_in_use;
				} else if (code == Errors.ERR_SO) {
					code = R.string.err_so;
				}
				Toast.makeText(getApplicationContext(), code, Toast.LENGTH_LONG).show();
				finish();
		    } else {
				registerListeners();
				mMsger.sendEntry();
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
		setContentView(R.layout.activity_lan_messager);
		
		mAdapter = new UserExpandableListAdapter(this);
		
		mList = (ExpandableListView) findViewById(R.id.listview);
		mList.setAdapter(mAdapter);
		mList.setOnChildClickListener(mAdapter);
		mList.setOnGroupClickListener(mAdapter);
		mList.setOnGroupExpandListener(mAdapter);
		mList.setOnGroupCollapseListener(mAdapter);
		
		HandlerThread ht = new HandlerThread(ContentUpdater.TAG);
		ht.start();
		mContentUpdater = new ContentUpdater(ht.getLooper());
		
		mHandler = new H();
		
		Intent service = new Intent(this, LanMessengerService.class);
		bindService(service, servConn, BIND_AUTO_CREATE);
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
		unbindService(servConn);
		unregisterListeners(); //onServiceDisconnected may not be invoked.
		mContentUpdater.quit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_lan_messager, menu);
		return true;
	}
	
	private void registerListeners() {
		if (mMsger == null) return;
		if (mListener == null) mListener = new LanMessengerListener();
		mMsger.registerDataSetObserver(mListener);
		mMsger.regitsterOnErrorListener(mListener);
	}
	
	private void unregisterListeners() {
		if (mMsger != null && mListener != null) {
			mMsger.unregisterDataSetObserver(mListener);
			mMsger.unregitsterOnErrorListener(mListener);
			mListener = null;
		}
	}

	//-------- Prepare users and groups --------
	private class ContentUpdater extends Handler {
		private static final String TAG = "ContentUpdater";
		
		private ContentUpdater(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case H.MSG_DATASET_CHANGED :
					List<Group> groups = mMsger.getGroupsAndUsers(); 
					mAdapter.setContent(groups);
					if (!mHandler.hasMessages(H.MSG_DATASET_CHANGED)) 
						mHandler.sendEmptyMessage(H.MSG_DATASET_CHANGED);
					break;
				
				case H.MSG_DATASET_INVALIDATED :
					//TODO: make some work
					if (!mHandler.hasMessages(H.MSG_DATASET_INVALIDATED)) 
						mHandler.sendEmptyMessage(H.MSG_DATASET_INVALIDATED);
					break;
			}
		}
		
		private boolean quit() {
			Looper looper = getLooper();
	        if (looper != null) {
	            looper.quit();
	            return true;
	        }
	        return false;
		}
	}
	
}
