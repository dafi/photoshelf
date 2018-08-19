package com.ternaryop.photoshelf.util.post

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.core.content.ContextCompat
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.db.TumblrPostCache
import com.ternaryop.photoshelf.util.network.ApiManager
import com.ternaryop.photoshelf.view.ColorItemDecoration
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.saveDraft
import com.ternaryop.tumblr.schedulePost
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
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

    fun saveAsDraft(postList: List<TumblrPost>): Single<List<PostActionResult>> {
        postAction = SAVE_AS_DRAFT
        return executePostAction(postList, Consumer {
            TumblrManager.getInstance(context).saveDraft(blogName, it.postId)
            DBHelper.getInstance(context).tumblrPostCacheDAO.insertItem(it, TumblrPostCache.CACHE_TYPE_DRAFT)
        })
    }

    fun delete(postList: List<TumblrPost>): Single<List<PostActionResult>> {
        postAction = DELETE
        return executePostAction(postList, Consumer {
            TumblrManager.getInstance(context).deletePost(blogName, it.postId)
            ApiManager.postService(context)
                .deletePost(it.postId)
                .subscribe()
        })
    }

    fun publish(postList: List<TumblrPost>): Single<List<PostActionResult>> {
        postAction = PUBLISH
        return executePostAction(postList, Consumer {
            TumblrManager.getInstance(context).publishPost(blogName, it.postId)
        })
    }

    fun schedule(post: TumblrPost, scheduleTimestamp: Calendar): Single<List<PostActionResult>> {
        postAction = SCHEDULE
        this.scheduleTimestamp = scheduleTimestamp
        return executePostAction(listOf(post), Consumer {
            TumblrManager.getInstance(context).schedulePost(blogName, it, scheduleTimestamp.timeInMillis)
        })
    }

    fun edit(post: TumblrPhotoPost,
        title: String, tags: String, selectedBlogName: String): Single<List<PostActionResult>> {
        postAction = EDIT
        return executePostAction(listOf(post), Consumer {
            val newValues = mutableMapOf(
                "id" to post.postId.toString(),
                "caption" to title,
                "tags" to tags
            )
            TumblrManager.getInstance(context).editPost(selectedBlogName, newValues)
            ApiManager.postService(context)
                .editTags(post.postId, TumblrPost.tagsFromString(tags))
                .subscribe {
                    post.tagsFromString(tags)
                    post.caption = title
                }
        })
    }

    private fun executePostAction(postList: List<TumblrPost>,
        consumer: Consumer<TumblrPost>): Single<List<PostActionResult>> {
        return Observable
            .fromIterable(postList)
            .map { post ->
                try {
                    consumer.accept(post)
                    PostActionResult(post)
                } catch (e: Throwable) {
                    PostActionResult(post, e)
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { result -> listener.onNext(this, result) }
            .toList()
            .doOnSuccess { resultList -> listener.onComplete(this, resultList) }
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