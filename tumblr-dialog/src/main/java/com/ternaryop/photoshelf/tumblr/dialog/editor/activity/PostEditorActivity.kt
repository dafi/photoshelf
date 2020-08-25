package com.ternaryop.photoshelf.tumblr.dialog.editor.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.ternaryop.photoshelf.activity.AbsPhotoShelfActivity
import com.ternaryop.photoshelf.fragment.appFragmentFactory
import com.ternaryop.photoshelf.tumblr.dialog.EditPostEditorData
import com.ternaryop.photoshelf.tumblr.dialog.NewPostEditorData
import com.ternaryop.photoshelf.tumblr.dialog.PostEditorData
import com.ternaryop.photoshelf.tumblr.dialog.R
import com.ternaryop.photoshelf.tumblr.dialog.TumblrPostDialog.Companion.ARG_POST_DATA
import com.ternaryop.photoshelf.tumblr.dialog.editor.fragment.PostEditorFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PostEditorActivity : AbsPhotoShelfActivity() {
    override val contentViewLayoutId: Int = R.layout.activity_tumblr_post
    override val contentFrameId: Int = R.id.content_frame

    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = appFragmentFactory
        super.onCreate(savedInstanceState)
    }

    override fun createFragment(): Fragment? =
        supportFragmentManager.fragmentFactory.instantiate(
            classLoader, PostEditorFragment::class.java.name).apply {
            arguments = intent.extras
        }

    companion object {
        fun newPostEditorIntent(
            context: Context,
            newPostDialogData: NewPostEditorData,
        ) = postEditorIntent(context, newPostDialogData)

        fun editPostEditorIntent(
            context: Context,
            editPostDialogData: EditPostEditorData,
        ) = postEditorIntent(context, editPostDialogData)

        fun postEditorIntent(
            context: Context,
            postEditorData: PostEditorData,
        ) = Intent(context, PostEditorActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtras(bundleOf(
                ARG_POST_DATA to postEditorData))
        }
    }
}
