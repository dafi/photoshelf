package com.ternaryop.photoshelf.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

import com.ternaryop.photoshelf.AppSupport;
import com.ternaryop.photoshelf.Constants;
import com.ternaryop.photoshelf.fragment.FragmentActivityStatus;

public abstract class AbsPhotoShelfActivity extends Activity implements FragmentActivityStatus  {
    protected AppSupport appSupport;
    private String blogName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        appSupport = new AppSupport(this);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            blogName = bundle.getString(Constants.EXTRA_BLOG_NAME);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // clicked the actionbar
                // close and return to caller
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean isDrawerOpen() {
        return false;
    }

    @Override
    public AppSupport getAppSupport() {
        return appSupport;
    }

    public String getBlogName() {
        return blogName == null ? appSupport.getSelectedBlogName() : blogName;
    }
}
