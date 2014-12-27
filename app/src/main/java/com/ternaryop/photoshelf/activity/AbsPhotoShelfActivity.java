package com.ternaryop.photoshelf.activity;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.ternaryop.photoshelf.AppSupport;
import com.ternaryop.photoshelf.Constants;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.fragment.FragmentActivityStatus;

public abstract class AbsPhotoShelfActivity extends ActionBarActivity implements FragmentActivityStatus  {
    protected AppSupport appSupport;
    private String blogName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentViewLayoutId());
        setupActionBar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        appSupport = new AppSupport(this);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            blogName = bundle.getString(Constants.EXTRA_BLOG_NAME);
        }
        Fragment fragment = createFragment();
        if (fragment != null) {
            getFragmentManager().beginTransaction().add(R.id.content_frame, fragment).commit();
        }
    }

    /**
     * The subclass doesn't call setContentView directly to avoid side effects (action mode bar doesn't overlap the
     * toolbar but is shown above) but pass the layout id to use
     * @return the layout id to use
     */
    public abstract int getContentViewLayoutId();

    /**
     * The fragment is added programmatically because in many cases its creation from XML layout can collide with
     * the supportActionBar creation (eg the fragment needs the actionBar but it can't be created until the xml is full instantiated so it will be null)
     * @return the fragment to use or null if no fragment must be added
     */
    public abstract Fragment createFragment();

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

    private Toolbar setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.photoshelf_toolbar);
        setSupportActionBar(toolbar);

        return toolbar;
    }

    @Override
    public Toolbar getToolbar() {
        return (Toolbar) findViewById(R.id.photoshelf_toolbar);
    }
}
