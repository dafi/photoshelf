package com.ternaryop.photoshelf.tumblr.dialog.editor.adapter

import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ternaryop.photoshelf.tumblr.dialog.R
import com.ternaryop.widget.CheckableImageView

class ThumbnailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val thumbImage = itemView.findViewById<View>(R.id.thumbnail_image) as CheckableImageView

    fun bindModel(url: String, thumbnailSize: Int) {
        displayImage(url, thumbnailSize)
    }

    private fun displayImage(url: String, thumbnailSize: Int) {
        setImageDimension(thumbnailSize, thumbnailSize)
        // thumbnail urls could be the same than destination images (i.e. very large images) causing
        // a huge memory footprint so we resize the images using fit/centerCrop
        Glide
            .with(itemView)
            .load(url)
            .fitCenter()
            .into(thumbImage)
    }

    private fun setImageDimension(width: Int, height: Int) {
        // convert from pixel to DIP
        thumbImage.layoutParams.width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            width.toFloat(), itemView.context.resources.displayMetrics).toInt()
        thumbImage.layoutParams.height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            height.toFloat(), itemView.context.resources.displayMetrics).toInt()
    }
}
