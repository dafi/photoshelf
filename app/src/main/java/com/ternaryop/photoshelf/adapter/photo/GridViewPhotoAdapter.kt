package com.ternaryop.photoshelf.adapter.photo

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice
import com.ternaryop.photoshelf.adapter.SelectionArrayViewHolder
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.photoshelf.api.birthday.ImageSize
import com.ternaryop.photoshelf.api.birthday.getClosestPhotoByWidth
import com.ternaryop.tumblr.TumblrAltSize
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.utils.date.yearsBetweenDates
import com.ternaryop.widget.CheckableImageView
import java.lang.Exception
import java.util.Locale

fun TumblrAltSize.toImageSize() = ImageSize(width, height, url)
fun List<TumblrAltSize>.toImageSize() = map { it.toImageSize() }

class GridViewPhotoAdapter(private val context: Context)
    : RecyclerView.Adapter<GridViewPhotoAdapter.ViewHolder>(), View.OnClickListener, View.OnLongClickListener {
    private val items: MutableList<Birthday> = mutableListOf()

    var isShowButtons: Boolean = false

    var onPhotoBrowseClick: OnPhotoBrowseClickMultiChoice? = null

    val selection = SelectionArrayViewHolder(this)
    val selectedItems: List<Birthday>
        get() = selection.selectedPositions.map { getItem(it) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.gridview_photo_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.bindModel(item, isShowButtons, selection.isSelected(position))

        val listener = if (onPhotoBrowseClick == null) null else this
        if (isShowButtons && listener != null) {
            holder.setOnClickListeners(listener)
        }

        holder.setOnClickMultiChoiceListeners(listener, this)
    }

    override fun getItemCount(): Int = items.size

    fun getItem(position: Int): Birthday = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    fun clear() = items.clear()

    fun addAll(posts: List<Birthday>) = items.addAll(posts)

    fun sort() = items.sortWith(Comparator { lhr, rhs -> lhr.name.compareTo(rhs.name) })

    fun updatePostByTag(newPost: TumblrPhotoPost, notifyChange: Boolean) {
        val name = newPost.tags[0]
        val index = items.indexOfFirst { it.name.equals(name, ignoreCase = true) }

        if (index == -1) {
            return
        }
        val birthdayInfo = items[index]
        items[index] = Birthday(
            birthdayInfo.name,
            birthdayInfo.birthdate,
            newPost.firstPhotoAltSize!!.toImageSize())

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

        fun bindModel(item: Birthday, showButtons: Boolean, checked: Boolean) {
            setVisibility(showButtons)
            updateTitles(item)
            displayImage(item, checked)
        }

        private fun updateTitles(item: Birthday) {
            caption.text = String.format(Locale.US, "%s, %d", item.name, item.birthdate.yearsBetweenDates())
        }

        private fun setVisibility(showButtons: Boolean) {
            showImageAction.visibility = if (showButtons) View.VISIBLE else View.INVISIBLE
            bgAction.visibility = if (showButtons) View.VISIBLE else View.INVISIBLE
        }

        private fun displayImage(item: Birthday, checked: Boolean) {
            Picasso
                .get()
                .load(item.getClosestPhotoByWidth(TumblrAltSize.IMAGE_WIDTH_250)!!.url)
                .placeholder(R.drawable.stub)
                .noFade()
                .into(thumbImage, object: Callback {
                    override fun onSuccess() {
                        thumbImage.isChecked = checked
                    }

                    override fun onError(e: Exception?) {}
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