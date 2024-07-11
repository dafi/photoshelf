@file:Suppress("MaxLineLength")

package com.ternaryop.photoshelf.activity

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.birthday.browser.fragment.BirthdayBrowserFragment
import com.ternaryop.photoshelf.birthday.publisher.fragment.BirthdayPublisherFragment
import com.ternaryop.photoshelf.core.prefs.selectedBlogName
import com.ternaryop.photoshelf.feedly.fragment.FeedlyContentType
import com.ternaryop.photoshelf.feedly.fragment.FeedlyListFragment
import com.ternaryop.photoshelf.fragment.BestOfFragment
import com.ternaryop.photoshelf.fragment.FragmentActivityStatus
import com.ternaryop.photoshelf.fragment.appFragmentFactory
import com.ternaryop.photoshelf.fragment.preference.MainPreferenceFragment
import com.ternaryop.photoshelf.fragment.preference.PreferenceCategorySelector
import com.ternaryop.photoshelf.home.fragment.HomeFragment
import com.ternaryop.photoshelf.imagepicker.fragment.EXTRA_URL
import com.ternaryop.photoshelf.imagepicker.fragment.ImagePickerFragment
import com.ternaryop.photoshelf.lifecycle.EventObserver
import com.ternaryop.photoshelf.lifecycle.Status
import com.ternaryop.photoshelf.tagnavigator.fragment.TagListFragment
import com.ternaryop.photoshelf.tagphotobrowser.fragment.TagPhotoBrowserFragment
import com.ternaryop.photoshelf.tumblr.dialog.blog.BlogSpinnerAdapter
import com.ternaryop.photoshelf.tumblr.ui.draft.fragment.DraftListFragment
import com.ternaryop.photoshelf.tumblr.ui.publish.fragment.PublishedPostsListFragment
import com.ternaryop.photoshelf.tumblr.ui.schedule.fragment.ScheduledListFragment
import com.ternaryop.photoshelf.util.askNotificationsPermission
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.utils.dialog.showErrorDialog
import com.ternaryop.utils.drawer.activity.DrawerActionBarActivity
import com.ternaryop.utils.drawer.adapter.DrawerAdapter
import com.ternaryop.utils.drawer.adapter.DrawerItem
import dagger.hilt.android.AndroidEntryPoint

private const val ASK_NOTIFICATION_PERMISSION = 2

@AndroidEntryPoint
class MainActivity :
    DrawerActionBarActivity(),
    FragmentActivityStatus,
    PreferenceFragmentCompat.OnPreferenceStartScreenCallback,
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private lateinit var blogList: Spinner
    private lateinit var prefs: SharedPreferences

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = appFragmentFactory
        super.onCreate(savedInstanceState)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        rebuildDrawerMenu()

        blogList = findViewById(R.id.blogs_spinner)
        blogList.onItemSelectedListener = BlogItemSelectedListener()

        viewModel.result.observe(
            this,
            EventObserver { result ->
                when (result) {
                    is MainActivityModelResult.BirthdaysCount ->
                        onUpdateCount(DRAWER_ITEM_BIRTHDAYS_TODAY, result.command.data ?: 0)
                    is MainActivityModelResult.DraftCount ->
                        onUpdateCount(DRAWER_ITEM_DRAFT, result.command.data ?: 0)
                    is MainActivityModelResult.ScheduledCount ->
                        onUpdateCount(DRAWER_ITEM_SCHEDULE, result.command.data ?: 0)
                    is MainActivityModelResult.BlogNames -> onBlogNames(result)
                    is MainActivityModelResult.TumblrAuthenticated -> onTumblrAuthenticated(result)
                }
            }
        )

        val logged = TumblrManager.isLogged(this)
        if (savedInstanceState == null) {
            onAppStarted(logged)
        }
        enableUI(logged)

        askNotificationsPermission(this, R.string.notification_permission_rationale, ASK_NOTIFICATION_PERMISSION)
    }

    private fun onAppStarted(logged: Boolean) {
        if (logged) {
            if (!handleShortcutAction()) {
                showHome()
            }
        } else {
            showSettings()
        }
    }

    override val activityLayoutResId: Int
        get() = R.layout.activity_main

    private fun showHome() {
        selectClickedItem(0)
        openDrawer()
    }

    private fun handleShortcutAction(): Boolean {
        when (intent.action) {
            SHORTCUT_ACTION_DRAFT -> selectClickedItem(DRAWER_ITEM_DRAFT)
            SHORTCUT_ACTION_SCHEDULED -> selectClickedItem(DRAWER_ITEM_SCHEDULE)
            SHORTCUT_ACTION_FEEDLY_SAVED -> selectClickedItem(DRAWER_ITEM_FEEDLY_READ_LATER)
            SHORTCUT_ACTION_FEEDLY_UNREAD -> selectClickedItem(DRAWER_ITEM_FEEDLY_UNREAD)
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

        adapter.add(DrawerItem(DRAWER_ITEM_DRAFT, getString(com.ternaryop.photoshelf.tumblr.dialog.R.string.draft_title), DraftListFragment::class.java, true))
        adapter.add(DrawerItem(DRAWER_ITEM_SCHEDULE, getString(R.string.schedule_title), ScheduledListFragment::class.java, true))
        adapter.add(DrawerItem(DRAWER_ITEM_PUBLISHED_POST, getString(R.string.published_post), PublishedPostsListFragment::class.java))
        // Tags
        adapter.add(DrawerItem.DRAWER_ITEM_DIVIDER)
        adapter.add(DrawerItem(DRAWER_ITEM_BROWSE_IMAGES_BY_TAGS, getString(R.string.browse_images_by_tags_title), TagPhotoBrowserFragment::class.java))

        adapter.add(
            DrawerItem(
                DRAWER_ITEM_BROWSE_TAGS, getString(R.string.browse_tags_title), TagListFragment::class.java,
                argumentsBuilder = {
                    prefs.selectedBlogName?.let { bundleOf(TagListFragment.ARG_BLOG_NAME to it) }
                }
            )
        )

        // Extras
        adapter.add(DrawerItem.DRAWER_ITEM_DIVIDER)
        adapter.add(DrawerItem(DRAWER_ITEM_BIRTHDAYS_BROWSER, getString(R.string.birthdays_browser_title), BirthdayBrowserFragment::class.java))
        adapter.add(DrawerItem(DRAWER_ITEM_BIRTHDAYS_TODAY, getString(R.string.birthdays_today_title), BirthdayPublisherFragment::class.java, true))
        adapter.add(DrawerItem(DRAWER_ITEM_BEST_OF, getString(R.string.best_of), BestOfFragment::class.java))
        adapter.add(DrawerItem(DRAWER_ITEM_FEEDLY_READ_LATER, getString(R.string.feedly_saved_title), FeedlyListFragment::class.java))
        adapter.add(
            DrawerItem(
                DRAWER_ITEM_FEEDLY_UNREAD,
                getString(R.string.feedly_unread_title), FeedlyListFragment::class.java,
                arguments = bundleOf(FeedlyListFragment.ARG_CONTENT_TYPE to FeedlyContentType.Unread)
            )
        )

        adapter.add(
            DrawerItem(
                DRAWER_ITEM_TEST_PAGE,
                getString(R.string.test_page_title), ImagePickerFragment::class.java, arguments = bundleOf(EXTRA_URL to getString(R.string.test_page_url))
            )
        )
        // Settings
        adapter.add(DrawerItem.DRAWER_ITEM_DIVIDER)
        adapter.add(DrawerItem(DRAWER_ITEM_SETTINGS, getString(com.ternaryop.photoshelf.feedly.R.string.settings), MainPreferenceFragment::class.java))

        return adapter
    }

    override fun onResume() {
        super.onResume()

        viewModel.handleCallbackTumblrUri(intent.data)
    }

    private inner class BlogItemSelectedListener : OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
            val blogName = blogList.selectedItem as String
            if (blogName != prefs.selectedBlogName) {
                prefs.selectedBlogName = blogName
                viewModel.clearCounters()
            }
            viewModel.birthdaysCount()
            viewModel.draftCount(blogName)
            viewModel.scheduledCount(blogName)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
    }

    private fun onUpdateCount(itemId: Int, count: Int) {
        adapter.getItemById(itemId)?.badge = if (count > 0) count.toString() else null
        adapter.notifyDataSetChanged()
    }

    private fun onBlogNames(result: MainActivityModelResult.BlogNames) {
        when (result.command.status) {
            Status.SUCCESS -> result.command.data?.also { blogNames -> fillBlogList(blogNames) }
            Status.ERROR -> result.command.error?.also { it.showErrorDialog(this) }
            Status.PROGRESS -> {}
        }
    }

    private fun onTumblrAuthenticated(result: MainActivityModelResult.TumblrAuthenticated) {
        when (result.command.status) {
            Status.SUCCESS -> result.command.data?.also { handled ->
                if (handled) {
                    tumblrAuthenticated()
                    showHome()
                } else {
                    showSettings()
                }
            }
            Status.ERROR -> result.command.error?.also { it.showErrorDialog(this) }
            Status.PROGRESS -> {}
        }
    }

    private fun enableUI(enabled: Boolean) {
        if (enabled) {
            viewModel.fetchBlogNames()
        }
        drawerToggle.isDrawerIndicatorEnabled = enabled
        adapter.isSelectionEnabled = enabled
        adapter.notifyDataSetChanged()
    }

    private fun tumblrAuthenticated() {
        Toast.makeText(
            this,
            getString(R.string.authentication_success_title),
            Toast.LENGTH_LONG
        ).show()
        enableUI(true)
    }

    override fun onDrawerItemSelected(drawerItem: DrawerItem) {
        try {
            val fragment = drawerItem.instantiateFragment(applicationContext, supportFragmentManager)
            supportFragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit()
        } catch (e: Exception) {
            e.showErrorDialog(application)
            e.printStackTrace()
        }
    }

    private fun fillBlogList(blogNames: List<String>) {
        val adapter = BlogSpinnerAdapter(this, blogNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        blogList.adapter = adapter

        val selectedName = prefs.selectedBlogName
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

    override val toolbar: Toolbar
        get() = findViewById<View>(com.ternaryop.photoshelf.imageviewer.R.id.drawer_toolbar) as Toolbar

    override fun onPreferenceStartScreen(
        caller: PreferenceFragmentCompat,
        pref: PreferenceScreen
    ): Boolean {
        val fragment = PreferenceCategorySelector.fragmentFromCategory(pref.key) ?: return false
        return PreferenceCategorySelector.openScreen(caller, pref, fragment)
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        val prefFragment = pref.fragment ?: return false
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            prefFragment
        )
        return PreferenceCategorySelector.openScreen(caller, pref, fragment)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // when PreferenceScreen opens a new fragment isn't possible to to come back
        // using the hamburger menu, so we check manually if we are on some settings fragment
        if (supportFragmentManager.fragments.size > 1 && supportFragmentManager.fragments[0] is MainPreferenceFragment) {
            supportFragmentManager.popBackStack()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
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
        private const val DRAWER_ITEM_FEEDLY_READ_LATER = 11
        private const val DRAWER_ITEM_FEEDLY_UNREAD = 12

        const val SHORTCUT_ACTION_DRAFT = "com.ternaryop.photoshelf.shortcut.draft"
        const val SHORTCUT_ACTION_SCHEDULED = "com.ternaryop.photoshelf.shortcut.scheduled"
        const val SHORTCUT_ACTION_FEEDLY_SAVED = "com.ternaryop.photoshelf.shortcut.feedly.saved"
        const val SHORTCUT_ACTION_FEEDLY_UNREAD = "com.ternaryop.photoshelf.shortcut.feedly.unread"
    }
}
