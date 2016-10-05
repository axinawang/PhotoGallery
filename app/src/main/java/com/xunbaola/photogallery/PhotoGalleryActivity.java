package com.xunbaola.photogallery;

import android.app.SearchManager;
import android.content.Intent;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    private static final String TAG ="PhotoGalleryActivity" ;


    @Override
    protected Fragment createFragment() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");

    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())){
            String query=intent.getStringExtra(SearchManager.QUERY);
            Log.d(TAG,"received a new search query: "+query);
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString(FlickrFetchr.PREF_SEARCH_QUERY,query)
                    .commit();

        }
        FragmentManager fm=getSupportFragmentManager();
        PhotoGalleryFragment fragment= (PhotoGalleryFragment) fm.findFragmentById(R.id.fragmentContainer);
        fragment.updateItems();


    }




}
