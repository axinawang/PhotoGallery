package com.xunbaola.photogallery;

import android.support.v4.app.Fragment;

/**
 * Created by Administrator on 2016/10/1.
 */
public class PhotoPageActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return new PhotoPageFragment();
    }
}
