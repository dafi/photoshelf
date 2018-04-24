package com.ternaryop.photoshelf.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import com.ternaryop.feedly.FeedlyContent
import com.ternaryop.feedly.FeedlyManager
import com.ternaryop.feedly.FeedlyRateLimit
import com.ternaryop.feedly.SimpleFeedlyContent
import com.ternaryop.feedly.TokenExpiredException
import com.ternaryop.photoshelf.BuildConfig
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.activity.ImagePickerActivity
import com.ternaryop.photoshelf.activity.TagPhotoBrowserActivity
import com.ternaryop.photoshelf.adapter.feedly.FeedlyContentAdapter
import com.ternaryop.photoshelf.adapter.feedly.FeedlyContentDelegate
import com.ternaryop.photoshelf.adapter.feedly.FeedlyContentSortSwitcher.Companion.LAST_PUBLISH_TIMESTAMP
import com.ternaryop.photoshelf.adapter.feedly.FeedlyContentSortSwitcher.Companion.SAVED_TIMESTAMP
import com.ternaryop.photoshelf.adapter.feedly.FeedlyContentSortSwitcher.Companion.TITLE_NAME
import com.ternaryop.photoshelf.adapter.feedly.OnFeedlyContentClick
import com.ternaryop.photoshelf.adapter.feedly.toContentDelegate
import com.ternaryop.photoshelf.view.PhotoShelfSwipe
import com.ternaryop.utils.json.readJson
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import java.util.concurrent.Callable

class SavedContentListFragment : AbsPhotoShelfFragment(), OnFeedlyContentClick {

    private lateinit var adapter: FeedlyContentAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var feedlyManager: FeedlyManager
    private lateinit var preferences: SharedPreferences
    private lateinit var photoShelfSwipe: PhotoShelfSwipe

    private val newerThanHours: Int
        get() = preferences.getInt(PREF_NEWER_THAN_HOURS, DEFAULT_NEWER_THAN_HOURS)

    private val maxFetchItemCount: Int
        get() = preferences.getInt(PREF_MAX_FETCH_ITEMS_COUNT, DEFAULT_MAX_FETCH_ITEMS_COUNT)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.saved_content_list, container, false)

        initRecyclerView(rootView)

        setHasOptionsMenu(true)

        photoShelfSwipe = rootView!!.findViewById(R.id.swipe_container)
        photoShelfSwipe.setOnRefreshListener { refresh(true) }
        return rootView
    }

    private fun initRecyclerView(rootView: View) {
        adapter = FeedlyContentAdapter(context!!)

        recyclerView = rootView.findViewById(R.id.list)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context!!)
        recyclerView.adapter = adapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        preferences = PreferenceManager.getDefaultSharedPreferences(context!!)

        adapter.sortSwitcher.setType(preferences.getInt(PREF_SORT_TYPE, TITLE_NAME))
        adapter.clickListener = this

        feedlyManager = FeedlyManager(
            preferences.getString(PREF_FEEDLY_ACCESS_TOKEN, getString(R.string.FEEDLY_ACCESS_TOKEN))!!,
            getString(R.string.FEEDLY_USER_ID),
            getString(R.string.FEEDLY_REFRESH_TOKEN))

        refresh(false)
    }

    private fun refresh(deleteItemsIfAllowed: Boolean) {
        // do not start another refresh if the current one is running
        if (photoShelfSwipe.isWaitingResult) {
            return
        }
        Single
            .fromCallable(callableFeedlyReader(deleteItemsIfAllowed))
            .compose(photoShelfSwipe.applySwipe())
            .subscribe(object : FeedlyObserver<List<FeedlyContentDelegate>>() {
                override fun onSuccess(posts: List<FeedlyContentDelegate>) {
                    setItems(posts)
                }
            })
    }

    private fun callableFeedlyReader(deleteItemsIfAllowed: Boolean): Callable<List<FeedlyContentDelegate>> {
        return Callable {
            if (BuildConfig.DEBUG) {
                fakeCall()
            } else {
                deleteItems(deleteItemsIfAllowed)
                readSavedContents()
            }.toContentDelegate(context!!, blogName!!)
        }
    }

    private fun setItems(items: List<FeedlyContentDelegate>) {
        adapter.clear()
        adapter.addAll(items)
        adapter.sort()
        adapter.notifyDataSetChanged()
        scrollToPosition(0)

        refreshUI()
    }

    private fun deleteItems(deleteItemsIfAllowed: Boolean) {
        if (deleteItemsIfAllowed && deleteOnRefresh()) {
            val idList = adapter.uncheckedItems.map { it.id }
            feedlyManager.markSaved(idList, false)
        }
    }

    private fun readSavedContents(): List<FeedlyContent> {
        val ms = System.currentTimeMillis() - newerThanHours * ONE_HOUR_MILLIS
        return feedlyManager.getStreamContents(feedlyManager.globalSavedTag, maxFetchItemCount, ms, null)
    }

    private fun fakeCall(): List<FeedlyContent> {
        context!!.assets.open("sample/feedly.json").use { stream ->
            val items = stream.readJson().getJSONArray("items")
            return (0 until items.length()).mapTo(mutableListOf<FeedlyContent>()) {
                SimpleFeedlyContent(items.getJSONObject(it))
            }
        }
    }

    override fun refreshUI() {
        supportActionBar?.apply {
            subtitle = resources.getQuantityString(
                R.plurals.posts_count,
                adapter.itemCount,
                adapter.itemCount)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.saved_content, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        when (adapter.sortSwitcher.currentSortable.sortId) {
            TITLE_NAME -> menu.findItem(R.id.sort_title_name).isChecked = true
            SAVED_TIMESTAMP -> menu.findItem(R.id.sort_saved_time).isChecked = true
            LAST_PUBLISH_TIMESTAMP -> menu.findItem(R.id.sort_published_tag).isChecked = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val isChecked = !item.isChecked

        when (item.itemId) {
            R.id.action_refresh -> {
                refresh(true)
                return true
            }
            R.id.action_api_usage -> {
                showAPIUsage()
                return true
            }
            R.id.action_refresh_token -> {
                refreshToken()
                return true
            }
            R.id.action_settings -> {
                settings()
                return true
            }
            R.id.sort_title_name -> {
                item.isChecked = isChecked
                adapter.sortBy(TITLE_NAME)
                adapter.notifyDataSetChanged()
                scrollToPosition(0)
                saveSortSettings()
                return true
            }
            R.id.sort_saved_time -> {
                item.isChecked = isChecked
                adapter.sortBy(SAVED_TIMESTAMP)
                adapter.notifyDataSetChanged()
                scrollToPosition(0)
                saveSortSettings()
                return true
            }
            R.id.sort_published_tag -> {
                item.isChecked = isChecked
                adapter.sortBy(LAST_PUBLISH_TIMESTAMP)
                adapter.notifyDataSetChanged()
                scrollToPosition(0)
                saveSortSettings()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun refreshToken() {
        Single
            .fromCallable {
                feedlyManager.refreshAccessToken(
                    getString(R.string.FEEDLY_CLIENT_ID),
                    getString(R.string.FEEDLY_CLIENT_SECRET))
            }
            .compose(photoShelfSwipe.applySwipe())
            .subscribe(object : FeedlyObserver<String>() {
                override fun onSuccess(accessToken: String) {
                    preferences.edit().putString(PREF_FEEDLY_ACCESS_TOKEN, accessToken).apply()
                    feedlyManager.accessToken = preferences.getString(PREF_FEEDLY_ACCESS_TOKEN, accessToken)
                    // hide swipe otherwise refresh() exists immediately
                    photoShelfSwipe.setRefreshingAndWaitingResult(false)
                    refresh(true)
                }
            })
    }

    private fun saveSortSettings() {
        preferences
            .edit()
            .putInt(PREF_SORT_TYPE, adapter.sortSwitcher.currentSortable.sortId)
            .putBoolean(PREF_SORT_ASCENDING, adapter.sortSwitcher.currentSortable.isAscending)
            .apply()
    }

    private fun scrollToPosition(position: Int) {
        // offset set to 0 put the item to the top
        (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
    }

    private fun showAPIUsage() {
        AlertDialog.Builder(context!!)
            .setTitle(R.string.api_usage)
            .setMessage(getString(R.string.feedly_api_calls_count, FeedlyRateLimit.apiCallsCount) + "\n"
                + getString(R.string.feedly_api_reset_limit, FeedlyRateLimit.apiResetLimitAsString))
            .show()
    }

    @SuppressLint("InflateParams") // for dialogs passing null for root is valid, ignore the warning
    private fun settings() {
        val settingsView = activity!!.layoutInflater.inflate(R.layout.saved_content_settings, null)
        fillSettingsView(settingsView)
        AlertDialog.Builder(context!!)
            .setTitle(R.string.settings)
            .setView(settingsView)
            .setPositiveButton(android.R.string.ok) { _, _ -> updateSettings(settingsView) }
            .setNegativeButton(R.string.cancel_title) { dialog, _ -> dialog.cancel() }
            .show()
    }

    private fun updateSettings(view: View) {
        val fetch = view.findViewById<EditText>(R.id.max_fetch_items_count)
        val newerThanHours = view.findViewById<EditText>(R.id.newer_than_hours)
        val deleteOnRefresh = view.findViewById<CheckBox>(R.id.delete_on_refresh)
        preferences.edit()
            .putInt(PREF_MAX_FETCH_ITEMS_COUNT, Integer.parseInt(fetch.text.toString()))
            .putInt(PREF_NEWER_THAN_HOURS, Integer.parseInt(newerThanHours.text.toString()))
            .putBoolean(PREF_DELETE_ON_REFRESH, deleteOnRefresh.isChecked)
            .apply()
    }

    private fun fillSettingsView(view: View) {
        val fetchView = view.findViewById<EditText>(R.id.max_fetch_items_count)
        val newerThanHoursView = view.findViewById<EditText>(R.id.newer_than_hours)
        val deleteOnRefreshView = view.findViewById<CheckBox>(R.id.delete_on_refresh)

        fetchView.setText(maxFetchItemCount.toString())
        newerThanHoursView.setText(newerThanHours.toString())
        deleteOnRefreshView.isChecked = deleteOnRefresh()
    }

    override fun onTitleClick(position: Int) {
        ImagePickerActivity.startImagePicker(context!!, adapter.getItem(position).originId)
    }

    override fun onTagClick(position: Int) {
        TagPhotoBrowserActivity.startPhotoBrowserActivity(context!!,
            blogName!!, adapter.getItem(position).tag!!, false)
    }

    override fun onToggleClick(position: Int, checked: Boolean) {
        if (deleteOnRefresh()) {
            return
        }
        Completable
            .fromAction { feedlyManager.markSaved(listOf(adapter.getItem(position).id), checked) }
            .compose(photoShelfSwipe.applyCompletableSwipe<Void>())
            .doOnSubscribe { d -> compositeDisposable.add(d) }
            .subscribe({ }) { t -> showSnackbar(makeSnake(recyclerView, t)) }
    }

    private fun deleteOnRefresh(): Boolean {
        return preferences.getBoolean(PREF_DELETE_ON_REFRESH, false)
    }

    override fun makeSnake(view: View, t: Throwable): Snackbar {
        if (t is TokenExpiredException) {
            val snackbar = Snackbar.make(recyclerView, R.string.token_expired, Snackbar.LENGTH_INDEFINITE)
            snackbar
                .setActionTextColor(ContextCompat.getColor(context!!, R.color.snack_error_color))
                .setAction(resources.getString(R.string.refresh).toLowerCase()) { refreshToken() }
            return snackbar
        }
        return super.makeSnake(view, t)
    }

    internal abstract inner class FeedlyObserver<T> : SingleObserver<T> {
        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(t: Throwable) {
            showSnackbar(makeSnake(recyclerView, t))
        }
    }

    companion object {
        const val PREF_MAX_FETCH_ITEMS_COUNT = "savedContent.MaxFetchItemCount"
        const val PREF_NEWER_THAN_HOURS = "savedContent.NewerThanHours"
        const val PREF_DELETE_ON_REFRESH = "savedContent.DeleteOnRefresh"
        const val PREF_SORT_TYPE = "savedContent.SortType"
        const val PREF_SORT_ASCENDING = "savedContent.SortAscending"

        const val DEFAULT_MAX_FETCH_ITEMS_COUNT = 300
        const val DEFAULT_NEWER_THAN_HOURS = 24
        const val ONE_HOUR_MILLIS = 60 * 60 * 1000
        const val PREF_FEEDLY_ACCESS_TOKEN = "feedlyAccessToken"
    }
}
