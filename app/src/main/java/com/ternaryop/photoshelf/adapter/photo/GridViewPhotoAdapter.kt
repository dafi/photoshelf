package com.ternaryop.photoshelf.adapter.photo

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.ternaryop.lazyimageloader.ImageLoader
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice
import com.ternaryop.photoshelf.adapter.SelectionArrayViewHolder
import com.ternaryop.photoshelf.db.Birthday
import com.ternaryop.tumblr.TumblrAltSize
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.widget.CheckableImageView
import java.util.Locale

typealias BirthdayPhotoPair = Pair<Birthday, TumblrPhotoPost>

class GridViewPhotoAdapter(private val context: Context, prefix: String)
    : RecyclerView.Adapter<GridViewPhotoAdapter.ViewHolder>(), View.OnClickListener, View.OnLongClickListener {
    private val imageLoader: ImageLoader = ImageLoader(context.applicationContext, prefix, R.drawable.stub)
    private val items: MutableList<BirthdayPhotoPair> = mutableListOf()

    var isShowButtons: Boolean = false

    var onPhotoBrowseClick: OnPhotoBrowseClickMultiChoice? = null

    val selection = SelectionArrayViewHolder(this)
    val selectedItems: List<BirthdayPhotoPair>
        get() = selection.selectedPositions.map { getItem(it) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.gridview_photo_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.bindModel(item, imageLoader, isShowButtons)

        val listener = if (onPhotoBrowseClick == null) null else this
        if (isShowButtons && listener != null) {
            holder.setOnClickListeners(listener)
        }
        holder.setVisibility(isShowButtons)
        holder.isChecked = selection.isSelected(position)

        holder.setOnClickMultiChoiceListeners(listener, this)
    }

    override fun getItemCount(): Int = items.size

    fun getItem(position: Int): BirthdayPhotoPair = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    fun clear() = items.clear()

    fun addAll(posts: List<BirthdayPhotoPair>) = items.addAll(posts)

    fun updatePostByTag(newPost: TumblrPhotoPost, notifyChange: Boolean) {
        val name = newPost.tags[0]
        val index = items.indexOfFirst { it.second.tags[0].equals(name, ignoreCase = true) }

        if (index == -1) {
            return
        }
        items[index] = Pair(items[index].first, newPost)

        if (notifyChange) {
            notifyDataSetChanged()
        }
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

    fun selectAll() = selection.setSelectedRange(0, itemCount, true)

    @Suppress("MemberVisibilityCanBePrivate")
    class ViewHolder(vi: View) : RecyclerView.ViewHolder(vi) {
        val caption = vi.findViewById<View>(R.id.caption) as TextView
        val thumbImage = vi.findViewById<View>(R.id.thumbnail_image) as CheckableImageView
        val bgAction = vi.findViewById<View>(R.id.bg_actions) as ImageView
        val showImageAction = vi.findViewById<View>(R.id.ic_show_image_action) as ImageView
        var isChecked = false

        fun bindModel(item: BirthdayPhotoPair, imageLoader: ImageLoader, showButtons: Boolean) {
            setVisibility(showButtons)
            updateTitles(item)
            displayImage(item.second, imageLoader)
        }

        private fun updateTitles(item: BirthdayPhotoPair) {
            caption.text = String.format(Locale.US, "%s, %d", item.second.tags[0], item.first.age)
        }

        fun setVisibility(showButtons: Boolean) {
            showImageAction.visibility = if (showButtons) View.VISIBLE else View.INVISIBLE
            bgAction.visibility = if (showButtons) View.VISIBLE else View.INVISIBLE
        }

        private fun displayImage(post: TumblrPhotoPost, imageLoader: ImageLoader) {
            imageLoader.displayDrawable(post.getClosestPhotoByWidth(TumblrAltSize.IMAGE_WIDTH_250)!!.url, false,
                object: ImageLoader.ImageLoaderCallback {
                    override fun display(drawable: Drawable) {
                        thumbImage.setImageDrawable(drawable)
                        thumbImage.isChecked = isChecked
                    }
            })
        }

        fun setOnClickListeners(listener: View.OnClickListener) {
            showImageAction.setOnClickListener(listener)
            showImageAction.tag = adapterPosition
        }

        fun setOnClickMultiChoiceListeners(listener: View.OnClickListener?,
            longClickListener: View.OnLongClickListener) {
            if (listener != null) {
                val position = adapterPosition
                itemView.setOnClickListener(listener)
                itemView.setOnLongClickListener(longClickListener)
                itemView.isLongClickable = true
                itemView.tag = position
            }
        }
    }
}