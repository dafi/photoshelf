package com.ternaryop.photoshelf.fragment

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import com.ternaryop.photoshelf.EXTRA_URL
import com.ternaryop.photoshelf.ImageUrlRetriever
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.activity.ImageViewerActivity
import com.ternaryop.photoshelf.adapter.ImagePickerAdapter
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice
import com.ternaryop.photoshelf.dialogs.TumblrPostDialog
import com.ternaryop.photoshelf.extractor.ImageGallery
import com.ternaryop.photoshelf.parsers.AndroidTitleParserConfig
import com.ternaryop.photoshelf.parsers.TitleParser
import com.ternaryop.photoshelf.view.AutofitGridLayoutManager
import com.ternaryop.utils.DialogUtils
import com.ternaryop.widget.ProgressHighlightViewLayout

const val MAX_DETAIL_LINES = 3

class ImagePickerFragment : AbsPhotoShelfFragment(), OnPhotoBrowseClickMultiChoice, ActionMode.Callback {
    private lateinit var gridView: RecyclerView
    private lateinit var progressHighlightViewLayout: ProgressHighlightViewLayout

    private lateinit var imageUrlRetriever: ImageUrlRetriever
    private lateinit var imagePickerAdapter: ImagePickerAdapter
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
            val intent = activity.intent
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
        activity.setTitle(R.string.image_picker_activity_title)

        progressHighlightViewLayout = rootView.findViewById(android.R.id.empty)
        progressHighlightViewLayout.animation = AnimationUtils.loadAnimation(activity, R.anim.fade_loop)

        imagePickerAdapter = ImagePickerAdapter(activity)
        imagePickerAdapter.setOnPhotoBrowseClick(this)
        imagePickerAdapter.setEmptyView(progressHighlightViewLayout)
        imageUrlRetriever = ImageUrlRetriever(activity, rootView.findViewById(R.id.progressbar))

        gridView = rootView.findViewById(R.id.gridview)
        gridView.adapter = imagePickerAdapter
        gridView.setHasFixedSize(true)
        gridView.layoutManager = AutofitGridLayoutManager(activity, resources.getDimension(R.dimen.image_picker_grid_width).toInt())

        setHasOptionsMenu(true)

        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        openUrl(textWithUrl)
    }

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
            AlertDialog.Builder(activity)
                .setTitle(R.string.url_not_found)
                .setMessage(getString(R.string.url_not_found_description, textWithUrl))
                .show()
        } else {
            readImageGallery(url)
        }
    }

    private fun readImageGallery(url: String) {
        imageUrlRetriever.readImageGallery(url)
                .doOnSubscribe { disposable -> compositeDisposable.add(disposable) }
                .subscribe({ this.onGalleryRetrieved(it) }
                ) { throwable -> DialogUtils.showErrorDialog(activity, throwable) }
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
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        this.actionMode = null
        imagePickerAdapter.showButtons = false
        imagePickerAdapter.getSelection().clear()
    }

    private fun retrieveImages(useFile: Boolean) {
        imageUrlRetriever.retrieve(imagePickerAdapter.selectedItems, useFile)
                .doOnSubscribe { disposable -> compositeDisposable.add(disposable) }
                .toList()
                .subscribe({ this.onImagesRetrieved(it) }
                ) { throwable -> DialogUtils.showSimpleMessageDialog(activity, R.string.url_not_found, throwable.localizedMessage) }
    }

    private fun onImagesRetrieved(imageUriList: List<Uri>) {
        try {
            val titleData = TitleParser.instance(AndroidTitleParserConfig(activity)).parseTitle(parsableTitle)
            val args = Bundle()

            args.putParcelableArrayList(TumblrPostDialog.ARG_IMAGE_URLS, ArrayList(imageUriList))
            args.putString(TumblrPostDialog.ARG_HTML_TITLE, titleData.toHtml())
            args.putString(TumblrPostDialog.ARG_SOURCE_TITLE, parsableTitle)

            args.putStringArrayList(TumblrPostDialog.ARG_INITIAL_TAG_LIST, ArrayList(titleData.tags))

            TumblrPostDialog.newInstance(args, null).show(fragmentManager, "dialog")
        } catch (e: Exception) {
            AlertDialog.Builder(activity)
                    .setTitle(R.string.parsing_error)
                    .setMessage(e.localizedMessage)
                    .show()
        }
    }

    private fun onGalleryRetrieved(imageGallery: ImageGallery) {
        progressHighlightViewLayout.stopProgress()
        detailsText = imageGallery.title
        parsableTitle = buildParsableTitle(imageGallery)
        showDetails(Snackbar.LENGTH_LONG)
        val imageInfoList = imageGallery.imageInfoList
        supportActionBar?.subtitle = resources.getQuantityString(R.plurals.image_found, imageInfoList.size, imageInfoList.size)
        imagePickerAdapter.addAll(imageInfoList)
    }

    override fun onTagClick(position: Int, clickedTag: String) {}

    override fun onThumbnailImageClick(position: Int) {
        val imageInfo = imagePickerAdapter.getItem(position)
        val imageUrl = imageInfo.imageUrl
        if (imageUrl == null) {
            imageUrlRetriever.retrieve(listOf(imageInfo), false)
                    .doOnSubscribe { disposable -> compositeDisposable.add(disposable) }
                    .take(1)
                    .subscribe({ uri ->
                        // cache retrieved value
                        val url = uri.toString()
                        imageInfo.imageUrl = url
                        ImageViewerActivity.startImageViewer(activity, url)
                    }
                    ) { throwable -> DialogUtils.showSimpleMessageDialog(activity, R.string.url_not_found, throwable.localizedMessage) }
        } else {
            ImageViewerActivity.startImageViewer(activity, imageUrl)
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
            actionMode = activity.startActionMode(this)
        }
        updateSelection(position)
    }

    private fun updateSelection(position: Int) {
        val selection = imagePickerAdapter.getSelection()
        selection.toggle(position)
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
        sbView.setBackgroundColor(ContextCompat.getColor(activity, R.color.image_picker_detail_text_bg))
        val textView = sbView.findViewById<TextView>(android.support.design.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(activity, R.color.image_picker_detail_text_text))
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
