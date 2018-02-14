package com.ternaryop.photoshelf.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.ternaryop.lazyimageloader.ImageLoader
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.db.Birthday
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.widget.CheckableImageView
import java.util.Locale

class GridViewPhotoAdapter(private val context: Context, prefix: String) : RecyclerView.Adapter<GridViewPhotoAdapter.ViewHolder>(), View.OnClickListener, View.OnLongClickListener {
    private val imageLoader: ImageLoader = ImageLoader(context.applicationContext, prefix, R.drawable.stub)
    var isShowButtons: Boolean = false

    private var onPhotoBrowseClick: OnPhotoBrowseClickMultiChoice? = null
    private val items: MutableList<Pair<Birthday, TumblrPhotoPost>> = mutableListOf()

    internal val selection = SelectionArrayViewHolder(this)

    val selectedItems: List<Pair<Birthday, TumblrPhotoPost>>
        get() = getSelection().selectedPositions.map { getItem(it) }

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
        holder.thumbImage.isChecked = selection.isSelected(position)

        holder.setOnClickMultiChoiceListeners(listener, this)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun getItem(position: Int): Pair<Birthday, TumblrPhotoPost> {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun clear() {
        items.clear()
    }

    fun addAll(posts: List<Pair<Birthday, TumblrPhotoPost>>) {
        items.addAll(posts)
    }

    fun updatePostByTag(newPost: TumblrPhotoPost, notifyChange: Boolean) {
        val name = newPost.tags[0]

        for (i in 0 until itemCount) {
            val item = getItem(i)
            val post = item.second
            if (post.tags[0].equals(name, ignoreCase = true)) {
                items[i] = Pair.create(item.first, newPost)

                if (notifyChange) {
                    notifyDataSetChanged()
                }
                break
            }
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

    fun setOnPhotoBrowseClick(onPhotoBrowseClick: OnPhotoBrowseClickMultiChoice) {
        this.onPhotoBrowseClick = onPhotoBrowseClick
    }

    fun getSelection(): Selection {
        return selection
    }

    fun selectAll() {
        getSelection().setSelectedRange(0, itemCount, true)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    class ViewHolder(vi: View) : RecyclerView.ViewHolder(vi) {
        val caption = vi.findViewById<View>(R.id.caption) as TextView
        val thumbImage = vi.findViewById<View>(R.id.thumbnail_image) as CheckableImageView
        val bgAction = vi.findViewById<View>(R.id.bg_actions) as ImageView
        val showImageAction = vi.findViewById<View>(R.id.ic_show_image_action) as ImageView

        fun bindModel(item: Pair<Birthday, TumblrPhotoPost>, imageLoader: ImageLoader, showButtons: Boolean) {
            setVisibility(showButtons)
            updateTitles(item)
            displayImage(item.second, imageLoader)
        }

        private fun updateTitles(item: Pair<Birthday, TumblrPhotoPost>) {
            caption.text = String.format(Locale.US, "%s, %d", item.second.tags[0], item.first.age)
        }

        fun setVisibility(showButtons: Boolean) {
            showImageAction.visibility = if (showButtons) View.VISIBLE else View.INVISIBLE
            bgAction.visibility = if (showButtons) View.VISIBLE else View.INVISIBLE
        }

        private fun displayImage(post: TumblrPhotoPost, imageLoader: ImageLoader) {
            imageLoader.displayImage(post.getClosestPhotoByWidth(250)!!.url, thumbImage)
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
}