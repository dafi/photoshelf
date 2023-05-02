package com.ternaryop.photoshelf.imagepicker.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.ternaryop.photoshelf.activity.ImageViewerActivityStarter
import com.ternaryop.photoshelf.activity.ImageViewerData
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice
import com.ternaryop.photoshelf.api.extractor.ImageGallery
import com.ternaryop.photoshelf.fragment.AbsPhotoShelfFragment
import com.ternaryop.photoshelf.imagepicker.R
import com.ternaryop.photoshelf.imagepicker.adapter.ImagePickerAdapter
import com.ternaryop.photoshelf.imagepicker.service.OnPublish
import com.ternaryop.photoshelf.imagepicker.service.PostPublisherService
import com.ternaryop.photoshelf.lifecycle.EventObserver
import com.ternaryop.photoshelf.lifecycle.Status
import com.ternaryop.photoshelf.tumblr.dialog.NewPostEditorData
import com.ternaryop.photoshelf.tumblr.dialog.NewPostEditorResult
import com.ternaryop.photoshelf.tumblr.dialog.PostEditorActivityResultContracts
import com.ternaryop.photoshelf.tumblr.dialog.TumblrPostDialog
import com.ternaryop.photoshelf.tumblr.dialog.TumblrPostDialog.Companion.EXTRA_THUMBNAILS_ITEMS
import com.ternaryop.photoshelf.view.PhotoShelfSwipe
import com.ternaryop.utils.dialog.DialogUtils
import com.ternaryop.utils.dialog.showErrorDialog
import com.ternaryop.utils.recyclerview.AutofitGridLayoutManager
import com.ternaryop.widget.ProgressHighlightViewLayout
import dagger.hilt.android.AndroidEntryPoint

// The url can contain extraneous text
const val EXTRA_URL = "com.ternaryop.photoshelf.extra.URL"

@AndroidEntryPoint
class ImagePickerFragment(
    private val imageViewerActivityStarter: ImageViewerActivityStarter,
    tumblrPostDialog: TumblrPostDialog,
    private val publishClassName: Class<out OnPublish>
) : AbsPhotoShelfFragment(),
    OnPhotoBrowseClickMultiChoice,
    ActionMode.Callback,
    MenuProvider {
    private lateinit var gridView: RecyclerView
    private lateinit var progressHighlightViewLayout: ProgressHighlightViewLayout
    private lateinit var progressbar: ProgressBar
    private lateinit var photoShelfSwipe: PhotoShelfSwipe

    private lateinit var imagePickerAdapter: ImagePickerAdapter
    private lateinit var selectedItemsViewContainer: SelectedItemsViewContainer
    private var imageGallery: ImageGallery? = null
    private val viewModel: ImagePickerViewModel by viewModels()

    // contains the last edited data (title, tags), so it's not necessary to edit the title again
    // if the publish dialog is reopen
    var lastEditorResult: NewPostEditorResult? = null

    private val activityResult = registerForActivityResult(PostEditorActivityResultContracts.New(tumblrPostDialog)) {
        it?.also { onPublish(it) }
    }

    // Search on fragment arguments
    private val textWithUrl: String?
        get() {
            val arguments = arguments
            if (arguments != null && arguments.containsKey(EXTRA_URL)) {
                return arguments.getString(EXTRA_URL)
            }
            // Search on activity intent
            // Get intent, action and MIME type
            val intent = requireActivity().intent
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_image_picker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.result.observe(
            viewLifecycleOwner,
            EventObserver { result ->
                when (result) {
                    is ImagePickerModelResult.Gallery -> onGalleryModelResult(result)
                    is ImagePickerModelResult.ImageList -> onImageListModelResult(result)
                    is ImagePickerModelResult.Image -> onImageModelResult(result)
                }
            }
        )

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        setupUI(view, requireContext())

        refreshUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().setTitle(R.string.image_picker_activity_title)
    }

    private fun onGalleryModelResult(result: ImagePickerModelResult.Gallery) {
        when (result.command.status) {
            Status.SUCCESS -> onGalleryRetrieved(result.command.data?.gallery)
            Status.ERROR -> showError(result.command.error, false)
            Status.PROGRESS -> {}
        }
    }

    private fun onImageListModelResult(result: ImagePickerModelResult.ImageList) {
        when (result.command.status) {
            Status.SUCCESS -> onRetrievedImageList(result.command.data)
            Status.ERROR -> showError(result.command.error, true)
            Status.PROGRESS -> progressbar.incrementProgressBy(1)
        }
    }

    private fun onImageModelResult(result: ImagePickerModelResult.Image) {
        when (result.command.status) {
            Status.SUCCESS -> onRetrievedSingleImage(result.command.data)
            Status.ERROR -> showError(result.command.error, true)
            Status.PROGRESS -> progressbar.incrementProgressBy(1)
        }
    }

    private fun showError(error: Throwable?, showAlert: Boolean) {
        progressbar.visibility = GONE
        progressHighlightViewLayout.stopProgress()
        photoShelfSwipe.setRefreshingAndWaitingResult(false)

        if (showAlert) {
            DialogUtils.showSimpleMessageDialog(
                requireContext(),
                R.string.url_not_found,
                error?.localizedMessage ?: ""
            )
        } else {
            snackbarHolder.show(gridView, error, resources.getString(R.string.refresh)) { refreshUI() }
        }
    }

    private fun onRetrievedSingleImage(uri: ImageInfoUriPair?) {
        progressbar.visibility = GONE
        uri ?: return
        // cache retrieved value
        val url = uri.second.toString()
        uri.first.imageUrl = url
        imageViewerActivityStarter.startImageViewer(requireContext(), ImageViewerData(url))
    }

    private fun setupUI(view: View, context: Context) {
        val layoutManager = AutofitGridLayoutManager(
            context,
            resources.getDimension(R.dimen.image_picker_grid_width).toInt()
        )

        // setup selectedItemsViewContainer before any other view to be sure the constraintLayout isn't modified
        // (e.g. if progressHighlightViewLayout.visibility is changed before its setup the new value is used)
        setupSelectedItemsViewContainer(view, context, layoutManager)

        progressHighlightViewLayout = view.findViewById(android.R.id.empty)
        progressHighlightViewLayout.progressAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_loop)

        imagePickerAdapter = ImagePickerAdapter(context)
        imagePickerAdapter.setOnPhotoBrowseClick(this)
        imagePickerAdapter.setEmptyView(progressHighlightViewLayout)

        gridView = view.findViewById(R.id.gridview)
        gridView.adapter = imagePickerAdapter
        gridView.setHasFixedSize(true)
        gridView.layoutManager = layoutManager
        // animating the constraintLayout results in grid animations, so we disable them
        gridView.itemAnimator = null

        progressbar = view.findViewById(R.id.progressbar)

        photoShelfSwipe = view.findViewById(R.id.swipe_container)
        photoShelfSwipe.setOnRefreshListener { refreshUI() }
    }

    private fun setupSelectedItemsViewContainer(view: View, context: Context, layoutManager: AutofitGridLayoutManager) {
        selectedItemsViewContainer = SelectedItemsViewContainer(
            context,
            view.findViewById(R.id.constraintlayout),
            view.findViewById(R.id.selectedItems)
        )
        selectedItemsViewContainer.adapter
            .setOnPhotoBrowseClick(object : OnPhotoBrowseClickMultiChoice {
                override fun onItemClick(position: Int) {
                    val index = imagePickerAdapter.getIndex(selectedItemsViewContainer.adapter.getItem(position))
                    layoutManager.scrollToPosition(index)
                }

                override fun onItemLongClick(position: Int) = Unit
                override fun onTagClick(position: Int, clickedTag: String) = Unit
                override fun onThumbnailImageClick(position: Int) = Unit
                override fun onOverflowClick(position: Int, view: View) = Unit
            })
    }

    override fun refreshUI() = openUrl(textWithUrl)

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        job.cancel()
    }

    private fun openUrl(textWithUrl: String?) {
        if (textWithUrl == null) {
            return
        }
        val url = "(https?:.*)".toRegex().find(textWithUrl)?.value

        if (url == null) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.url_not_found)
                .setMessage(getString(R.string.url_not_found_description, textWithUrl))
                .show()
            return
        }

        progressHighlightViewLayout.startProgress()
        progressHighlightViewLayout.visibility = VISIBLE
        photoShelfSwipe.setRefreshingAndWaitingResult(true)
        imagePickerAdapter.clear()

        val message = resources.getQuantityString(R.plurals.download_url_with_count, 1, 0)
        currentTextView.text = message

        viewModel.readImageGallery(url)
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.title = getString(R.string.select_images)
        mode.subtitle = resources.getQuantityString(
            R.plurals.selected_items_total,
            1,
            1,
            imagePickerAdapter.itemCount
        )
        mode.menuInflater.inflate(R.menu.image_picker_context, menu)
        imagePickerAdapter.showButtons = true
        imagePickerAdapter.notifyItemRangeChanged(0, imagePickerAdapter.itemCount)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        photoShelfSwipe.isEnabled = false
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.showDialog -> {
                showProgressbar(imagePickerAdapter.selectedItems.size)
                viewModel.imageList(imagePickerAdapter.selectedItems)
                finish(mode)
                true
            }
            R.id.create_from_file -> {
                showProgressbar(imagePickerAdapter.selectedItems.size)
                viewModel.imageList(imagePickerAdapter.selectedItems, requireContext().cacheDir)
                finish(mode)
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
        photoShelfSwipe.isEnabled = true
    }

    private fun finish(mode: ActionMode) {
        mode.finish()
        // if visibility is set (to GONE) on layout file
        // the constraintLayout restores it to GONE after any view modification
        // this means the progress bar is not visible while progress goes on
        // The workaround consists to make the progressbar visible
        // when the actionMode is 'finished' programmatically
        progressbar.visibility = VISIBLE
    }

    private fun onRetrievedImageList(imageUriList: List<ImageInfoUriPair>?) {
        progressbar.visibility = GONE

        imageUriList ?: return
        val imageGallery = imageGallery ?: return

        try {
            val firstTag = lastEditorResult?.tags?.replace(",.*$".toRegex(), "")?.let { listOf(it) }
            val data = NewPostEditorData(
                imageUriList.map { it.second.toString() },
                requireBlogName,
                imageGallery.parsableTitle,
                lastEditorResult?.htmlTitle ?: imageGallery.titleParsed.html,
                firstTag ?: imageGallery.titleParsed.tags,
                mapOf(
                    EXTRA_THUMBNAILS_ITEMS to imageUriList.map { it.first.thumbnailUrl }
                )
            )
            activityResult.launch(data)
        } catch (e: Exception) {
            e.showErrorDialog(requireContext())
        }
    }

    private fun onGalleryRetrieved(imageGallery: ImageGallery?) {
        progressHighlightViewLayout.stopProgress()
        photoShelfSwipe.setRefreshingAndWaitingResult(false)

        imageGallery ?: return

        this.imageGallery = imageGallery
        showDetails(Snackbar.LENGTH_LONG)
        val imageInfoList = imageGallery.imageInfoList
        supportActionBar?.subtitle = resources.getQuantityString(
            R.plurals.image_found,
            imageInfoList.size,
            imageInfoList.size
        )
        imagePickerAdapter.addAll(imageInfoList)

        requireActivity().invalidateOptionsMenu()
    }

    override fun onTagClick(position: Int, clickedTag: String) = Unit

    override fun onThumbnailImageClick(position: Int) {
        val imageInfo = imagePickerAdapter.getItem(position)
        val imageUrl = imageInfo.imageUrl
        if (imageUrl == null) {
            showProgressbar(1)
            viewModel.image(imageInfo)
        } else {
            imageViewerActivityStarter.startImageViewer(requireContext(), ImageViewerData(imageUrl))
        }
    }

    override fun onOverflowClick(position: Int, view: View) = Unit

    override fun onItemClick(position: Int) {
        if (actionMode == null) {
            onThumbnailImageClick(position)
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

    private fun updateSelection(position: Int) {
        val selection = imagePickerAdapter.selection
        selection.toggle(position)
        selectedItemsViewContainer.updateList(imagePickerAdapter.selectedItems)
        if (selection.itemCount == 0) {
            actionMode?.finish()
        } else {
            val selectionCount = selection.itemCount
            actionMode?.subtitle = resources.getQuantityString(
                R.plurals.selected_items_total,
                selectionCount,
                selectionCount,
                imagePickerAdapter.itemCount
            )
        }
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.image_picker, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.action_image_viewer_details).isVisible = imageGallery != null
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_image_viewer_details -> {
                showDetails(Snackbar.LENGTH_INDEFINITE)
                true
            }
            else -> false
        }
    }

    private fun showDetails(duration: Int) {
        snackbarHolder.backgroundColor = ContextCompat.getColor(requireContext(), R.color.image_picker_detail_text_bg)
        snackbarHolder.textColor = ContextCompat.getColor(requireContext(), R.color.image_picker_detail_text_text)
        snackbarHolder.show(Snackbar.make(gridView, imageGallery?.title ?: "No title", duration))
    }

    private fun showProgressbar(max: Int) {
        progressbar.progress = 0
        progressbar.max = max
        progressbar.visibility = VISIBLE
    }

    private fun onPublish(resultData: NewPostEditorResult) {
        lastEditorResult = resultData
        resultData
            .urls
            .forEach { url ->
                PostPublisherService.startPublish(
                    requireContext(),
                    resultData.toPostPublisherData(url, publishClassName.name)
                )
            }
    }
}
