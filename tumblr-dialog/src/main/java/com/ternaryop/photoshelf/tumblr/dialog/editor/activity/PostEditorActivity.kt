package com.ternaryop.photoshelf.tumblr.dialog.editor.activity

import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.ternaryop.photoshelf.activity.AbsPhotoShelfActivity
import com.ternaryop.photoshelf.tumblr.dialog.EditPostEditorData
import com.ternaryop.photoshelf.tumblr.dialog.NewPostEditorData
import com.ternaryop.photoshelf.tumblr.dialog.PostEditorData
import com.ternaryop.photoshelf.tumblr.dialog.R
import com.ternaryop.photoshelf.tumblr.dialog.TumblrPostDialog.Companion.ARG_POST_DATA
import com.ternaryop.photoshelf.tumblr.dialog.editor.fragment.PostEditorFragment
import org.koin.android.ext.android.inject

class PostEditorActivity : AbsPhotoShelfActivity() {
    private val fragmentFactory: FragmentFactory by inject()

    override val contentViewLayoutId: Int = R.layout.activity_tumblr_post
    override val contentFrameId: Int = R.id.content_frame

    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = fragmentFactory
        super.onCreate(savedInstanceState)
    }

    override fun createFragment(): Fragment? =
        supportFragmentManager.fragmentFactory.instantiate(
            classLoader, PostEditorFragment::class.java.name).apply {
            arguments = intent.extras
        }

    companion object {
        fun startNewPostForResult(
            newPostDialogData: NewPostEditorData,
            fragment: Fragment,
            requestCode: Int
        ) {
            startActivity(newPostDialogData, fragment, requestCode)
        }

        fun startEditPostForResult(
            editPostDialogData: EditPostEditorData,
            fragment: Fragment,
            requestCode: Int
        ) {
            startActivity(editPostDialogData, fragment, requestCode)
        }

        private fun startActivity(
            postEditorData: PostEditorData,
            fragment: Fragment,
            requestCode: Int
        ) {
            val intent = Intent(fragment.requireContext(), PostEditorActivity::class.java)
            intent.action = Intent.ACTION_VIEW
            intent.putExtras(bundleOf(
                ARG_POST_DATA to postEditorData))
            fragment.startActivityForResult(intent, requestCode)
        }
    }
}
