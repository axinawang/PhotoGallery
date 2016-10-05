package com.xunbaola.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2016/9/7.
 */
public class ThumbnailDownloader<Token> extends HandlerThread {
    private static final java.lang.String TAG ="ThumbnailDownloader" ;
    private static final int MESSAGE_DOWNLOAD =0 ;
    private static final int MESSAGE_DOWNLOAD_CACHE =1 ;
    Handler mHandler;
    Map<Token,String> requestMap= Collections.synchronizedMap(new HashMap<Token, String>());
    Map<String,String> requestCacheMap= Collections.synchronizedMap(new HashMap<String, String>());
Handler mResponseHandler;
    Listener<Token> mListener;
    LruCache<String,Bitmap> mCache;
    public interface Listener<Token>{
        void onThumbnailDownloaded(Token token,Bitmap thumbnail);
    }

    public void setListener(Listener<Token> listener) {
        mListener = listener;
    }

    public ThumbnailDownloader() {
        super(TAG);
    }
    public ThumbnailDownloader(Handler responseHandler,LruCache<String,Bitmap> cache) {
        super(TAG);
        mResponseHandler=responseHandler;
        mCache=cache;
    }
    public void queueThumbnail(Token token,String url){
        Log.d(TAG,"Got a url: "+url);
        requestMap.put(token,url);
        mHandler.obtainMessage(MESSAGE_DOWNLOAD,token).sendToTarget();
    }
    public void queueThumbnailCache(String id,String url){
        Log.d(TAG,"Got a cache url: "+url);
        requestCacheMap.put(id,url);
        mHandler.obtainMessage(MESSAGE_DOWNLOAD_CACHE,id).sendToTarget();
    }

    @Override
    protected void onLooperPrepared() {
        mHandler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if (msg.what==MESSAGE_DOWNLOAD) {
                    Token token= (Token) msg.obj;
                    Log.d(TAG,"Got a request Url: "+requestMap.get(token));
                    handleRequest(token);
                }else if (msg.what==MESSAGE_DOWNLOAD_CACHE){
                    String id= (String) msg.obj;
                    Log.d(TAG,"Got a request cache Url "+requestCacheMap.get(id));
                    handleRequestCache(id);
                }
            }


        };

    }

    private void handleRequestCache(String id) {
        String url=requestCacheMap.get(id);
        if (url == null) {
            return;
        }
        try {
            byte[] bitmapBytes;
            Bitmap bitmap;
            if (mCache.get(url) == null) {
                bitmapBytes=new FlickrFetchr().getUrlBytes(url);
                Log.d(TAG,"downloading image in handleRequestCache()");
                bitmap = BitmapFactory.decodeByteArray(bitmapBytes,0,bitmapBytes.length);
                mCache.put(url,bitmap);
            }
        } catch (IOException e) {
            Log.d(TAG,"Error downloading image in handleRequestCache()",e);
        }

    }

    private void handleRequest(final Token token) {
       final String url=requestMap.get(token);
        if (url == null) {
            return;
        }
        try {
            byte[] bitmapBytes;
            final Bitmap bitmap;
            if (mCache.get(url) == null) {
                bitmapBytes=new FlickrFetchr().getUrlBytes(url);
                bitmap = BitmapFactory.decodeByteArray(bitmapBytes,0,bitmapBytes.length);
                mCache.put(url,bitmap);
            }else {
                bitmap=mCache.get(url);
            }


            Log.d(TAG,"Bitmap created");
            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (requestMap.get(token)!=url) {
                        return;
                    }
                    requestMap.remove(token);
                    mListener.onThumbnailDownloaded(token,bitmap);
                }
            });
        } catch (IOException e) {
            Log.d(TAG,"Error downloading image",e);
        }
    }
    public void clearQueue(){
        mHandler.removeMessages(MESSAGE_DOWNLOAD);
        mHandler.removeMessages(MESSAGE_DOWNLOAD_CACHE);
        requestMap.clear();
        requestCacheMap.clear();
    }
}
