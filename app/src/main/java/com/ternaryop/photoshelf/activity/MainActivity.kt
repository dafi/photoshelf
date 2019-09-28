@file:Suppress("MaxLineLength")
package com.ternaryop.photoshelf.activity

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.navigateUp
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.EXTRA_URL
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.BlogSpinnerAdapter
import com.ternaryop.photoshelf.event.CounterEvent
import com.ternaryop.photoshelf.fragment.FragmentActivityStatus
import com.ternaryop.photoshelf.fragment.TagListFragment
import com.ternaryop.photoshelf.fragment.preference.PreferenceCategorySelector
import com.ternaryop.photoshelf.service.CounterIntentService
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.utils.dialog.showErrorDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.coroutines.CoroutineContext

class MainActivity : AbsDrawerActionBarActivity(),
    FragmentActivityStatus, PreferenceFragmentCompat.OnPreferenceStartScreenCallback, CoroutineScope {
    override lateinit var appSupport: AppSupport
    private lateinit var blogList: Spinner
    val blogName: String?
        get() = appSupport.selectedBlogName
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override val topLevelDestinationIds: Set<Int>
        get() = setOf(
            R.id.nav_home,
            R.id.nav_draft,
            R.id.nav_schedule,
            R.id.nav_published,
            R.id.nav_browse_images_by_tags,
            R.id.nav_browse_tags,
            R.id.nav_birthdays_browse,
            R.id.nav_birthdays_today,
            R.id.nav_bestof,
            R.id.nav_feedly,
            R.id.nav_test_page,
            R.id.nav_settings
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        job = Job()
        appSupport = AppSupport(this)

        navView.setNavigationItemSelectedListener(this)

        blogList = navView.getHeaderView(0).findViewById(R.id.blogs_spinner)
        blogList.onItemSelectedListener = BlogItemSelectedListener()
        val logged = TumblrManager.isLogged(this)
        if (savedInstanceState == null) {
            onAppStarted(logged)
        }
        enableUI(logged)
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
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

    public override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    public override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override val activityLayoutResId: Int
        get() = R.layout.activity_main

    private fun showHome() {
        selectClickedItem(R.id.nav_home)
        openDrawer()
    }

    private fun showSettings() {
        selectClickedItem(R.id.nav_settings)
    }

    private fun handleShortcutAction(): Boolean {
        when (intent.action) {
            SHORTCUT_ACTION_DRAFT -> selectClickedItem(R.id.nav_draft)
            SHORTCUT_ACTION_SCHEDULED -> selectClickedItem(R.id.nav_schedule)
            SHORTCUT_ACTION_PUBLISHED -> selectClickedItem(R.id.nav_published)
            SHORTCUT_ACTION_FEEDLY -> selectClickedItem(R.id.nav_feedly)
            else -> return false
        }
        return true
    }

    override fun onResume() {
        super.onResume()

        if (!TumblrManager.isLogged(this)) {
            // if we are returning from authentication then enable the UI
            launch {
                try {
                    val handled = withContext(Dispatchers.IO) {TumblrManager.handleOpenURI(applicationContext, intent.data) }
                    // show the preference only if we aren't in the middle of URI handling and not already logged in
                    if (handled) {
                        tumblrAuthenticated()
                        showHome()
                    } else {
                        showSettings()
                    }
                } catch (t: Throwable) {
                    tumblrAuthenticationError(t)
                }
            }
        }
    }

    private inner class BlogItemSelectedListener : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
            val blogName = blogList.selectedItem as String
            appSupport.selectedBlogName = blogName
            refreshCounters(blogName)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }

    fun refreshCounters(blogName: String) {
        CounterIntentService.fetchCounter(this, blogName, CounterEvent.BIRTHDAY)
        CounterIntentService.fetchCounter(this, blogName, CounterEvent.DRAFT)
        CounterIntentService.fetchCounter(this, blogName, CounterEvent.SCHEDULE)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @Suppress("unused")
    fun onCounterEvent(event: CounterEvent) {
        val value = if (event.count > 0) event.count.toString() else null

        when (event.type) {
            CounterEvent.BIRTHDAY -> setBadgeText(R.id.nav_birthdays_today, value)
            CounterEvent.DRAFT -> setBadgeText(R.id.nav_draft, value)
            CounterEvent.SCHEDULE -> setBadgeText(R.id.nav_schedule, value)
            CounterEvent.NONE -> {
            }
        }
    }

    private fun enableUI(enabled: Boolean) {
        if (enabled) {
            launch {
                try {
                    val blogSetNames = appSupport.fetchBlogNames(applicationContext)
                    fillBlogList(blogSetNames)
                } catch (t: Throwable) {
                    t.showErrorDialog(applicationContext)
                }
            }
        }
        isDrawerIndicatorEnabled = enabled
//        drawerToggle.isDrawerIndicatorEnabled = enabled
//        adapter.isSelectionEnabled = enabled
//        adapter.notifyDataSetChanged()
    }

    private fun tumblrAuthenticated() {
        Toast.makeText(this,
            getString(R.string.authentication_success_title),
            Toast.LENGTH_LONG)
            .show()
        // after authentication cache blog names
        launch {
            try {
                appSupport.fetchBlogNames(applicationContext)
                enableUI(true)
            } catch (t: Throwable) {
                t.showErrorDialog(applicationContext)
            }
        }
    }

    private fun tumblrAuthenticationError(error: Throwable) = error.showErrorDialog(this)

    private fun fillBlogList(blogNames: List<String>) {
        val adapter = BlogSpinnerAdapter(this, blogNames)
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

    override val drawerToolbar: Toolbar
        get() = toolbar

    override val toolbar: Toolbar
        get() = findViewById(R.id.toolbar)

    override fun onPreferenceStartScreen(caller: PreferenceFragmentCompat?, pref: PreferenceScreen?): Boolean {
        PreferenceCategorySelector.openScreen(caller, pref)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onDrawerItemSelected(menuItem: MenuItem) : Boolean {
        val bundle = when(menuItem.itemId) {
            R.id.nav_browse_tags -> appSupport.selectedBlogName?.let {
                bundleOf(TagListFragment.ARG_BLOG_NAME to it)
            }
            R.id.nav_test_page -> bundleOf(
                EXTRA_URL to resources.getString(R.string.test_page_url))
            else -> null
        }

        // pressing back from top level destination closes the app instead of returning to home
        val navOptions = NavOptions
            .Builder()
            .setPopUpTo(R.id.nav_home, true)
            .build()
        navController.navigate(menuItem.itemId, bundle, navOptions)
        return true
    }

    companion object {
        const val SHORTCUT_ACTION_DRAFT = "com.ternaryop.photoshelf.shortcut.draft"
        const val SHORTCUT_ACTION_SCHEDULED = "com.ternaryop.photoshelf.shortcut.scheduled"
        const val SHORTCUT_ACTION_PUBLISHED = "com.ternaryop.photoshelf.shortcut.published"
        const val SHORTCUT_ACTION_FEEDLY = "com.ternaryop.photoshelf.shortcut.feedly"
    }
}
