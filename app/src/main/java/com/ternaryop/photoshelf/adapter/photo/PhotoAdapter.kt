package com.ternaryop.photoshelf.adapter.photo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.preference.PreferenceManager
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.AbsBaseAdapter
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClick
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.adapter.SelectionArrayViewHolder
import com.ternaryop.photoshelf.adapter.photo.PhotoSortSwitcher.Companion.LAST_PUBLISHED_TAG
import com.ternaryop.photoshelf.adapter.photo.PhotoSortSwitcher.Companion.UPLOAD_TIME
import com.ternaryop.photoshelf.event.CounterEvent
import org.greenrobot.eventbus.EventBus
import java.util.Calendar

class PhotoAdapter(private val context: Context)
    : AbsBaseAdapter<PhotoViewHolder>(), View.OnClickListener, View.OnLongClickListener, Filterable {

    private val allPosts = mutableListOf<PhotoShelfPost>()
    private var visiblePosts = allPosts
    private var onPhotoBrowseClick: OnPhotoBrowseClick? = null
    private val thumbnailWidth = PreferenceManager
        .getDefaultSharedPreferences(context).getString("thumbnail_width", "75")!!.toInt()

    val sortSwitcher = PhotoSortSwitcher()

    var counterType = CounterEvent.NONE
    val selection = SelectionArrayViewHolder(this)

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun publishResults(constraint: CharSequence, results: Filter.FilterResults) {
                if (results.values === allPosts) {
                    visiblePosts = allPosts
                } else {
                    @Suppress("UNCHECKED_CAST")
                    visiblePosts = results.values as MutableList<PhotoShelfPost>
                    PhotoGroup.calcGroupIds(visiblePosts)
                }
                notifyDataSetChanged()
            }

            override fun performFiltering(constraint: CharSequence): Filter.FilterResults {
                val results = Filter.FilterResults()
                val pattern = constraint.toString().trim { it <= ' ' }

                if (pattern.isEmpty()) {
                    results.count = allPosts.size
                    results.values = allPosts
                } else {
                    val filteredPosts = allPosts.filter { it.firstTag.contains(pattern, true) }

                    results.count = filteredPosts.size
                    results.values = filteredPosts
                }

                return results
            }
        }
    }

    val photoList: List<PhotoShelfPost>
        get() = visiblePosts

    val selectedPosts: List<PhotoShelfPost>
        get() = selection.selectedPositions.map { getItem(it) }

    fun setOnPhotoBrowseClick(onPhotoBrowseClick: OnPhotoBrowseClick) {
        this.onPhotoBrowseClick = onPhotoBrowseClick
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        return PhotoViewHolder(LayoutInflater.from(context).inflate(R.layout.list_row, parent, false))
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val listener = if (onPhotoBrowseClick == null) null else this
        val showUploadTime = sortSwitcher.sortable.sortId == UPLOAD_TIME
        val post = visiblePosts[position]
        holder.bindModel(post, thumbnailWidth, showUploadTime)
        holder.setOnClickListeners(listener)
        if (onPhotoBrowseClick is OnPhotoBrowseClickMultiChoice) {
            holder.setOnClickMultiChoiceListeners(listener, this)
        }
        holder.itemView.isSelected = selection.isSelected(position)
    }

    override fun getItemCount() = visiblePosts.size

    override fun onClick(v: View) {
        when (v.id) {
            R.id.thumbnail_image -> onPhotoBrowseClick!!.onThumbnailImageClick(v.tag as Int)
            R.id.menu -> onPhotoBrowseClick!!.onOverflowClick(v.tag as Int, v)
            R.id.list_row -> (onPhotoBrowseClick as OnPhotoBrowseClickMultiChoice).onItemClick(v.tag as Int)
            R.id.tag_text_view -> {
                val position = (v.parent as ViewGroup).tag as Int
                onPhotoBrowseClick!!.onTagClick(position, v.tag as String)
            }
        }
    }

    override fun onLongClick(v: View): Boolean {
        (onPhotoBrowseClick as OnPhotoBrowseClickMultiChoice).onItemLongClick(v.tag as Int)
        return true
    }

    fun getItem(position: Int) = visiblePosts[position]

    fun getPosition(post: PhotoShelfPost) = visiblePosts.indexOf(post)

    fun setPosts(collection: Collection<PhotoShelfPost>) {
        allPosts.clear()
        allPosts.addAll(collection)
        notifyDataSetChanged()
    }

    fun clear() {
        visiblePosts.clear()
        allPosts.clear()
        notifyDataSetChanged()
    }

    fun remove(item: PhotoShelfPost): Int {
        // if they point to the same list then remove the item only once
        if (visiblePosts !== allPosts) {
            allPosts.remove(item)
        }
        val position = visiblePosts.indexOf(item)
        if (position >= 0) {
            visiblePosts.removeAt(position)
            notifyItemRemoved(position)
        }
        return position
    }

    fun removeAndRecalcGroups(item: PhotoShelfPost, lastPublishDateTime: Calendar) {
        moveGroup(item, remove(item))
        var isSortNeeded = false
        val tag = item.firstTag

        for (post in visiblePosts) {
            if (post.firstTag.equals(tag, ignoreCase = true)) {
                isSortNeeded = true
                post.lastPublishedTimestamp = lastPublishDateTime.timeInMillis
            }
        }
        if (isSortNeeded && sortSwitcher.sortable.sortId == LAST_PUBLISHED_TAG) {
            sort()
        } else {
            PhotoGroup.calcGroupIds(visiblePosts)
        }
    }

    private fun moveGroup(item: PhotoShelfPost, position: Int) {
        val range = PhotoGroup.getRangeFromPosition(visiblePosts, position, item.groupId)
        if (range != null) {
            notifyItemRangeRemoved(range.lower, range.upper - range.lower)
        }
    }

    fun sortBy(sortType: Int, isAscending: Boolean) {
        sortSwitcher.setType(sortType, isAscending)
        sort()
    }

    fun toogleSortBy(sortType: Int) {
        if (sortSwitcher.changeDirection(sortType)) {
            sort()
        }
    }

    fun sort() {
        sortSwitcher.sort(visiblePosts)
        PhotoGroup.calcGroupIds(visiblePosts)
    }

    fun notifyCountChanged() {
        if (counterType == CounterEvent.NONE) {
            return
        }
        EventBus.getDefault().post(CounterEvent(counterType, itemCount))
    }

    /**
     * @return the tag index if found, -1 otherwise
     */
    fun findTagIndex(tag: String?): Int {
        tag ?: return -1
        return photoList.indexOfFirst { it.firstTag.compareTo(tag, ignoreCase = true) == 0 }
    }

    fun tagArrayList() = photoList.mapTo(ArrayList()) { it.firstTag }
}