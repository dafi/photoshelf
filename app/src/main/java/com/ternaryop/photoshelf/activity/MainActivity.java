package com.ternaryop.photoshelf.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.Toast;

import com.ternaryop.photoshelf.AppSupport;
import com.ternaryop.photoshelf.AppSupport.AppSupportCallback;
import com.ternaryop.photoshelf.Constants;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.adapter.BlogSpinnerAdapter;
import com.ternaryop.photoshelf.counter.AbsCountRetriever;
import com.ternaryop.photoshelf.counter.BirthdaysCountRetriever;
import com.ternaryop.photoshelf.counter.DraftCountRetriever;
import com.ternaryop.photoshelf.counter.QueueCountRetriever;
import com.ternaryop.photoshelf.fragment.BestOfFragment;
import com.ternaryop.photoshelf.fragment.BirthdaysBrowserFragment;
import com.ternaryop.photoshelf.fragment.BirthdaysPublisherFragment;
import com.ternaryop.photoshelf.fragment.DraftListFragment;
import com.ternaryop.photoshelf.fragment.FragmentActivityStatus;
import com.ternaryop.photoshelf.fragment.HomeFragment;
import com.ternaryop.photoshelf.fragment.ImagePickerFragment;
import com.ternaryop.photoshelf.fragment.PhotoPreferencesFragment;
import com.ternaryop.photoshelf.fragment.PublishedPostsListFragment;
import com.ternaryop.photoshelf.fragment.SavedContentListFragment;
import com.ternaryop.photoshelf.fragment.ScheduledListFragment;
import com.ternaryop.photoshelf.fragment.TagListFragment;
import com.ternaryop.photoshelf.fragment.TagPhotoBrowserFragment;
import com.ternaryop.tumblr.AuthenticationCallback;
import com.ternaryop.tumblr.Blog;
import com.ternaryop.tumblr.Callback;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.utils.DialogUtils;
import com.ternaryop.utils.drawer.activity.DrawerActionBarActivity;
import com.ternaryop.utils.drawer.adapter.DrawerAdapter;
import com.ternaryop.utils.drawer.adapter.DrawerItem;
import com.ternaryop.utils.drawer.counter.CountChangedListener;
import com.ternaryop.utils.drawer.counter.CountProvider;
import com.ternaryop.utils.drawer.counter.CountRetriever;

public class MainActivity extends DrawerActionBarActivity implements AuthenticationCallback, FragmentActivityStatus {
    private static final String LOADER_PREFIX_AVATAR = "avatar";
    private AppSupport appSupport;
    private Spinner blogList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appSupport = new AppSupport(this);
        rebuildDrawerMenu();

        blogList = (Spinner) findViewById(R.id.blogs_spinner);
        blogList.setOnItemSelectedListener(new BlogItemSelectedListener());

        boolean logged = Tumblr.isLogged(this);
        if (savedInstanceState == null) {
            if (logged) {
                showHome();
            } else {
                showSettings();
            }
        }
        enableUI(logged);
    }

    @Override
    protected int getActivityLayoutResId() {
        return R.layout.activity_main;
    }

    private void showHome() {
        selectClickedItem(0);
        openDrawer();
    }

    private void showSettings() {
        selectClickedItem(getAdapter().getCount() - 1);
    }

    @Override
    protected DrawerAdapter initDrawerAdapter() {
        DrawerAdapter adapter = new DrawerAdapter(this);

        adapter.add(new DrawerItem(getString(R.string.home), HomeFragment.class));

        adapter.add(new DrawerItem(getString(R.string.draft_title), DraftListFragment.class,
                true, new DraftCountRetriever(this, getBlogName(), adapter)));
        adapter.add(new DrawerItem(getString(R.string.schedule_title), ScheduledListFragment.class,
                true, new QueueCountRetriever(this, getBlogName(), adapter)));
        adapter.add(new DrawerItem(getString(R.string.published_post), PublishedPostsListFragment.class));

        // Tags
        adapter.add(DrawerItem.DRAWER_ITEM_DIVIDER);
        adapter.add(new DrawerItem(getString(R.string.browse_images_by_tags_title), TagPhotoBrowserFragment.class));
        adapter.add(new DrawerItem(getString(R.string.browse_tags_title), TagListFragment.class));

        // Extras
        adapter.add(DrawerItem.DRAWER_ITEM_DIVIDER);
        adapter.add(new DrawerItem(getString(R.string.birthdays_browser_title), BirthdaysBrowserFragment.class));
        adapter.add(new DrawerItem(getString(R.string.birthdays_today_title), BirthdaysPublisherFragment.class,
                true, new BirthdaysCountRetriever(this, getBlogName(), adapter)));
        adapter.add(new DrawerItem(getString(R.string.best_of), BestOfFragment.class));
        adapter.add(new DrawerItem("Feedly", SavedContentListFragment.class));
        adapter.add(new DrawerItem(getString(R.string.test_page_title), ImagePickerFragment.class));

        // Settings
        adapter.add(DrawerItem.DRAWER_ITEM_DIVIDER);
        adapter.add(new DrawerItem(getString(R.string.settings), PhotoPreferencesFragment.class));

        return adapter;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!Tumblr.isLogged(this)) {
            // if we are returning from authentication then enable the UI
            boolean handled = Tumblr.handleOpenURI(this, getIntent().getData(), this);

            // show the preference only if we aren't in the middle of URI handling and not already logged in
            if (handled) {
                showHome();
            } else {
                showSettings();
            }
        }
    }

    private class BlogItemSelectedListener implements OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            String blogName = (String) blogList.getSelectedItem();
            appSupport.setSelectedBlogName(blogName);
            refreshCounters(blogName);
        }

        public void refreshCounters(String blogName) {
            DrawerAdapter adapter = getAdapter();
            for (int i = 0; i < adapter.getCount(); i++) {
                DrawerItem item = adapter.getItem(i);
                if (item.getCountRetriever() != null) {
                    ((AbsCountRetriever)item.getCountRetriever()).setBlogName(blogName);
                }
            }
            adapter.notifyDataSetChanged();
        }

        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    private void enableUI(boolean enabled) {
        if (enabled) {
            fetchBlogNames();
        }
        getDrawerToggle().setDrawerIndicatorEnabled(enabled);
        getAdapter().setSelectionEnabled(enabled);
        getAdapter().notifyDataSetChanged();
    }

    @Override
    public void tumblrAuthenticated(final String token, final String tokenSecret, final Exception error) {
        if (error == null) {
            Toast.makeText(this,
                    getString(R.string.authentication_success_title),
                    Toast.LENGTH_LONG)
                    .show();
            // after authentication cache blog names
            appSupport.fetchBlogNames(this, new AppSupportCallback() {
                @Override
                public void onComplete(AppSupport appSupport, Exception error) {
                    enableUI(token != null && tokenSecret != null);
                }
            });
        } else {
            DialogUtils.showErrorDialog(this, error);
        }
    }

    public String getBlogName() {
        return appSupport.getSelectedBlogName();
    }

    @Override
    protected void onDrawerItemSelected(DrawerItem drawerItem) {
        try {
            Fragment fragment = drawerItem.instantiateFragment(getApplicationContext());

            CountRetriever countRetriever = drawerItem.getCountRetriever();
            if (fragment instanceof CountProvider && countRetriever instanceof CountChangedListener) {
                ((CountProvider)fragment).setCountChangedListener((CountChangedListener) countRetriever);
            }

            // pass parameter to test page
            if (fragment instanceof ImagePickerFragment) {
                Bundle args = new Bundle();
                args.putString(Constants.EXTRA_URL, getString(R.string.test_page_url));
                fragment.setArguments(args);
            }

            getFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();
        } catch (Exception e) {
            DialogUtils.showErrorDialog(getApplication(), e);
            e.printStackTrace();
        }
    }

    private void fetchBlogNames() {
        Tumblr.getSharedTumblr(this).getBlogList(new Callback<Blog[]>() {

            @Override
            public void complete(Blog[] result) {
                List<String> blogNames = new ArrayList<String>(result.length);
                for (Blog blog : result) {
                    blogNames.add(blog.getName());
                }
                appSupport.setBlogList(blogNames);
                fillBlogList(blogNames);
            }

            @Override
            public void failure(Exception e) {
            }
        });
    }

    private void fillBlogList(List<String> blogNames) {
        BlogSpinnerAdapter adapter = new BlogSpinnerAdapter(this, LOADER_PREFIX_AVATAR, blogNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        blogList.setAdapter(adapter);

        String selectedName = appSupport.getSelectedBlogName();
        if (selectedName != null) {
            int position = adapter.getPosition(selectedName);
            if (position >= 0) {
                blogList.setSelection(position);
            }
        }
    }

    @Override
    public AppSupport getAppSupport() {
        return appSupport;
    }

    @Override
    public Toolbar getToolbar() {
        return (Toolbar) findViewById(R.id.drawer_toolbar);
    }
}
