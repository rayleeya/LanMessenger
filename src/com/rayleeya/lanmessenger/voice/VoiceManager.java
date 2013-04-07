package com.rayleeya.lanmessenger.voice;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import com.rayleeya.lanmessenger.BuildConfig;

public class VoiceManager {

	private static final String TAG = "VoiceManager";
	private static final boolean DEBUG = BuildConfig.DEBUG;
	
	public static final int DEFAULT_SAMPLE_RATE = 1024;
	public static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	public static final int DEFAULT_AUDIO_ENCODIGNG = AudioFormat.ENCODING_PCM_8BIT;
	
	private int mSampleRate = DEFAULT_SAMPLE_RATE;
	private int mChannelConfig = DEFAULT_CHANNEL_CONFIG;
	private int mAudioFormat = DEFAULT_AUDIO_ENCODIGNG;
	private int mBufferSize;
	
	private boolean mRunning;
	private AudioRecordThread mRecordThread;
	private AudioTrackThread mTrackThread;
	
	private VoiceReader mVoiceReader;
	public interface VoiceReader {
		public void read(byte[] data);
	}
	
	private VoiceWriter mVoiceWriter;
	public interface VoiceWriter {
		public void write(byte data);
	}
	
	public VoiceManager() {
		mRecordThread = new AudioRecordThread();
		mTrackThread = new AudioTrackThread();
	}
	
	public void adjustMinBufferSize(int maxSize) {
		mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, mAudioFormat);
		if (mBufferSize > maxSize) mBufferSize = maxSize;
	}
	
	public void start() {
		if (!mRunning) {
			mRecordThread.start();
			mTrackThread.start();
			mRunning = true;
		}
	}
	
	public void stop() {
		if (mRunning) {
			mRecordThread.interrupt();
			mTrackThread.interrupt();
			mRunning = false;
		}
	}
	
	//-------- Audio Record Thread
	private class AudioRecordThread extends Thread {
		
		private static final String TAG = "AudioRecordThread";
		
		private AudioRecord mRecorder;
		
		private AudioRecordThread() {
			super(TAG);
			mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 
										mSampleRate, mChannelConfig, mAudioFormat,
										mBufferSize);
		}
		
		public void run() {
			int size = mBufferSize;
			AudioRecord recorder = mRecorder;
			VoiceReader reader = mVoiceReader;
			if (reader == null) throw new NullPointerException("VoiceHandler is null");
			
			byte[] buffer = new byte[size];
			recorder.startRecording();
			
			while (mRunning) {
				int len = recorder.read(buffer, 0, size);
				byte[] data = new byte[len];
				System.arraycopy(buffer, 0, data, 0, len);
				reader.read(data);
			}
			
			recorder.stop();
		}
	}
	
	private class AudioTrackThread extends Thread {
		private static final String TAG = "AudioTrackThread";
		
		private AudioTrack mTracker;
		
		private AudioTrackThread() {
			super(TAG);
			mTracker = new AudioTrack(AudioManager.STREAM_MUSIC, 
										mSampleRate, mChannelConfig, mAudioFormat,
										mBufferSize, AudioTrack.MODE_STREAM);
		}
		
		public void run() {
			int size = mBufferSize;
			AudioTrack tracker = mTracker;
			VoiceWriter handler = mVoiceWriter;
			if (handler == null) throw new NullPointerException("VoiceWriter is null");
			
			byte[] buffer = new byte[size];
			tracker.play();
			
			while (mRunning) {
				int len = tracker.write(buffer, 0, size);
			}
			
			tracker.stop();
		}
	}
	
	//------------------------------------------------------------------------------------------
	public void setVoiceReader(VoiceReader reader) {
		mVoiceReader = reader;
	}
	
	public VoiceReader getVoiceReader() {
		return mVoiceReader;
	}
	
	public void setVoiceWriter(VoiceWriter writer) {
		mVoiceWriter = writer;
	}
	
	public VoiceWriter getVoiceWriter() {
		return mVoiceWriter;
	}
}
