package com.ternaryop.photoshelf.tumblr.dialog

import android.content.Context
import android.content.Intent
import androidx.fragment.app.DialogFragment
import com.ternaryop.tumblr.TumblrPost
import java.io.Serializable
import java.util.Calendar

open class PostEditorData(
    val blogName: String,
    val sourceTitle: String,
    val htmlSourceTitle: String,
    val tags: List<String>,
    val extras: Map<String, Any>? = null
) : Serializable

open class PostEditorResult(
    val blogName: String,
    val htmlTitle: String,
    val tags: String,
    val extras: Map<String, Any>? = null
) : Serializable

class NewPostEditorData(
    val imageUrls: List<String>,
    blogName: String,
    sourceTitle: String,
    htmlSourceTitle: String,
    tags: List<String>,
    extras: Map<String, Any>? = null
) : PostEditorData(blogName, sourceTitle, htmlSourceTitle, tags, extras)

class NewPostEditorResult(
    val isPublish: Boolean,
    val urls: List<String>,
    blogName: String,
    htmlTitle: String,
    tags: String,
    extras: Map<String, Any>? = null
) : PostEditorResult(blogName, htmlTitle, tags, extras)

class EditPostEditorData(
    blogName: String,
    sourceTitle: String,
    htmlSourceTitle: String,
    tags: List<String>,
    extras: Map<String, Any>? = null
) : PostEditorData(blogName, sourceTitle, htmlSourceTitle, tags, extras)

data class SchedulePostData(
    val post: TumblrPost,
    val dateTime: Calendar
) : Serializable

interface TumblrPostDialog {
    fun newPostEditorIntent(
        context: Context,
        newPostDialogData: NewPostEditorData,
    ) : Intent

    fun editPostEditorIntent(
        context: Context,
        editPostDialogData: EditPostEditorData,
    ) : Intent

    fun schedulePostDialog(
        post: TumblrPost,
        scheduleDateTime: Calendar,
        requestKey: String
    ): DialogFragment

    companion object {
        const val ARG_POST_DATA = "postData"
        const val ARG_RESULT = "result"
        const val EXTRA_MAX_TAGS_MRU_ITEMS = "postEditor_maxTagsMruItems"
        const val EXTRA_MAX_HIGHLIGHTED_TAGS_MRU_ITEMS = "postEditor_maxHighlightedTagsMruItems"
        const val EXTRA_THUMBNAILS_ITEMS = "postEditor_thumbnailsItems"
        const val EXTRA_THUMBNAILS_SIZE = "postEditor_thumbnailsSize"
    }
}
