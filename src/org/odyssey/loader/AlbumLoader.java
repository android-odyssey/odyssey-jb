package org.odyssey.loader;

import java.util.ArrayList;
import java.util.List;

import org.odyssey.MusicLibraryHelper;
import org.odyssey.databasemodel.AlbumModel;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v4.content.AsyncTaskLoader;

public class AlbumLoader extends AsyncTaskLoader<List<AlbumModel>> {

    private final static String TAG = "OdysseyAlbumLoader";
    private long mArtistID;
    private Context mContext;

    public AlbumLoader(Context context, long artist) {
        super(context);
        mContext = context;
        mArtistID = artist;

        // Starts the loading of backgroundData
        forceLoad();
    }

    /*
     * Creates an list of all albums on this device and caches them inside the
     * memory(non-Javadoc)
     * 
     * @see android.support.v4.content.AsyncTaskLoader#loadInBackground()
     */
    @Override
    public List<AlbumModel> loadInBackground() {
        // Create cursor for content retrieval
        Cursor albumCursor;
        if (mArtistID == -1) {
            albumCursor = mContext.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionAlbums, "", null, MediaStore.Audio.Albums.ALBUM + " COLLATE NOCASE");
        } else {
            albumCursor = mContext.getContentResolver().query(MediaStore.Audio.Artists.Albums.getContentUri("external", mArtistID), MusicLibraryHelper.projectionAlbums, "", null, MediaStore.Audio.Albums.ALBUM + " COLLATE NOCASE");
        }
        ArrayList<AlbumModel> albums = new ArrayList<AlbumModel>();
        for (int i = 0; i < albumCursor.getCount(); i++) {
            albumCursor.moveToPosition(i);
            String albumKey = albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY));
            String albumTitle = albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
            String imagePath = albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
            String artistTitle = albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
            AlbumModel album = new AlbumModel(albumTitle, imagePath, artistTitle, albumKey);
            albums.add(album);

        }
        albumCursor.close();
        return albums;
    }

}