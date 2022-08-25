package com.ternaryop.photoshelf.tumblr.ui.core.postaction

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.appcompat.app.AlertDialog
import com.ternaryop.photoshelf.tumblr.ui.core.R

@PluralsRes
fun PostAction.getConfirmStringId(): Int {
    return when (this) {
        is PostAction.Publish -> R.plurals.publish_post_confirm
        is PostAction.Delete -> R.plurals.delete_post_confirm
        is PostAction.SaveAsDraft -> R.plurals.save_to_draft_confirm
        else -> throw AssertionError("No confirm string for $this")
    }
}

fun PostAction.showConfirmDialog(context: Context, onOkCallback: (PostAction) -> Unit) {
    val message = context.resources.getQuantityString(
        getConfirmStringId(),
        postList.size,
        postList.size,
        postList[0].firstTag
    )
    AlertDialog.Builder(context)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok) { _, _ -> onOkCallback(this) }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}
