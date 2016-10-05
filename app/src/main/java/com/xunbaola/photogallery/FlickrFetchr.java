package com.xunbaola.photogallery;

import android.net.Uri;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Administrator on 2016/9/4.
 */
public class FlickrFetchr {
    public static final String TAG="FlickrFetchr";
    public static final String PREF_SEARCH_QUERY="searchQuery";//使用sharepreference存储查询字符串的键
    public static final String PREF_LAST_RESULT_ID="lastResultId";//使用sharepreference存储最后一次获取图片的Id的键
    public static final int PERPAGE=12; //每页显示的照片数量
    private static final String ENDPOINT="https://api.flickr.com/services/rest/";
    private static final String API_KEY="376d6f72be679c40a54d67a3c9bcb191";
    private static final String METHOD_GET_RECENT="flickr.photos.getRecent";
    private static final String PARAM_EXTRAS="extras";
    private static final String EXTRA_SMALL_URL="url_s";
    private static final String XML_PHOTO= "photo";
    private static final String XML_PHOTOS = "photos";
    //seach
    private static final String METHOD_SEARCH = "flickr.photos.search";
    private static final String PARAM_TEXT="text";
    private String mTotal;//图片数量
    /**
     * 从指定url获取原始数据并返回一个字节流数组
     * @param urlSpec 指定url
     * @return 字节流数组
     * @throws IOException
     */
    byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 从指定url获取原始数据并返回一个字符串
     * @param urlSpec 指定url
     * @return 字符串
     * @throws IOException
     */
    public String getUrl(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }


    void parseItems(ArrayList<GalleryItem> items, XmlPullParser parser) throws IOException, XmlPullParserException {
        int eventType=parser.next();
        while (eventType!=XmlPullParser.END_DOCUMENT){
            if (eventType== XmlPullParser.START_TAG) {
                if(XML_PHOTO.equals(parser.getName())){
                    String id=parser.getAttributeValue(null,"id");
                    String caption=parser.getAttributeValue(null,"title");
                    String smallUrl=parser.getAttributeValue(null,EXTRA_SMALL_URL);
                    String owner=parser.getAttributeValue(null,"owner");

                    GalleryItem item=new GalleryItem();
                    item.setId(id);
                    item.setCaption(caption);
                    item.setUrl(smallUrl);
                    item.setOwner(owner);
                    items.add(item);
                    Log.d(TAG,item.toString());
                }else if (XML_PHOTOS.equals(parser.getName())){
                    mTotal=parser.getAttributeValue(null,"total");
                }

            }
            eventType=parser.next();
        }
    }

    /**
     *
     * @param url
     * @return
     */
    public ArrayList<GalleryItem> downloadGalleryItems(String url) {
        ArrayList<GalleryItem> items=new ArrayList<GalleryItem>();
        try {
            String xmlString=getUrl(url);
            Log.d(TAG,"Received xml : "+xmlString);
            XmlPullParser parser= XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(new StringReader(xmlString));
            parseItems(items,parser);
        } catch (IOException e) {
            Log.e(TAG,"Failed to fetch items: "+ e);
        } catch (XmlPullParserException e) {
            Log.e(TAG,"Failed to parse items: "+ e);
        }
        return items;
    }
    /**
     * 获取最近照片数据
     * @param
     * @return
     */
    public ArrayList<GalleryItem> fetchItems() {

        String url= Uri.parse(ENDPOINT).buildUpon().appendQueryParameter("method",METHOD_GET_RECENT)
                .appendQueryParameter("api_key",API_KEY)
                .appendQueryParameter(PARAM_EXTRAS,EXTRA_SMALL_URL)
                .build().toString();
        Log.d(TAG,url);

        return downloadGalleryItems(url);
    }
    /**
     * 获取第page页的最近的xml形式的照片数据
     * @param page
     * @return
     */
    public ArrayList<GalleryItem> fetchItems(int page) {

        String url= Uri.parse(ENDPOINT).buildUpon().appendQueryParameter("method",METHOD_GET_RECENT)
                .appendQueryParameter("api_key",API_KEY)
                .appendQueryParameter(PARAM_EXTRAS,EXTRA_SMALL_URL)
                .appendQueryParameter("page",page+"")
                .build().toString();
        Log.d(TAG,url);

        return downloadGalleryItems(url);
    }
public ArrayList<GalleryItem> search(String query,int page){
    String url= Uri.parse(ENDPOINT).buildUpon().appendQueryParameter("method",METHOD_SEARCH)
            .appendQueryParameter("api_key",API_KEY)
            .appendQueryParameter(PARAM_EXTRAS,EXTRA_SMALL_URL)
            .appendQueryParameter(PARAM_TEXT,query).appendQueryParameter("page",page+"")
            .build().toString();
    Log.d(TAG,url);
    return downloadGalleryItems(url);
}

    public String getTotal() {
        return mTotal;
    }
    /**
     * 获取页数
     * @return
     */
    /*public int getPageNum(){
        int pages=0;
        String url= Uri.parse(ENDPOINT).buildUpon().appendQueryParameter("method",METHOD_GET_RECENT)
                .appendQueryParameter("api_key",API_KEY)
                .appendQueryParameter(PARAM_EXTRAS,EXTRA_SMALL_URL)
                .appendQueryParameter("per_page",PERPAGE+"")
                .build().toString();
        Log.d(TAG,url);
        try {
            String xmlString=getUrl(url);
            Log.d(TAG,"Received xml : "+xmlString);
            XmlPullParser parser= XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(new StringReader(xmlString));
            int eventType=parser.next();
            while (eventType!=XmlPullParser.END_DOCUMENT){
                if (eventType== XmlPullParser.START_TAG&&XML_PHOTOS.equals(parser.getName())) {
                    pages=Integer.parseInt(parser.getAttributeValue(null,"pages"));
                    Log.d(TAG,"pages:"+pages);
                    break;
                }
                eventType=parser.next();
            }
        } catch (IOException e) {
            Log.e(TAG,"Failed to fetch items: "+ e);
        } catch (XmlPullParserException e) {
            Log.e(TAG,"Failed to parse items: "+ e);
        }
        return pages;
    }*/
}
