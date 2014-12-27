package com.ternaryop.photoshelf.activity;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.ternaryop.photoshelf.Constants;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.fragment.TagPhotoBrowserFragment;

public class TagPhotoBrowserActivity extends AbsPhotoShelfActivity {
    @Override
    public int getContentViewLayoutId() {
        return R.layout.activity_tag_photo_browser;
    }

    @Override
    public Fragment createFragment() {
        return new TagPhotoBrowserFragment();
    }

    public static void startPhotoBrowserActivity(Context context, String blogName, String postTag, boolean allowSearch) {
        Intent intent = new Intent(context, TagPhotoBrowserActivity.class);
        Bundle bundle = new Bundle();

        bundle.putString(Constants.EXTRA_BLOG_NAME, blogName);
        bundle.putString(Constants.EXTRA_BROWSE_TAG, postTag);
        bundle.putBoolean(Constants.EXTRA_ALLOW_SEARCH, allowSearch);
        intent.putExtras(bundle);

        context.startActivity(intent);
    }

    public static void startPhotoBrowserActivityForResult(Fragment fragment, String blogName, String postTag, int requestCode, boolean allowSearch) {
        Intent intent = new Intent(fragment.getActivity(), TagPhotoBrowserActivity.class);
        Bundle bundle = new Bundle();

        bundle.putString(Constants.EXTRA_BLOG_NAME, blogName);
        bundle.putString(Constants.EXTRA_BROWSE_TAG, postTag);
        bundle.putBoolean(Constants.EXTRA_ALLOW_SEARCH, allowSearch);
        intent.putExtras(bundle);

        fragment.startActivityForResult(intent, requestCode);
    }
}
