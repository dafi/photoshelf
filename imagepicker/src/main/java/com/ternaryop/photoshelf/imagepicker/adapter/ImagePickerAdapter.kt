package com.ternaryop.photoshelf.imagepicker.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.api.load
import coil.size.Scale
import coil.target.ImageViewTarget
import com.ternaryop.photoshelf.adapter.AbsBaseAdapter
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice
import com.ternaryop.photoshelf.adapter.SelectionArrayViewHolder
import com.ternaryop.photoshelf.api.extractor.ImageInfo
import com.ternaryop.photoshelf.imagepicker.R
import com.ternaryop.widget.CheckableImageView

class ImagePickerAdapter(
    private val context: Context
) : AbsBaseAdapter<ImagePickerAdapter.ViewHolder>(), View.OnClickListener, View.OnLongClickListener {
    private var onPhotoBrowseClick: OnPhotoBrowseClickMultiChoice? = null
    private val items: MutableList<ImageInfo> = mutableListOf()
    var showButtons: Boolean = false

    val selection = SelectionArrayViewHolder(this)

    val selectedItems: List<ImageInfo>
        get() = selection.selectedPositions.map { getItem(it) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.gridview_photo_picker_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listener: View.OnClickListener? = if (onPhotoBrowseClick == null) null else this
        holder.bindModel(items[position], showButtons, selection.isSelected(position))
        if (showButtons && listener != null) {
            holder.setOnClickListeners(listener)
        }
        holder.setOnClickMultiChoiceListeners(listener, this)
    }

    override fun getItemCount() = items.size

    fun getItem(position: Int) = items[position]

    fun getIndex(item: ImageInfo) = items.indexOfFirst { it == item }

    fun addAll(list: List<ImageInfo>) {
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun clear() {
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(0, size)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.ic_show_image_action -> onPhotoBrowseClick?.onThumbnailImageClick(v.tag as Int)
            R.id.list_row -> onPhotoBrowseClick?.onItemClick(v.tag as Int)
        }
    }

    override fun onLongClick(v: View): Boolean {
        onPhotoBrowseClick?.onItemLongClick(v.tag as Int)
        return true
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val showImageAction = itemView.findViewById<View>(R.id.ic_show_image_action) as ImageView
        internal val thumbImage = itemView.findViewById<View>(R.id.thumbnail_image) as CheckableImageView
        internal val bgAction = itemView.findViewById<View>(R.id.bg_actions) as ImageView

        fun bindModel(imageInfo: ImageInfo, showButtons: Boolean, checked: Boolean) {
            setVisibility(showButtons)
            displayImage(imageInfo, checked)
        }

        private fun setVisibility(showButtons: Boolean) {
            showImageAction.visibility = if (showButtons) View.VISIBLE else View.INVISIBLE
            bgAction.visibility = if (showButtons) View.VISIBLE else View.INVISIBLE
        }

        private fun displayImage(imageInfo: ImageInfo, checked: Boolean) {
            // thumbnail urls could be the same than destination images (i.e. very large images) causing
            // a huge memory footprint so we resize the images using fit/centerCrop
            val thumbnailUrl = imageInfo.thumbnailUrl ?: return
            thumbImage.load(thumbnailUrl) {
                placeholder(R.drawable.stub)
                scale(Scale.FILL)
                target(object : ImageViewTarget(thumbImage) {
                    override fun onSuccess(result: Drawable) {
                        super.onSuccess(result)
                        thumbImage.isChecked = checked
                    }
                })
            }
        }

        fun setOnClickListeners(listener: View.OnClickListener) {
            showImageAction.setOnClickListener(listener)
            showImageAction.tag = adapterPosition
        }

        fun setOnClickMultiChoiceListeners(
            listener: View.OnClickListener?,
            longClickListener: View.OnLongClickListener
        ) {
            if (listener != null) {
                val position = adapterPosition
                itemView.setOnClickListener(listener)
                itemView.setOnLongClickListener(longClickListener)
                itemView.isLongClickable = true
                itemView.tag = position
            }
        }
    }

    fun setOnPhotoBrowseClick(onPhotoBrowseClick: OnPhotoBrowseClickMultiChoice) {
        this.onPhotoBrowseClick = onPhotoBrowseClick
    }
}
