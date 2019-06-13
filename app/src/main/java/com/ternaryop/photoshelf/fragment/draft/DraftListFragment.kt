package com.ternaryop.photoshelf.fragment.draft

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils.SECOND_IN_MILLIS
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ternaryop.photoshelf.DraftPostHelper
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.adapter.photo.PhotoSortSwitcher.Companion.LAST_PUBLISHED_TAG
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.db.TumblrPostCache
import com.ternaryop.photoshelf.db.TumblrPostCacheDAO
import com.ternaryop.photoshelf.dialogs.OnCloseDialogListener
import com.ternaryop.photoshelf.dialogs.SchedulePostDialog
import com.ternaryop.photoshelf.dialogs.TagNavigatorDialog
import com.ternaryop.photoshelf.event.CounterEvent
import com.ternaryop.photoshelf.fragment.AbsPostsListFragment
import com.ternaryop.photoshelf.fragment.BottomMenuSheetDialogFragment
import com.ternaryop.photoshelf.util.post.OnScrollPostFetcher
import com.ternaryop.photoshelf.util.post.PostActionExecutor
import com.ternaryop.photoshelf.util.post.PostActionExecutor.Companion.DELETE
import com.ternaryop.photoshelf.util.post.PostActionExecutor.Companion.EDIT
import com.ternaryop.photoshelf.util.post.PostActionExecutor.Companion.PUBLISH
import com.ternaryop.photoshelf.util.post.PostActionExecutor.Companion.SCHEDULE
import com.ternaryop.photoshelf.util.post.PostActionResult
import com.ternaryop.photoshelf.util.post.completedList
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.utils.date.millisecond
import com.ternaryop.utils.date.second
import com.ternaryop.utils.dialog.showErrorDialog
import com.ternaryop.widget.ProgressHighlightViewLayout
import com.ternaryop.widget.WaitingResultSwipeRefreshLayout
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.util.Calendar
import java.util.Date

class DraftListFragment : AbsPostsListFragment(), SwipeRefreshLayout.OnRefreshListener {
    private lateinit var queuedPosts: List<TumblrPost>
    private var lastScheduledDate: Calendar? = null
    private lateinit var swipeLayout: WaitingResultSwipeRefreshLayout

    private lateinit var progressHighlightViewLayout: ProgressHighlightViewLayout
    private lateinit var draftPostHelper: DraftPostHelper
    lateinit var draftCache: TumblrPostCacheDAO

    private val currentTextView: TextView
        get() = progressHighlightViewLayout.currentView as TextView

    override val actionModeMenuId: Int
        get() = R.menu.draft_context

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val draftEmptyView = View.inflate(context!!, R.layout.draft_empty_list, view as ViewGroup?)
        progressHighlightViewLayout = draftEmptyView.findViewById(android.R.id.empty)
        progressHighlightViewLayout.progressAnimation = AnimationUtils.loadAnimation(context!!, R.anim.fade_loop)
        photoAdapter.counterType = CounterEvent.DRAFT

        swipeLayout = view.findViewById(R.id.swipe_container)
        swipeLayout.setColorScheme(R.array.progress_swipe_colors)
        swipeLayout.setOnRefreshListener(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        photoAdapter.setOnPhotoBrowseClick(this)
        loadSettings()

        draftPostHelper = DraftPostHelper(context!!, blogName!!)
        draftCache = DBHelper.getInstance(context!!).tumblrPostCacheDAO

        refreshCache()
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        if (childFragment !is BottomMenuSheetDialogFragment) {
            return
        }
        childFragment.menuListener = when (childFragment.tag) {
            FRAGMENT_TAG_SORT -> DraftSortBottomMenuListener(this, photoAdapter.sortSwitcher)
            FRAGMENT_TAG_REFRESH -> DraftRefreshBottomMenuListener(this)
            else -> throw IllegalArgumentException("Invalid tag ${childFragment.tag}")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.draft, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> {
                BottomMenuSheetDialogFragment().show(childFragmentManager, FRAGMENT_TAG_REFRESH)
                return true
            }
            R.id.action_sort -> {
                BottomMenuSheetDialogFragment().show(childFragmentManager, FRAGMENT_TAG_SORT)
                return true
            }
            R.id.action_tag_navigator -> {
                fragmentManager?.also {
                    TagNavigatorDialog.newInstance(photoAdapter.photoList,
                        this, TAG_NAVIGATOR_DIALOG).show(it, FRAGMENT_TAG_NAVIGATOR)
                }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun refreshCache() {
        // do not start another refresh if the current one is running
        if (swipeLayout.isWaitingResult) {
            return
        }
        onRefreshStarted()

        val preferences = PreferenceManager.getDefaultSharedPreferences(context!!)
        currentTextView.text = context!!.resources.getString(R.string.start_import_title)
        val lastTimestamp = preferences.getLong(PREF_DRAFT_LAST_TIMESTAMP, -1)
        compositeDisposable.add(
            ApiManager.postService().getLastPublishedTimestamp(blogName!!, lastTimestamp)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { it.response }
            .subscribe({ last ->
                currentTextView.text = context!!.resources.getQuantityString(
                    R.plurals.posts_read_count,
                    last.importCount,
                    last.importCount)
                progressHighlightViewLayout.incrementProgress()
                preferences.edit().putLong(PREF_DRAFT_LAST_TIMESTAMP, last.lastPublishTimestamp).apply()
                // delete from cache the published posts
                last.publishedIdList?.let { draftCache.delete(it, TumblrPostCache.CACHE_TYPE_DRAFT, blogName!!) }
                fetchPosts(postFetcher)
            })
            { t ->
                onRefreshCompleted()
                t.showErrorDialog(context!!)
            }
        )
    }

    private fun onRefreshStarted() {
        photoAdapter.clear()
        progressHighlightViewLayout.visibility = View.VISIBLE
        progressHighlightViewLayout.startProgress()
        swipeLayout.setRefreshingAndWaitingResult(true)
    }

    private fun onRefreshCompleted() {
        swipeLayout.setRefreshingAndWaitingResult(false)
        progressHighlightViewLayout.stopProgress()
        progressHighlightViewLayout.visibility = View.GONE
    }

    override fun fetchPosts(listener: OnScrollPostFetcher) {
        val maxTimestamp = draftCache.findMostRecentTimestamp(blogName!!, TumblrPostCache.CACHE_TYPE_DRAFT)
        compositeDisposable.add(
            Single
                .zip<List<TumblrPost>, List<TumblrPost>, List<TumblrPost>>(
                    draftPostHelper.getNewerDraftPosts(maxTimestamp).subscribeOn(Schedulers.io()),
                    draftPostHelper.queuePosts.subscribeOn(Schedulers.io()),
                    BiFunction<List<TumblrPost>, List<TumblrPost>, List<TumblrPost>>
                    { newerDraftPosts, queuePosts -> this.getCachedPhotoPosts(newerDraftPosts, queuePosts) }
                )
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess { progressHighlightViewLayout.incrementProgress() }
                .observeOn(Schedulers.newThread())
                .flatMap { draftPosts ->
                    val tagsForDraftPosts = draftPostHelper.groupPostByTag(draftPosts)
                    val tagsForQueuePosts = draftPostHelper.groupPostByTag(queuedPosts)
                    draftPostHelper
                        .getTagLastPublishedMap(tagsForDraftPosts.keys)
                        .map { lastPublished ->
                            draftPostHelper.getPhotoShelfPosts(
                                tagsForDraftPosts,
                                tagsForQueuePosts,
                                lastPublished)
                        }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { disposable -> compositeDisposable.add(disposable) }
                .doFinally {
                    onRefreshCompleted()
                    refreshUI()
                }
                .subscribe({ posts ->
                    photoAdapter.addAll(posts)
                    photoAdapter.sort()
                }) { t -> t.showErrorDialog(context!!) }
        )
    }

    private fun getCachedPhotoPosts(newerDraftPosts: List<TumblrPost>, queuePosts: List<TumblrPost>): List<TumblrPost> {
        // save queue for future use
        this.queuedPosts = queuePosts

        // update the cache with new draft posts
        draftCache.write(newerDraftPosts, TumblrPostCache.CACHE_TYPE_DRAFT)
        // delete from the cache any post moved from draft to scheduled
        draftCache.delete(queuePosts, TumblrPostCache.CACHE_TYPE_DRAFT)
        // return the updated/cleaned up cache
        return draftCache.read(blogName!!, TumblrPostCache.CACHE_TYPE_DRAFT)
    }

    private fun showScheduleDialog(item: PhotoShelfPost) {
        SchedulePostDialog(context!!,
            item,
            findScheduleTime(),
            object : OnCloseDialogListener<SchedulePostDialog> {
                override fun onClose(source: SchedulePostDialog, button: Int): Boolean {
                    if (button == DialogInterface.BUTTON_NEGATIVE) {
                        return true
                    }
                    val d = postActionExecutor
                        .schedule(item, source.scheduleDateTime)
                        .doFinally { source.dismiss() }
                        .subscribe(
                            { },
                            { t -> t.showErrorDialog(context!!) })
                    compositeDisposable.add(d)
                    return false
                }
            }).show()
    }

    private fun findScheduleTime(): Calendar {
        val cal: Calendar

        if (lastScheduledDate == null) {
            val maxScheduledTime = queuedPosts.maxBy { it.scheduledPublishTime }
                ?.run { scheduledPublishTime * SECOND_IN_MILLIS } ?: System.currentTimeMillis()

            // Calendar.MINUTE isn't reset otherwise the calc may be inaccurate
            cal = Calendar.getInstance()
            cal.time = Date(maxScheduledTime)
            cal.second = 0
            cal.millisecond = 0
        } else {
            cal = lastScheduledDate!!.clone() as Calendar
        }
        // set next queued post time
        cal.add(Calendar.MINUTE, fragmentActivityStatus.appSupport.defaultScheduleMinutesTimeSpan)

        return cal
    }

    override fun handleMenuItem(item: MenuItem, postList: List<PhotoShelfPost>, mode: ActionMode?): Boolean {
        when (item.itemId) {
            R.id.post_schedule -> {
                showScheduleDialog(postList[0])
                return true
            }
        }
        return super.handleMenuItem(item, postList, mode)
    }

    override fun onRefresh() {
        refreshCache()
    }

    override fun onComplete(executor: PostActionExecutor, resultList: List<PostActionResult>) {
        val completedList = resultList.completedList()

        if (completedList.isNotEmpty()) {
            when (executor.postAction) {
                EDIT -> draftCache.updateItem(completedList[0].post, TumblrPostCache.CACHE_TYPE_DRAFT)
                PUBLISH, DELETE -> completedList.forEach { draftCache.deleteItem(it.post) }
                SCHEDULE -> onScheduleRefreshUI(completedList[0].post as PhotoShelfPost, executor.scheduleTimestamp)
            }
        }
        super.onComplete(executor, resultList)
    }

    private fun onScheduleRefreshUI(item: PhotoShelfPost, scheduledDateTime: Calendar?) {
        if (scheduledDateTime != null) {
            lastScheduledDate = scheduledDateTime.clone() as Calendar
            photoAdapter.removeAndRecalcGroups(item, scheduledDateTime)
        }
        draftCache.deleteItem(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            TAG_NAVIGATOR_DIALOG -> if (resultCode == Activity.RESULT_OK && data != null) {
                scrollToPosition(TagNavigatorDialog.findTagIndex(photoAdapter.photoList, data))
            }
        }
    }

    private fun scrollToPosition(position: Int) {
        // offset set to 0 put the item to the top
        (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
    }

    private fun loadSettings() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context!!)
        photoAdapter.sortSwitcher.setType(
            preferences.getInt(PREF_DRAFT_SORT_TYPE, LAST_PUBLISHED_TAG),
            preferences.getBoolean(PREF_DRAFT_SORT_ASCENDING, true))
    }

    private fun saveSettings() {
        PreferenceManager.getDefaultSharedPreferences(context!!)
            .edit()
            .putInt(PREF_DRAFT_SORT_TYPE, photoAdapter.sortSwitcher.sortable.sortId)
            .putBoolean(PREF_DRAFT_SORT_ASCENDING, photoAdapter.sortSwitcher.sortable.isAscending)
            .apply()
    }

    fun sortBy(sortType: Int, isAscending: Boolean) {
        photoAdapter.sortBy(sortType, isAscending)
        photoAdapter.notifyDataSetChanged()
        scrollToPosition(0)
        saveSettings()
    }

    companion object {
        private const val TAG_NAVIGATOR_DIALOG = 1

        const val FRAGMENT_TAG_REFRESH = "refresh"
        const val FRAGMENT_TAG_SORT = "sort"
        const val FRAGMENT_TAG_NAVIGATOR = "navigator"

        const val PREF_DRAFT_SORT_TYPE = "draft_sort_type"
        const val PREF_DRAFT_SORT_ASCENDING = "draft_sort_ascending"
        const val PREF_DRAFT_LAST_TIMESTAMP = "draft_last_timeStamp"
    }
}

