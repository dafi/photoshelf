package com.ternaryop.photoshelf.tumblr.ui.core.postaction

import android.content.Context
import androidx.core.content.ContextCompat
import com.ternaryop.photoshelf.tumblr.ui.core.R
import com.ternaryop.utils.recyclerview.ColorItemDecoration

class PostActionColorItemDecoration(private val context: Context) : ColorItemDecoration() {
    fun setColor(postAction: PostAction) {
        val color = when (postAction) {
            is PostAction.SaveAsDraft -> R.color.photo_item_animation_save_as_draft_bg
            is PostAction.Delete -> R.color.photo_item_animation_delete_bg
            is PostAction.Publish -> R.color.photo_item_animation_publish_bg
            is PostAction.Schedule -> R.color.photo_item_animation_schedule_bg
            else -> R.color.post_normal_background_color
        }
        setColor(ContextCompat.getColor(context, color))
    }
}
