package org.odyssey.playbackservice;

import org.odyssey.IOdysseyNowPlayingCallback;
import org.odyssey.playbackservice.TrackItem;

interface IOdysseyPlaybackService {
	
	// Controls the player with predefined actions
	void play(in TrackItem track);
	void pause();
	void resume();
	void stop();
	void next();
	void previous();
	void togglePause();
	
	/**
	 * position = position in current track ( in seconds)
	 */
	void seekTo(int position);
	// Returns time of current playing title
	int getTrackPosition();
	int getTrackDuration();
	
	// If currently playing return this song otherwise null
	TrackItem getCurrentSong();
	
	TrackItem getPlaylistSong(int index);
	int getPlaylistSize();
	
	/** 
	 * position = playlist position of jump target
	 */
	void jumpTo(int position);
	
	void setRandom(int random);
	void setRepeat(int repeat);
	
	int getRandom();
	int getRepeat();
	
	// track is the full uri with "file://" !
	void setNextTrack(String track);
	
	void enqueueTrackAsNext(in TrackItem track);
	
	void enqueueTrack(in TrackItem track);
	void enqueueTracks(in List<TrackItem> tracks);
	void dequeueTrack(in TrackItem track);
	void dequeueTracks(in List<TrackItem> tracks);
	void dequeueTrackIndex(int index);
	void clearPlaylist();
	
	void getCurrentList(out List<TrackItem> tracks);
	
	// Information getters
	String getArtist();
	String getAlbum();
	String getTrackname();
	int getTrackNo();
	int getBitrate();
	int getSamplerate();
	
	
	void registerNowPlayingReceiver(IOdysseyNowPlayingCallback receiver);
	void unregisterNowPlayingReceiver(IOdysseyNowPlayingCallback receiver);
}
