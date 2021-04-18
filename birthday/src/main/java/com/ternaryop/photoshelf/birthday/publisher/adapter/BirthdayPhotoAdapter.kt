package com.ternaryop.photoshelf.birthday.publisher.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.target.ImageViewTarget
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice
import com.ternaryop.photoshelf.adapter.SelectionArrayViewHolder
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.photoshelf.api.birthday.getClosestPhotoByWidth
import com.ternaryop.photoshelf.birthday.R
import com.ternaryop.tumblr.TumblrAltSize
import com.ternaryop.utils.date.yearsBetweenDates
import com.ternaryop.widget.CheckableImageView
import java.util.Locale

class BirthdayPhotoAdapter(
    private val context: Context
) : RecyclerView.Adapter<BirthdayPhotoAdapter.ViewHolder>(), View.OnClickListener, View.OnLongClickListener {
    private val items: MutableList<Birthday> = mutableListOf()

    var isShowButtons: Boolean = false

    var onPhotoBrowseClick: OnPhotoBrowseClickMultiChoice? = null

    val selection = SelectionArrayViewHolder(this)
    val selectedItems: List<Birthday>
        get() = selection.selectedPositions.map { getItem(it) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.birthday_photo_item, parent, false))
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

    fun setBirthdays(collection: Collection<Birthday>) {
        items.clear()
        items.addAll(collection)
    }

    fun sort() = items.sortWith { lhr, rhs -> lhr.name.compareTo(rhs.name) }

    fun updatePost(birthday: Birthday, notifyChange: Boolean) {
        val name = birthday.name
        val index = items.indexOfFirst { it.name.equals(name, ignoreCase = true) }

        if (index == -1) {
            return
        }
        items[index] = birthday

        if (notifyChange) {
            notifyItemChanged(index)
        }
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
            val birthdate = item.birthdate
            caption.text = if (birthdate == null) {
                ""
            } else {
                String.format(Locale.US, "%s, %d", item.name, birthdate.yearsBetweenDates())
            }
        }

        private fun setVisibility(showButtons: Boolean) {
            showImageAction.visibility = if (showButtons) View.VISIBLE else View.INVISIBLE
            bgAction.visibility = if (showButtons) View.VISIBLE else View.INVISIBLE
        }

        private fun displayImage(item: Birthday, checked: Boolean) {
            thumbImage.load(checkNotNull(item.getClosestPhotoByWidth(TumblrAltSize.IMAGE_WIDTH_250)).url) {
                placeholder(R.drawable.stub)
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
            showImageAction.tag = bindingAdapterPosition
        }

        fun setOnClickMultiChoiceListeners(
            listener: View.OnClickListener?,
            longClickListener: View.OnLongClickListener
        ) {
            if (listener != null) {
                val position = bindingAdapterPosition
                itemView.setOnClickListener(listener)
                itemView.setOnLongClickListener(longClickListener)
                itemView.isLongClickable = true
                itemView.tag = position
            }
        }
    }
}
