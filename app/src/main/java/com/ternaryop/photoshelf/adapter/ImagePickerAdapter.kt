package com.ternaryop.photoshelf.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.ternaryop.lazyimageloader.ImageLoader
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.extractor.ImageInfo
import com.ternaryop.widget.CheckableImageView

class ImagePickerAdapter(private val context: Context) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>(), View.OnClickListener, View.OnLongClickListener {
    private val imageLoader: ImageLoader = ImageLoader(context.applicationContext, "picker", R.drawable.stub)
    private var onPhotoBrowseClick: OnPhotoBrowseClickMultiChoice? = null
    private val items: MutableList<ImageInfo> = mutableListOf()
    var showButtons: Boolean = false

    internal val selection = SelectionArrayViewHolder(this)

    val selectedItems: List<ImageInfo>
        get() = getSelection().selectedPositions.map { getItem(it) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.gridview_photo_picker_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listener: View.OnClickListener? = if (onPhotoBrowseClick == null) null else this
        holder.bindModel(items[position], imageLoader, showButtons)
        if (showButtons && listener != null) {
            holder.setOnClickListeners(listener)
        }
        holder.setOnClickMultiChoiceListeners(listener, this)
        holder.thumbImage.isChecked = selection.isSelected(position)
    }

    override fun getItemCount() = items.size

    fun getItem(position: Int) = items[position]

    fun addAll(list: Array<ImageInfo>) {
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.ic_show_image_action -> onPhotoBrowseClick!!.onThumbnailImageClick(v.tag as Int)
            R.id.list_row -> onPhotoBrowseClick!!.onItemClick(v.tag as Int)
        }
    }

    override fun onLongClick(v: View): Boolean {
        onPhotoBrowseClick!!.onItemLongClick(v.tag as Int)
        return true
    }

    @Suppress("MemberVisibilityCanBePrivate")
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val showImageAction = itemView.findViewById<View>(R.id.ic_show_image_action) as ImageView
        internal val thumbImage = itemView.findViewById<View>(R.id.thumbnail_image) as CheckableImageView
        internal val bgAction = itemView.findViewById<View>(R.id.bg_actions) as ImageView

        fun bindModel(imageInfo: ImageInfo, imageLoader: ImageLoader, showButtons: Boolean) {
            setVisibility(showButtons)
            displayImage(imageInfo, imageLoader)
        }

        private fun setVisibility(showButtons: Boolean) {
            showImageAction.visibility = if (showButtons) View.VISIBLE else View.INVISIBLE
            bgAction.visibility = if (showButtons) View.VISIBLE else View.INVISIBLE
        }

        private fun displayImage(imageInfo: ImageInfo, imageLoader: ImageLoader) {
            imageLoader.displayImage(imageInfo.thumbnailUrl, thumbImage)
        }

        fun setOnClickListeners(listener: View.OnClickListener) {
            showImageAction.setOnClickListener(listener)
            showImageAction.tag = adapterPosition
        }

        fun setOnClickMultiChoiceListeners(listener: View.OnClickListener?, longClickListener: View.OnLongClickListener) {
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

    fun getSelection(): Selection {
        return selection
    }

    fun setEmptyView(view: View?) {
        if (view != null) {
            registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    super.onChanged()
                    view.visibility = if (itemCount == 0) View.VISIBLE else View.GONE
                }
            })
        }
    }
}