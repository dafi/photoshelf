package com.ternaryop.photoshelf.activity;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

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

    public static void startImagePicker(Context context, String url) {
        Intent intent = new Intent(context, ImagePickerActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        context.startActivity(intent, null);
    }
}
