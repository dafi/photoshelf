package com.ternaryop.photoshelf.fragment

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.annotation.PluralsRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.util.Pair
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import com.ternaryop.photoshelf.EXTRA_POST
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.activity.ImageViewerActivity
import com.ternaryop.photoshelf.activity.TagPhotoBrowserActivity
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice
import com.ternaryop.photoshelf.adapter.PhotoAdapter
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.db.TumblrPostCache
import com.ternaryop.photoshelf.dialogs.TumblrPostDialog
import com.ternaryop.photoshelf.view.ColorItemDecoration
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.utils.DialogUtils
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers

const val POST_ACTION_PUBLISH = 1
const val POST_ACTION_DELETE = 2
const val POST_ACTION_EDIT = 3
const val POST_ACTION_SAVE_AS_DRAFT = 4

abstract class AbsPostsListFragment : AbsPhotoShelfFragment(), OnPhotoBrowseClickMultiChoice, SearchView.OnQueryTextListener, ActionMode.Callback {

    protected lateinit var photoAdapter: PhotoAdapter
    protected var offset: Int = 0
    protected var hasMorePosts: Boolean = false
    protected open var isScrolling: Boolean = false
    protected var totalPosts: Long = 0
    protected lateinit var recyclerView: RecyclerView
    protected var searchView: SearchView? = null

    open val singleSelectionMenuIds: IntArray by lazy {
        intArrayOf(R.id.post_schedule, R.id.post_edit, R.id.group_menu_image_dimension, R.id.show_post)
    }

    private var actionMode: ActionMode? = null
    protected lateinit var colorItemDecoration: ColorItemDecoration

    protected open val postListViewResource: Int
        get() = R.layout.fragment_photo_list

    protected abstract val actionModeMenuId: Int

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(postListViewResource, container, false)

        photoAdapter = PhotoAdapter(activity, LOADER_PREFIX_POSTS_THUMB)

        recyclerView = rootView.findViewById(R.id.list)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = photoAdapter
        colorItemDecoration = ColorItemDecoration()
        recyclerView.addItemDecoration(colorItemDecoration)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                val layoutManager = recyclerView!!.layoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItem = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

                val loadMore = totalItemCount > 0 && firstVisibleItem + visibleItemCount >= totalItemCount

                if (loadMore && hasMorePosts && !isScrolling) {
                    offset += Tumblr.MAX_POST_PER_REQUEST
                    readPhotoPosts()
                }
            }
        })

        setHasOptionsMenu(true)
        return rootView
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val isMenuVisible = !fragmentActivityStatus.isDrawerMenuOpen
        menu.setGroupVisible(R.id.menu_photo_action_bar, isMenuVisible)
        setupSearchView(menu)
        super.onPrepareOptionsMenu(menu)
    }

    protected abstract fun readPhotoPosts()

    fun finish(post: TumblrPhotoPost) {
        val data = Intent()
        data.putExtra(EXTRA_POST, post)
        // Activity finished ok, return the data
        activity.setResult(Activity.RESULT_OK, data)
        activity.finish()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.setTitle(R.string.select_posts)
        mode.subtitle = resources.getQuantityString(R.plurals.selected_items, 1, 1)
        val inflater = mode.menuInflater
        inflater.inflate(actionModeMenuId, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return true
    }

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

    private fun browseImageBySize(post: PhotoShelfPost) {
        val arrayAdapter = ArrayAdapter(
                activity,
                android.R.layout.select_dialog_item,
                post.firstPhotoAltSize!!)

        // Show the cancel button without setting a listener
        // because it isn't necessary
        val builder = AlertDialog.Builder(activity)
                .setTitle(getString(R.string.menu_header_show_image, post.firstTag))
                .setNegativeButton(android.R.string.cancel, null)

        builder.setAdapter(arrayAdapter
        ) { _, which ->
            val item = arrayAdapter.getItem(which)
            if (item != null) {
                ImageViewerActivity.startImageViewer(activity, item.url, post)
            }
        }
        builder.show()
    }

    private fun showPost(post: PhotoShelfPost) {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(post.postUrl)
        startActivity(i)
    }

    private fun showConfirmDialog(postAction: Int, mode: ActionMode?, postsList: List<PhotoShelfPost>) {
        val dialogClickListener = DialogInterface.OnClickListener { _, _ ->
            when (postAction) {
                POST_ACTION_PUBLISH -> publishPost(mode, postsList)
                POST_ACTION_DELETE -> deletePost(mode, postsList)
                POST_ACTION_SAVE_AS_DRAFT -> saveAsDraft(mode, postsList)
            }
        }

        val message = resources.getQuantityString(getActionConfirmStringId(postAction),
                postsList.size,
                postsList.size,
                postsList[0].firstTag)
        AlertDialog.Builder(activity)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, dialogClickListener)
                .setNegativeButton(android.R.string.no, null)
                .show()
    }

    @PluralsRes
    private fun getActionConfirmStringId(postAction: Int): Int {
        return when (postAction) {
            POST_ACTION_PUBLISH -> R.plurals.publish_post_confirm
            POST_ACTION_DELETE -> R.plurals.delete_post_confirm
            POST_ACTION_SAVE_AS_DRAFT -> R.plurals.save_to_draft_confirm
            else -> throw AssertionError("Invalid post action")
        }
    }

    private fun saveAsDraft(mode: ActionMode?, postList: List<PhotoShelfPost>) {
        colorItemDecoration.setColor(ContextCompat.getColor(activity, R.color.photo_item_animation_save_as_draft_bg))
        executePostAction(mode, postList, Consumer { post ->
            Tumblr.getSharedTumblr(activity).saveDraft(
                    blogName!!,
                    post.postId)
            DBHelper.getInstance(activity).tumblrPostCacheDAO.insertItem(post, TumblrPostCache.CACHE_TYPE_DRAFT)
            onPostAction(post, POST_ACTION_SAVE_AS_DRAFT, POST_ACTION_OK)
        })
    }

    private fun deletePost(mode: ActionMode?, postList: List<PhotoShelfPost>) {
        colorItemDecoration.setColor(ContextCompat.getColor(activity, R.color.photo_item_animation_delete_bg))
        executePostAction(mode, postList, Consumer { post ->
            Tumblr.getSharedTumblr(activity).deletePost(blogName!!, post.postId)
            DBHelper.getInstance(activity).postDAO.deleteById(post.postId)
            onPostAction(post, POST_ACTION_DELETE, POST_ACTION_OK)
        })
    }

    private fun publishPost(mode: ActionMode?, postList: List<PhotoShelfPost>) {
        colorItemDecoration.setColor(ContextCompat.getColor(activity, R.color.photo_item_animation_publish_bg))
        executePostAction(mode, postList, Consumer { post ->
            Tumblr.getSharedTumblr(activity).publishPost(blogName!!, post.postId)
            onPostAction(post, POST_ACTION_PUBLISH, POST_ACTION_OK)
        })
    }

    override fun refreshUI() {
        if (searchView != null && searchView!!.isIconified) {
            if (hasMorePosts) {
                supportActionBar!!.subtitle = getString(R.string.post_count_1_of_x,
                        photoAdapter.itemCount,
                        totalPosts)
            } else {
                supportActionBar!!.subtitle = resources.getQuantityString(
                        R.plurals.posts_count,
                        photoAdapter.itemCount,
                        photoAdapter.itemCount)
                photoAdapter.notifyCountChanged()
            }
        }

        // use post() to resolve the following error:
        // Cannot call this method in a scroll callback. Scroll callbacks might be run during a measure & layout pass where you cannot change theRecyclerView data.
        // Any method call that might change the structure of the RecyclerView or the adapter contents should be postponed to the next frame.
        recyclerView.post {
            // notifyDataSetChanged() can 'hide' the remove item animation started by notifyItemRemoved()
            // so we wait for finished animations before call it
            recyclerView.itemAnimator.isRunning { photoAdapter.notifyDataSetChanged() }
        }
    }

    private fun updateUIAfterPostAction(mode: ActionMode?, postsWithError: List<Pair<PhotoShelfPost, Throwable?>>) {
        refreshUI()
        // all posts have been deleted so call actionMode.finish()
        if (postsWithError.isEmpty()) {
            if (mode != null) {
                // when action mode is on the finish() method could be called while the item animation is running stopping it
                // so we wait the animation is completed and then call finish()
                recyclerView.post { recyclerView.itemAnimator.isRunning({ mode.finish() }) }
            }
            return
        }
        // leave posts not processed checked
        photoAdapter.selection.clear()
        for (pair in postsWithError) {
            val position = photoAdapter.getPosition(pair.first)
            photoAdapter.selection.setSelected(position, true)
        }
        DialogUtils.showSimpleMessageDialog(activity,
                R.string.generic_error,
                activity.resources.getQuantityString(
                        R.plurals.general_posts_error,
                        postsWithError.size,
                        postsWithError[postsWithError.size - 1].second?.message,
                        postsWithError.size))
    }

    private fun executePostAction(mode: ActionMode?, postList: List<PhotoShelfPost>, consumer: Consumer<PhotoShelfPost>) {
        compositeDisposable.add(Observable
                .fromIterable(postList)
                .flatMap<Pair<PhotoShelfPost, Throwable?>> { post ->
                    try {
                        consumer.accept(post)
                        Observable.just(Pair.create(post, null as Throwable?))
                    } catch (e: Throwable) {
                        Observable.just(Pair.create(post, e))
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { pair ->
                    if (pair.second == null) {
                        photoAdapter.remove(pair.first)
                    }
                }
                .filter { pair -> pair.second != null }
                .toList()
                .subscribe { postsWithError -> updateUIAfterPostAction(mode, postsWithError) })
    }

    override fun onTagClick(position: Int, clickedTag: String) {
        TagPhotoBrowserActivity.startPhotoBrowserActivity(activity, blogName!!, clickedTag, false)
    }

    override fun onThumbnailImageClick(position: Int) {
        val post = photoAdapter.getItem(position)
        ImageViewerActivity.startImageViewer(activity, post.firstPhotoAltSize!![0].url, post)
    }

    override fun onOverflowClick(position: Int, view: View) {
        val popupMenu = PopupMenu(activity, view)
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
            actionMode = activity.startActionMode(this)
        }
        updateSelection(position)
    }

    private fun handleClickedThumbnail(position: Int) {
        val post = photoAdapter.getItem(position)
        if (activity.callingActivity == null) {
            onThumbnailImageClick(position)
        } else {
            finish(post)
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

    protected open fun handleMenuItem(item: MenuItem, postList: List<PhotoShelfPost>, mode: ActionMode? = null): Boolean {
        when (item.itemId) {
            R.id.post_publish -> {
                showConfirmDialog(POST_ACTION_PUBLISH, mode, postList)
                return true
            }
            R.id.group_menu_image_dimension -> {
                browseImageBySize(postList[0])
                return true
            }
            R.id.post_delete -> {
                showConfirmDialog(POST_ACTION_DELETE, mode, postList)
                return true
            }
            R.id.post_edit -> {
                showEditDialog(postList[0], mode)
                return true
            }
            R.id.post_save_draft -> {
                showConfirmDialog(POST_ACTION_SAVE_AS_DRAFT, mode, postList)
                return true
            }
            R.id.show_post -> {
                showPost(postList[0])
                return true
            }
            else -> return false
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
        offset = 0
        totalPosts = 0
        hasMorePosts = true
        photoAdapter.clear()
        readPhotoPosts()
    }

    /**
     * Overridden (if necessary) by subclasses to be informed about post action result,
     * the default implementation does nothing
     * @param post the post processed by action
     * @param postAction the action executed
     * @param resultCode on success POST_ACTION_OK
     */
    open fun onPostAction(post: TumblrPhotoPost, postAction: Int, resultCode: Int) {}

    override fun onEditDone(dialog: TumblrPostDialog, post: TumblrPhotoPost, completable: Completable) {
        completable
                .doOnSubscribe { d -> compositeDisposable.add(d) }
                .subscribe({
                    super@AbsPostsListFragment.onEditDone(dialog, post, completable)
                    onPostAction(post, POST_ACTION_EDIT, POST_ACTION_OK)
                }) { t -> DialogUtils.showErrorDialog(activity, t) }
    }

    companion object {
        const val POST_ACTION_OK = -1

        private const val LOADER_PREFIX_POSTS_THUMB = "postsThumb"
    }
}
