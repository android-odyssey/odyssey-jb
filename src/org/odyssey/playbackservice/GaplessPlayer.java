package org.odyssey.playbackservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.audiofx.AudioEffect;
import android.os.PowerManager;
import android.util.Log;

public class GaplessPlayer {
    private final static String TAG = "GaplessPlayer";

    public static enum REASON {
        IOError, SecurityError, StateError, ArgumentError;
    }

    private MediaPlayer mCurrentMediaPlayer = null;
    private boolean mCurrentPrepared = false;
    private boolean mSecondPrepared = false;
    private boolean mPlayOnPrepared = true;
    private boolean mSecondPreparing = false;
    private MediaPlayer mNextMediaPlayer = null;

    private String mPrimarySource = null;
    private String mSecondarySource = null;

    private int mPrepareTime = 0;

    private PlaybackService mPlaybackService;

    private Semaphore mSecondPreparingStart;

    public GaplessPlayer(PlaybackService service) {
        this.mTrackFinishedListeners = new ArrayList<GaplessPlayer.OnTrackFinishedListener>();
        this.mTrackStartListeners = new ArrayList<GaplessPlayer.OnTrackStartedListener>();
        mPlaybackService = service;
        mSecondPreparingStart = new Semaphore(1);
        Log.v(TAG, "MyPid: " + android.os.Process.myPid() + " MyTid: " + android.os.Process.myTid());
    }

    public void play(String uri) throws PlaybackException {
        play(uri, 0);
    }

    /**
     * Initializes the first mediaplayers with uri and prepares it so it can get
     * started
     * 
     * @param uri
     *            - Path to media file
     * @param play
     *            - should play file when prepared
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws IllegalStateException
     * @throws IOException
     */
    public void play(String uri, int jumpTime) throws PlaybackException {
        Log.v(TAG, "play(): " + jumpTime);
        // save play decision

        // Another player currently exists try reusing
        if (mCurrentMediaPlayer != null) {
            mCurrentMediaPlayer.reset();
            mCurrentMediaPlayer.release();
            mCurrentMediaPlayer = null;
        }
        mCurrentMediaPlayer = new MediaPlayer();
        mCurrentPrepared = false;
        mCurrentMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mCurrentMediaPlayer.setDataSource(uri);
        } catch (IllegalArgumentException e) {
            throw new PlaybackException(REASON.ArgumentError);
        } catch (SecurityException e) {
            throw new PlaybackException(REASON.SecurityError);
        } catch (IllegalStateException e) {
            throw new PlaybackException(REASON.StateError);
        } catch (IOException e) {
            throw new PlaybackException(REASON.IOError);
        }
        /*
         * Signal audio effect desire to android
         */
        Intent audioEffectIntent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
        audioEffectIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mCurrentMediaPlayer.getAudioSessionId());
        audioEffectIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, mPlaybackService.getPackageName());
        mPlaybackService.sendBroadcast(audioEffectIntent);
        mCurrentMediaPlayer.setAuxEffectSendLevel(1.0f);
        mPrimarySource = uri;
        mCurrentMediaPlayer.setOnCompletionListener(new TrackCompletionListener());
        mCurrentMediaPlayer.setOnPreparedListener(mPrimaryPreparedListener);
        mPrepareTime = jumpTime;
        mCurrentMediaPlayer.prepareAsync();
    }

    /**
     * Pauses the currently running mediaplayer If already paused it continues
     * the playback
     */
    public void togglePause() {
        // Check if Mediaplayer is running
        if (mCurrentMediaPlayer != null && mCurrentMediaPlayer.isPlaying()) {
            mCurrentMediaPlayer.pause();
        } else if (mCurrentMediaPlayer != null && !mCurrentMediaPlayer.isPlaying() && mCurrentPrepared) {
            mCurrentMediaPlayer.start();
            mCurrentMediaPlayer.setWakeMode(mPlaybackService.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        }

    }

    /**
     * Just pauses currently running player
     */
    public void pause() {
        if (mCurrentMediaPlayer != null && mCurrentMediaPlayer.isPlaying()) {
            mCurrentMediaPlayer.pause();
        }
    }

    /**
     * Resumes playback
     */
    public void resume() {
        // FIXME Catch illegal state exception
        if (mCurrentMediaPlayer != null) {
            mCurrentMediaPlayer.start();
        }
    }

    /**
     * Stops mediaplayback
     */
    public void stop() {
        if (mCurrentMediaPlayer != null) {
            if (mNextMediaPlayer != null && mSecondPrepared) {
                mCurrentMediaPlayer.setNextMediaPlayer(null);
                mNextMediaPlayer.reset();
                mNextMediaPlayer.release();
                mNextMediaPlayer = null;
                mSecondPrepared = false;
            }
            Log.v(TAG, "Player stopped");
            /*
             * Signal android desire to close audio effect session
             */
            Intent audioEffectIntent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
            audioEffectIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mCurrentMediaPlayer.getAudioSessionId());
            audioEffectIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, mPlaybackService.getPackageName());
            mPlaybackService.sendBroadcast(audioEffectIntent);
            if (mCurrentPrepared) {
                mCurrentMediaPlayer.reset();
                mCurrentMediaPlayer.release();
            }
            mCurrentMediaPlayer = null;
            mCurrentPrepared = false;
        }
    }

    public void seekTo(int position) {
        try {
            if (mCurrentMediaPlayer != null && mCurrentPrepared && position < mCurrentMediaPlayer.getDuration()) {
                Log.v(TAG, "Seeking to: " + position);
                mCurrentMediaPlayer.seekTo(position);
            } else {
                Log.v(TAG, "Not seeking to: " + position);
            }
        } catch (IllegalStateException exception) {
            Log.v(TAG, "Illegal state during seekTo");
        }
    }

    public int getPosition() {
        try {
            if (mCurrentMediaPlayer != null && mCurrentMediaPlayer.isPlaying()) {
                Log.v(TAG, "Returning position");
                return mCurrentMediaPlayer.getCurrentPosition();
            }
        } catch (IllegalStateException exception) {
            Log.v(TAG, "Illegal state during CurrentPositon");
            return 0;
        }
        return 0;
    }

    /**
     * Sets next mediaplayer to uri and start preparing it. if next mediaplayer
     * was already initialized it gets resetted
     * 
     * @param uri
     */
    public void setNextTrack(String uri) throws PlaybackException {
        mSecondPrepared = false;
        if (mCurrentMediaPlayer == null) {
            // This call makes absolutely no sense at this point so abort
            throw new PlaybackException(REASON.StateError);
        }
        // Next mediaplayer already set, reset
        if (mNextMediaPlayer != null) {
            mCurrentMediaPlayer.setNextMediaPlayer(null);
            mNextMediaPlayer.reset();
            mNextMediaPlayer.release();
            mNextMediaPlayer = null;
            Log.v(TAG, "Clear next Player");
        }
        if (uri != null) {
            mNextMediaPlayer = new MediaPlayer();
            mNextMediaPlayer.setOnPreparedListener(mSecondaryPreparedListener);
            Log.v(TAG, "Set next track to: " + uri);
            mNextMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mNextMediaPlayer.setDataSource(uri);
            } catch (IllegalArgumentException e) {
                throw new PlaybackException(REASON.ArgumentError);
            } catch (SecurityException e) {
                throw new PlaybackException(REASON.SecurityError);
            } catch (IllegalStateException e) {
                throw new PlaybackException(REASON.StateError);
            } catch (IOException e) {
                throw new PlaybackException(REASON.IOError);
            }
            mSecondarySource = uri;
            // Check if primary is prepared before preparing the second one
            try {
                mSecondPreparingStart.acquire();
            } catch (InterruptedException e) {
                // FIXME new reason
                throw new PlaybackException(REASON.StateError);
            }
            if (mCurrentPrepared && !mSecondPreparing) {
                Log.v(TAG, "Start preparing second");
                mSecondPreparing = true;
                mNextMediaPlayer.prepareAsync();
            }
            mSecondPreparingStart.release();
        }
    }

    private OnPreparedListener mPrimaryPreparedListener = new MediaPlayer.OnPreparedListener() {

        @Override
        public void onPrepared(MediaPlayer mp) {
            Log.v(TAG, "Primary MP prepared: " + mp);
            // If mp equals currentMediaPlayback it should start playing
            mCurrentPrepared = true;

            // only start playing if its desired

            // Check if an immedieate jump is requested
            if (mPrepareTime > 0) {
                Log.v(TAG, "Jumping to requested time before playing");
                mp.seekTo(mPrepareTime);
                mPrepareTime = 0;
            }
            mp.setWakeMode(mPlaybackService.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            mp.start();
            // Notify connected listeners
            for (OnTrackStartedListener listener : mTrackStartListeners) {
                listener.onTrackStarted(mPrimarySource);
            }

            try {
                mSecondPreparingStart.acquire();
            } catch (InterruptedException e) {
                // FIXME new reason

            }
            if (mSecondPrepared == false && mNextMediaPlayer != null && !mSecondPreparing) {
                mSecondPreparing = true;
                // Delayed initialization second mediaplayer
                Log.v(TAG, "start preparing second MP delayed");
                mNextMediaPlayer.prepareAsync();
            }
            mSecondPreparingStart.release();
            Log.v(TAG, "Primary fully prepareD");
        }
    };

    private OnPreparedListener mSecondaryPreparedListener = new MediaPlayer.OnPreparedListener() {

        @Override
        public void onPrepared(MediaPlayer mp) {
            mSecondPreparing = false;
            Log.v(TAG, "Second MP prepared: " + mp);
            // If it is nextMediaPlayer it should be set for currentMP
            mp.setWakeMode(mPlaybackService.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mSecondPrepared = true;
            /*
             * Attach equalizer effect
             */
            // Equalizer eq = new Equalizer(0,
            // mCurrentMediaPlayer.getAudioSessionId());
            // mCurrentMediaPlayer.attachAuxEffect(eq.getId());
            mCurrentMediaPlayer.setNextMediaPlayer(mp);
            Log.v(TAG, "Set Next MP");
        }
    };

    // Notification for Services using GaplessPlayer
    public interface OnTrackFinishedListener {
        void onTrackFinished();
    }

    public interface OnTrackStartedListener {
        void onTrackStarted(String URI);
    }

    // Track finish notification
    private ArrayList<OnTrackFinishedListener> mTrackFinishedListeners;

    public void setOnTrackFinishedListener(OnTrackFinishedListener listener) {
        mTrackFinishedListeners.add(listener);
    }

    public void removeOnTrackFinishedListener(OnTrackFinishedListener listener) {
        mTrackFinishedListeners.remove(listener);
    }

    // Track start notification
    private ArrayList<OnTrackStartedListener> mTrackStartListeners;

    public void setOnTrackStartListener(OnTrackStartedListener listener) {
        mTrackStartListeners.add(listener);
    }

    public void removeOnTrackStartListener(OnTrackStartedListener listener) {
        mTrackStartListeners.remove(listener);
    }

    public boolean isRunning() {
        if (mCurrentMediaPlayer != null) {
            return mCurrentMediaPlayer.isPlaying();
        }
        return false;
    }

    public boolean isPaused() {
        return mCurrentMediaPlayer != null && !mCurrentMediaPlayer.isPlaying() && mCurrentPrepared;
    }

    boolean isPrepared() {
        if (mCurrentMediaPlayer != null && mCurrentPrepared) {
            return true;
        }
        return false;
    }

    public void setVolume(float leftChannel, float rightChannel) {
        if (mCurrentMediaPlayer != null) {
            mCurrentMediaPlayer.setVolume(leftChannel, rightChannel);
        }
    }

    private class TrackCompletionListener implements MediaPlayer.OnCompletionListener {

        @Override
        public void onCompletion(MediaPlayer mp) {
            Log.v(TAG, "Track playback completed");
            // notify connected services
            for (OnTrackFinishedListener listener : mTrackFinishedListeners) {
                listener.onTrackFinished();
            }

            // Cleanup old MP
            int audioSessionID = mCurrentMediaPlayer.getAudioSessionId();

            // mCurrentMediaPlayer = null;
            // Set current MP to next MP
            if (mNextMediaPlayer != null && mSecondPrepared) {
                Log.v(TAG, "set next as current MP");
                mCurrentMediaPlayer = mNextMediaPlayer;
                mCurrentMediaPlayer.setOnCompletionListener(new TrackCompletionListener());
                mPrimarySource = mSecondarySource;
                mSecondarySource = "";
                mNextMediaPlayer = null;

                // Notify connected listeners
                for (OnTrackStartedListener listener : mTrackStartListeners) {
                    listener.onTrackStarted(mPrimarySource);
                }
            } else {
                /*
                 * Signal android desire to close audio effect session
                 */
                Intent audioEffectIntent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
                audioEffectIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionID);
                audioEffectIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, mPlaybackService.getPackageName());
                mCurrentMediaPlayer = null;
            }

            Log.v(TAG, "Releasing old MP");
            mp.release();
        }
    }

    private class MediaPlayerErrorListner implements MediaPlayer.OnErrorListener {

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            if (mp.equals(mCurrentMediaPlayer)) {
                // Signal PlaybackService to continue with next song
                mPlaybackService.setNextTrack();
            } else {
                // Probably second media player so ignore for now
            }
            return false;
        }

    }

    public class PlaybackException extends Exception {

        REASON mReason;

        public PlaybackException(REASON reason) {
            mReason = reason;
        }

        public REASON getReason() {
            return mReason;
        }
    }

}
