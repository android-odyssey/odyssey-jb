package org.odyssey;

import android.os.Parcel;
import android.os.Parcelable;

/*
 * This class is the parcelable which got send from the PlaybackService to notify
 * receivers like the main-GUI or possible later home screen widgets
 * 
 * PlaybackService --> NowPlayingInformation --> OdysseyApplication --> MainActivity
 * 											 |-> Homescreen Widget (later) 
 */

public final class NowPlayingInformation implements Parcelable {

    // Parcel data
    private int mPlaying;
    private String mPlayingURL;
    private int mPlayingIndex;
    private int mRepeat;
    private int mRandom;
    private int mPlaylistLength;
    private int mPlaylistPosition;

    public static Parcelable.Creator<NowPlayingInformation> CREATOR = new Parcelable.Creator<NowPlayingInformation>() {

        @Override
        public NowPlayingInformation createFromParcel(Parcel source) {
            int playing = source.readInt();
            String playingURL = source.readString();
            int playingIndex = source.readInt();
            int repeat = source.readInt();
            int random = source.readInt();
            int playlistlength = source.readInt();
            int playlistposition = source.readInt();

            return new NowPlayingInformation(playing, playingURL, playingIndex, repeat, random, playlistlength, playlistposition);
        }

        @Override
        public NowPlayingInformation[] newArray(int size) {
            return new NowPlayingInformation[size];
        }
    };

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    public NowPlayingInformation(int playing, String playingURL, int playingIndex, int repeat, int random, int playlistlength, int playlistposition) {
        mPlaying = playing;
        mPlayingURL = playingURL;
        mPlayingIndex = playingIndex;
        mRepeat = repeat;
        mRandom = random;
        mPlaylistLength = playlistlength;
        mPlaylistPosition = playlistposition;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mPlaying);
        dest.writeString(mPlayingURL);
        dest.writeInt(mPlayingIndex);
        dest.writeInt(mRepeat);
        dest.writeInt(mRandom);
        dest.writeInt(mPlaylistLength);
        dest.writeInt(mPlaylistPosition);
    }

    public int getPlaying() {
        return mPlaying;
    }

    public String getPlayingURL() {
        return mPlayingURL;
    }

    public String toString() {
        return "Playing: " + mPlaying + " URL: " + mPlayingURL + " index: " + mPlayingIndex + "repeat: " + mRepeat + "random: " + mRandom;
    }

    public int getPlayingIndex() {
        return mPlayingIndex;
    }

    public int getRepeat() {
        return mRepeat;
    }

    public int getRandom() {
        return mRandom;
    }

    public int getPlaylistLength() {
        return mPlaylistLength;
    }

    public int getPlaylistPosition() {
        return mPlaylistPosition;
    }

}
