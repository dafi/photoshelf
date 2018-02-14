package com.ternaryop.photoshelf.adapter

import android.content.Context
import android.preference.PreferenceManager
import android.support.v7.widget.RecyclerView
import android.util.Range
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import com.ternaryop.lazyimageloader.ImageLoader
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.event.CounterEvent
import com.ternaryop.photoshelf.util.sort.AbsSortable
import com.ternaryop.photoshelf.util.sort.Sortable
import org.greenrobot.eventbus.EventBus
import java.util.Calendar
import java.util.Collections

const val PHOTO_ADAPTER_SORT_TAG_NAME = 1
const val PHOTO_ADAPTER_SORT_LAST_PUBLISHED_TAG = 2
const val PHOTO_ADAPTER_SORT_UPLOAD_TIME = 3

class PhotoAdapter(private val context: Context, prefix: String) : RecyclerView.Adapter<PhotoViewHolder>(), View.OnClickListener, View.OnLongClickListener {

    private val imageLoader = ImageLoader(context.applicationContext, prefix, R.drawable.stub)
    private var visiblePosts: MutableList<PhotoShelfPost>
    private val allPosts = mutableListOf<PhotoShelfPost>()
    private var onPhotoBrowseClick: OnPhotoBrowseClick? = null
    private val thumbnailWidth: Int

    private val tagNameSortable: PhotoShelfPostSortable by lazy { TagNameSortable(true) }
    private val uploadTimeSortable: PhotoShelfPostSortable by lazy { UploadTimeSortable(true) }
    private var lastPublishedTagSortable: PhotoShelfPostSortable = LastPublishedTagSortable(true)
    private var _currentSortable: PhotoShelfPostSortable = lastPublishedTagSortable

    var counterType = CounterEvent.NONE
    val selection = SelectionArrayViewHolder(this)

    val filter: Filter
        get() = object : Filter() {

            override fun publishResults(constraint: CharSequence, results: Filter.FilterResults) {
                if (results.values === allPosts) {
                    visiblePosts = allPosts
                } else {
                    @Suppress("UNCHECKED_CAST")
                    visiblePosts = results.values as MutableList<PhotoShelfPost>
                    calcGroupIds()
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

    val currentSort: Int
        get() = _currentSortable.sortId

    val currentSortable: Sortable
        get() = _currentSortable

    val photoList: List<PhotoShelfPost>
        get() = visiblePosts

    val selectedPosts: List<PhotoShelfPost>
        get() = selection.selectedPositions.map { getItem(it) }

    init {
        visiblePosts = allPosts
        thumbnailWidth = PreferenceManager.getDefaultSharedPreferences(context).getString("thumbnail_width", "75").toInt()
    }

    fun setOnPhotoBrowseClick(onPhotoBrowseClick: OnPhotoBrowseClick) {
        this.onPhotoBrowseClick = onPhotoBrowseClick
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        return PhotoViewHolder(LayoutInflater.from(context).inflate(R.layout.list_row, parent, false))
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val listener = if (onPhotoBrowseClick == null) null else this
        val showUploadTime = currentSort == PHOTO_ADAPTER_SORT_UPLOAD_TIME
        val post = visiblePosts[position]
        holder.bindModel(post, imageLoader, thumbnailWidth, showUploadTime)
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

    fun calcGroupIds() {
        val count = itemCount

        if (count > 0) {
            var groupId = 0

            var last = getItem(0).firstTag
            getItem(0).groupId = groupId

            var i = 1
            while (i < count) {
                // set same groupId for all identical tags
                while (i < count && getItem(i).firstTag.equals(last, ignoreCase = true)) {
                    getItem(i++).groupId = groupId
                }
                if (i < count) {
                    ++groupId
                    getItem(i).groupId = groupId
                    last = getItem(i).firstTag
                }
                i++
            }
        }
    }

    fun getPosition(post: PhotoShelfPost) = visiblePosts.indexOf(post)

    fun addAll(collection: Collection<PhotoShelfPost>) {
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
        if (isSortNeeded && currentSort == PHOTO_ADAPTER_SORT_LAST_PUBLISHED_TAG) {
            sort()
        } else {
            calcGroupIds()
        }
    }

    private fun moveGroup(item: PhotoShelfPost, position: Int) {
        val range = getGroupRangeFromPosition(position, item.groupId)
        if (range != null) {
            notifyItemRangeRemoved(range.lower, range.upper - range.lower)
        }
    }

    private fun getGroupRangeFromPosition(position: Int, groupId: Int): Range<Int>? {
        if (position < 0) {
            return null
        }
        var min = position
        var max = position

        while (min > 0 && visiblePosts[min - 1].groupId == groupId) {
            --min
        }

        val lastIndex = visiblePosts.size - 1
        while (max < lastIndex && visiblePosts[max].groupId == groupId) {
            ++max
        }

        // group is empty
        return if (min == max) {
            null
        } else Range.create(min, max)
    }

    private fun sort(sortable: PhotoShelfPostSortable) {
        if (_currentSortable === sortable) {
            val ascending = sortable.isAscending
            sortable.isAscending = !ascending
        } else {
            _currentSortable.resetDefault()
            _currentSortable = sortable
        }
        _currentSortable.sort()
    }

    fun sortByTagName() {
        sort(tagNameSortable)
    }

    fun sortByLastPublishedTag() {
        if (currentSort == PHOTO_ADAPTER_SORT_LAST_PUBLISHED_TAG) {
            return
        }
        sort(lastPublishedTagSortable)
    }

    fun sortByUploadTime() {
        sort(uploadTimeSortable)
    }

    fun sort(sortType: Int, isAscending: Boolean) {
        _currentSortable = when (sortType) {
            PHOTO_ADAPTER_SORT_TAG_NAME -> tagNameSortable
            PHOTO_ADAPTER_SORT_LAST_PUBLISHED_TAG -> lastPublishedTagSortable
            PHOTO_ADAPTER_SORT_UPLOAD_TIME -> uploadTimeSortable
            else -> lastPublishedTagSortable
        }
        _currentSortable.isAscending = isAscending
    }

    /**
     * Sort the list using the last used sort method
     */
    fun sort() {
        _currentSortable.sort()
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

    private abstract inner class PhotoShelfPostSortable(isDefaultAscending: Boolean, sortId: Int) : AbsSortable(isDefaultAscending, sortId) {

        fun resetDefault() {
            isAscending = isDefaultAscending
        }
    }

    private inner class TagNameSortable(isDefaultAscending: Boolean) : PhotoShelfPostSortable(isDefaultAscending, PHOTO_ADAPTER_SORT_TAG_NAME) {

        override fun sort() {
            visiblePosts.sortWith(Comparator { lhs, rhs -> LastPublishedTimestampComparator.compareTag(lhs, rhs, isAscending) })
            calcGroupIds()
        }
    }

    private inner class LastPublishedTagSortable(isDefaultAscending: Boolean) : PhotoShelfPostSortable(isDefaultAscending, PHOTO_ADAPTER_SORT_LAST_PUBLISHED_TAG) {

        override fun sort() {
            Collections.sort(visiblePosts, LastPublishedTimestampComparator())
            calcGroupIds()
        }
    }

    private inner class UploadTimeSortable(isDefaultAscending: Boolean) : PhotoShelfPostSortable(isDefaultAscending, PHOTO_ADAPTER_SORT_UPLOAD_TIME) {
        override fun sort() {
            visiblePosts.sortWith(Comparator { lhs, rhs ->
                val diff = lhs.timestamp - rhs.timestamp
                val compare = if (diff < -1) -1 else if (diff > 1) 1 else 0
                when (compare) {
                    0 -> lhs.firstTag.compareTo(rhs.firstTag, true)
                    else -> if (isAscending) compare else -compare
                }
            })
            calcGroupIds()
        }
    }

    fun notifyCountChanged() {
        if (counterType == CounterEvent.NONE) {
            return
        }
        EventBus.getDefault().post(CounterEvent(counterType, itemCount.toLong()))
    }
}