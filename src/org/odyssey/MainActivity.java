package org.odyssey;

import java.util.ArrayList;

import org.odyssey.fragments.AboutFragment;
import org.odyssey.fragments.AlbumsSectionFragment;
import org.odyssey.fragments.AlbumsSectionFragment.OnAlbumSelectedListener;
import org.odyssey.fragments.AlbumsTracksFragment;
import org.odyssey.fragments.AllPlaylistsFragment;
import org.odyssey.fragments.AllPlaylistsFragment.OnPlaylistSelectedListener;
import org.odyssey.fragments.ArtistsAlbumsTabsFragment;
import org.odyssey.fragments.ArtistsAlbumsTabsFragment.OnAboutSelectedListener;
import org.odyssey.fragments.ArtistsAlbumsTabsFragment.OnPlayAllSelectedListener;
import org.odyssey.fragments.ArtistsAlbumsTabsFragment.OnSettingsSelectedListener;
import org.odyssey.fragments.ArtistsSectionFragment.OnArtistSelectedListener;
import org.odyssey.fragments.NowPlayingFragment;
import org.odyssey.fragments.PlaylistFragment;

import org.odyssey.fragments.PlaylistNameDialogFragment.OnPlaylistNameListener;
import org.odyssey.fragments.SavedPlaylistFragment;
import org.odyssey.fragments.SettingsFragment;
import org.odyssey.playbackservice.PlaybackService;
import org.odyssey.playbackservice.PlaybackService.RANDOMSTATE;
import org.odyssey.playbackservice.PlaybackService.REPEATSTATE;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.playbackservice.TrackItem;
import org.odyssey.views.QuickControl;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends FragmentActivity implements OnAlbumSelectedListener, OnArtistSelectedListener, OnAboutSelectedListener, OnSettingsSelectedListener, OnPlayAllSelectedListener, OnPlaylistSelectedListener, OnPlaylistNameListener {

    private static final String TAG = "OdysseyMainActivity";

    private PlaybackServiceConnection mServiceConnection;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mNaviBarList;
    private String[] mNaviBarTitles;

    private QuickControl mQuickControl;

    private String mRequestedFragment = "";

    private NowPlayingReceiver mNowPlayingReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // get Titles
        mNaviBarTitles = getResources().getStringArray(R.array.navibar_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNaviBarList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer
        // opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        mNaviBarList.setAdapter(new ArrayAdapter<String>(this, R.layout.navibar_list_item, mNaviBarTitles));
        mNaviBarList.setOnItemClickListener(new NaviBarItemClickListener());

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();

        actionBar.setHomeButtonEnabled(true);
        // disable up home function
        actionBar.setDisplayHomeAsUpEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
        mDrawerLayout, /* DrawerLayout object */
        R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
        R.string.drawer_open, /* "open drawer" description for accessibility */
        R.string.drawer_close /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu();

            }

            public void onDrawerOpened(View view) {
                invalidateOptionsMenu();

            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            ArtistsAlbumsTabsFragment mArtistsAlbumsTabsFragment = new ArtistsAlbumsTabsFragment();

            // Add the fragment to the 'fragmentContainer' FrameLayout
            getSupportFragmentManager().beginTransaction().add(R.id.fragmentFrame, mArtistsAlbumsTabsFragment).commit();
        }

        // Register callbacks in mainapplication which currently manages
        // callback from playback service process
        mQuickControl = (QuickControl) findViewById(R.id.quickControl);

    }

    @Override
    public void onAlbumSelected(String albumKey, String albumTitle, String albumCoverImagePath, String albumArtist) {

        mDrawerToggle.setDrawerIndicatorEnabled(false);
        // Create fragment and give it an argument for the selected article
        AlbumsTracksFragment newFragment = new AlbumsTracksFragment();
        Bundle args = new Bundle();
        args.putString(AlbumsTracksFragment.ARG_ALBUMKEY, albumKey);
        args.putString(AlbumsTracksFragment.ARG_ALBUMTITLE, albumTitle);
        args.putString(AlbumsTracksFragment.ARG_ALBUMART, albumCoverImagePath);
        args.putString(AlbumsTracksFragment.ARG_ALBUMARTIST, albumArtist);
        newFragment.setArguments(args);

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);

        // Replace whatever is in the fragment_container view with this
        // fragment,
        // and add the transaction to the back stack so the user can navigate
        // back
        transaction.replace(R.id.fragmentFrame, newFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        mRequestedFragment = "";
        mServiceConnection = new PlaybackServiceConnection(getApplicationContext());
        mServiceConnection.setNotifier(new ConnectionListener());
        mServiceConnection.openConnection();

        Log.v(TAG, "Resume mainactivity");
        Intent resumeIntent = getIntent();
        // setIntent(new Intent());

        if (resumeIntent != null && resumeIntent.getExtras() != null && resumeIntent.getExtras().getString("Fragment") != null && resumeIntent.getExtras().getString("Fragment").equals("currentsong")) {
            mRequestedFragment = "currentsong";
            Log.v(TAG, "Current song fragment requested: " + resumeIntent.getExtras().getString("Fragment"));
        }

        if (mNowPlayingReceiver != null) {
            unregisterReceiver(mNowPlayingReceiver);
            mNowPlayingReceiver = null;
        }
        mNowPlayingReceiver = new NowPlayingReceiver();
        registerReceiver(mNowPlayingReceiver, new IntentFilter(PlaybackService.MESSAGE_NEWTRACKINFORMATION));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mNowPlayingReceiver != null) {
            unregisterReceiver(mNowPlayingReceiver);
            mNowPlayingReceiver = null;
        }

        // TODO evaluate
        // Cleanup fragments?
        mServiceConnection.closeConnection();
        mServiceConnection = null;
    }

    @Override
    public void onArtistSelected(String artist, long artistID) {

        mDrawerToggle.setDrawerIndicatorEnabled(false);
        // Create fragment and give it an argument for the selected article
        AlbumsSectionFragment newFragment = new AlbumsSectionFragment();
        Bundle args = new Bundle();
        args.putString(AlbumsSectionFragment.ARG_ARTISTNAME, artist);
        args.putLong(AlbumsSectionFragment.ARG_ARTISTID, artistID);

        newFragment.setArguments(args);

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        // Replace whatever is in the fragment_container view with this
        // fragment,
        // and add the transaction to the back stack so the user can navigate
        // back
        transaction.replace(R.id.fragmentFrame, newFragment);
        transaction.addToBackStack("ArtistFragment");

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void onBackPressed() {

        invalidateOptionsMenu();
        android.support.v4.app.FragmentManager manager = getSupportFragmentManager();

        super.onBackPressed();

        // enable navigation bar when backstack empty
        if (manager.getBackStackEntryCount() == 0) {
            mDrawerToggle.setDrawerIndicatorEnabled(true);
        }

    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        android.support.v4.app.FragmentManager manager = getSupportFragmentManager();

        switch (item.getItemId()) {
        // Respond to the action bar's Up/Home button
        case android.R.id.home:
            if (manager.getBackStackEntryCount() > 0) {
                onBackPressed();
            } else {
                mDrawerToggle.setDrawerIndicatorEnabled(true);
                // The action bar home/up action should open or close the
                // drawer.
                // ActionBarDrawerToggle will take care of this.
                if (mDrawerToggle.onOptionsItemSelected(item)) {
                    return true;
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* The click listner for ListView in the navigation drawer */
    private class NaviBarItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.v(TAG, "ITEM CLICKED");
            invalidateOptionsMenu();
            // TODO check clear backstack

            // getSupportFragmentManager().popBackStack(null,
            // FragmentManager.POP_BACK_STACK_INCLUSIVE);

            // CLear backstack
            FragmentManager fm = getSupportFragmentManager();
            for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
                fm.popBackStack();
            }
            Log.v(TAG, "Popped Stack");
            android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            Log.v(TAG, "Transaction begun");
            transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);

            Log.v(TAG, "SWITCHING FRAGMENT");
            switch (position) {

            case 0:
                mDrawerToggle.setDrawerIndicatorEnabled(true);

                // Replace whatever is in the fragment_container view with this
                // fragment,
                transaction.replace(R.id.fragmentFrame, new ArtistsAlbumsTabsFragment());
                // transaction.addToBackStack("");

                // Commit the transaction
                transaction.commit();

                break;
            case 1:
                mDrawerToggle.setDrawerIndicatorEnabled(true);

                // Replace whatever is in the fragment_container view with this
                // fragment,
                transaction.replace(R.id.fragmentFrame, new AllPlaylistsFragment());
                // transaction.addToBackStack("");

                // Commit the transaction
                transaction.commit();

                invalidateOptionsMenu();

                break;

            case 2:
                mDrawerToggle.setDrawerIndicatorEnabled(true);

                // Replace whatever is in the fragment_container view with this
                // fragment,
                transaction.replace(R.id.fragmentFrame, new PlaylistFragment());
                // transaction.addToBackStack("");

                // Commit the transaction
                transaction.commit();

                invalidateOptionsMenu();

                break;

            case 3:
                mDrawerToggle.setDrawerIndicatorEnabled(true);

                // Replace whatever is in the fragment_container view with this
                // fragment,
                transaction.replace(R.id.fragmentFrame, new NowPlayingFragment());
                // transaction.addToBackStack("");

                // Commit the transaction
                transaction.commit();

                invalidateOptionsMenu();

                break;

            default:
                break;

            }

            // update selected item and title, then close the drawer
            mNaviBarList.setItemChecked(position, true);
            mDrawerLayout.closeDrawer(mNaviBarList);
        }
    }

    @Override
    public void onAboutSelected() {

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);

        mDrawerToggle.setDrawerIndicatorEnabled(false);

        AboutFragment mAboutFragment = new AboutFragment();
        // Replace whatever is in the fragment_container view with this
        // fragment,
        transaction.replace(R.id.fragmentFrame, mAboutFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();

        invalidateOptionsMenu();

    }

    @Override
    public void onSettingsSelected() {

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);

        mDrawerToggle.setDrawerIndicatorEnabled(false);

        SettingsFragment mSettingsFragment = new SettingsFragment();
        // Replace whatever is in the fragment_container view with this
        // fragment,
        transaction.replace(R.id.fragmentFrame, mSettingsFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();

        invalidateOptionsMenu();

    }

    @Override
    public void OnPlayAllSelected() {

        // play all tracks shuffled on device
        try {
            mServiceConnection.getPBS().playAllTracksShuffled();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void onPlaylistSelected(String playlistName, long playlistID) {

        mDrawerToggle.setDrawerIndicatorEnabled(false);
        // Create fragment and give it an argument for the selected playlist
        SavedPlaylistFragment newFragment = new SavedPlaylistFragment();
        Bundle args = new Bundle();
        args.putString(SavedPlaylistFragment.ARG_PLAYLISTNAME, playlistName);
        args.putLong(SavedPlaylistFragment.ARG_PLAYLISTID, playlistID);

        newFragment.setArguments(args);

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        // Replace whatever is in the fragment_container view with this
        // fragment,
        // and add the transaction to the back stack so the user can navigate
        // back
        transaction.replace(R.id.fragmentFrame, newFragment);
        transaction.addToBackStack("SavedPlaylistFragment");

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void onPlaylistNameSpecified(String name) {
        // Sending playlistname to playlistfragment

        // identify playlistfragment
        // FIXME make it save
        PlaylistFragment playlistFragment = (PlaylistFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentFrame);

        playlistFragment.savePlaylist(name);
    }

    public QuickControl getQuickControl() {
        return mQuickControl;
    }

    private class ConnectionListener implements PlaybackServiceConnection.ConnectionNotifier {

        @Override
        public void onConnect() {
            Log.v(TAG, "Service connected :)");
            // Update gui elements
            try {
                if (mRequestedFragment.equals("currentsong") && mServiceConnection.getPBS().getCurrentIndex() >= 0 && mServiceConnection.getPBS().getPlaying() == 1) {
                    Log.v(TAG, "Opening nowplaying fragment");
                    FragmentManager fm = getSupportFragmentManager();
                    for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
                        fm.popBackStack();
                    }
                    android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    // Launch current song fragment
                    NowPlayingFragment mNowPlayingFragment = new NowPlayingFragment();
                    // Replace whatever is in the fragment_container view with
                    // this
                    // fragment,
                    transaction.replace(R.id.fragmentFrame, mNowPlayingFragment);
                    // transaction.addToBackStack(null);

                    // Commit the transaction
                    transaction.commit();

                    invalidateOptionsMenu();
                }
                // Set nowplaying bottom info
                final boolean isRandom = mServiceConnection.getPBS().getRandom() == 1 ? true : false;
                final boolean songPlaying = mServiceConnection.getPBS().getPlaying() == 1 ? true : false;
                final boolean isRepeat = mServiceConnection.getPBS().getRepeat() == 1 ? true : false;
                final TrackItem trackItem = mServiceConnection.getPBS().getCurrentSong();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // update imagebuttons
                        if (songPlaying) {
                            mQuickControl.setPlayPauseButtonDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
                        } else {
                            mQuickControl.setPlayPauseButtonDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
                        }
                        if (isRepeat) {
                            mQuickControl.setRepeatButtonDrawable(getResources().getDrawable(R.drawable.ic_action_repeat_dark_active));
                        } else {
                            mQuickControl.setRepeatButtonDrawable(getResources().getDrawable(R.drawable.ic_action_repeat_dark));
                        }
                        if (isRandom) {
                            mQuickControl.setRandomButtonDrawable(getResources().getDrawable(R.drawable.ic_action_shuffle_dark_active));
                        } else {
                            mQuickControl.setRandomButtonDrawable(getResources().getDrawable(R.drawable.ic_action_shuffle_dark));
                        }
                        if (trackItem != null) {
                            mQuickControl.setTitleText(trackItem.getTrackTitle());
                            mQuickControl.setArtistText(trackItem.getTrackArtist());
                        }
                    }
                });
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnect() {
            // TODO Auto-generated method stub

        }

    }

    private class NowPlayingReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PlaybackService.MESSAGE_NEWTRACKINFORMATION)) {
                Log.v(TAG, "Received new information");
                // Extract nowplaying info
                ArrayList<NowPlayingInformation> infoArray = intent.getExtras().getParcelableArrayList(PlaybackService.INTENT_NOWPLAYINGNAME);
                if (infoArray.size() != 0) {
                    NowPlayingInformation info = infoArray.get(0);
                    final boolean songPlaying = (info.getPlaying() == 1) ? true : false;
                    final boolean isRepeat = (info.getRepeat() == REPEATSTATE.REPEAT_ALL.ordinal()) ? true : false;
                    final boolean isRandom = (info.getRandom() == RANDOMSTATE.RANDOM_ON.ordinal()) ? true : false;
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (songPlaying) {
                                mQuickControl.setPlayPauseButtonDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
                            } else {
                                mQuickControl.setPlayPauseButtonDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
                            }
                            if (isRepeat) {
                                mQuickControl.setRepeatButtonDrawable(getResources().getDrawable(R.drawable.ic_action_repeat_dark_active));
                            } else {
                                mQuickControl.setRepeatButtonDrawable(getResources().getDrawable(R.drawable.ic_action_repeat_dark));
                            }
                            if (isRandom) {
                                mQuickControl.setRandomButtonDrawable(getResources().getDrawable(R.drawable.ic_action_shuffle_dark_active));
                            } else {
                                mQuickControl.setRandomButtonDrawable(getResources().getDrawable(R.drawable.ic_action_shuffle_dark));
                            }
                        }
                    });
                }

                // Extract current track info
                final ArrayList<TrackItem> trackArray = intent.getExtras().getParcelableArrayList(PlaybackService.INTENT_TRACKITEMNAME);
                if (trackArray.size() != 0) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            TrackItem trackItem = trackArray.get(0);
                            mQuickControl.setTitleText(trackItem.getTrackTitle());
                            mQuickControl.setArtistText(trackItem.getTrackArtist());
                        }
                    });
                }
            }
        }
    }
}
