package org.odyssey.fragments;

import org.odyssey.MainActivity;
import org.odyssey.OdysseyApplication;
import org.odyssey.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class ArtistsAlbumsTabsFragment extends Fragment {

    ViewPager mViewPager;
    AppSectionsPagerAdapter mAppSectionsPagerAdapter;

    OnAboutSelectedListener mAboutSelectedCallback;
    OnSettingsSelectedListener mSettingsSelectedCallback;
    OnPlayAllSelectedListener mPlayAllSelectedCallback;

    // Listener for communication via container activity
    public interface OnAboutSelectedListener {
        public void onAboutSelected();
    }

    public interface OnSettingsSelectedListener {
        public void onSettingsSelected();
    }

    public interface OnPlayAllSelectedListener {
        public void OnPlayAllSelected();
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mAboutSelectedCallback = (OnAboutSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnAlbumSelectedListener");
        }

        try {
            mSettingsSelectedCallback = (OnSettingsSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnSettingsSelectedListener");
        }

        try {
            mPlayAllSelectedCallback = (OnPlayAllSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPlayAllSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        ((MainActivity) getActivity()).getQuickControl().setVisibility(View.VISIBLE);

        // indicate this fragment has its own menu
        setHasOptionsMenu(true);

        View rootView = inflater.inflate(R.layout.fragment_artists_albums_tabs, container, false);

        // Create the adapter that will return a fragment for each of the three
        // primary sections
        // of the app.
        mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getChildFragmentManager(), getActivity());

        // Set up the ViewPager, attaching the adapter and setting up a listener
        // for when the
        // user swipes between sections.
        mViewPager = (ViewPager) rootView.findViewById(R.id.pager);
        mViewPager.setAdapter(mAppSectionsPagerAdapter);

        // set start page to albumsection
        mViewPager.setCurrentItem(1);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.main, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
        case R.id.action_settings:
            mSettingsSelectedCallback.onSettingsSelected();
            return true;
        case R.id.action_about:
            mAboutSelectedCallback.onAboutSelected();
            return true;
        case R.id.action_playall:
            mPlayAllSelectedCallback.OnPlayAllSelected();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the primary sections of the app.
     */
    public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {
        Context mContext;
        static final int mNumberOfPages = 3;

        public AppSectionsPagerAdapter(FragmentManager fm, Context context) {
            super(fm);
            this.mContext = context;
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
            case 0:
                return new ArtistsSectionFragment();
            case 1:
                return new AlbumsSectionFragment();
            case 2:
                return new AllTracksFragment();
            default:
                return null;
            }
        }

        @Override
        public int getCount() {
            return mNumberOfPages;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
            case 0:
                return mContext.getText(R.string.section_title_artists);
            case 1:
                return mContext.getText(R.string.section_title_albums);
            case 2:
                return mContext.getText(R.string.section_title_alltracks);
            }
            return "";
        }
    }

}
