package com.xunbaola.photogallery;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;

import android.view.Menu;
import android.view.MenuInflater;

import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;


import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Administrator on 2016/9/4.
 */
public class PhotoGalleryFragment extends VisibleFragment {
    public static final String TAG="PhotoGalleryFragment";
    GridView mGridView;
    ArrayList<GalleryItem> mItems;
    LruCache<String,Bitmap> mCache;
    ThumbnailDownloader<ImageView> mThumbnailDownloader;
    private String mTotal;
    private int current_page=1;
    private int fetched_page=0;
    SearchView mSearchView;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        new FetchItemsTask().execute(current_page);
        //Intent i=new Intent(getActivity(),PollService.class);
        //PollService.setServiceAlarm(getActivity(),true);
        //获取系统分配给每个应用程序的最大内存，每个应用系统分配32M
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int mCacheSize = maxMemory / 8;
        //给LruCache分配1/8 4M
        mCache = new LruCache<String, Bitmap>(mCacheSize){

            //必须重写此方法，来测量Bitmap的大小
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }

        };
        mThumbnailDownloader =new ThumbnailDownloader<ImageView>(new Handler(),mCache);
        mThumbnailDownloader.setListener(new ThumbnailDownloader.Listener<ImageView>() {
            @Override
            public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
                if (isVisible()) {
                    imageView.setImageBitmap(thumbnail);
                }
            }

        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.d(TAG,"Background Thread started");

    }

    public void updateItems() {
        current_page=1;
        fetched_page=0;
        mSearchView.setIconified(true);
        new FetchItemsTask().execute(current_page);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View v=inflater.inflate(R.layout.fragment_photo_gallery,container,false);
        mGridView= (GridView) v.findViewById(R.id.gridView);
        setupAdapter();
        mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem+visibleItemCount==totalItemCount&&totalItemCount>0&&current_page==fetched_page){
                    current_page++;
                    new FetchItemsTask().execute(current_page);
                    Log.i(TAG,"currentpage= "+current_page);
                }
            }
        });
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                GalleryItem item=mItems.get(position);
                Uri photoPageUri=Uri.parse(item.getPhotoPageUrl());
                Intent i=new Intent(getActivity(),PhotoPageActivity.class);
                i.setData(photoPageUri);
                startActivity(i);
            }
        });
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.d(TAG,"Background thread destroyed");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery,menu);
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB){
            MenuItem item=menu.findItem(R.id.menu_item_search);
            mSearchView = (SearchView) MenuItemCompat.getActionView(item);

            SearchManager manager= (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
            ComponentName name=getActivity().getComponentName();
            SearchableInfo info=manager.getSearchableInfo(name);
            mSearchView.setSearchableInfo(info);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_item_search: //getActivity().onSearchRequested();return true;
               // mSearchView.setSubmitButtonEnabled(true);
                getActivity().startSearch(PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .getString(FlickrFetchr.PREF_SEARCH_QUERY,null),true,null,false);return true;
            case R.id.menu_item_clear:PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit()
                .putString(FlickrFetchr.PREF_SEARCH_QUERY,null)
                .commit();
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm=!PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(),shouldStartAlarm);
                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB) {
                    getActivity().invalidateOptionsMenu();
                }
                return true;
            default:return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item=menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            item.setTitle(R.string.stop_polling);

        }else {
            item.setTitle(R.string.start_polling);
        }
    }

    private void setupAdapter() {
        if (getActivity()==null||mGridView==null) {
            return;
        }
        if (mItems != null) {
            //mGridView.setAdapter(new ArrayAdapter<GalleryItem>(getActivity(),android.R.layout.simple_gallery_item,mItems));
            mGridView.setAdapter(new GalleryItemAdaper(mItems));
        }else {
            mGridView.setAdapter(null);
        }
    }
private class GalleryItemAdaper extends ArrayAdapter<GalleryItem>{

    public GalleryItemAdaper(ArrayList<GalleryItem> items) {
        super(getActivity(), 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView==null){
            convertView=getActivity().getLayoutInflater().inflate(R.layout.gallery_item,parent,false);
        }
        ImageView imageView= (ImageView) convertView.findViewById(R.id.gallery_item_imageView);
        imageView.setImageResource(R.drawable.replace);
        GalleryItem item=getItem(position);
        mThumbnailDownloader.queueThumbnail(imageView,item.getUrl());
        String url=item.getUrl();
        String id;
        if (mItems.size()>1) {
            int endpos=position-10;
            if (endpos<=0){
                endpos=0;
            }
            for (int i=position-1;i>=endpos;i--){
                url=mItems.get(i).getUrl();
                id=mItems.get(i).getId();
                if (url != null) {
                    mThumbnailDownloader.queueThumbnailCache(id,url);
                }

            }
            for (int i=position+1;i<=position+10;i++){
                if (i<mItems.size()){
                    url=mItems.get(i).getUrl();
                    id=mItems.get(i).getId();
                    if (url != null) {
                        mThumbnailDownloader.queueThumbnailCache(id,url);
                    }
                }else {break;}
            }
        }

        return convertView;

    }
}
    private class FetchItemsTask extends AsyncTask<Integer,Void,ArrayList<GalleryItem>>{
        @Override
        protected ArrayList<GalleryItem> doInBackground(Integer... params) {
            Activity activity=getActivity();
            if (activity == null) {
                return new ArrayList<GalleryItem>();
            }
            int page=params[0];
            String query= PreferenceManager.getDefaultSharedPreferences(activity)
                    .getString(FlickrFetchr.PREF_SEARCH_QUERY,null);
            if (query != null) {
                FlickrFetchr flickrFetchr=new FlickrFetchr();
                ArrayList<GalleryItem> items=flickrFetchr.search(query,page);
                mTotal=flickrFetchr.getTotal();
                return items;
            }else {
                return new FlickrFetchr().fetchItems(page);
            }

        }

        @Override
        protected void onPostExecute(ArrayList<GalleryItem> galleryItems) {
            mItems=galleryItems;
            setupAdapter();
            fetched_page++;
            if (mTotal != null) {
                Toast.makeText(getActivity(),"pic'total= "+mTotal,Toast.LENGTH_LONG).show();
            }

        }
    }
}
