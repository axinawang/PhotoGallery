package com.xunbaola.photogallery;

import android.app.Application;
import android.net.Uri;
import android.test.ApplicationTestCase;
import android.util.Log;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }
    public void testStartPageNumber(){
        new FlickrFetchr().fetchItems();
    }
}