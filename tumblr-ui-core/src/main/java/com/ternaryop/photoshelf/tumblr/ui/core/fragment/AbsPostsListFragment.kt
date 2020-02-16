package com.ternaryop.photoshelf.tumblr.ui.core.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.EXTRA_POST
import com.ternaryop.photoshelf.activity.ImageViewerActivityStarter
import com.ternaryop.photoshelf.activity.ImageViewerData
import com.ternaryop.photoshelf.activity.TagPhotoBrowserData
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice
import com.ternaryop.photoshelf.fragment.AbsPhotoShelfFragment
import com.ternaryop.photoshelf.tumblr.dialog.EditPostEditorData
import com.ternaryop.photoshelf.tumblr.dialog.PostEditorResult
import com.ternaryop.photoshelf.tumblr.dialog.TumblrPostDialog
import com.ternaryop.photoshelf.tumblr.dialog.TumblrPostDialog.Companion.ARG_RESULT
import com.ternaryop.photoshelf.tumblr.dialog.TumblrPostDialog.Companion.EXTRA_THUMBNAILS_ITEMS
import com.ternaryop.photoshelf.tumblr.ui.core.R
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.photo.PhotoAdapter
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.OnPostActionListener
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.PostAction
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.PostActionColorItemDecoration
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.PostActionExecutor
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.PostActionResult
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.errorList
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.showConfirmDialog
import com.ternaryop.photoshelf.tumblr.ui.core.prefs.thumbnailWidth
import com.ternaryop.tumblr.TumblrAltSize.Companion.IMAGE_WIDTH_75
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.android.browseImageBySize
import com.ternaryop.tumblr.android.finishActivity
import com.ternaryop.tumblr.android.viewPost
import com.ternaryop.utils.dialog.DialogUtils
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

private const val PHOTO_POST_ID = "photoPostId"

abstract class AbsPostsListFragment(
    private val imageViewerActivityStarter: ImageViewerActivityStarter,
    val tumblrPostDialog: TumblrPostDialog
) : AbsPhotoShelfFragment(),
    OnPostActionListener,
    OnPhotoBrowseClickMultiChoice,
    SearchView.OnQueryTextListener,
    ActionMode.Callback {

    protected lateinit var photoAdapter: PhotoAdapter
    protected lateinit var recyclerView: RecyclerView
    protected var searchView: SearchView? = null

    private var recyclerViewLayout: Parcelable? = null

    val onConfirm: (PostAction) -> Unit = { postAction -> launch { postActionExecutor.execute(postAction) } }

    open val singleSelectionMenuIds: IntArray by lazy {
        intArrayOf(R.id.post_edit, R.id.group_menu_image_dimension, R.id.show_post)
    }

    protected val postActionExecutor: PostActionExecutor by inject()
    private lateinit var postActionColorItemDecoration: PostActionColorItemDecoration

    protected abstract val actionModeMenuId: Int
    protected abstract val actionBarGroupMenuId: Int

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_photo_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val thumbnailWidth = PreferenceManager.getDefaultSharedPreferences(context)
            .thumbnailWidth(resources.getInteger(R.integer.thumbnail_width_value_default))

        photoAdapter = PhotoAdapter(requireContext(), thumbnailWidth)

        postActionExecutor.onPostActionListener = this
        postActionColorItemDecoration = PostActionColorItemDecoration(requireContext())

        recyclerView = view.findViewById(R.id.list)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = photoAdapter
        recyclerView.addItemDecoration(postActionColorItemDecoration)

        recyclerViewLayout = savedInstanceState?.getParcelable(KEY_STATE_RECYCLER_VIEW_LAYOUT)

        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.getItem(0)?.groupId?.also {
            val isMenuVisible = !fragmentActivityStatus.isDrawerMenuOpen
            menu.setGroupVisible(actionBarGroupMenuId, isMenuVisible)
        }
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        saveRecyclerViewLayout(outState)
    }

    override fun refreshUI() {
        updateTitleBar()

        // use post() to resolve the following error:
        // Cannot call this method in a scroll callback.
        // Scroll callbacks might be run during a measure & layout pass where you cannot change theRecyclerView data.
        // Any method call that might change the structure of the RecyclerView or the adapter contents
        // should be postponed to the next frame.
        recyclerView.post {
            // notifyDataSetChanged() can 'hide' the remove item animation started by notifyItemRemoved()
            // so we wait for finished animations before call it
            recyclerView.itemAnimator?.isRunning { photoAdapter.notifyDataSetChanged() }
            restoreRecyclerViewLayout()
        }
    }

    protected open fun updateTitleBar() {
        supportActionBar?.subtitle = resources.getQuantityString(
            R.plurals.posts_count,
            photoAdapter.itemCount,
            photoAdapter.itemCount)
    }

    override fun onTagClick(position: Int, clickedTag: String) {
        imageViewerActivityStarter.startTagPhotoBrowser(requireContext(),
            TagPhotoBrowserData(blogName, clickedTag, false))
    }

    override fun onThumbnailImageClick(position: Int) {
        val post = photoAdapter.getItem(position)
        val altSize = checkNotNull(post.firstPhotoAltSize)
        imageViewerActivityStarter.startImageViewer(requireContext(),
            ImageViewerData(altSize[0].url, post.caption, post.firstTag))
    }

    override fun onOverflowClick(position: Int, view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
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
            actionMode = requireActivity().startActionMode(this)
        }
        updateSelection(position)
    }

    private fun handleClickedThumbnail(position: Int) {
        if (requireActivity().callingActivity == null) {
            return
//            onThumbnailImageClick(position)
        } else {
            photoAdapter.getItem(position).finishActivity(requireActivity(), EXTRA_POST)
        }
    }

    private fun updateSelection(position: Int) {
        val selection = photoAdapter.selection
        selection.toggle(position)
        if (selection.itemCount == 0) {
            actionMode?.finish()
        } else {
            updateMenuItems()
            val selectionCount = selection.itemCount
            actionMode?.subtitle = resources.getQuantityString(
                    R.plurals.selected_items,
                    selectionCount,
                    selectionCount)
        }
    }

    protected open fun handleMenuItem(
        item: MenuItem,
        postList: List<PhotoShelfPost>,
        mode: ActionMode? = null
    ): Boolean {
        val blogName = blogName ?: return false

        return when (item.itemId) {
            R.id.group_menu_image_dimension -> {
                browseImageBySize(postList[0])
                true
            }
            R.id.post_delete -> {
                PostAction.Delete(blogName, postList).showConfirmDialog(requireContext(), onConfirm)
                true
            }
            R.id.post_edit -> {
                actionMode = mode
                showEditDialog(postList[0])
                true
            }
            R.id.show_post -> {
                postList[0].viewPost(this)
                true
            }
            else -> false
        }
    }

    private fun browseImageBySize(photo: PhotoShelfPost) {
        val title = getString(R.string.menu_header_show_image, photo.firstTag) + " (" + photo.postId + ")"
        photo.browseImageBySize(requireActivity(), title) { url, post ->
            imageViewerActivityStarter.startImageViewer(requireActivity(),
                ImageViewerData(url, post.caption, post.firstTag))
        }
    }

    protected open fun setupSearchView(menu: Menu): SearchView? {
        val searchMenu = menu.findItem(R.id.action_search)
        if (searchMenu != null) {
            searchView = (searchMenu.actionView as SearchView).also {
                it.queryHint = getString(R.string.enter_tag_hint)
                it.setOnQueryTextListener(this)
            }
        }
        return searchView
    }

    override fun onQueryTextChange(newText: String): Boolean {
        photoAdapter.filter.filter(newText)
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return true
    }

    override fun onStart(postAction: PostAction, executor: PostActionExecutor) {
        postActionColorItemDecoration.setColor(postAction)
    }

    override fun onComplete(postAction: PostAction, executor: PostActionExecutor, resultList: List<PostActionResult>) {
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
        DialogUtils.showSimpleMessageDialog(requireContext(),
            R.string.generic_error,
            requireContext().resources.getQuantityString(
                R.plurals.general_posts_error,
                errorList.size,
                errorList[errorList.size - 1].error?.message,
                errorList.size))
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

    override fun onNext(postAction: PostAction, executor: PostActionExecutor, result: PostActionResult) {
        when (postAction) {
            is PostAction.SaveAsDraft,
            is PostAction.Delete,
            is PostAction.Publish -> {
                if (!result.hasError() && result.post is PhotoShelfPost) {
                    photoAdapter.remove(result.post)
                    removeFromCache(result.post)
                }
            }
        }
    }

    abstract fun removeFromCache(post: PhotoShelfPost)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == EDIT_POST_REQUEST_CODE) {
            (data?.extras?.getSerializable(ARG_RESULT) as? PostEditorResult)?.also { onEdit(it) }
        }
    }

    private fun onEdit(postEditorResult: PostEditorResult) {
        val postId = checkNotNull(postEditorResult.extras?.get(PHOTO_POST_ID) as Long)
        val item = photoAdapter.findItem(postId) ?: return
        launch {
            with(postEditorResult) {
                postActionExecutor.execute(PostAction.Edit(blogName, item, htmlTitle, tags))
            }
        }
    }

    private fun showEditDialog(item: TumblrPhotoPost) {
        val data = EditPostEditorData(
            item.blogName,
            item.caption,
            item.caption,
            item.tags,
            mapOf(PHOTO_POST_ID to item.postId,
                EXTRA_THUMBNAILS_ITEMS to listOf(item.getClosestPhotoByWidth(IMAGE_WIDTH_75)?.url)))
        tumblrPostDialog.editPostEditor(data, this, EDIT_POST_REQUEST_CODE)
    }

    protected open fun restoreRecyclerViewLayout() {
        recyclerViewLayout?.also {
            recyclerView.layoutManager?.onRestoreInstanceState(recyclerViewLayout)
        }
        recyclerViewLayout = null
    }

    protected open fun saveRecyclerViewLayout(outState: Bundle) {
        recyclerView.layoutManager?.onSaveInstanceState()?.also {
            outState.putParcelable(KEY_STATE_RECYCLER_VIEW_LAYOUT, it)
        }
    }

    companion object {
        const val EDIT_POST_REQUEST_CODE = 1
        const val KEY_STATE_RECYCLER_VIEW_LAYOUT = "recyclerViewLayout"
    }
}
