package com.ternaryop.photoshelf.birthday.publisher.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ternaryop.photoshelf.EXTRA_POST
import com.ternaryop.photoshelf.activity.ImageViewerActivityStarter
import com.ternaryop.photoshelf.activity.TagPhotoBrowserData
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.photoshelf.birthday.R
import com.ternaryop.photoshelf.birthday.publisher.adapter.BirthdayPhotoAdapter
import com.ternaryop.photoshelf.birthday.service.BirthdayPublisherService
import com.ternaryop.photoshelf.fragment.AbsPhotoShelfFragment
import com.ternaryop.photoshelf.lifecycle.EventObserver
import com.ternaryop.photoshelf.lifecycle.Status
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.utils.dialog.showErrorDialog
import com.ternaryop.utils.recyclerview.AutofitGridLayoutManager
import com.ternaryop.widget.WaitingResultSwipeRefreshLayout
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.Locale

private const val PICK_IMAGE_REQUEST_CODE = 100

@AndroidEntryPoint
class BirthdayPublisherFragment(
    private val imageViewerActivityStarter: ImageViewerActivityStarter
) : AbsPhotoShelfFragment(),
    SwipeRefreshLayout.OnRefreshListener, OnPhotoBrowseClickMultiChoice, ActionMode.Callback {

    private lateinit var birthdayPhotoAdapter: BirthdayPhotoAdapter
    private lateinit var swipeLayout: WaitingResultSwipeRefreshLayout

    private val viewModel: BirthdayPublisherViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_birthday_publisher, container, false)

        birthdayPhotoAdapter = BirthdayPhotoAdapter(requireContext())
        birthdayPhotoAdapter.onPhotoBrowseClick = this

        val layout = AutofitGridLayoutManager(requireContext(),
            resources.getDimension(R.dimen.grid_layout_thumb_width).toInt())
        val gridView = rootView.findViewById<RecyclerView>(R.id.gridview)
        gridView.adapter = birthdayPhotoAdapter
        gridView.setHasFixedSize(true)
        gridView.layoutManager = layout

        swipeLayout = rootView.findViewById(R.id.swipe_container)
        swipeLayout.setOnRefreshListener(this)
        swipeLayout.isRefreshing = true
        refresh()

        setHasOptionsMenu(true)

        viewModel.result.observe(viewLifecycleOwner, EventObserver { result ->
            when (result) {
                is BirthdayPublisherModelResult.ListByDate -> onBirthdayList(result)
            }
        })

        return rootView
    }

    private fun refresh() {
        // do not start another refresh if the current one is running
        if (swipeLayout.isWaitingResult) {
            return
        }
        val count = birthdayPhotoAdapter.itemCount
        birthdayPhotoAdapter.clear()
        birthdayPhotoAdapter.notifyItemRangeRemoved(0, count)
        val now = Calendar.getInstance(Locale.US)
        viewModel.listByDate(now, requireBlogName)
        swipeLayout.setRefreshingAndWaitingResult(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.birthday_publisher, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.clearBirthdays()
                refresh()
                true
            }
            R.id.action_selectall -> {
                selectAll()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun selectAll() {
        if (actionMode == null) {
            actionMode = requireActivity().startActionMode(this)
        }
        birthdayPhotoAdapter.selectAll()
        updateSubTitle()
    }

    private fun publish(mode: ActionMode, publishAsDraft: Boolean) {
        val selectedBirthdays = ArrayList<Birthday>()
        val names = ArrayList<String>()

        for (pair in birthdayPhotoAdapter.selectedItems) {
            names.add(pair.name)
            selectedBirthdays.add(pair)
        }

        BirthdayPublisherService.startPublish(
            requireContext(), selectedBirthdays, requireBlogName, publishAsDraft)
        Toast.makeText(requireContext(),
            getString(R.string.sending_cake_title, TextUtils.join(", ", names)),
            Toast.LENGTH_LONG).show()
        mode.finish()
    }

    override fun onItemClick(position: Int) {
        if (actionMode == null) {
            onThumbnailImageClick(position)
        } else {
            updateSelection(position)
        }
    }

    override fun onTagClick(position: Int, clickedTag: String) = Unit

    override fun onItemLongClick(position: Int) {
        if (actionMode == null) {
            actionMode = requireActivity().startActionMode(this)
        }
        birthdayPhotoAdapter.selection.toggle(position)
    }

    private fun updateSelection(position: Int) {
        val selection = birthdayPhotoAdapter.selection
        selection.toggle(position)
        if (selection.itemCount == 0) {
            actionMode?.finish()
        } else {
            updateSubTitle()
        }
    }

    private fun updateSubTitle() {
        val selection = birthdayPhotoAdapter.selection
        val selectionCount = selection.itemCount
        actionMode?.subtitle = resources.getQuantityString(
                R.plurals.selected_items_total,
                selectionCount,
                selectionCount,
                birthdayPhotoAdapter.itemCount)
    }

    override fun onThumbnailImageClick(position: Int) {
        val birthdate = birthdayPhotoAdapter.getItem(position)
        imageViewerActivityStarter.startTagPhotoBrowserForResult(this,
            PICK_IMAGE_REQUEST_CODE,
            TagPhotoBrowserData(blogName, birthdate.name, false))
    }

    override fun onOverflowClick(position: Int, view: View) = Unit

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE_REQUEST_CODE && data != null) {
            val post = data.getSerializableExtra(EXTRA_POST) as TumblrPhotoPost
            viewModel.updatePostByTag(post)?.also { birthdayPhotoAdapter.updatePost(it, true) }
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.title = getString(R.string.select_images)
        mode.subtitle = resources.getQuantityString(
                R.plurals.selected_items_total,
                1,
                1,
                birthdayPhotoAdapter.itemCount)
        mode.menuInflater.inflate(R.menu.birthday_publisher_context, menu)
        birthdayPhotoAdapter.isShowButtons = true
        birthdayPhotoAdapter.notifyItemRangeChanged(0, birthdayPhotoAdapter.itemCount)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        this.actionMode = null
        birthdayPhotoAdapter.isShowButtons = false
        birthdayPhotoAdapter.selection.clear()
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_publish -> {
                publish(mode, false)
                true
            }
            R.id.action_draft -> {
                publish(mode, true)
                true
            }
            else -> false
        }
    }

    override fun onRefresh() {
        refresh()
    }

    private fun onBirthdayList(result: BirthdayPublisherModelResult.ListByDate) {
        swipeLayout.setRefreshingAndWaitingResult(false)
        when (result.command.status) {
            Status.SUCCESS -> {
                result.command.data?.also { birthdays ->
                    birthdayPhotoAdapter.setBirthdays(birthdays)
                    birthdayPhotoAdapter.sort()
                    birthdayPhotoAdapter.notifyDataSetChanged()
                }
            }
            Status.ERROR -> {
                result.command.error?.also { it.showErrorDialog(requireContext()) }
            }
            Status.PROGRESS -> {
            }
        }
    }
}
