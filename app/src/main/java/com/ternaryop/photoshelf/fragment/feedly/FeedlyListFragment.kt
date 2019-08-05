package com.ternaryop.photoshelf.fragment.feedly

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.GsonBuilder
import com.ternaryop.feedly.AccessToken
import com.ternaryop.feedly.FeedlyClient
import com.ternaryop.feedly.FeedlyRateLimit
import com.ternaryop.feedly.SimpleFeedlyContent
import com.ternaryop.feedly.StreamContent
import com.ternaryop.feedly.StreamContentFindParam
import com.ternaryop.feedly.TokenExpiredException
import com.ternaryop.photoshelf.BuildConfig
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.activity.ImagePickerActivity
import com.ternaryop.photoshelf.activity.TagPhotoBrowserActivity
import com.ternaryop.photoshelf.adapter.feedly.FeedlyContentAdapter
import com.ternaryop.photoshelf.adapter.feedly.FeedlyContentDelegate
import com.ternaryop.photoshelf.adapter.feedly.OnFeedlyContentClick
import com.ternaryop.photoshelf.adapter.feedly.titles
import com.ternaryop.photoshelf.adapter.feedly.toContentDelegate
import com.ternaryop.photoshelf.adapter.feedly.update
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.post.titlesRequestBody
import com.ternaryop.photoshelf.dialogs.FeedlyCategoriesDialog
import com.ternaryop.photoshelf.dialogs.OnCloseDialogListener
import com.ternaryop.photoshelf.fragment.AbsPhotoShelfFragment
import com.ternaryop.photoshelf.fragment.BottomMenuSheetDialogFragment
import com.ternaryop.photoshelf.view.PhotoShelfSwipe
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import java.io.InputStreamReader

class FeedlyListFragment : AbsPhotoShelfFragment(), OnFeedlyContentClick {
    private lateinit var adapter: FeedlyContentAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var feedlyClient: FeedlyClient
    private lateinit var preferences: FeedlyPrefs
    private lateinit var photoShelfSwipe: PhotoShelfSwipe

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.saved_content_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView(view)

        setHasOptionsMenu(true)

        photoShelfSwipe = view.findViewById(R.id.swipe_container)
        photoShelfSwipe.setOnRefreshListener { refresh(true) }
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
        preferences = FeedlyPrefs(context!!)

        adapter.sortSwitcher.setType(preferences.getSortType())
        adapter.clickListener = this

        feedlyClient = FeedlyClient(
            preferences.accessToken ?: getString(R.string.FEEDLY_ACCESS_TOKEN),
            getString(R.string.FEEDLY_USER_ID),
            getString(R.string.FEEDLY_REFRESH_TOKEN))

        refresh(false)
    }

    private fun refresh(deleteItemsIfAllowed: Boolean) {
        // do not start another refresh if the current one is running
        if (photoShelfSwipe.isWaitingResult) {
            return
        }
        getFeedlyContentDelegate(deleteItemsIfAllowed)
            .compose(photoShelfSwipe.applySwipe())
            .subscribe(object : FeedlyObserver<List<FeedlyContentDelegate>>() {
                override fun onSuccess(posts: List<FeedlyContentDelegate>) {
                    setItems(posts)
                }
            })
    }

    private fun getFeedlyContentDelegate(deleteItemsIfAllowed: Boolean): Single<List<FeedlyContentDelegate>> {
        return readStreamContent(deleteItemsIfAllowed)
            .map { filterCategories(it) }
            .map { it.toContentDelegate() }
            .flatMap { list ->
                ApiManager.postService()
                    .getMapLastPublishedTimestampTag(blogName!!, titlesRequestBody(list.titles()))
                    .map {
                        list.update(it.response.pairs)
                        list
                    }
            }
    }

    private fun filterCategories(streamContent: StreamContent): List<SimpleFeedlyContent> {
        val selectedCategories = preferences.selectedCategoriesId

        if (selectedCategories.isEmpty()) {
            return streamContent.items
        }
        return streamContent.items.filter { sc ->
            sc.categories?.any { cat -> selectedCategories.any { cat.id == it } } ?: true
        }
    }

    private fun readStreamContent(deleteItemsIfAllowed: Boolean): Single<StreamContent> {
        return if (BuildConfig.DEBUG) {
            fakeCall()
        } else {
            deleteItems(deleteItemsIfAllowed).andThen(Single.defer { getNewerSavedContent() })
        }
    }

    private fun setItems(items: List<FeedlyContentDelegate>) {
        adapter.clear()
        adapter.addAll(items)
        adapter.sort()
        scrollToPosition(0)

        refreshUI()
    }

    private fun deleteItems(deleteItemsIfAllowed: Boolean): Completable {
        if (deleteItemsIfAllowed && preferences.deleteOnRefresh) {
            val idList = adapter.uncheckedItems.map { it.id }
            return feedlyClient.markSaved(idList, false)
        }
        return Completable.complete()
    }

    private fun getNewerSavedContent(): Single<StreamContent> {
        val ms = System.currentTimeMillis() - preferences.newerThanHours * ONE_HOUR_MILLIS
        val params = StreamContentFindParam(preferences.maxFetchItemCount, ms)
        return feedlyClient.getStreamContents(feedlyClient.globalSavedTag, params.toQueryMap())
    }

    private fun fakeCall(): Single<StreamContent> {
        return Single.fromCallable {
            context!!.assets.open("sample/feedly.json").use { stream ->
                GsonBuilder().create().fromJson(InputStreamReader(stream), StreamContent::class.java)
            }
        }
    }

    override fun refreshUI() {
        supportActionBar?.subtitle = subtitleFromTags()
    }

    private fun subtitleFromTags(): String {
        val groupedNotNullTags = adapter.allContents.filter { it.tag != null }.map { it.tag!! }.groupBy { it }
        val nullTags = adapter.allContents.count { it.tag == null }

        if (nullTags + groupedNotNullTags.size == adapter.itemCount) {
            return resources.getQuantityString(
                R.plurals.posts_count,
                adapter.itemCount,
                adapter.itemCount)
        }
        return resources.getQuantityString(
            R.plurals.tags_in_posts,
            groupedNotNullTags.size,
            groupedNotNullTags.size,
            adapter.itemCount)
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        if (childFragment !is BottomMenuSheetDialogFragment) {
            return
        }
        childFragment.menuListener = when (childFragment.tag) {
            FRAGMENT_TAG_SORT -> FeedlySortBottomMenuListener(this, adapter.sortSwitcher)
            else -> throw IllegalArgumentException("Invalid tag ${childFragment.tag}")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.feedly, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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
            R.id.action_sort -> {
                BottomMenuSheetDialogFragment().show(childFragmentManager, FRAGMENT_TAG_SORT)
                return true
            }
            R.id.action_feedly_categories -> {
                selectCategories()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun refreshToken() {
        feedlyClient.refreshAccessToken(
            getString(R.string.FEEDLY_CLIENT_ID),
            getString(R.string.FEEDLY_CLIENT_SECRET))
            .compose(photoShelfSwipe.applySwipe())
            .subscribe(object : FeedlyObserver<AccessToken>() {
                override fun onSuccess(accessToken: AccessToken) {
                    preferences.accessToken = accessToken.accessToken
                    feedlyClient.accessToken = preferences.accessToken ?: accessToken.accessToken
                    // hide swipe otherwise refresh() exists immediately
                    photoShelfSwipe.setRefreshingAndWaitingResult(false)
                    refresh(true)
                }
            })
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
        preferences.saveOtherSettings(
            Integer.parseInt(fetch.text.toString()),
            Integer.parseInt(newerThanHours.text.toString()),
            deleteOnRefresh.isChecked
        )
    }

    private fun fillSettingsView(view: View) {
        val fetchView = view.findViewById<EditText>(R.id.max_fetch_items_count)
        val newerThanHoursView = view.findViewById<EditText>(R.id.newer_than_hours)
        val deleteOnRefreshView = view.findViewById<CheckBox>(R.id.delete_on_refresh)

        fetchView.setText(preferences.maxFetchItemCount.toString())
        newerThanHoursView.setText(preferences.newerThanHours.toString())
        deleteOnRefreshView.isChecked = preferences.deleteOnRefresh
    }

    override fun onTitleClick(position: Int) {
        ImagePickerActivity.startImagePicker(context!!, adapter.getItem(position).originId)
    }

    override fun onTagClick(position: Int) {
        TagPhotoBrowserActivity.startPhotoBrowserActivity(context!!,
            blogName!!, adapter.getItem(position).tag!!, false)
    }

    override fun onToggleClick(position: Int, checked: Boolean) {
        if (preferences.deleteOnRefresh) {
            if (!checked) {
                adapter.moveToBottom(position)
            }
            return
        }
        val d = feedlyClient.markSaved(listOf(adapter.getItem(position).id), checked)
            .compose(photoShelfSwipe.applyCompletableSwipe())
            .subscribe({
                if (!checked) {
                    adapter.moveToBottom(position)
                }
            }) { t -> showSnackbar(makeSnake(recyclerView, t)) }
        compositeDisposable.add(d)
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

    fun sortBy(sortType: Int) {
        adapter.sortBy(sortType)
        adapter.notifyDataSetChanged()
        scrollToPosition(0)
        preferences.saveSortSettings(adapter.sortSwitcher.currentSortable.sortId,
            adapter.sortSwitcher.currentSortable.isAscending)
    }

    private fun selectCategories() {
        FeedlyCategoriesDialog(activity!!, feedlyClient, object: OnCloseDialogListener<FeedlyCategoriesDialog> {
            override fun onClose(source: FeedlyCategoriesDialog, button: Int): Boolean {
                if (button == DialogInterface.BUTTON_POSITIVE) {
                    refresh(false)
                }
                return true
            }
        }).show()
    }

    companion object {
        const val FRAGMENT_TAG_SORT = "sort"

        const val ONE_HOUR_MILLIS = 60 * 60 * 1000
    }
}
