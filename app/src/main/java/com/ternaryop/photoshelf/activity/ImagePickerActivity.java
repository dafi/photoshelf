package com.ternaryop.photoshelf.activity;

import android.app.Fragment;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.fragment.ImagePickerFragment;

public class ImagePickerActivity extends AbsPhotoShelfActivity {
    @Override
    public int getContentViewLayoutId() {
        return R.layout.activity_image_picker;
    }

    @Override
    public Fragment createFragment() {
        return new ImagePickerFragment();
    }
}
