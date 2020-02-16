package com.ternaryop.photoshelf.tumblr.ui.core.postaction

import com.ternaryop.tumblr.TumblrPost

/**
 * Created by dave on 22/02/18.
 * Hold the values returned from a post action
 */
class PostActionResult(val post: TumblrPost, val error: Throwable? = null) {
    fun hasError() = error != null
}

fun List<PostActionResult>.completedList(): List<PostActionResult> = filter { !it.hasError() }
fun List<PostActionResult>.errorList(): List<PostActionResult> = filter { it.hasError() }
