package com.ternaryop.photoshelf.feedly.fragment

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.feedly.FeedlyRateLimit
import com.ternaryop.photoshelf.activity.ImageViewerActivityStarter
import com.ternaryop.photoshelf.activity.TagPhotoBrowserData
import com.ternaryop.photoshelf.feedly.R
import com.ternaryop.photoshelf.feedly.adapter.FeedlyContentAdapter
import com.ternaryop.photoshelf.feedly.adapter.FeedlyContentViewHolder
import com.ternaryop.photoshelf.feedly.adapter.OnFeedlyContentClick
import com.ternaryop.photoshelf.feedly.dialog.FeedlyCategoriesDialog
import com.ternaryop.photoshelf.feedly.prefs.FeedlyPrefs
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

private const val CATEGORIES_DIALOG_REQUEST_KEY = "categoriesRequestKey"

@AndroidEntryPoint
class FeedlyListFragment(
    private val imageViewerActivityStarter: ImageViewerActivityStarter
) : AbsPhotoShelfFragment(),
    OnFeedlyContentClick,
    FragmentResultListener,
    MenuProvider {
    private lateinit var adapter: FeedlyContentAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var preferences: FeedlyPrefs
    private lateinit var photoShelfSwipe: PhotoShelfSwipe
    private lateinit var content: Content
    private val viewModel: FeedlyViewModel by viewModels()

    override val snackbarHolder: SnackbarHolder by lazy {
        FeedlySnackbarHolder(
            {
                photoShelfSwipe.setRefreshingAndWaitingResult(true)
                viewModel.refreshToken()
            },
            { refresh() }
        ).apply { lifecycle.addObserver(this) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_feedly_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView(view)

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        photoShelfSwipe = view.findViewById(R.id.swipe_container)
        photoShelfSwipe.setOnRefreshListener { refresh() }

        preferences = FeedlyPrefs(requireContext())

        adapter.sortSwitcher.setType(preferences.getSortType())
        adapter.clickListener = this

        adapter.setEmptyView(view.findViewById(android.R.id.empty)) { adapter ->
            updateApiCounters(view, adapter)
        }

        viewModel.result.observe(
            viewLifecycleOwner,
            EventObserver { result ->
                when (result) {
                    is FeedlyModelResult.Content -> onContent(result)
                    is FeedlyModelResult.MarkSaved -> onMarkSaved(result)
                    is FeedlyModelResult.AccessTokenRefresh -> onAccessTokenRefreshed(result)
                    else -> throw AssertionError("No valid $result")
                }
            }
        )

        photoShelfSwipe.setRefreshingAndWaitingResult(true)

        content = newContentFrom(arguments?.getSerializable(ARG_CONTENT_TYPE), viewModel)

        content.read(requireBlogName, null)

        parentFragmentManager.setFragmentResultListener(CATEGORIES_DIALOG_REQUEST_KEY, viewLifecycleOwner, this)
    }

    private fun initRecyclerView(rootView: View) {
        adapter = FeedlyContentAdapter(requireContext())

        recyclerView = rootView.findViewById(R.id.list)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.addToggleSwipe { position, isChecked ->
            onToggleClick(position, isChecked)
            adapter.notifyItemChanged(position)
        }
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
            Status.PROGRESS -> {}
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
            Status.PROGRESS -> {}
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
            Status.PROGRESS -> {}
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

        val text = if (nullTags + groupedNotNullTags.size == adapter.itemCount) {
            resources.getQuantityString(
                R.plurals.item_count,
                adapter.itemCount,
                adapter.itemCount
            )
        } else {
            resources.getQuantityString(
                R.plurals.tags_in_posts,
                groupedNotNullTags.size,
                groupedNotNullTags.size,
                adapter.itemCount
            )
        }
        return text + unreadCountText()
    }

    private fun unreadCountText(): String {
        val count = adapter.allContents.count { it.isChecked }
        return if (count < adapter.allContents.size) {
            ", " + resources.getString(R.string.unread_other_count, count)
        } else {
            ""
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        childFragmentManager.addFragmentOnAttachListener { _, childFragment ->
            if (childFragment is BottomMenuSheetDialogFragment) {
                childFragment.menuListener = when (childFragment.tag) {
                    FRAGMENT_TAG_SORT -> FeedlySortBottomMenuListener(this, adapter.sortSwitcher)
                    else -> throw IllegalArgumentException("Invalid tag ${childFragment.tag}")
                }
            }
        }
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.feedly, menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
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
            R.id.action_sort -> {
                BottomMenuSheetDialogFragment().show(childFragmentManager, FRAGMENT_TAG_SORT)
                return true
            }
            R.id.action_feedly_categories -> {
                selectCategories()
                return true
            }
            else -> return false
        }
    }

    private fun refresh() {
        photoShelfSwipe.setRefreshingAndWaitingResult(true)
        viewModel.contentList.clear()
        content.read(requireBlogName, getDeleteIdList(true))
    }

    private fun showAPIUsage() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.api_usage)
            .setMessage(
                getString(R.string.feedly_api_calls_count) + " " + FeedlyRateLimit.apiCallsCount +
                    "\n" + getString(R.string.feedly_api_reset_limit) + " " + FeedlyRateLimit.apiResetLimitAsString
            ).show()
    }

    override fun onTitleClick(position: Int) {
        val url = adapter.getItem(position).run { canonicalUrl ?: originId }
        imageViewerActivityStarter.startImagePicker(requireContext(), url)
        val urls = adapter.allContents.near(position, preferences.pickerFetchItemCount)
            .map { it.canonicalUrl ?: it.originId }
        imageViewerActivityStarter.startImagePickerPrefetch(urls)
    }

    override fun onTagClick(position: Int) {
        val tag = adapter.getItem(position).tag ?: return
        requireContext().startActivity(
            imageViewerActivityStarter.tagPhotoBrowserIntent(
                requireContext(),
                TagPhotoBrowserData(requireBlogName, tag, false)
            )
        )
    }

    override fun onToggleClick(position: Int, checked: Boolean) {
        if (preferences.deleteOnRefresh) {
            if (checked) {
                adapter.notifyItemChanged(position)
            } else {
                if (!adapter.moveToBottom(position)) {
                    // position doesn't change but item can change its appearance (eg color, background, ...)
                    adapter.notifyItemChanged(position)
                }
                viewModel.contentList.moveToBottom(position)
            }
            refreshUI()
            return
        }
        photoShelfSwipe.setRefreshingAndWaitingResult(true)
        content.remove(MarkSavedData(listOf(adapter.getItem(position).id), checked, listOf(position)))
    }

    fun sortBy(sortType: Int) {
        adapter.sortBy(sortType)
        adapter.notifyDataSetChanged()
        recyclerView.scrollItemOnTopByPosition(0)
        preferences.saveSortSettings(
            adapter.sortSwitcher.currentSortable.sortId,
            adapter.sortSwitcher.currentSortable.isAscending
        )
    }

    private fun selectCategories() {
        FeedlyCategoriesDialog
            .newInstance(preferences.selectedCategoriesId, CATEGORIES_DIALOG_REQUEST_KEY)
            .show(parentFragmentManager, FRAGMENT_TAG_CATEGORIES)
    }

    private fun onSelected(selectedCategoriesId: Set<String>) {
        preferences.selectedCategoriesId = selectedCategoriesId
        photoShelfSwipe.setRefreshingAndWaitingResult(true)
        viewModel.contentList.clear()
        content.read(requireBlogName, getDeleteIdList(false))
    }

    @Suppress("UNCHECKED_CAST")
    override fun onFragmentResult(requestKey: String, result: Bundle) {
        when (requestKey) {
            CATEGORIES_DIALOG_REQUEST_KEY -> onSelected(
                result.getSerializable(FeedlyCategoriesDialog.EXTRA_SELECTED_CATEGORIES_ID) as Set<String>
            )
        }
    }

    private fun updateApiCounters(
        view: View,
        adapter: RecyclerView.Adapter<FeedlyContentViewHolder>
    ): Boolean {
        if (adapter.itemCount == 0) {
            view.findViewById<TextView>(R.id.api_calls_count).text =
                FeedlyRateLimit.apiCallsCount.toString()
            view.findViewById<TextView>(R.id.api_reset_limit).text =
                FeedlyRateLimit.apiResetLimitAsString
        }
        return true
    }

    companion object {
        const val ARG_CONTENT_TYPE = "contentType"
    }
}
