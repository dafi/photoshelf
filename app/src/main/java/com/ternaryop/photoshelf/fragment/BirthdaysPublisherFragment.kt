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
import com.ternaryop.photoshelf.adapter.GridViewPhotoAdapter
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice
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
private const val LOADER_PREFIX = "mediumThumb"

class BirthdaysPublisherFragment : AbsPhotoShelfFragment(), SwipeRefreshLayout.OnRefreshListener, OnPhotoBrowseClickMultiChoice, ActionMode.Callback {

    private lateinit var gridViewPhotoAdapter: GridViewPhotoAdapter
    private lateinit var swipeLayout: WaitingResultSwipeRefreshLayout
    private var actionMode: ActionMode? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_birthdays_publisher, container, false)

        gridViewPhotoAdapter = GridViewPhotoAdapter(activity, LOADER_PREFIX)
        gridViewPhotoAdapter.setOnPhotoBrowseClick(this)

        val layout = AutofitGridLayoutManager(activity, resources.getDimension(R.dimen.grid_layout_thumb_width).toInt())
        val gridView = rootView.findViewById<RecyclerView>(R.id.gridview)
        gridView.adapter = gridViewPhotoAdapter
        gridView.setHasFixedSize(true)
        gridView.layoutManager = layout

        swipeLayout = rootView.findViewById(R.id.swipe_container)
        swipeLayout.setColorScheme(R.array.progress_swipe_colors)
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
        val now = Calendar.getInstance(Locale.US)
        PublishIntentService.startBirthdayListIntent(activity, now)
        swipeLayout.setRefreshingAndWaintingResult(true)
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
            actionMode = activity.startActionMode(this)
        }
        gridViewPhotoAdapter.selectAll()
        updateSubTitle()
    }

    private fun publish(mode: ActionMode, publishAsDraft: Boolean) {
        val posts = ArrayList<TumblrPhotoPost>()
        val names = ArrayList<String>()

        for (pair in gridViewPhotoAdapter.selectedItems) {
            names.add(pair.second.tags[0])
            posts.add(pair.second)
        }

        PublishIntentService.startPublishBirthdayIntent(activity, posts, blogName!!, publishAsDraft)
        Toast.makeText(activity, getString(R.string.sending_cake_title, TextUtils.join(", ", names)), Toast.LENGTH_LONG).show()
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
            actionMode = activity.startActionMode(this)
        }
        gridViewPhotoAdapter.getSelection().toggle(position)
    }

    private fun updateSelection(position: Int) {
        val selection = gridViewPhotoAdapter.getSelection()
        selection.toggle(position)
        if (selection.itemCount == 0) {
            actionMode?.finish()
        } else {
            updateSubTitle()
        }
    }

    private fun updateSubTitle() {
        val selection = gridViewPhotoAdapter.getSelection()
        val selectionCount = selection.itemCount
        actionMode?.subtitle = resources.getQuantityString(
                R.plurals.selected_items_total,
                selectionCount,
                selectionCount,
                gridViewPhotoAdapter.itemCount)
    }

    override fun onThumbnailImageClick(position: Int) {
        val post = gridViewPhotoAdapter.getItem(position).second
        TagPhotoBrowserActivity.startPhotoBrowserActivityForResult(this, blogName!!,
                post.tags[0],
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
        gridViewPhotoAdapter.getSelection().clear()
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
        swipeLayout.setRefreshingAndWaintingResult(false)
        gridViewPhotoAdapter.clear()
        gridViewPhotoAdapter.addAll(event.birthdayList)
        gridViewPhotoAdapter.notifyDataSetChanged()
    }
}
