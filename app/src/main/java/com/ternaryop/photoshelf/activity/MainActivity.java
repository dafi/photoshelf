package com.ternaryop.photoshelf.activity;

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
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.adapter.BlogSpinnerAdapter;
import com.ternaryop.photoshelf.drawer.ItemTestDrawerItem;
import com.ternaryop.photoshelf.event.CounterEvent;
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
import com.ternaryop.photoshelf.service.CounterIntentService;
import com.ternaryop.tumblr.AuthenticationCallback;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.utils.DialogUtils;
import com.ternaryop.utils.drawer.activity.DrawerActionBarActivity;
import com.ternaryop.utils.drawer.adapter.DrawerAdapter;
import com.ternaryop.utils.drawer.adapter.DrawerItem;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActivity extends DrawerActionBarActivity implements AuthenticationCallback, FragmentActivityStatus {
    private static final String LOADER_PREFIX_AVATAR = "avatar";

    private final static int DRAWER_ITEM_HOME = 0;
    private final static int DRAWER_ITEM_DRAFT = 1;
    private final static int DRAWER_ITEM_SCHEDULE = 2;
    private final static int DRAWER_ITEM_PUBLISHED_POST = 3;
    private final static int DRAWER_ITEM_BROWSE_IMAGES_BY_TAGS = 4;
    private final static int DRAWER_ITEM_BROWSE_TAGS = 5;
    private final static int DRAWER_ITEM_BIRTHDAYS_BROWSER = 6;
    private final static int DRAWER_ITEM_BIRTHDAYS_TODAY = 7;
    private final static int DRAWER_ITEM_BEST_OF = 8;
    private final static int DRAWER_ITEM_TEST_PAGE = 9;
    private final static int DRAWER_ITEM_SETTINGS = 10;
    private final static int DRAWER_ITEM_FEEDLY = 11;

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
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
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

        adapter.add(new DrawerItem(DRAWER_ITEM_HOME, getString(R.string.home), HomeFragment.class));

        adapter.add(new DrawerItem(DRAWER_ITEM_DRAFT, getString(R.string.draft_title), DraftListFragment.class, true));
        adapter.add(new DrawerItem(DRAWER_ITEM_SCHEDULE, getString(R.string.schedule_title), ScheduledListFragment.class, true));
        adapter.add(new DrawerItem(DRAWER_ITEM_PUBLISHED_POST, getString(R.string.published_post), PublishedPostsListFragment.class));

        // Tags
        adapter.add(DrawerItem.DRAWER_ITEM_DIVIDER);
        adapter.add(new DrawerItem(DRAWER_ITEM_BROWSE_IMAGES_BY_TAGS, getString(R.string.browse_images_by_tags_title), TagPhotoBrowserFragment.class));
        adapter.add(new DrawerItem(DRAWER_ITEM_BROWSE_TAGS, getString(R.string.browse_tags_title), TagListFragment.class));

        // Extras
        adapter.add(DrawerItem.DRAWER_ITEM_DIVIDER);
        adapter.add(new DrawerItem(DRAWER_ITEM_BIRTHDAYS_BROWSER, getString(R.string.birthdays_browser_title), BirthdaysBrowserFragment.class));
        adapter.add(new DrawerItem(DRAWER_ITEM_BIRTHDAYS_TODAY, getString(R.string.birthdays_today_title), BirthdaysPublisherFragment.class,  true));
        adapter.add(new DrawerItem(DRAWER_ITEM_BEST_OF, getString(R.string.best_of), BestOfFragment.class));
        adapter.add(new DrawerItem(DRAWER_ITEM_FEEDLY, "Feedly", SavedContentListFragment.class));
        adapter.add(new ItemTestDrawerItem(DRAWER_ITEM_TEST_PAGE, getString(R.string.test_page_title), ImagePickerFragment.class));

        // Settings
        adapter.add(DrawerItem.DRAWER_ITEM_DIVIDER);
        adapter.add(new DrawerItem(DRAWER_ITEM_SETTINGS, getString(R.string.settings), PhotoPreferencesFragment.class));

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
            CounterIntentService.fetchCounter(MainActivity.this, blogName, CounterEvent.BIRTHDAY);
            CounterIntentService.fetchCounter(MainActivity.this, blogName, CounterEvent.DRAFT);
            CounterIntentService.fetchCounter(MainActivity.this, blogName, CounterEvent.SCHEDULE);
        }

        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCounterEvent(CounterEvent event) {
        String value = event.getCount() > 0 ? String.valueOf(event.getCount()) : null;

        switch (event.getType()) {
            case CounterEvent.BIRTHDAY:
                getAdapter().getItemById(DRAWER_ITEM_BIRTHDAYS_TODAY).setBadge(value);
                break;
            case CounterEvent.DRAFT:
                getAdapter().getItemById(DRAWER_ITEM_DRAFT).setBadge(value);
                break;
            case CounterEvent.SCHEDULE:
                getAdapter().getItemById(DRAWER_ITEM_SCHEDULE).setBadge(value);
                break;
            case CounterEvent.NONE:
                break;
        }
        getAdapter().notifyDataSetChanged();
    }

    private void enableUI(boolean enabled) {
        if (enabled) {
            appSupport.fetchBlogNames(this)
                    .subscribe(new SingleObserver<List<String>>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(List<String> blogNames) {
                            fillBlogList(blogNames);
                        }

                        @Override
                        public void onError(Throwable e) {
                            DialogUtils.showErrorDialog(getApplicationContext(), e);
                        }
                    });
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
            appSupport.fetchBlogNames(this)
                    .subscribe(new SingleObserver<List<String>>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(List<String> value) {
                            enableUI(token != null && tokenSecret != null);
                        }

                        @Override
                        public void onError(Throwable e) {
                            DialogUtils.showErrorDialog(MainActivity.this, e);
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
            getFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();
        } catch (Exception e) {
            DialogUtils.showErrorDialog(getApplication(), e);
            e.printStackTrace();
        }
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
