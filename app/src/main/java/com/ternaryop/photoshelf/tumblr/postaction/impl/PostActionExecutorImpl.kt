package com.ternaryop.photoshelf.tumblr.postaction.impl

import com.ternaryop.photoshelf.api.post.PostService
import com.ternaryop.photoshelf.db.TumblrPostCache
import com.ternaryop.photoshelf.db.TumblrPostCacheDAO
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.OnPostActionListener
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.PostAction
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.PostActionExecutor
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.PostActionResult
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.tumblr.saveDraft
import com.ternaryop.tumblr.schedulePost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PostActionExecutorImpl(
    private val tumblr: Tumblr,
    private val tumblrPostCacheDAO: TumblrPostCacheDAO,
    private val postService: PostService
) : PostActionExecutor {

    private var listener: OnPostActionListener? = null
    override var onPostActionListener: OnPostActionListener?
        get() = listener
        set(value) { listener = value }

    override suspend fun execute(postAction: PostAction) {
        execute(postAction, postAction.postList) { post ->
            when (postAction) {
                is PostAction.SaveAsDraft -> saveAsDraft(postAction, post)
                is PostAction.Delete -> delete(postAction, post)
                is PostAction.Publish -> publish(postAction, post)
                is PostAction.Schedule -> schedule(postAction, post)
                is PostAction.Edit -> edit(postAction, post)
            }
        }
    }

    private fun saveAsDraft(postAction: PostAction.SaveAsDraft, post: TumblrPost) {
        tumblr.saveDraft(postAction.blogName, post.postId)
        tumblrPostCacheDAO.insertItem(post, TumblrPostCache.CACHE_TYPE_DRAFT)
    }

    private suspend fun delete(postAction: PostAction.Delete, post: TumblrPost) {
        tumblr.deletePost(postAction.blogName, post.postId)
        postService.deletePost(post.postId)
    }

    private fun publish(postAction: PostAction.Publish, post: TumblrPost) {
        tumblr.publishPost(postAction.blogName, post.postId)
    }

    private fun schedule(postAction: PostAction.Schedule, post: TumblrPost) {
        tumblr.schedulePost(postAction.blogName, post, postAction.scheduleDate.timeInMillis)
    }

    private suspend fun edit(postAction: PostAction.Edit, post: TumblrPost) {
        val newValues = mutableMapOf(
            "id" to post.postId.toString(),
            "caption" to postAction.title,
            "tags" to postAction.tags
        )
        tumblr.editPost(postAction.blogName, newValues)
        postService.editTags(post.postId, TumblrPost.tagsFromString(postAction.tags))
        postAction.post.tagsFromString(postAction.tags)
        postAction.post.caption = postAction.title
    }

    private suspend fun execute(
        postAction: PostAction,
        postList: List<TumblrPost>,
        consumer: suspend (TumblrPost) -> Unit
    ) {
        onPostActionListener?.onStart(postAction, this)
        val resultList = postList.map { post ->
            val result = try {
                withContext(Dispatchers.IO) { consumer(post) }
                PostActionResult(post)
            } catch (e: Throwable) {
                e.printStackTrace()
                PostActionResult(post, e)
            }
            onPostActionListener?.onNext(postAction, this@PostActionExecutorImpl, result)
            result
        }
        onPostActionListener?.onComplete(postAction, this@PostActionExecutorImpl, resultList)
    }
}
