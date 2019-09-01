package com.ternaryop.photoshelf.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.ternaryop.photoshelf.R
import com.ternaryop.tumblr.TumblrPhotoPost

class EditTumblrPostDialog : TumblrPostDialog() {
    private lateinit var photoPost: TumblrPhotoPost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoPost = checkNotNull(arguments?.getSerializable(ARG_PHOTO_POST) as? TumblrPhotoPost)
    }

    override fun onCreatePostDialog(view: View): Dialog {
        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setNegativeButton(R.string.cancel_title) { _, _ -> job.cancel() }
            .setPositiveButton(R.string.edit_post_title) { _, _ -> editPost() }
            .create()
    }

    override fun setupUI(view: View) {
        super.setupUI(view)
        toolbar.setTitle(R.string.edit_post_title)
        view.findViewById<View>(R.id.blog).visibility = View.GONE
        view.findViewById<View>(R.id.refreshBlogList).visibility = View.GONE
    }

    private fun editPost() {
        updateMruList()
        (targetFragment as? OnEditPostListener)?.also {
            it.onEdit(this, EditData(photoPost, titleHolder.htmlTitle, tagsHolder.tags))
        }
    }

    interface OnEditPostListener {
        fun onEdit(dialog: EditTumblrPostDialog, editData: EditData)
    }

    data class EditData(
        val photoPost: TumblrPhotoPost,
        val htmlTitle: String,
        val tags: String
    )

    companion object {
        private const val ARG_PHOTO_POST = "photoPost"

        fun newInstance(
            dialogData: PostDialogData,
            photoPost: TumblrPhotoPost,
            target: Fragment? = null) = EditTumblrPostDialog().apply {
            arguments = bundleOf(
                ARG_DATA to dialogData,
                ARG_PHOTO_POST to photoPost
            )
            setTargetFragment(target, 0)
        }
    }
}
