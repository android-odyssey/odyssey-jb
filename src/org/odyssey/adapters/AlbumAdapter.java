package org.odyssey.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.odyssey.databasemodel.AlbumModel;
import org.odyssey.views.GridItem;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.SectionIndexer;

public class AlbumAdapter extends BaseAdapter implements SectionIndexer {

    private final static String TAG = "OdysseyAlbumAdapter";

    ArrayList<String> mSectionList;
    ArrayList<Integer> mSectionPositions;
    HashMap<Character, Integer> mPositionSectionMap;
    private Context mContext;

    private List<AlbumModel> mModelData;

    private GridView mRootGrid;
    private int mScrollSpeed;

    public AlbumAdapter(Context context, GridView rootGrid) {
        super();

        mSectionList = new ArrayList<String>();
        mSectionPositions = new ArrayList<Integer>();
        mPositionSectionMap = new HashMap<Character, Integer>();
        mContext = context;
        mModelData = new ArrayList<AlbumModel>();

        mRootGrid = rootGrid;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Log.v(TAG,"Getting view: " + position);
        AlbumModel album = mModelData.get(position);
        String label = album.getAlbumName();
        String imageURL = album.getAlbumArtURL();
        // Log.v(TAG,"Got album: " + album);

        if (convertView != null) {
            // Log.v(TAG,"REUSE");
            GridItem gridItem = (GridItem) convertView;
            gridItem.setText(label);
            gridItem.setImageURL(imageURL);
        } else {
            convertView = new GridItem(mContext, label, imageURL, new android.widget.AbsListView.LayoutParams(mRootGrid.getColumnWidth(), mRootGrid.getColumnWidth()));
            // Log.v(TAG,"Created view");
        }

        if (mScrollSpeed == 0) {
            ((GridItem) convertView).startCoverImageTask();
        }
        return convertView;
    }

    public void swapModel(List<AlbumModel> albums) {
        Log.v(TAG, "Swapping data model");
        if (albums == null) {
            mModelData.clear();
        } else {
            mModelData = albums;
        }
        // create sectionlist for fastscrolling

        mSectionList.clear();
        mSectionPositions.clear();
        mPositionSectionMap.clear();
        if (mModelData.size() > 0) {
            char lastSection = 0;

            AlbumModel currentAlbum = mModelData.get(0);

            lastSection = currentAlbum.getAlbumName().toUpperCase().charAt(0);

            mSectionList.add("" + lastSection);
            mSectionPositions.add(0);
            mPositionSectionMap.put(lastSection, mSectionList.size() - 1);

            for (int i = 1; i < getCount(); i++) {

                currentAlbum = mModelData.get(i);

                char currentSection = currentAlbum.getAlbumName().toUpperCase().charAt(0);

                if (lastSection != currentSection) {
                    mSectionList.add("" + currentSection);

                    lastSection = currentSection;
                    mSectionPositions.add(i);
                    mPositionSectionMap.put(currentSection, mSectionList.size() - 1);
                }

            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        return mSectionPositions.get(sectionIndex);
    }

    @Override
    public int getSectionForPosition(int pos) {

        String albumName = mModelData.get(pos).getAlbumName();

        char albumSection = albumName.toUpperCase().charAt(0);

        if (mPositionSectionMap.containsKey(albumSection)) {
            int sectionIndex = mPositionSectionMap.get(albumSection);
            return sectionIndex;
        }
        return 0;
    }

    @Override
    public Object[] getSections() {
        return mSectionList.toArray();
    }

    @Override
    public int getCount() {
        return mModelData.size();
    }

    @Override
    public Object getItem(int position) {
        return mModelData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setScrollSpeed(int speed) {
        mScrollSpeed = speed;
    }

}