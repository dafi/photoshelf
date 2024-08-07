package com.ternaryop.photoshelf.tumblr.ui.draft.fragment

import android.content.Context
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ternaryop.compat.os.getSerializableCompat
import com.ternaryop.photoshelf.activity.ImageViewerActivityStarter
import com.ternaryop.photoshelf.fragment.BottomMenuSheetDialogFragment
import com.ternaryop.photoshelf.lifecycle.EventObserver
import com.ternaryop.photoshelf.lifecycle.Status
import com.ternaryop.photoshelf.tagnavigator.dialog.TagNavigatorDialog
import com.ternaryop.photoshelf.tagnavigator.dialog.TagNavigatorDialog.Companion.EXTRA_SELECTED_TAG
import com.ternaryop.photoshelf.tumblr.dialog.SchedulePostData
import com.ternaryop.photoshelf.tumblr.dialog.SchedulePostDialog
import com.ternaryop.photoshelf.tumblr.dialog.TumblrPostDialog
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.photo.saveSettings
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.switcher.AdapterSwitcherConfig
import com.ternaryop.photoshelf.tumblr.ui.core.fragment.AbsPostsListFragment
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.PostAction
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.PostActionExecutor
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.PostActionResult
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.completedList
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.showConfirmDialog
import com.ternaryop.photoshelf.tumblr.ui.draft.DraftCache
import com.ternaryop.photoshelf.tumblr.ui.draft.R
import com.ternaryop.photoshelf.tumblr.ui.draft.prefs.defaultScheduleMinutesTimeSpan
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.utils.dialog.showErrorDialog
import com.ternaryop.utils.recyclerview.scrollItemOnTopByPosition
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DraftListFragment(
    iav: ImageViewerActivityStarter,
    pd: TumblrPostDialog
) : AbsPostsListFragment(iav, pd),
    SwipeRefreshLayout.OnRefreshListener,
    FragmentResultListener {
    private lateinit var queuedPosts: List<TumblrPost>

    @Inject
    lateinit var draftCache: DraftCache
    private val viewModel: DraftListViewModel by viewModels()
    private lateinit var refreshHolder: RefreshHolder

    private val scheduleDate: ScheduleDate by lazy {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        ScheduleDate(prefs.defaultScheduleMinutesTimeSpan(requireContext()))
    }

    override val actionModeMenuId: Int
        get() = R.menu.draft_context

    override val actionBarGroupMenuId: Int
        get() = R.id.menu_photo_action_bar

    override val singleSelectionMenuIds: IntArray by lazy {
        intArrayOf(R.id.post_schedule) + super.singleSelectionMenuIds
    }

    override val adapterSwitcherConfig: AdapterSwitcherConfig
        get() = AdapterSwitcherConfig("DraftList", true)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val draftEmptyView = View.inflate(requireContext(), R.layout.draft_empty_list, view as ViewGroup?)
        refreshHolder = RefreshHolder(
            requireContext(),
            draftEmptyView.findViewById(android.R.id.empty),
            view.findViewById(com.ternaryop.photoshelf.tumblr.ui.core.R.id.swipe_container),
            this
        )

        viewModel.result.observe(
            viewLifecycleOwner,
            EventObserver { result ->
                when (result) {
                    is DraftListModelResult.FetchPosts -> onFetchPosts(result)
                }
            }
        )

        parentFragmentManager.setFragmentResultListener(TAG_NAVIGATOR_DIALOG_REQUEST_KEY, viewLifecycleOwner, this)
        parentFragmentManager.setFragmentResultListener(TAG_SCHEDULE_DIALOG_REQUEST_KEY, viewLifecycleOwner, this)

        fetchPosts(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        childFragmentManager.addFragmentOnAttachListener { _, childFragment ->
            if (childFragment is BottomMenuSheetDialogFragment) {
                when (childFragment.tag) {
                    FRAGMENT_TAG_SORT -> DraftSortBottomMenuListener(this, photoAdapter.sortSwitcher)
                    FRAGMENT_TAG_REFRESH -> DraftRefreshBottomMenuListener(this)
                    else -> null
                }?.let { childFragment.menuListener = it }
            }
        }
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.draft, menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
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
                TagNavigatorDialog.newInstance(
                    photoAdapter.tagArrayList(),
                    TAG_NAVIGATOR_DIALOG_REQUEST_KEY
                ).show(parentFragmentManager, FRAGMENT_TAG_NAVIGATOR)
                return true
            }
            else -> return super.onMenuItemSelected(item)
        }
    }

    fun fetchPosts(fetchCache: Boolean) {
        // do not start another refresh if the current one is running
        if (refreshHolder.isRefreshing) {
            return
        }
        if (!fetchCache) {
            viewModel.clearCache()
            photoAdapter.clear()
        }
        refreshHolder.onStarted()
        viewModel.fetchPosts(requireBlogName)
    }

    private fun onFetchPosts(result: DraftListModelResult.FetchPosts) {
        when (result.command.status) {
            Status.SUCCESS -> {
                refreshHolder.onCompleted()
                result.command.data?.also { data ->
                    this.queuedPosts = data.queuePosts
                    photoAdapter.setPosts(data.newerDraftPosts)
                    photoAdapter.sort()
                    refreshUI()
                }
            }
            Status.ERROR -> {
                refreshHolder.onCompleted()
                refreshUI()
                result.command.error?.also { it.showErrorDialog(requireContext()) }
            }
            Status.PROGRESS -> {
                result.command.progressData?.also { progressData ->
                    if (progressData.step == DraftListModelResult.PROGRESS_STEP_IMPORTED_POSTS) {
                        refreshHolder.progressIndicator.text = requireContext().resources.getQuantityString(
                            R.plurals.posts_read_count,
                            progressData.itemCount,
                            progressData.itemCount
                        )
                    }
                    refreshHolder.advanceProgressIndicator()
                }
            }
        }
    }

    private fun showScheduleDialog(item: PhotoShelfPost) {
        tumblrPostDialog.schedulePostDialog(item, scheduleDate.peekNext(queuedPosts), TAG_SCHEDULE_DIALOG_REQUEST_KEY)
            .show(parentFragmentManager, TAG_SCHEDULE_DIALOG)
    }

    private fun onSchedule(dialog: DialogFragment, schedulePostData: SchedulePostData) {
        val blogName = blogName ?: return
        launch {
            postActionExecutor.execute(
                PostAction.Schedule(blogName, schedulePostData.post, schedulePostData.dateTime)
            )
            dialog.dismiss()
        }
    }

    override fun handleMenuItem(item: MenuItem, postList: List<PhotoShelfPost>, mode: ActionMode?): Boolean {
        val blogName = blogName ?: return false

        return when (item.itemId) {
            R.id.post_schedule -> {
                showScheduleDialog(postList[0])
                true
            }
            R.id.post_publish -> {
                PostAction.Publish(blogName, postList).showConfirmDialog(requireContext(), onConfirm)
                true
            }
            else -> super.handleMenuItem(item, postList, mode)
        }
    }

    override fun onRefresh() {
        fetchPosts(false)
    }

    override fun onComplete(postAction: PostAction, executor: PostActionExecutor, resultList: List<PostActionResult>) {
        val completedList = resultList.completedList()

        if (completedList.isNotEmpty()) {
            when (postAction) {
                is PostAction.Edit -> draftCache.update(postAction.post)
                is PostAction.Publish,
                is PostAction.Delete -> completedList.forEach { draftCache.delete(it.post) }
                is PostAction.Schedule -> onScheduleRefreshUI(postAction)
                else -> throw AssertionError("No valid action $postAction")
            }
        }
        super.onComplete(postAction, executor, resultList)
    }

    private fun onScheduleRefreshUI(postAction: PostAction.Schedule) {
        this.scheduleDate.currentDate = postAction.scheduleDate
        val item = postAction.post as PhotoShelfPost
        photoAdapter.removeAndRecalcGroups(item, postAction.scheduleDate)
        draftCache.delete(item)
        removeFromCache(item)
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        when (requestKey) {
            TAG_NAVIGATOR_DIALOG_REQUEST_KEY ->
                result.getString(EXTRA_SELECTED_TAG)?.also { tag ->
                    (recyclerView.layoutManager as LinearLayoutManager)
                        .scrollToPositionWithOffset(photoAdapter.findTagIndex(tag), 1)
                }
            TAG_SCHEDULE_DIALOG_REQUEST_KEY -> result.getSerializableCompat(
                SchedulePostDialog.EXTRA_SCHEDULE_DATA,
                SchedulePostData::class.java
            )?.also { schedulePostData ->
                onSchedule(
                    parentFragmentManager.getFragment(result, SchedulePostDialog.EXTRA_DIALOG) as DialogFragment,
                    schedulePostData
                )
            }
        }
    }

    fun sortBy(sortType: Int, isAscending: Boolean) {
        photoAdapter.sortBy(sortType, isAscending)
        photoAdapter.notifyDataSetChanged()
        recyclerView.scrollItemOnTopByPosition(0)

        PreferenceManager.getDefaultSharedPreferences(recyclerView.context).edit().also {
            photoAdapterSwitcher.adapterGroup.adapter.sortSwitcher.saveSettings(
                adapterSwitcherConfig.prefNamePrefix,
                it
            )
        }.apply()
    }

    override fun removeFromCache(post: PhotoShelfPost) {
        viewModel.removeFromCache(post)
    }

    override fun updateTitleBar() {
        viewModel.updateCount(photoAdapter.itemCount)
        super.updateTitleBar()
    }

    companion object {
        private const val TAG_NAVIGATOR_DIALOG_REQUEST_KEY = "tagNavigatorRequestKey"
        private const val TAG_SCHEDULE_DIALOG_REQUEST_KEY = "scheduleRequestKey"
        private const val TAG_SCHEDULE_DIALOG = "scheduleDialog"

        const val FRAGMENT_TAG_REFRESH = "refresh"
        const val FRAGMENT_TAG_SORT = "sort"
        const val FRAGMENT_TAG_NAVIGATOR = "navigator"
    }
}
