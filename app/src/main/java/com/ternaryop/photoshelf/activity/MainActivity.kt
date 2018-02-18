package com.ternaryop.photoshelf.activity

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Spinner
import android.widget.Toast
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.BlogSpinnerAdapter
import com.ternaryop.photoshelf.drawer.ItemTestDrawerItem
import com.ternaryop.photoshelf.event.CounterEvent
import com.ternaryop.photoshelf.fragment.BestOfFragment
import com.ternaryop.photoshelf.fragment.BirthdaysBrowserFragment
import com.ternaryop.photoshelf.fragment.BirthdaysPublisherFragment
import com.ternaryop.photoshelf.fragment.DraftListFragment
import com.ternaryop.photoshelf.fragment.FragmentActivityStatus
import com.ternaryop.photoshelf.fragment.HomeFragment
import com.ternaryop.photoshelf.fragment.ImagePickerFragment
import com.ternaryop.photoshelf.fragment.PhotoPreferencesFragment
import com.ternaryop.photoshelf.fragment.PublishedPostsListFragment
import com.ternaryop.photoshelf.fragment.SavedContentListFragment
import com.ternaryop.photoshelf.fragment.ScheduledListFragment
import com.ternaryop.photoshelf.fragment.TagListFragment
import com.ternaryop.photoshelf.fragment.TagPhotoBrowserFragment
import com.ternaryop.photoshelf.service.CounterIntentService
import com.ternaryop.tumblr.AuthenticationCallback
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.utils.DialogUtils
import com.ternaryop.utils.drawer.activity.DrawerActionBarActivity
import com.ternaryop.utils.drawer.adapter.DrawerAdapter
import com.ternaryop.utils.drawer.adapter.DrawerItem
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : DrawerActionBarActivity(), AuthenticationCallback, FragmentActivityStatus {

    override lateinit var appSupport: AppSupport
    private lateinit var blogList: Spinner

    val blogName: String?
        get() = appSupport.selectedBlogName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appSupport = AppSupport(this)
        rebuildDrawerMenu()

        blogList = findViewById(R.id.blogs_spinner)
        blogList.onItemSelectedListener = BlogItemSelectedListener()

        val logged = Tumblr.isLogged(this)
        if (savedInstanceState == null) {
            if (logged) {
                if (!handleShortcutAction()) {
                    showHome()
                }
            } else {
                showSettings()
            }
        }
        enableUI(logged)
    }

    public override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    public override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun getActivityLayoutResId() = R.layout.activity_main

    private fun showHome() {
        selectClickedItem(0)
        openDrawer()
    }

    private fun handleShortcutAction(): Boolean {
        val action = intent.action ?: return false
        when {
            SHORTCUT_ACTION_DRAFT == action -> selectClickedItem(DRAWER_ITEM_DRAFT)
            SHORTCUT_ACTION_SCHEDULED == action -> selectClickedItem(DRAWER_ITEM_SCHEDULE)
            SHORTCUT_ACTION_PUBLISHED == action -> selectClickedItem(DRAWER_ITEM_PUBLISHED_POST)
            SHORTCUT_ACTION_FEEDLY == action -> selectClickedItem(DRAWER_ITEM_FEEDLY)
            else -> return false
        }
        return true
    }

    private fun showSettings() {
        selectClickedItem(adapter.count - 1)
    }

    override fun initDrawerAdapter(): DrawerAdapter {
        val adapter = DrawerAdapter(this)

        adapter.add(DrawerItem(DRAWER_ITEM_HOME, getString(R.string.home), HomeFragment::class.java))

        adapter.add(DrawerItem(DRAWER_ITEM_DRAFT, getString(R.string.draft_title), DraftListFragment::class.java, true))
        adapter.add(DrawerItem(DRAWER_ITEM_SCHEDULE, getString(R.string.schedule_title), ScheduledListFragment::class.java, true))
        adapter.add(DrawerItem(DRAWER_ITEM_PUBLISHED_POST, getString(R.string.published_post), PublishedPostsListFragment::class.java))

        // Tags
        adapter.add(DrawerItem.DRAWER_ITEM_DIVIDER)
        adapter.add(DrawerItem(DRAWER_ITEM_BROWSE_IMAGES_BY_TAGS, getString(R.string.browse_images_by_tags_title), TagPhotoBrowserFragment::class.java))
        adapter.add(DrawerItem(DRAWER_ITEM_BROWSE_TAGS, getString(R.string.browse_tags_title), TagListFragment::class.java))

        // Extras
        adapter.add(DrawerItem.DRAWER_ITEM_DIVIDER)
        adapter.add(DrawerItem(DRAWER_ITEM_BIRTHDAYS_BROWSER, getString(R.string.birthdays_browser_title), BirthdaysBrowserFragment::class.java))
        adapter.add(DrawerItem(DRAWER_ITEM_BIRTHDAYS_TODAY, getString(R.string.birthdays_today_title), BirthdaysPublisherFragment::class.java, true))
        adapter.add(DrawerItem(DRAWER_ITEM_BEST_OF, getString(R.string.best_of), BestOfFragment::class.java))
        adapter.add(DrawerItem(DRAWER_ITEM_FEEDLY, "Feedly", SavedContentListFragment::class.java))
        adapter.add(ItemTestDrawerItem(DRAWER_ITEM_TEST_PAGE, getString(R.string.test_page_title), ImagePickerFragment::class.java))

        // Settings
        adapter.add(DrawerItem.DRAWER_ITEM_DIVIDER)
        adapter.add(DrawerItem(DRAWER_ITEM_SETTINGS, getString(R.string.settings), PhotoPreferencesFragment::class.java))

        return adapter
    }

    override fun onResume() {
        super.onResume()

        if (!Tumblr.isLogged(this)) {
            // if we are returning from authentication then enable the UI
            val handled = Tumblr.handleOpenURI(this, intent.data, this)

            // show the preference only if we aren't in the middle of URI handling and not already logged in
            if (handled) {
                showHome()
            } else {
                showSettings()
            }
        }
    }

    private inner class BlogItemSelectedListener : OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
            val blogName = blogList.selectedItem as String
            appSupport.selectedBlogName = blogName
            refreshCounters(blogName)
        }

        override fun onNothingSelected(parent: AdapterView<*>) {}
    }

    fun refreshCounters(blogName: String) {
        CounterIntentService.fetchCounter(this, blogName, CounterEvent.BIRTHDAY)
        CounterIntentService.fetchCounter(this, blogName, CounterEvent.DRAFT)
        CounterIntentService.fetchCounter(this, blogName, CounterEvent.SCHEDULE)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCounterEvent(event: CounterEvent) {
        val value = if (event.count > 0) event.count.toString() else null

        when (event.type) {
            CounterEvent.BIRTHDAY -> adapter.getItemById(DRAWER_ITEM_BIRTHDAYS_TODAY).badge = value
            CounterEvent.DRAFT -> adapter.getItemById(DRAWER_ITEM_DRAFT).badge = value
            CounterEvent.SCHEDULE -> adapter.getItemById(DRAWER_ITEM_SCHEDULE).badge = value
            CounterEvent.NONE -> {
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun enableUI(enabled: Boolean) {
        if (enabled) {
            appSupport.fetchBlogNames(this)
                    .subscribe(object : SingleObserver<List<String>> {
                        override fun onSubscribe(d: Disposable) {
                        }

                        override fun onSuccess(blogNames: List<String>) {
                            fillBlogList(blogNames)
                        }

                        override fun onError(e: Throwable) {
                            DialogUtils.showErrorDialog(applicationContext, e)
                        }
                    })
        }
        drawerToggle.isDrawerIndicatorEnabled = enabled
        adapter.isSelectionEnabled = enabled
        adapter.notifyDataSetChanged()
    }

    override fun tumblrAuthenticated(token: String?, tokenSecret: String?, error: Exception?) {
        if (error == null) {
            Toast.makeText(this,
                    getString(R.string.authentication_success_title),
                    Toast.LENGTH_LONG)
                    .show()
            // after authentication cache blog names
            appSupport.fetchBlogNames(this)
                    .subscribe(object : SingleObserver<List<String>> {
                        override fun onSubscribe(d: Disposable) {
                        }

                        override fun onSuccess(value: List<String>) {
                            enableUI(token != null && tokenSecret != null)
                        }

                        override fun onError(e: Throwable) {
                            DialogUtils.showErrorDialog(this@MainActivity, e)
                        }
                    })
        } else {
            DialogUtils.showErrorDialog(this, error)
        }
    }

    override fun onDrawerItemSelected(drawerItem: DrawerItem) {
        try {
            val fragment = drawerItem.instantiateFragment(applicationContext)
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit()
        } catch (e: Exception) {
            DialogUtils.showErrorDialog(application, e)
            e.printStackTrace()
        }
    }

    private fun fillBlogList(blogNames: List<String>) {
        val adapter = BlogSpinnerAdapter(this, LOADER_PREFIX_AVATAR, blogNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        blogList.adapter = adapter

        val selectedName = appSupport.selectedBlogName
        if (selectedName != null) {
            val position = adapter.getPosition(selectedName)
            if (position >= 0) {
                blogList.setSelection(position)
            }
        }
    }

    override val isDrawerMenuOpen: Boolean
        get() = this.isDrawerOpen

    override val drawerToolbar: Toolbar
        get() = toolbar

    override fun getToolbar(): Toolbar {
        return findViewById<View>(R.id.drawer_toolbar) as Toolbar
    }

    companion object {
        private const val LOADER_PREFIX_AVATAR = "avatar"

        private const val DRAWER_ITEM_HOME = 0
        private const val DRAWER_ITEM_DRAFT = 1
        private const val DRAWER_ITEM_SCHEDULE = 2
        private const val DRAWER_ITEM_PUBLISHED_POST = 3
        private const val DRAWER_ITEM_BROWSE_IMAGES_BY_TAGS = 4
        private const val DRAWER_ITEM_BROWSE_TAGS = 5
        private const val DRAWER_ITEM_BIRTHDAYS_BROWSER = 6
        private const val DRAWER_ITEM_BIRTHDAYS_TODAY = 7
        private const val DRAWER_ITEM_BEST_OF = 8
        private const val DRAWER_ITEM_TEST_PAGE = 9
        private const val DRAWER_ITEM_SETTINGS = 10
        private const val DRAWER_ITEM_FEEDLY = 11

        const val SHORTCUT_ACTION_DRAFT = "com.ternaryop.photoshelf.shortcut.draft"
        const val SHORTCUT_ACTION_SCHEDULED = "com.ternaryop.photoshelf.shortcut.scheduled"
        const val SHORTCUT_ACTION_PUBLISHED = "com.ternaryop.photoshelf.shortcut.published"
        const val SHORTCUT_ACTION_FEEDLY = "com.ternaryop.photoshelf.shortcut.feedly"
    }
}
