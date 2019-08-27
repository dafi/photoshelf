package com.ternaryop.photoshelf.util.post

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.core.content.ContextCompat
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.db.TumblrPostCache
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.saveDraft
import com.ternaryop.tumblr.schedulePost
import com.ternaryop.utils.recyclerview.ColorItemDecoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

interface OnPostActionListener {
    fun onComplete(executor: PostActionExecutor, resultList: List<PostActionResult>)
    fun onNext(executor: PostActionExecutor, result: PostActionResult)
}

/**
 * Created by dave on 22/02/18.
 * Group all actions available on posts (ie save as draft, edit, publish and schedule) and interaction with UI
 */
class PostActionExecutor(private val context: Context,
    private val blogName: String, private val listener: OnPostActionListener) {
    var scheduleTimestamp: Calendar? = null
        private set
    var postAction = NONE
        set(value) {
            field = value
            val color = when (value) {
                SAVE_AS_DRAFT -> R.color.photo_item_animation_save_as_draft_bg
                DELETE -> R.color.photo_item_animation_delete_bg
                PUBLISH -> R.color.photo_item_animation_publish_bg
                SCHEDULE -> R.color.photo_item_animation_schedule_bg
                else -> R.color.post_normal_background_color
            }
            colorItemDecoration.setColor(ContextCompat.getColor(context, color))
        }

    val colorItemDecoration = ColorItemDecoration()

    suspend fun saveAsDraft(postList: List<TumblrPost>) {
        postAction = SAVE_AS_DRAFT
        executePostAction(postList) {
            TumblrManager.getInstance(context).saveDraft(blogName, it.postId)
            DBHelper.getInstance(context).tumblrPostCacheDAO.insertItem(it, TumblrPostCache.CACHE_TYPE_DRAFT)
        }
    }

    suspend fun delete(postList: List<TumblrPost>) {
        postAction = DELETE
        executePostAction(postList) {
            TumblrManager.getInstance(context).deletePost(blogName, it.postId)
            ApiManager.postService().deletePost(it.postId)
        }
    }

    suspend fun publish(postList: List<TumblrPost>) {
        postAction = PUBLISH
        executePostAction(postList) {
            TumblrManager.getInstance(context).publishPost(blogName, it.postId)
        }
    }

    suspend fun schedule(post: TumblrPost, scheduleTimestamp: Calendar) {
        postAction = SCHEDULE
        this.scheduleTimestamp = scheduleTimestamp
        executePostAction(listOf(post)) {
            TumblrManager.getInstance(context).schedulePost(blogName, it, scheduleTimestamp.timeInMillis)
        }
    }

    suspend fun edit(post: TumblrPhotoPost,
        title: String, tags: String, selectedBlogName: String) {
        postAction = EDIT
        executePostAction(listOf(post)) {
            val newValues = mutableMapOf(
                "id" to post.postId.toString(),
                "caption" to title,
                "tags" to tags
            )
            TumblrManager.getInstance(context).editPost(selectedBlogName, newValues)
            ApiManager.postService().editTags(post.postId, TumblrPost.tagsFromString(tags))
            post.tagsFromString(tags)
            post.caption = title
        }
    }

    private suspend fun executePostAction(
        postList: List<TumblrPost>,
        consumer: suspend (TumblrPost) -> Unit) {
        val resultList = postList.map { post ->
            val result = try {
                withContext(Dispatchers.IO) { consumer(post) }
                PostActionResult(post)
            } catch (e: Throwable) {
                e.printStackTrace()
                PostActionResult(post, e)
            }
            listener.onNext(this@PostActionExecutor, result)
            result
        }
        listener.onComplete(this@PostActionExecutor, resultList)
    }

    companion object {
        const val NONE = 0
        const val PUBLISH = 1
        const val DELETE = 2
        const val EDIT = 3
        const val SAVE_AS_DRAFT = 4
        const val SCHEDULE = 5

        @PluralsRes
        fun getConfirmStringId(postAction: Int): Int {
            return when (postAction) {
                PUBLISH -> R.plurals.publish_post_confirm
                DELETE -> R.plurals.delete_post_confirm
                SAVE_AS_DRAFT -> R.plurals.save_to_draft_confirm
                else -> throw AssertionError("No confirm string for $postAction")
            }
        }
    }
}