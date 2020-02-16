package com.ternaryop.photoshelf.tumblr.ui.core.postaction

import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.TumblrPost
import java.util.Calendar

sealed class PostAction(val blogName: String, val postList: List<TumblrPost>) {
    class Publish(blogName: String, postList: List<TumblrPost>) : PostAction(blogName, postList)
    class Delete(blogName: String, postList: List<TumblrPost>) : PostAction(blogName, postList)
    class Edit(blogName: String, val post: TumblrPhotoPost, val title: String, val tags: String) :
        PostAction(blogName, listOf(post))
    class SaveAsDraft(blogName: String, postList: List<TumblrPost>) : PostAction(blogName, postList)
    class Schedule(blogName: String, val post: TumblrPost, val scheduleDate: Calendar) :
        PostAction(blogName, listOf(post))
}

interface OnPostActionListener {
    fun onStart(postAction: PostAction, executor: PostActionExecutor)
    fun onComplete(postAction: PostAction, executor: PostActionExecutor, resultList: List<PostActionResult>)
    fun onNext(postAction: PostAction, executor: PostActionExecutor, result: PostActionResult)
}

/**
 * Created by dave on 22/02/18.
 * Group all actions available on posts (ie save as draft, edit, publish and schedule)
 */
interface PostActionExecutor {
    var onPostActionListener: OnPostActionListener?
    suspend fun execute(postAction: PostAction)
}
