package com.ternaryop.photoshelf.activity;

import android.os.Bundle;

import com.ternaryop.photoshelf.R;

public class ImagePickerActivity extends AbsPhotoShelfActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_picker);
    }
}
