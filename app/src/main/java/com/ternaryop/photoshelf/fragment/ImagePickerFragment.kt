package com.ternaryop.photoshelf.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.google.android.material.snackbar.Snackbar
import com.ternaryop.photoshelf.EXTRA_URL
import com.ternaryop.photoshelf.ImageUrlRetriever
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.activity.ImageViewerActivity
import com.ternaryop.photoshelf.adapter.ImagePickerAdapter
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice
import com.ternaryop.photoshelf.api.extractor.ImageGallery
import com.ternaryop.photoshelf.api.extractor.ImageInfo
import com.ternaryop.photoshelf.dialogs.PostDialogData
import com.ternaryop.photoshelf.dialogs.TumblrPostDialog
import com.ternaryop.photoshelf.parsers.AndroidTitleParserConfig
import com.ternaryop.photoshelf.parsers.TitleParser
import com.ternaryop.utils.dialog.DialogUtils
import com.ternaryop.utils.recyclerview.AutofitGridLayoutManager
import com.ternaryop.widget.ProgressHighlightViewLayout

const val MAX_DETAIL_LINES = 3

class ImagePickerFragment : AbsPhotoShelfFragment(), OnPhotoBrowseClickMultiChoice, ActionMode.Callback {
    private lateinit var gridView: RecyclerView
    private lateinit var progressHighlightViewLayout: ProgressHighlightViewLayout

    private lateinit var imageUrlRetriever: ImageUrlRetriever
    private lateinit var imagePickerAdapter: ImagePickerAdapter
    private lateinit var constraintLayout: ConstraintLayout
    private lateinit var selectedItemsViewContainer: SelectedItemsViewContainer
    private var detailsText: String? = null
    private lateinit var parsableTitle: String

    // Search on fragment arguments
    private val textWithUrl: String?
        get() {
            val arguments = arguments
            if (arguments != null && arguments.containsKey(EXTRA_URL)) {
                return arguments.getString(EXTRA_URL)
            }
            // Search on activity intent
            // Get intent, action and MIME type
            val intent = activity!!.intent
            val action = intent.action
            val type = intent.type
            val uri = intent.data

            var textWithUrl: String? = null

            if (Intent.ACTION_SEND == action && type != null) {
                if ("text/plain" == type) {
                    textWithUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
                }
            } else if (Intent.ACTION_VIEW == action && uri != null) {
                textWithUrl = uri.toString()
            }
            return textWithUrl
        }

    private val currentTextView: TextView
        get() = progressHighlightViewLayout.currentView as TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_image_picker, container, false)
        activity!!.setTitle(R.string.image_picker_activity_title)

        progressHighlightViewLayout = rootView.findViewById(android.R.id.empty)
        progressHighlightViewLayout.progressAnimation = AnimationUtils.loadAnimation(context!!, R.anim.fade_loop)
        progressHighlightViewLayout.visibility = View.VISIBLE

        imagePickerAdapter = ImagePickerAdapter(context!!)
        imagePickerAdapter.setOnPhotoBrowseClick(this)
        imagePickerAdapter.setEmptyView(progressHighlightViewLayout)
        imageUrlRetriever = ImageUrlRetriever(context!!, rootView.findViewById(R.id.progressbar))

        gridView = rootView.findViewById(R.id.gridview)
        gridView.adapter = imagePickerAdapter
        gridView.setHasFixedSize(true)
        gridView.layoutManager = AutofitGridLayoutManager(context!!,
            resources.getDimension(R.dimen.image_picker_grid_width).toInt())
        // animating the constraintLayout results in grid animations, so we disable them
        gridView.itemAnimator = null

        constraintLayout = rootView.findViewById(R.id.constraintlayout)
        selectedItemsViewContainer = SelectedItemsViewContainer(
            context!!,
            constraintLayout,
            rootView.findViewById(R.id.selectedItems))
        selectedItemsViewContainer.adapter
            .setOnPhotoBrowseClick(object: OnPhotoBrowseClickMultiChoice {
            override fun onItemClick(position: Int) {
                val index = imagePickerAdapter.getIndex(selectedItemsViewContainer.adapter.getItem(position))
                gridView.layoutManager!!.scrollToPosition(index)
            }

            override fun onItemLongClick(position: Int) {}
            override fun onTagClick(position: Int, clickedTag: String) {}
            override fun onThumbnailImageClick(position: Int) {}
            override fun onOverflowClick(position: Int, view: View) {}
        })

        setHasOptionsMenu(true)

        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        refreshUI()
    }

    override fun refreshUI() = openUrl(textWithUrl)

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        compositeDisposable.clear()
    }

    private fun openUrl(textWithUrl: String?) {
        if (textWithUrl == null) {
            return
        }
        progressHighlightViewLayout.startProgress()
        val message = resources.getQuantityString(R.plurals.download_url_with_count, 1, 0)
        currentTextView.text = message
        val url = "(https?:.*)".toRegex().find(textWithUrl)?.value

        if (url == null) {
            AlertDialog.Builder(context!!)
                .setTitle(R.string.url_not_found)
                .setMessage(getString(R.string.url_not_found_description, textWithUrl))
                .show()
        } else {
            readImageGallery(url)
        }
    }

    private fun readImageGallery(url: String) {
        val d = imageUrlRetriever.readImageGallery(url)
                .doFinally { progressHighlightViewLayout.stopProgress() }
                .subscribe({ this.onGalleryRetrieved(it.response.gallery) }
                ) { t -> showSnackbar(makeSnack(gridView, t.localizedMessage) { refreshUI() }) }
        compositeDisposable.add(d)
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.title = getString(R.string.select_images)
        mode.subtitle = resources.getQuantityString(
                R.plurals.selected_items_total,
                1,
                1,
                imagePickerAdapter.itemCount)
        mode.menuInflater.inflate(R.menu.image_picker_context, menu)
        imagePickerAdapter.showButtons = true
        imagePickerAdapter.notifyDataSetChanged()
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.showDialog -> {
                retrieveImages(false)
                mode.finish()
                true
            }
            R.id.create_from_file -> {
                retrieveImages(true)
                mode.finish()
                true
            }
            R.id.show_selected_items -> {
                selectedItemsViewContainer.toggleVisibility()
                true
            }
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        this.actionMode = null
        imagePickerAdapter.showButtons = false
        imagePickerAdapter.selection.clear()
        selectedItemsViewContainer.updateList(emptyList())
    }

    private fun retrieveImages(useFile: Boolean) {
        val d = imageUrlRetriever.retrieve(imagePickerAdapter.selectedItems, useFile)
                .toList()
                .subscribe({ this.onImagesRetrieved(it) }
                ) { throwable ->
                    DialogUtils.showSimpleMessageDialog(context!!, R.string.url_not_found, throwable.localizedMessage) }
        compositeDisposable.add(d)
    }

    private fun onImagesRetrieved(imageUriList: List<Uri>) {
        try {
            val titleData = TitleParser.instance(AndroidTitleParserConfig(context!!)).parseTitle(parsableTitle)
            TumblrPostDialog.newInstance(PostDialogData(parsableTitle,
                titleData.toHtml(), titleData.tags, imageUriList), null).show(fragmentManager, "dialog")
        } catch (e: Exception) {
            AlertDialog.Builder(context!!)
                    .setTitle(R.string.parsing_error)
                    .setMessage(e.localizedMessage)
                    .show()
        }
    }

    private fun onGalleryRetrieved(imageGallery: ImageGallery) {
        detailsText = imageGallery.title
        parsableTitle = buildParsableTitle(imageGallery)
        showDetails(Snackbar.LENGTH_LONG)
        val imageInfoList = imageGallery.imageInfoList
        supportActionBar?.subtitle = resources.getQuantityString(R.plurals.image_found,
            imageInfoList.size, imageInfoList.size)
        imagePickerAdapter.addAll(imageInfoList)
    }

    override fun onTagClick(position: Int, clickedTag: String) {}

    override fun onThumbnailImageClick(position: Int) {
        val imageInfo = imagePickerAdapter.getItem(position)
        val imageUrl = imageInfo.imageUrl
        if (imageUrl == null) {
            val d = imageUrlRetriever.retrieve(listOf(imageInfo), false)
                    .take(1)
                    .subscribe({ uri ->
                        // cache retrieved value
                        val url = uri.toString()
                        imageInfo.imageUrl = url
                        ImageViewerActivity.startImageViewer(context!!, url)
                    }
                    ) { throwable -> DialogUtils.showSimpleMessageDialog(context!!,
                        R.string.url_not_found, throwable.localizedMessage) }
            compositeDisposable.add(d)
        } else {
            ImageViewerActivity.startImageViewer(context!!, imageUrl)
        }
    }

    override fun onOverflowClick(position: Int, view: View) {}

    override fun onItemClick(position: Int) {
        if (actionMode == null) {
            onThumbnailImageClick(position)
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

    private fun updateSelection(position: Int) {
        val selection = imagePickerAdapter.selection
        selection.toggle(position)
        selectedItemsViewContainer.updateList(imagePickerAdapter.selectedItems)
        if (selection.itemCount == 0) {
            actionMode!!.finish()
        } else {
            val selectionCount = selection.itemCount
            actionMode!!.subtitle = resources.getQuantityString(
                    R.plurals.selected_items_total,
                    selectionCount,
                    selectionCount,
                    imagePickerAdapter.itemCount)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.image_picker, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_image_viewer_details -> {
                showDetails(Snackbar.LENGTH_INDEFINITE)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDetails(duration: Int) {
        val snackbar = Snackbar.make(gridView, detailsText!!, duration)
        val sbView = snackbar.view
        sbView.setBackgroundColor(ContextCompat.getColor(context!!, R.color.image_picker_detail_text_bg))
        val textView = sbView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(context!!, R.color.image_picker_detail_text_text))
        textView.maxLines = MAX_DETAIL_LINES
        snackbar.show()
    }

    /**
     * Return a string that can be used by the title parser
     * @param imageGallery the source
     * @return the title plus domain string
     */
    private fun buildParsableTitle(imageGallery: ImageGallery): String {
        return imageGallery.title + " ::::: " + imageGallery.domain
    }
}

private class SelectedItemsViewContainer(
    val context: Context,
    private val constraintLayout: ConstraintLayout,
    selectionListView: RecyclerView) {
    val adapter = ImagePickerAdapter(context)
    private var isVisible = false
    private val constraintSet = ConstraintSet()

    init {
        selectionListView.adapter = adapter
        selectionListView.setHasFixedSize(true)
        selectionListView.layoutManager = LinearLayoutManager(context, HORIZONTAL, false)

        constraintSet.clone(context, R.layout.fragment_image_picker)
    }

    fun toggleVisibility() = show(!isVisible)

    fun show(show: Boolean) {
        isVisible = show
        if (isVisible) {
            constraintSet.clear(R.id.selectedItems, ConstraintSet.TOP)
        } else {
            constraintSet.connect(R.id.selectedItems, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        }

        val transition = ChangeBounds()
        transition.interpolator = AnticipateOvershootInterpolator(1.0f)
        transition.duration = 1000

        TransitionManager.beginDelayedTransition(constraintLayout, transition)

        constraintSet.applyTo(constraintLayout)
    }

    fun updateList(items: List<ImageInfo>) {
        adapter.clear()
        if (items.isEmpty()) {
            show(false)
        } else {
            adapter.addAll(items)
        }
    }

}