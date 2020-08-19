package com.ternaryop.photoshelf.tumblr.dialog.impl

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
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
    override fun newPostEditor(newPostEditorData: NewPostEditorData, fragment: Fragment, requestCode: Int) {
        PostEditorActivity.startNewPostForResult(newPostEditorData, fragment, requestCode)
    }

    override fun editPostEditor(editPostEditorData: EditPostEditorData, fragment: Fragment, requestCode: Int) {
        PostEditorActivity.startEditPostForResult(editPostEditorData, fragment, requestCode)
    }

    override fun schedulePostDialog(post: TumblrPost, scheduleDateTime: Calendar, target: Fragment?): DialogFragment =
        SchedulePostDialog.newInstance(post, scheduleDateTime, target)
}
