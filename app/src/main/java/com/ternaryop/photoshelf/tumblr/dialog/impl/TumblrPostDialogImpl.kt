package com.ternaryop.photoshelf.tumblr.dialog.impl

import android.content.Context
import androidx.fragment.app.DialogFragment
import com.ternaryop.photoshelf.tumblr.dialog.EditPostEditorData
import com.ternaryop.photoshelf.tumblr.dialog.NewPostEditorData
import com.ternaryop.photoshelf.tumblr.dialog.SchedulePostDialog
import com.ternaryop.photoshelf.tumblr.dialog.TumblrPostDialog
import com.ternaryop.photoshelf.tumblr.dialog.editor.activity.PostEditorActivity
import com.ternaryop.tumblr.TumblrPost
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TumblrPostDialogImpl @Inject constructor() : TumblrPostDialog {
    override fun newPostEditorIntent(
        context: Context,
        newPostDialogData: NewPostEditorData,
    ) = PostEditorActivity.newPostEditorIntent(context, newPostDialogData)

    override fun editPostEditorIntent(
        context: Context,
        editPostDialogData: EditPostEditorData,
    ) = PostEditorActivity.editPostEditorIntent(context, editPostDialogData)

    override fun schedulePostDialog(post: TumblrPost, scheduleDateTime: Calendar, requestKey: String): DialogFragment =
        SchedulePostDialog.newInstance(post, scheduleDateTime, requestKey)
}
