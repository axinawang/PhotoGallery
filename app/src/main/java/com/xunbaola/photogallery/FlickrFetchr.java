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
    private static final String ENDPOINT="https://api.flickr.com/services/rest/";
    private static final String API_KEY="376d6f72be679c40a54d67a3c9bcb191";
    private static final String METHOD_GET_RECENT="flickr.photos.getRecent";
    private static final String PARAM_EXTRAS="extras";
    private static final String EXTRA_SMALL_URL="url_s";
    private static final String XML_PHOTO= "photo";
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

    /**
     * 获取最近的xml形式的照片数据
     */
    public ArrayList<GalleryItem> fetchItems(){
        ArrayList<GalleryItem> items=new ArrayList<GalleryItem>();
        String url= Uri.parse(ENDPOINT).buildUpon().appendQueryParameter("method",METHOD_GET_RECENT)
                .appendQueryParameter("api_key",API_KEY)
                .appendQueryParameter(PARAM_EXTRAS,EXTRA_SMALL_URL)
                .build().toString();
        Log.d(TAG,url);
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
    void parseItems(ArrayList<GalleryItem> items, XmlPullParser parser) throws IOException, XmlPullParserException {
        int eventType=parser.next();
        while (eventType!=XmlPullParser.END_DOCUMENT){
            if (eventType== XmlPullParser.START_TAG&&XML_PHOTO.equals(parser.getName())) {
                String id=parser.getAttributeValue(null,"id");
                String caption=parser.getAttributeValue(null,"title");
                String smallUrl=parser.getAttributeValue(null,EXTRA_SMALL_URL);
                GalleryItem item=new GalleryItem();
                item.setId(id);
                item.setCaption(caption);
                item.setUrl(smallUrl);
                items.add(item);
                Log.d(TAG,item.toString());
            }
            eventType=parser.next();
        }
    }
}
