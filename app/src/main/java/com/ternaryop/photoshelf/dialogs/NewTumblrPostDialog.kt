package com.ternaryop.photoshelf.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.ternaryop.photoshelf.R
import kotlinx.coroutines.launch

class NewTumblrPostDialog: TumblrPostDialog() {
    private lateinit var blogList: BlogList
    private lateinit var imageUrls: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageUrls = checkNotNull(arguments?.getStringArrayList(ARG_IMAGE_URLS))
    }

    override fun onCreatePostDialog(view: View): Dialog {
        val onClickPublishListener = OnClickPublishListener()
        view.findViewById<View>(R.id.refreshBlogList)
            .setOnClickListener {
                launch {
                    blogList.fetchBlogNames(dialog as AlertDialog)
                }
            }
        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setNegativeButton(R.string.cancel_title) { _, _ -> job.cancel() }
            .setNeutralButton(R.string.publish_post, onClickPublishListener)
            .setPositiveButton(R.string.draft_title, onClickPublishListener)
            .create()
    }

    override fun setupUI(view: View) {
        super.setupUI(view)

        blogList = BlogList(requireContext(), view.findViewById(R.id.blog),
            object : BlogList.OnBlogItemSelectedListener() {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    tagsHolder.updateBlogName(blogList.selectedBlogName)
                }
            })

        val size = imageUrls.size
        toolbar.title = requireContext().resources.getQuantityString(
            R.plurals.post_image,
            size,
            size)
    }

    override fun onStart() {
        super.onStart()

        launch { blogList.loadList(dialog as AlertDialog) }
    }

    private inner class OnClickPublishListener : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            blogList.saveBlogName()

            updateMruList()
            (targetFragment as? OnPublishPostListener)?.also { fm ->
                fm.onPublish(this@NewTumblrPostDialog, PublishData(
                    which == DialogInterface.BUTTON_NEUTRAL,
                    blogList.selectedBlogName,
                    imageUrls.map { Uri.parse(it) },
                    titleHolder.htmlTitle,
                    tagsHolder.tags))
            }
        }
    }

    interface OnPublishPostListener {
        fun onPublish(dialog: NewTumblrPostDialog, data: PublishData)
    }

    data class PublishData(
        val isPublish: Boolean,
        val blogName: String,
        val urls: List<Uri>,
        val postTitle: String,
        val postTags: String)

    companion object {
        private const val ARG_IMAGE_URLS = "imageUrls"

        fun newInstance(
            dialogData: PostDialogData,
            imageUrls: List<String>,
            target: Fragment? = null) = NewTumblrPostDialog().apply {
            arguments = bundleOf(
                ARG_DATA to dialogData,
                ARG_IMAGE_URLS to imageUrls)
            setTargetFragment(target, 0)
        }
    }
}
