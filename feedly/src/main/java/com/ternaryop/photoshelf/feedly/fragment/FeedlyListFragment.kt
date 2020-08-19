package com.ternaryop.photoshelf.feedly.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.feedly.FeedlyRateLimit
import com.ternaryop.photoshelf.activity.ImageViewerActivityStarter
import com.ternaryop.photoshelf.activity.TagPhotoBrowserData
import com.ternaryop.photoshelf.feedly.R
import com.ternaryop.photoshelf.feedly.adapter.FeedlyContentAdapter
import com.ternaryop.photoshelf.feedly.adapter.OnFeedlyContentClick
import com.ternaryop.photoshelf.feedly.dialog.FeedlyCategoriesDialog
import com.ternaryop.photoshelf.feedly.dialog.FeedlySettingsData
import com.ternaryop.photoshelf.feedly.dialog.FeedlySettingsDialog
import com.ternaryop.photoshelf.feedly.dialog.OnFeedlyCategoriesListener
import com.ternaryop.photoshelf.feedly.dialog.OnFeedlySettingsListener
import com.ternaryop.photoshelf.feedly.prefs.FeedlyPrefs
import com.ternaryop.photoshelf.feedly.prefs.fromSettings
import com.ternaryop.photoshelf.feedly.prefs.toSettings
import com.ternaryop.photoshelf.feedly.view.snackbar.FeedlySnackbarHolder
import com.ternaryop.photoshelf.fragment.AbsPhotoShelfFragment
import com.ternaryop.photoshelf.fragment.BottomMenuSheetDialogFragment
import com.ternaryop.photoshelf.lifecycle.EventObserver
import com.ternaryop.photoshelf.lifecycle.Status
import com.ternaryop.photoshelf.util.near
import com.ternaryop.photoshelf.util.post.moveToBottom
import com.ternaryop.photoshelf.view.PhotoShelfSwipe
import com.ternaryop.photoshelf.view.snackbar.SnackbarHolder
import com.ternaryop.utils.recyclerview.scrollItemOnTopByPosition
import dagger.hilt.android.AndroidEntryPoint

private const val FRAGMENT_TAG_SORT = "sort"
private const val FRAGMENT_TAG_CATEGORIES = "categories"
private const val FRAGMENT_TAG_SETTINGS = "settings"

private const val PICKER_FETCH_ITEM_COUNT = 3

@AndroidEntryPoint
class FeedlyListFragment(
    private val imageViewerActivityStarter: ImageViewerActivityStarter
) : AbsPhotoShelfFragment(),
    OnFeedlyContentClick,
    OnFeedlyCategoriesListener,
    OnFeedlySettingsListener {
    private lateinit var adapter: FeedlyContentAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var preferences: FeedlyPrefs
    private lateinit var photoShelfSwipe: PhotoShelfSwipe
    private val viewModel: FeedlyViewModel by viewModels()

    override val snackbarHolder: SnackbarHolder by lazy {
        FeedlySnackbarHolder(
            {
                photoShelfSwipe.setRefreshingAndWaitingResult(true)
                viewModel.refreshToken()
            },
            { refresh() })
            .apply { lifecycle.addObserver(this) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_feedly_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView(view)

        setHasOptionsMenu(true)

        photoShelfSwipe = view.findViewById(R.id.swipe_container)
        photoShelfSwipe.setOnRefreshListener { refresh() }
    }

    private fun initRecyclerView(rootView: View) {
        adapter = FeedlyContentAdapter(requireContext())

        recyclerView = rootView.findViewById(R.id.list)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        preferences = FeedlyPrefs(requireContext())

        adapter.sortSwitcher.setType(preferences.getSortType())
        adapter.clickListener = this

        viewModel.result.observe(viewLifecycleOwner, EventObserver { result ->
            when (result) {
                is FeedlyModelResult.Content -> onContent(result)
                is FeedlyModelResult.MarkSaved -> onMarkSaved(result)
                is FeedlyModelResult.AccessTokenRefresh -> onAccessTokenRefreshed(result)
            }
        })

        photoShelfSwipe.setRefreshingAndWaitingResult(true)
        viewModel.content(requireBlogName, null)
    }

    private fun onContent(result: FeedlyModelResult.Content) {
        when (result.command.status) {
            Status.SUCCESS -> {
                photoShelfSwipe.setRefreshingAndWaitingResult(false)
                setItems(result)
            }
            Status.ERROR -> {
                photoShelfSwipe.setRefreshingAndWaitingResult(false)
                snackbarHolder.show(recyclerView, result.command.error)
            }
            Status.PROGRESS -> { }
        }
    }

    private fun onAccessTokenRefreshed(result: FeedlyModelResult.AccessTokenRefresh) {
        when (result.command.status) {
            Status.SUCCESS -> {
                // start a new refresh so the swipe refreshing is set to true
                refresh()
            }
            Status.ERROR -> {
                photoShelfSwipe.setRefreshingAndWaitingResult(false)
                snackbarHolder.show(recyclerView, result.command.error)
            }
            Status.PROGRESS -> { }
        }
    }

    private fun onMarkSaved(result: FeedlyModelResult.MarkSaved) {
        when (result.command.status) {
            Status.SUCCESS -> {
                photoShelfSwipe.setRefreshingAndWaitingResult(false)
                result.command.data?.also { data ->
                    if (!data.checked) {
                        adapter.moveToBottom(data.positionList[0])
                        viewModel.contentList.moveToBottom(data.positionList[0])
                    }
                }
            }
            Status.ERROR -> {
                photoShelfSwipe.setRefreshingAndWaitingResult(false)
                snackbarHolder.show(recyclerView, result.command.error)
            }
            Status.PROGRESS -> { }
        }
    }

    private fun setItems(result: FeedlyModelResult.Content) {
        val data = result.command.data ?: return

        adapter.clear()
        adapter.addAll(data.list)
        adapter.sort()
        recyclerView.scrollItemOnTopByPosition(0)

        refreshUI()
    }

    private fun getDeleteIdList(deleteItemsIfAllowed: Boolean): List<String>? {
        if (deleteItemsIfAllowed && preferences.deleteOnRefresh) {
            return adapter.uncheckedItems.map { it.id }
        }
        return null
    }

    override fun refreshUI() {
        supportActionBar?.subtitle = subtitleFromTags()
    }

    private fun subtitleFromTags(): String {
        val groupedNotNullTags = adapter.allContents.mapNotNull { it.tag }.groupBy { it }
        val nullTags = adapter.allContents.count { it.tag == null }

        if (nullTags + groupedNotNullTags.size == adapter.itemCount) {
            return resources.getQuantityString(
                R.plurals.item_count,
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
                refresh()
                return true
            }
            R.id.action_api_usage -> {
                showAPIUsage()
                return true
            }
            R.id.action_refresh_token -> {
                photoShelfSwipe.setRefreshingAndWaitingResult(true)
                viewModel.refreshToken()
                return true
            }
            R.id.action_settings -> {
                FeedlySettingsDialog
                    .newInstance(preferences.toSettings(), this)
                    .show(parentFragmentManager, FRAGMENT_TAG_SETTINGS)
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

    private fun refresh() {
        photoShelfSwipe.setRefreshingAndWaitingResult(true)
        viewModel.contentList.clear()
        viewModel.content(requireBlogName, getDeleteIdList(true))
    }

    private fun showAPIUsage() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.api_usage)
            .setMessage(getString(R.string.feedly_api_calls_count, FeedlyRateLimit.apiCallsCount) +
                "\n" + getString(R.string.feedly_api_reset_limit, FeedlyRateLimit.apiResetLimitAsString))
            .show()
    }

    override fun onTitleClick(position: Int) {
        val url = adapter.getItem(position).run { canonicalUrl ?: originId }
        imageViewerActivityStarter.startImagePicker(requireContext(), url)
        val urls = adapter.allContents.near(position, PICKER_FETCH_ITEM_COUNT).map { it.canonicalUrl ?: it.originId }
        imageViewerActivityStarter.startImagePickerPrefetch(urls)
    }

    override fun onTagClick(position: Int) {
        val tag = adapter.getItem(position).tag ?: return
        imageViewerActivityStarter.startTagPhotoBrowser(requireContext(),
            TagPhotoBrowserData(requireBlogName, tag, false))
    }

    override fun onToggleClick(position: Int, checked: Boolean) {
        if (preferences.deleteOnRefresh) {
            if (!checked) {
                adapter.moveToBottom(position)
                viewModel.contentList.moveToBottom(position)
            }
            return
        }
        photoShelfSwipe.setRefreshingAndWaitingResult(true)
        viewModel.markSaved(MarkSavedData(listOf(adapter.getItem(position).id), checked, listOf(position)))
    }

    fun sortBy(sortType: Int) {
        adapter.sortBy(sortType)
        adapter.notifyDataSetChanged()
        recyclerView.scrollItemOnTopByPosition(0)
        preferences.saveSortSettings(adapter.sortSwitcher.currentSortable.sortId,
            adapter.sortSwitcher.currentSortable.isAscending)
    }

    private fun selectCategories() {
        FeedlyCategoriesDialog
            .newInstance(preferences.selectedCategoriesId, this)
            .show(parentFragmentManager, FRAGMENT_TAG_CATEGORIES)
    }

    override fun onSettings(dialog: DialogFragment, settingsData: FeedlySettingsData) {
        preferences.fromSettings(settingsData)
    }

    override fun onSelected(dialog: DialogFragment, selectedCategoriesId: Set<String>) {
        preferences.selectedCategoriesId = selectedCategoriesId
        photoShelfSwipe.setRefreshingAndWaitingResult(true)
        viewModel.contentList.clear()
        viewModel.content(requireBlogName, getDeleteIdList(false))
    }
}
