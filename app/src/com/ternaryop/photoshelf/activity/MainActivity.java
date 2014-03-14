package com.ternaryop.photoshelf.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.ternaryop.photoshelf.AppSupport;
import com.ternaryop.photoshelf.AppSupport.AppSupportCallback;
import com.ternaryop.photoshelf.Constants;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.adapter.BlogSpinnerAdapter;
import com.ternaryop.photoshelf.adapter.DrawerAdapter;
import com.ternaryop.photoshelf.adapter.DrawerItem;
import com.ternaryop.photoshelf.counter.BirthdaysCountRetriever;
import com.ternaryop.photoshelf.counter.CountChangedListener;
import com.ternaryop.photoshelf.counter.CountProvider;
import com.ternaryop.photoshelf.counter.CountRetriever;
import com.ternaryop.photoshelf.counter.DraftCountRetriever;
import com.ternaryop.photoshelf.counter.QueueCountRetriever;
import com.ternaryop.photoshelf.fragment.BirthdaysFragment;
import com.ternaryop.photoshelf.fragment.BirthdaysPublisherFragment;
import com.ternaryop.photoshelf.fragment.DraftListFragment;
import com.ternaryop.photoshelf.fragment.FragmentActivityStatus;
import com.ternaryop.photoshelf.fragment.ImagePickerFragment;
import com.ternaryop.photoshelf.fragment.PublishedPostsListFragment;
import com.ternaryop.photoshelf.fragment.ScheduledListFragment;
import com.ternaryop.photoshelf.fragment.TagListFragment;
import com.ternaryop.photoshelf.fragment.TagPhotoBrowserFragment;
import com.ternaryop.tumblr.AuthenticationCallback;
import com.ternaryop.tumblr.Blog;
import com.ternaryop.tumblr.Callback;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.utils.DialogUtils;

public class MainActivity extends Activity implements AuthenticationCallback, FragmentActivityStatus {
    private static final String LOADER_PREFIX_AVATAR = "avatar";
    private AppSupport appSupport;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private LinearLayout drawerLinearLayout;
    private ListView drawerList;

    private DrawerAdapter adapter;

    // nav drawer title
    private CharSequence drawerTitle;

    // used to store app title
    private CharSequence title;
    
    private Spinner blogList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appSupport = new AppSupport(this);
        
        setContentView(R.layout.activity_main);

        title = drawerTitle = getTitle();

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        drawerLinearLayout = (LinearLayout) findViewById(R.id.drawer_frame);
        
        drawerList = (ListView) findViewById(R.id.drawer_list);
        drawerList.setOnItemClickListener(new DrawerItemClickListener());
        drawerList.setAdapter(initDrawerAdapter());

        // enabling action bar app icon and behaving it as toggle button
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                R.drawable.ic_drawer,
                R.string.drawer_open,
                R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(title);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(drawerTitle);
                invalidateOptionsMenu();
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);

        blogList = (Spinner) findViewById(R.id.blogs_spinner);
        blogList.setOnItemSelectedListener(new BlogItemSelectedListener());
        
        if (savedInstanceState == null) {
            drawerLayout.openDrawer(drawerLinearLayout);
        }
        enableUI(Tumblr.isLogged(this));
    }

    private DrawerAdapter initDrawerAdapter() {
        adapter = new DrawerAdapter(getApplicationContext());
        adapter.add(new DrawerItem(getString(R.string.draft_title), DraftListFragment.class,
                true, new DraftCountRetriever(this, getBlogName(), adapter)));
        adapter.add(new DrawerItem(getString(R.string.schedule_title), ScheduledListFragment.class,
                true, new QueueCountRetriever(this, getBlogName(), adapter)));
        adapter.add(new DrawerItem(getString(R.string.published_post), PublishedPostsListFragment.class));

        // Tags
        adapter.add(new DrawerItem(getString(R.string.tags_title)));
        adapter.add(new DrawerItem(getString(R.string.browse_images_by_tags_title), TagPhotoBrowserFragment.class));
        adapter.add(new DrawerItem(getString(R.string.browse_tags_title), TagListFragment.class));

        // Extras
        adapter.add(new DrawerItem(getString(R.string.extras_title), null));
        adapter.add(new DrawerItem(getString(R.string.birthdays_title), BirthdaysFragment.class));
        adapter.add(new DrawerItem(getString(R.string.birthdays_today_title), BirthdaysPublisherFragment.class,
                true, new BirthdaysCountRetriever(this, getBlogName(), adapter)));
        adapter.add(new DrawerItem(getString(R.string.test_page_title), ImagePickerFragment.class));
        
        return adapter;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        drawerToggle.onConfigurationChanged(newConfig);
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        if (!Tumblr.isLogged(this)) {
            // if we are returning from authentication then enable the UI
            boolean handled = Tumblr.handleOpenURI(this, getIntent().getData(), this);

            // show the preference only if we aren't in the middle of URI handling and not already logged in
            if (!handled) {
                PhotoPreferencesActivity.startPreferencesActivityForResult(this);
            }
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }
    
    private class BlogItemSelectedListener implements OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            String blogName = (String) blogList.getSelectedItem();
            appSupport.setSelectedBlogName(blogName);
            adapter.refreshCounters(blogName);
        }

        public void onNothingSelected(AdapterView<?> parent) {
        }
    }
    
    private void enableUI(boolean enabled) {
        if (enabled) {
            fetchBlogNames();
        }
        adapter.setSelectionEnabled(enabled);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
        case R.id.action_settings:
            PhotoPreferencesActivity.startPreferencesActivityForResult(this);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
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

    private void selectItem(int position) {
        try {
            DrawerItem drawerItem = adapter.getItem(position);
            Fragment fragment = Fragment.instantiate(getApplicationContext(),
                    drawerItem.getFragmentClass().getName());
            
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

            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, fragment).commit();

            drawerList.setItemChecked(position, true);
            drawerList.setSelection(position);
            setTitle(drawerItem.getTitle());
            drawerLayout.closeDrawer(drawerLinearLayout);
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
                for (int i = 0; i < result.length; i++) {
                    blogNames.add(result[i].getName());
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
    public void setTitle(CharSequence title) {
        this.title = title;
        getActionBar().setTitle(title);
    }

    @Override
    public boolean isDrawerOpen() {
        return drawerLayout.isDrawerOpen(drawerLinearLayout);
    }
    
    @Override
    public AppSupport getAppSupport() {
        return appSupport;
    }
}
