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
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.ternaryop.feedly.FeedlyRateLimit
import com.ternaryop.feedly.TokenExpiredException
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.activity.ImagePickerActivity
import com.ternaryop.photoshelf.activity.TagPhotoBrowserActivity
import com.ternaryop.photoshelf.adapter.feedly.FeedlyContentAdapter
import com.ternaryop.photoshelf.adapter.feedly.OnFeedlyContentClick
import com.ternaryop.photoshelf.dialogs.FeedlyCategoriesDialog
import com.ternaryop.photoshelf.dialogs.OnCloseDialogListener
import com.ternaryop.photoshelf.fragment.AbsPhotoShelfFragment
import com.ternaryop.photoshelf.fragment.BottomMenuSheetDialogFragment
import com.ternaryop.photoshelf.lifecycle.EventObserver
import com.ternaryop.photoshelf.lifecycle.Status
import com.ternaryop.photoshelf.util.post.moveToBottom
import com.ternaryop.photoshelf.view.PhotoShelfSwipe
import java.util.Locale

private const val FRAGMENT_TAG_SORT = "sort"

class FeedlyListFragment : AbsPhotoShelfFragment(), OnFeedlyContentClick {
    private lateinit var adapter: FeedlyContentAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var preferences: FeedlyPrefs
    private lateinit var photoShelfSwipe: PhotoShelfSwipe
    private lateinit var viewModel: FeedlyViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_feedly_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView(view)

        setHasOptionsMenu(true)

        photoShelfSwipe = view.findViewById(R.id.swipe_container)
        photoShelfSwipe.setOnRefreshListener {
            photoShelfSwipe.setRefreshingAndWaitingResult(true)
            viewModel.contentList.clear()
            viewModel.content(blogName!!, getDeleteIdList(true))
        }
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

        viewModel = ViewModelProviders.of(this)
            .get(FeedlyViewModel::class.java)

        viewModel.result.observe(viewLifecycleOwner, EventObserver { result ->
            when (result) {
                is FeedlyModelResult.Content -> onContent(result)
                is FeedlyModelResult.MarkSaved -> onMarkSaved(result)
                is FeedlyModelResult.AccessTokenRefresh -> onAccessTokenRefreshed(result)
            }
        })

        photoShelfSwipe.setRefreshingAndWaitingResult(true)
        viewModel.content(blogName!!, null)
    }

    private fun onContent(result: FeedlyModelResult.Content) {
        when (result.command.status) {
            Status.SUCCESS -> {
                photoShelfSwipe.setRefreshingAndWaitingResult(false)
                setItems(result)
            }
            Status.ERROR -> {
                photoShelfSwipe.setRefreshingAndWaitingResult(false)
                result.command.error?.also { showSnackbar(makeSnake(recyclerView, it)) }
            }
            Status.PROGRESS -> { }
        }
    }

    private fun onAccessTokenRefreshed(result: FeedlyModelResult.AccessTokenRefresh) {
        when (result.command.status) {
            Status.SUCCESS -> {
                // start a new refresh so the swipe refreshing is set to true
                photoShelfSwipe.setRefreshingAndWaitingResult(true)
                viewModel.contentList.clear()
                viewModel.content(blogName!!, getDeleteIdList(true))
            }
            Status.ERROR -> {
                photoShelfSwipe.setRefreshingAndWaitingResult(false)
                result.command.error?.also { showSnackbar(makeSnake(recyclerView, it)) }
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
                result.command.error?.also { showSnackbar(makeSnake(recyclerView, it)) }
            }
            Status.PROGRESS -> { }
        }
    }

    private fun setItems(result: FeedlyModelResult.Content) {
        result.command.data ?: return

        adapter.clear()
        adapter.addAll(result.command.data.list)
        adapter.sort()
        scrollToPosition(0)

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
                photoShelfSwipe.setRefreshingAndWaitingResult(true)
                viewModel.contentList.clear()
                viewModel.content(blogName!!, getDeleteIdList(true))
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
                viewModel.contentList.moveToBottom(position)
            }
            return
        }
        photoShelfSwipe.setRefreshingAndWaitingResult(true)
        viewModel.markSaved(MarkSavedData(listOf(adapter.getItem(position).id), checked, listOf(position)))
    }

    override fun makeSnake(view: View, t: Throwable): Snackbar {
        if (t is TokenExpiredException) {
            val snackbar = Snackbar.make(recyclerView, R.string.token_expired, Snackbar.LENGTH_INDEFINITE)
            snackbar
                .setActionTextColor(ContextCompat.getColor(context!!, R.color.snack_error_color))
                .setAction(resources.getString(R.string.refresh).toLowerCase(Locale.getDefault())) {
                    photoShelfSwipe.setRefreshingAndWaitingResult(true)
                    viewModel.refreshToken()
                }
            return snackbar
        }
        return super.makeSnake(view, t)
    }

    fun sortBy(sortType: Int) {
        adapter.sortBy(sortType)
        adapter.notifyDataSetChanged()
        scrollToPosition(0)
        preferences.saveSortSettings(adapter.sortSwitcher.currentSortable.sortId,
            adapter.sortSwitcher.currentSortable.isAscending)
    }

    private fun selectCategories() {
        fragmentManager?.also {
            FeedlyCategoriesDialog.newInstance(object : OnCloseDialogListener<FeedlyCategoriesDialog> {
                override fun onClose(source: FeedlyCategoriesDialog, button: Int): Boolean {
                    if (button == DialogInterface.BUTTON_POSITIVE) {
                        photoShelfSwipe.setRefreshingAndWaitingResult(true)
                        viewModel.contentList.clear()
                        viewModel.content(blogName!!, getDeleteIdList(false))
                    }
                    return true
                }
            }).show(it, "dialog")
        }
    }
}
