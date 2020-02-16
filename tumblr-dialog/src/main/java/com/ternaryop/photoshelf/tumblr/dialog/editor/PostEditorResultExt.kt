package com.ternaryop.photoshelf.tumblr.dialog.editor

import android.app.Activity
import android.content.Intent
import com.ternaryop.photoshelf.tumblr.dialog.PostEditorResult
import com.ternaryop.photoshelf.tumblr.dialog.TumblrPostDialog

fun PostEditorResult.finishActivity(activity: Activity) {
    val data = Intent()
    data.putExtra(TumblrPostDialog.ARG_RESULT, this)
    activity.setResult(Activity.RESULT_OK, data)
    activity.finish()
}
