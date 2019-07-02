package com.ternaryop.photoshelf.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.EXTRA_POST
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.activity.ImageViewerActivity
import com.ternaryop.photoshelf.activity.TagPhotoBrowserActivity
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.adapter.photo.PhotoAdapter
import com.ternaryop.photoshelf.dialogs.PostDialogData
import com.ternaryop.photoshelf.dialogs.TumblrPostDialog
import com.ternaryop.photoshelf.util.post.OnPostActionListener
import com.ternaryop.photoshelf.util.post.OnScrollPostFetcher
import com.ternaryop.photoshelf.util.post.PostActionExecutor
import com.ternaryop.photoshelf.util.post.PostActionExecutor.Companion.DELETE
import com.ternaryop.photoshelf.util.post.PostActionExecutor.Companion.PUBLISH
import com.ternaryop.photoshelf.util.post.PostActionExecutor.Companion.SAVE_AS_DRAFT
import com.ternaryop.photoshelf.util.post.PostActionResult
import com.ternaryop.photoshelf.util.post.errorList
import com.ternaryop.photoshelf.util.post.showErrorDialog
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.android.browseImageBySize
import com.ternaryop.tumblr.android.finishActivity
import com.ternaryop.tumblr.android.viewPost
import com.ternaryop.utils.dialog.showErrorDialog

abstract class AbsPostsListFragment : AbsPhotoShelfFragment(), OnPostActionListener, OnScrollPostFetcher.PostFetcher,
    OnPhotoBrowseClickMultiChoice, TumblrPostDialog.PostListener, SearchView.OnQueryTextListener, ActionMode.Callback {

    protected lateinit var photoAdapter: PhotoAdapter
    protected lateinit var recyclerView: RecyclerView
    protected var searchView: SearchView? = null

    protected lateinit var postFetcher: OnScrollPostFetcher

    open val singleSelectionMenuIds: IntArray by lazy {
        intArrayOf(R.id.post_schedule, R.id.post_edit, R.id.group_menu_image_dimension, R.id.show_post)
    }

    protected lateinit var postActionExecutor: PostActionExecutor

    protected abstract val actionModeMenuId: Int

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_photo_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoAdapter = PhotoAdapter(context!!)
        postActionExecutor = PostActionExecutor(context!!, blogName!!, this)

        postFetcher = OnScrollPostFetcher(this, Tumblr.MAX_POST_PER_REQUEST)

        recyclerView = view.findViewById(R.id.list)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context!!)
        recyclerView.adapter = photoAdapter
        recyclerView.addItemDecoration(postActionExecutor.colorItemDecoration)
        recyclerView.addOnScrollListener(postFetcher)

        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val isMenuVisible = !fragmentActivityStatus.isDrawerMenuOpen
        menu.setGroupVisible(R.id.menu_photo_action_bar, isMenuVisible)
        setupSearchView(menu)
        super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.setTitle(R.string.select_posts)
        mode.subtitle = resources.getQuantityString(R.plurals.selected_items, 1, 1)
        val inflater = mode.menuInflater
        inflater.inflate(actionModeMenuId, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = true

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return handleMenuItem(item, photoAdapter.selectedPosts, mode)
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        this.actionMode = null
        photoAdapter.selection.clear()
    }

    private fun updateMenuItems() {
        val selectCount = photoAdapter.selection.itemCount
        val singleSelection = selectCount == 1

        for (itemId in singleSelectionMenuIds) {
            actionMode?.menu?.findItem(itemId)?.isVisible = singleSelection
        }
    }

    private fun showConfirmDialog(postAction: Int, postsList: List<PhotoShelfPost>) {
        val dialogClickListener = DialogInterface.OnClickListener { _, _ ->
            when (postAction) {
                PUBLISH -> postActionExecutor.publish(postsList)
                DELETE -> postActionExecutor.delete(postsList)
                SAVE_AS_DRAFT -> postActionExecutor.saveAsDraft(postsList)
                else -> throw AssertionError("PostAction $postAction not supported")
            }
                .doOnSubscribe { d -> compositeDisposable.add(d) }
                .subscribe({}, {})
        }

        val message = resources.getQuantityString(PostActionExecutor.getConfirmStringId(postAction),
            postsList.size,
            postsList.size,
            postsList[0].firstTag)
        AlertDialog.Builder(context!!)
            .setMessage(message)
            .setPositiveButton(android.R.string.yes, dialogClickListener)
            .setNegativeButton(android.R.string.no, null)
            .show()
    }

    override fun refreshUI() {
        supportActionBar?.subtitle = if (postFetcher.hasMorePosts) {
            getString(R.string.post_count_1_of_x,
                photoAdapter.itemCount,
                postFetcher.totalPosts)
        } else {
            photoAdapter.notifyCountChanged()
            resources.getQuantityString(
                R.plurals.posts_count,
                photoAdapter.itemCount,
                photoAdapter.itemCount)
        }

        // use post() to resolve the following error:
        // Cannot call this method in a scroll callback.
        // Scroll callbacks might be run during a measure & layout pass where you cannot change theRecyclerView data.
        // Any method call that might change the structure of the RecyclerView or the adapter contents
        // should be postponed to the next frame.
        recyclerView.post {
            // notifyDataSetChanged() can 'hide' the remove item animation started by notifyItemRemoved()
            // so we wait for finished animations before call it
            recyclerView.itemAnimator?.isRunning { photoAdapter.notifyDataSetChanged() }
        }
    }

    override fun onTagClick(position: Int, clickedTag: String) {
        TagPhotoBrowserActivity.startPhotoBrowserActivity(context!!, blogName!!, clickedTag, false)
    }

    override fun onThumbnailImageClick(position: Int) {
        val post = photoAdapter.getItem(position)
        ImageViewerActivity.startImageViewer(context!!, post.firstPhotoAltSize!![0].url, post)
    }

    override fun onOverflowClick(position: Int, view: View) {
        val popupMenu = PopupMenu(context!!, view)
        popupMenu.menuInflater.inflate(actionModeMenuId, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { handleMenuItem(it, listOf(photoAdapter.getItem(position))) }
        popupMenu.show()
    }

    override fun onItemClick(position: Int) {
        if (actionMode == null) {
            handleClickedThumbnail(position)
        } else {
            updateSelection(position)
        }
    }

    override fun onItemLongClick(position: Int) {
        if (actionMode == null) {
            actionMode = activity!!.startActionMode(this)
        }
        updateSelection(position)
    }

    private fun handleClickedThumbnail(position: Int) {
        if (activity!!.callingActivity == null) {
//            onThumbnailImageClick(position)
        } else {
            photoAdapter.getItem(position).finishActivity(activity!!, EXTRA_POST)
        }
    }

    private fun updateSelection(position: Int) {
        val selection = photoAdapter.selection
        selection.toggle(position)
        if (selection.itemCount == 0) {
            actionMode!!.finish()
        } else {
            updateMenuItems()
            val selectionCount = selection.itemCount
            actionMode!!.subtitle = resources.getQuantityString(
                    R.plurals.selected_items,
                    selectionCount,
                    selectionCount)
        }
    }

    protected open fun handleMenuItem(item: MenuItem,
        postList: List<PhotoShelfPost>, mode: ActionMode? = null): Boolean {
        when (item.itemId) {
            R.id.post_publish -> {
                showConfirmDialog(PUBLISH, postList)
                return true
            }
            R.id.group_menu_image_dimension -> {
                browseImageBySize(postList[0])
                return true
            }
            R.id.post_delete -> {
                showConfirmDialog(DELETE, postList)
                return true
            }
            R.id.post_edit -> {
                showEditDialog(postList[0], mode)
                return true
            }
            R.id.post_save_draft -> {
                showConfirmDialog(SAVE_AS_DRAFT, postList)
                return true
            }
            R.id.show_post -> {
                postList[0].viewPost(this)
                return true
            }
            else -> return false
        }
    }

    private fun browseImageBySize(photo: PhotoShelfPost) {
        val title = getString(R.string.menu_header_show_image, photo.firstTag) + " (" + photo.postId + ")"
        photo.browseImageBySize(activity!!, title) { url, post ->
            ImageViewerActivity.startImageViewer(activity!!, url, post)
        }
    }

    protected open fun setupSearchView(menu: Menu): SearchView {
        val searchMenu = menu.findItem(R.id.action_search)
        if (searchMenu != null) {
            searchView = searchMenu.actionView as SearchView
            searchView!!.queryHint = getString(R.string.enter_tag_hint)
            searchView!!.setOnQueryTextListener(this)
        }
        return searchView!!
    }

    override fun onQueryTextChange(newText: String): Boolean {
        photoAdapter.filter.filter(newText)
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return true
    }

    protected fun resetAndReloadPhotoPosts() {
        postFetcher.reset()
        photoAdapter.clear()
        fetchPosts(postFetcher)
    }

    override fun onComplete(executor: PostActionExecutor, resultList: List<PostActionResult>) {
        refreshUI()
        val errorList = resultList.errorList()
        // all posts have been deleted so call actionMode.finish()
        if (errorList.isEmpty()) {
            if (actionMode != null) {
                // when action mode is on the finish() method could be called
                // while the item animation is running stopping it
                // so we wait the animation is completed and then call finish()
                recyclerView.post { recyclerView.itemAnimator?.isRunning { actionMode?.finish() } }
            }
            return
        }
        selectPosts(errorList)
        errorList.showErrorDialog(context!!)
    }

    private fun selectPosts(results: List<PostActionResult>) {
        // select posts only if there is an action mode
        if (actionMode == null) {
            return
        }
        photoAdapter.selection.clear()
        for (result in results) {
            val position = photoAdapter.getPosition(result.post as PhotoShelfPost)
            photoAdapter.selection.setSelected(position, true)
        }
    }

    override fun onNext(executor: PostActionExecutor, result: PostActionResult) {
        when (executor.postAction) {
            SAVE_AS_DRAFT, DELETE, PUBLISH -> {
                if (!result.hasError() && result.post is PhotoShelfPost) {
                    photoAdapter.remove(result.post)
                }
            }
        }
    }

    override fun onEdit(dialog: TumblrPostDialog, post: TumblrPhotoPost, selectedBlogName: String) {
        val d = postActionExecutor.edit(post, dialog.titleHolder.htmlTitle, dialog.tagsHolder.tags, selectedBlogName)
                .doOnSubscribe { d -> compositeDisposable.add(d) }
                .subscribe({ }, { t -> t.showErrorDialog(context!!) })
        compositeDisposable.add(d)
    }

    private fun showEditDialog(item: TumblrPhotoPost, mode: ActionMode?) {
        fragmentManager?.also {
            actionMode = mode
            TumblrPostDialog.newInstance(PostDialogData(item), this).show(it, "dialog")
        }
    }

}
