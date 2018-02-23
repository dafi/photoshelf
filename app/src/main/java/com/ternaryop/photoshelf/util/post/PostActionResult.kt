package com.ternaryop.photoshelf.util.post

import android.content.Context
import com.ternaryop.photoshelf.R
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.utils.DialogUtils

/**
 * Created by dave on 22/02/18.
 * Hold the values returned from a post action
 */
class PostActionResult(val post: TumblrPost, val error: Throwable? = null) {
    fun hasError() = error != null
}

fun List<PostActionResult>.completedList(): List<PostActionResult> {
    return filter { !it.hasError() }
}

fun List<PostActionResult>.errorList(): List<PostActionResult> {
    return filter { it.hasError() }
}

fun List<PostActionResult>.showErrorDialog(context: Context) {
    DialogUtils.showSimpleMessageDialog(context,
        R.string.generic_error,
        context.resources.getQuantityString(
            R.plurals.general_posts_error,
            size,
            this[size - 1].error?.message,
            size))
}

