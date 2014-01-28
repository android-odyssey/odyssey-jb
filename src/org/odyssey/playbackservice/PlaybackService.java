package org.odyssey.playbackservice;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.odyssey.MainActivity;
import org.odyssey.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

public class PlaybackService extends Service implements
		MediaPlayer.OnPreparedListener {

	public static final String TAG = "PlaybackService";
	public static final int NOTIFICATION_ID = 42;

	public static final String ACTION_TESTPLAY = "org.odyssey.testplay";
	public static final String ACTION_PLAY = "org.odyssey.play";
	public static final String ACTION_PAUSE = "org.odyssey.pause";
	public static final String ACTION_NEXT = "org.odyssey.next";
	public static final String ACTION_PREVIOUS = "org.odyssey.previous";
	public static final String ACTION_SEEKTO = "org.odyssey.seekto";
	public static final String ACTION_STOP = "org.odyssey.stop";
	public static final String ACTION_QUIT = "org.odyssey.quit";

	private Looper mLooper;
	private HandlerThread mHandlerThread;
	private PlaybackServiceHandler mHandler;

	// Mediaplayback stuff
	private GaplessPlayer mPlayer;
	private ArrayList<String> mCurrentList;

	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG, "Bind:" + intent.getType());
		return new PlaybackServiceStub(this);
	}

	@Override
	public boolean onUnbind(final Intent intent) {
		Log.v(TAG, "Unbind");
		return true;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.v(TAG, "Odyssey PlaybackService onCreate");
		// Start Handlerthread
		mHandlerThread = new HandlerThread(TAG + "Thread",
				Process.THREAD_PRIORITY_BACKGROUND);
		mHandlerThread.start();
		mLooper = mHandlerThread.getLooper();
		mHandler = new PlaybackServiceHandler(mLooper, this);

		// Create MediaPlayer
		mPlayer = new GaplessPlayer(this);
		Log.v(TAG, "Service created");

		// Set listeners
		mPlayer.setOnTrackStartListener(new PlaybackStartListener(this));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG, "onStartCommand");
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "Odyssey PlaybackService done", Toast.LENGTH_SHORT)
				.show();
	}

	public void playURI(String uri) {
		try {
			mPlayer.play(uri);
		} catch (IOException e) {
			Log.e(TAG, "URI: " + uri + "can't be played");
		}
		// Create notification
	}

	@Override
	public void onPrepared(MediaPlayer arg0) {
		// TODO Auto-generated method stub

	}

	private PlaybackServiceHandler getHandler() {
		return mHandler;
	}

	public void startTestPlayback() {

	}

	public void startPlayback() {

	}

	public List<String> getCurrentList() {
		return mCurrentList;
	}

	private final static class PlaybackServiceStub extends
			IOdysseyPlaybackService.Stub {
		private final WeakReference<PlaybackService> mService;

		public PlaybackServiceStub(PlaybackService service) {
			mService = new WeakReference<PlaybackService>(service);
		}

		@Override
		public void play(String uri) throws RemoteException {
			// Create play control object
			ControlObject obj = new ControlObject(
					ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAY, uri);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void pause() throws RemoteException {
			// Create pause control object
			ControlObject obj = new ControlObject(
					ControlObject.PLAYBACK_ACTION.ODYSSEY_PAUSE);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void stop() throws RemoteException {
			// Create stop control object
			ControlObject obj = new ControlObject(
					ControlObject.PLAYBACK_ACTION.ODYSSEY_STOP);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void setNextTrack(String uri) throws RemoteException {
			// Create nexttrack control object
			ControlObject obj = new ControlObject(
					ControlObject.PLAYBACK_ACTION.ODYSSEY_SETNEXTRACK, uri);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void enqueueTracks(List<String> tracks) throws RemoteException {
			// Create enqueuetracks control object
			ControlObject obj = new ControlObject(
					ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUETRACKS,
					(ArrayList<String>) tracks);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void enqueueTrack(String track) throws RemoteException {
			// Create enqueuetrack control object
			ControlObject obj = new ControlObject(
					ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUETRACK, track);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void dequeueTrack(String track) throws RemoteException {
			// Create dequeuetrack control object
			ControlObject obj = new ControlObject(
					ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUETRACK, track);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void dequeueTracks(List<String> tracks) throws RemoteException {
			// Create dequeuetracks control object
			ControlObject obj = new ControlObject(
					ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUETRACKS,
					(ArrayList<String>) tracks);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public List<String> getCurrentList() throws RemoteException {
			return mService.get().getCurrentList();
		}

		@Override
		public void setRandom(boolean random) throws RemoteException {
			// Create random control object
			ControlObject obj = new ControlObject(
					ControlObject.PLAYBACK_ACTION.ODYSSEY_RANDOM, random);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void setRepeat(boolean repeat) throws RemoteException {
			// Create repeat control object
			ControlObject obj = new ControlObject(
					ControlObject.PLAYBACK_ACTION.ODYSSEY_REPEAT, repeat);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public String getArtist() throws RemoteException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getAlbum() throws RemoteException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getTrackname() throws RemoteException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getTrackNo() throws RemoteException {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getBitrate() throws RemoteException {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getSamplerate() throws RemoteException {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void seekTo(int position) throws RemoteException {
			ControlObject obj = new ControlObject(
					ControlObject.PLAYBACK_ACTION.ODYSSEY_SEEKTO, position);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void jumpTo(int position) throws RemoteException {
			ControlObject obj = new ControlObject(
					ControlObject.PLAYBACK_ACTION.ODYSSEY_JUMPTO, position);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}
	}

	private class PlaybackStartListener implements
			GaplessPlayer.OnTrackStartedListener {
		private PlaybackService mPlaybackService;

		public PlaybackStartListener(PlaybackService service) {
			mPlaybackService = service;
		}

		@Override
		public void onTrackStarted(String URI) {
			Log.v(TAG, "track starded: " + URI);
			NotificationCompat.Builder builder = new NotificationCompat.Builder(
					mPlaybackService).setSmallIcon(R.drawable.ic_stat_odys)
					.setContentTitle("Odyssey").setContentText(URI);

			Intent resultIntent = new Intent(mPlaybackService,
					MainActivity.class);

			TaskStackBuilder stackBuilder = TaskStackBuilder
					.create(mPlaybackService);

			stackBuilder.addParentStack(MainActivity.class);
			stackBuilder.addNextIntent(resultIntent);

			PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
					0, PendingIntent.FLAG_UPDATE_CURRENT);

			builder.setContentIntent(resultPendingIntent);
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			// Make notification persistent
			builder.setOngoing(true);
			Notification notification = builder.build();
			notificationManager.notify(NOTIFICATION_ID, notification);
			startForeground(NOTIFICATION_ID, notification);
		}

	}

}
