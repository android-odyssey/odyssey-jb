package org.odyssey;

import org.odyssey.playbackservice.TrackItem;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.provider.MediaStore;

public class MusicLibraryHelper {
    private static final String TAG = "MusicLibraryHelper";
    public static final String[] projectionAlbums = { MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ALBUM_KEY, MediaStore.Audio.Albums.NUMBER_OF_SONGS, MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART,
            MediaStore.Audio.Albums.ARTIST };
    public static final String[] projectionArtists = { MediaStore.Audio.Artists.ARTIST, MediaStore.Audio.Artists.ARTIST_KEY, MediaStore.Audio.Artists.NUMBER_OF_TRACKS, MediaStore.Audio.Artists._ID, MediaStore.Audio.Artists.NUMBER_OF_ALBUMS };
    public static final String[] projectionTracks = { MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.ALBUM_KEY, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA };
    public static final String[] projectionPlaylistTracks = { MediaStore.Audio.Playlists.Members.TITLE, MediaStore.Audio.Playlists.Members.DISPLAY_NAME, MediaStore.Audio.Playlists.Members.TRACK, MediaStore.Audio.Playlists.Members.ALBUM_KEY,
            MediaStore.Audio.Playlists.Members.DURATION, MediaStore.Audio.Playlists.Members.ALBUM, MediaStore.Audio.Playlists.Members.ARTIST, MediaStore.Audio.Playlists.Members.DATA };

    public static final String[] projectionPlaylists = { MediaStore.Audio.Playlists.NAME, MediaStore.Audio.Playlists._ID };

    /**
     * Resolves the url into an comfortably trackitem which contains artist and
     * title
     * 
     * @param url
     * @param resolver
     * @return
     */
    // FIXME ALBUM MISSING
    public static TrackItem getTrackItemFromURL(String url, ContentResolver resolver) {
        String selection = MediaStore.Audio.Media.DATA + "= ?";
        String[] selectionArgs = { url };
        Cursor trackCursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projectionTracks, selection, selectionArgs, MediaStore.Audio.Media.TITLE);

        String title = "";
        String artist = "";
        String album = "";
        int trackno = 0;
        long duration = 0;
        String albumKey = "";

        if (trackCursor != null && trackCursor.getCount() > 0) {
            trackCursor.moveToFirst();
            title = trackCursor.getString(trackCursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            artist = trackCursor.getString(trackCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            album = trackCursor.getString(trackCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            trackno = trackCursor.getInt(trackCursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
            duration = trackCursor.getLong(trackCursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            albumKey = trackCursor.getString(trackCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY));
        }

        trackCursor.close();

        return new TrackItem(title, artist, album, url, trackno, duration, albumKey);
    }

    public static class CoverBitmapGenerator {
        private CoverBitmapListener mListener;
        private Context mContext;
        private TrackItem mTrack;
        private Thread mGeneratorThread;

        public CoverBitmapGenerator(Context context, CoverBitmapListener listener) {
            mContext = context;
            mListener = listener;
        }

        public void getImage(TrackItem track) {
            if (track != null) {
                mTrack = track;
                // Create generator thread
                mGeneratorThread = new Thread(new DownloadRunner());
                mGeneratorThread.start();
            }
        }

        private class DownloadRunner implements Runnable {

            @Override
            public void run() {
                String where = android.provider.MediaStore.Audio.Albums.ALBUM_KEY + "=?";

                String whereVal[] = { mTrack.getTrackAlbumKey() };

                Cursor cursor = mContext.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.Albums.ALBUM_ART }, where, whereVal, "");

                String coverPath = null;
                if (cursor.moveToFirst()) {
                    coverPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                }
                if (coverPath != null) {
                    BitmapDrawable cover = (BitmapDrawable) BitmapDrawable.createFromPath(coverPath);
                    mListener.receiveBitmap(cover);
                }
            }

        }

    }

    public interface CoverBitmapListener {
        public void receiveBitmap(BitmapDrawable bm);
    }
}
