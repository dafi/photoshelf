package com.ternaryop.photoshelf.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.ternaryop.photoshelf.EXTRA_POST
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.activity.TagPhotoBrowserActivity
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice
import com.ternaryop.photoshelf.adapter.photo.GridViewPhotoAdapter
import com.ternaryop.photoshelf.api.birthday.BirthdayManager
import com.ternaryop.photoshelf.event.BirthdayEvent
import com.ternaryop.photoshelf.service.PublishIntentService
import com.ternaryop.photoshelf.view.AutofitGridLayoutManager
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.widget.WaitingResultSwipeRefreshLayout
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Calendar
import java.util.Locale

private const val PICK_IMAGE_REQUEST_CODE = 100

class BirthdaysPublisherFragment
    : AbsPhotoShelfFragment(),
    SwipeRefreshLayout.OnRefreshListener, OnPhotoBrowseClickMultiChoice, ActionMode.Callback {

    private lateinit var gridViewPhotoAdapter: GridViewPhotoAdapter
    private lateinit var swipeLayout: WaitingResultSwipeRefreshLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_birthdays_publisher, container, false)

        gridViewPhotoAdapter = GridViewPhotoAdapter(context!!)
        gridViewPhotoAdapter.onPhotoBrowseClick = this

        val layout = AutofitGridLayoutManager(context!!,
            resources.getDimension(R.dimen.grid_layout_thumb_width).toInt())
        val gridView = rootView.findViewById<RecyclerView>(R.id.gridview)
        gridView.adapter = gridViewPhotoAdapter
        gridView.setHasFixedSize(true)
        gridView.layoutManager = layout

        swipeLayout = rootView.findViewById(R.id.swipe_container)
        swipeLayout.setOnRefreshListener(this)
        swipeLayout.isRefreshing = true
        refresh()

        setHasOptionsMenu(true)

        return rootView
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    private fun refresh() {
        // do not start another refresh if the current one is running
        if (swipeLayout.isWaitingResult) {
            return
        }
        val count = gridViewPhotoAdapter.itemCount
        gridViewPhotoAdapter.clear()
        gridViewPhotoAdapter.notifyItemRangeRemoved(0, count)
        val now = Calendar.getInstance(Locale.US)
        PublishIntentService.startBirthdayListIntent(context!!, now, blogName!!)
        swipeLayout.setRefreshingAndWaitingResult(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.birtdays_publisher, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
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
            actionMode = activity!!.startActionMode(this)
        }
        gridViewPhotoAdapter.selectAll()
        updateSubTitle()
    }

    private fun publish(mode: ActionMode, publishAsDraft: Boolean) {
        val selectedBirthdays = ArrayList<BirthdayManager.Birthday>()
        val names = ArrayList<String>()

        for (pair in gridViewPhotoAdapter.selectedItems) {
            names.add(pair.name)
            selectedBirthdays.add(pair)
        }

        PublishIntentService.startPublishBirthdayIntent(context!!, selectedBirthdays, blogName!!, publishAsDraft)
        Toast.makeText(context!!,
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

    override fun onTagClick(position: Int, clickedTag: String) {}

    override fun onItemLongClick(position: Int) {
        if (actionMode == null) {
            actionMode = activity!!.startActionMode(this)
        }
        gridViewPhotoAdapter.selection.toggle(position)
    }

    private fun updateSelection(position: Int) {
        val selection = gridViewPhotoAdapter.selection
        selection.toggle(position)
        if (selection.itemCount == 0) {
            actionMode?.finish()
        } else {
            updateSubTitle()
        }
    }

    private fun updateSubTitle() {
        val selection = gridViewPhotoAdapter.selection
        val selectionCount = selection.itemCount
        actionMode?.subtitle = resources.getQuantityString(
                R.plurals.selected_items_total,
                selectionCount,
                selectionCount,
                gridViewPhotoAdapter.itemCount)
    }

    override fun onThumbnailImageClick(position: Int) {
        val birthdate = gridViewPhotoAdapter.getItem(position)
        TagPhotoBrowserActivity.startPhotoBrowserActivityForResult(this, blogName!!,
                birthdate.name,
                PICK_IMAGE_REQUEST_CODE,
                false)
    }

    override fun onOverflowClick(position: Int, view: View) {}

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE_REQUEST_CODE && data != null) {
            val post = data.getSerializableExtra(EXTRA_POST) as TumblrPhotoPost
            gridViewPhotoAdapter.updatePostByTag(post, true)
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.title = getString(R.string.select_images)
        mode.subtitle = resources.getQuantityString(
                R.plurals.selected_items_total,
                1,
                1,
                gridViewPhotoAdapter.itemCount)
        mode.menuInflater.inflate(R.menu.birtdays_publisher_context, menu)
        gridViewPhotoAdapter.isShowButtons = true
        gridViewPhotoAdapter.notifyDataSetChanged()
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        this.actionMode = null
        gridViewPhotoAdapter.isShowButtons = false
        gridViewPhotoAdapter.selection.clear()
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBirthdayEvent(event: BirthdayEvent) {
        if (event.birthdayResult == null) {
            swipeLayout.setRefreshingAndWaitingResult(false)
            gridViewPhotoAdapter.sort()
            gridViewPhotoAdapter.notifyDataSetChanged()
        } else {
            val position = gridViewPhotoAdapter.itemCount
            event.birthdayResult.birthdates?.let { list ->
                gridViewPhotoAdapter.addAll(list)
                gridViewPhotoAdapter.notifyItemRangeInserted(position, list.size)
            }
        }
    }
}
