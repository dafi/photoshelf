package com.ternaryop.photoshelf.adapter.feedly

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import com.ternaryop.feedly.FeedlyContent
import com.ternaryop.lazyimageloader.ImageLoader
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.util.sort.AbsSortable

const val FEEDLY_SORT_TITLE_NAME = 1
const val FEEDLY_SORT_SAVED_TIMESTAMP = 2
const val FEEDLY_SORT_LAST_PUBLISH_TIMESTAMP = 3
private const val PREFIX_FAVICON = "favicon"

class FeedlyContentAdapter(private val context: Context, private val tumblrName: String) : RecyclerView.Adapter<FeedlyContentViewHolder>(), View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private val allContents = mutableListOf<FeedlyContentDelegate>()
    private val imageLoader = ImageLoader(context.applicationContext, PREFIX_FAVICON, R.drawable.stub)

    private val titleNameSortable: AbsSortable by lazy {
        object : AbsSortable(true, FEEDLY_SORT_TITLE_NAME) {
            override fun sort() = allContents.sortWith(Comparator { c1, c2 -> c1.compareTitle(c2) })
        }
    }

    private val saveTimestampSortable: AbsSortable by lazy {
        object : AbsSortable(true, FEEDLY_SORT_SAVED_TIMESTAMP) {
            override fun sort() = allContents.sortWith(Comparator { c1, c2 -> c1.compareActionTimestamp(c2) })
        }
    }

    private val lastPublishTimestampSortable: AbsSortable by lazy {
        object : AbsSortable(true, FEEDLY_SORT_LAST_PUBLISH_TIMESTAMP) {
            override fun sort() {
                updateLastPublishTimestamp()
                allContents.sortWith(Comparator { c1, c2 -> c1.compareLastTimestamp(c2) })
            }

            private fun updateLastPublishTimestamp() {
                val titles = HashSet<String>(allContents.size)
                for (fc in allContents) {
                    // replace any no no-break space with whitespace
                    // see http://www.regular-expressions.info/unicode.html for \p{Zs}
                    titles.add(fc.title.replace("""\p{Zs}""".toRegex(), " "))
                    fc.lastPublishTimestamp = java.lang.Long.MIN_VALUE
                }
                val list = DBHelper.getInstance(context).postTagDAO
                        .getListPairLastPublishedTimestampTag(titles, tumblrName)
                for (fc in allContents) {
                    val title = fc.title
                    for (timeTag in list) {
                        if (timeTag.second.regionMatches(0, title, 0, timeTag.second.length, ignoreCase = true)) {
                            fc.lastPublishTimestamp = timeTag.first
                        }
                    }
                }
            }
        }
    }

    var currentSortable = titleNameSortable
        private set

    var clickListener: OnFeedlyContentClick? = null

    val uncheckedItems: List<FeedlyContent>
        get() = allContents.filterNot { it.isChecked }

    var currentSort: Int
        get() = currentSortable.sortId
        set(sortType) {
            when (sortType) {
                FEEDLY_SORT_TITLE_NAME -> currentSortable = titleNameSortable
                FEEDLY_SORT_SAVED_TIMESTAMP -> currentSortable = saveTimestampSortable
                FEEDLY_SORT_LAST_PUBLISH_TIMESTAMP -> currentSortable = lastPublishTimestampSortable
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedlyContentViewHolder {
        return FeedlyContentViewHolder(LayoutInflater.from(context).inflate(R.layout.list_row_2, parent, false))
    }

    override fun onBindViewHolder(holder: FeedlyContentViewHolder, position: Int) {
        holder.bindModel(allContents[position], imageLoader)
        setClickListeners(holder, position)
    }

    private fun setClickListeners(holder: FeedlyContentViewHolder, position: Int) {
        if (clickListener == null) {
            holder.setOnClickListeners(allContents[position], null)
            holder.setOnCheckedChangeListener(allContents[position], null)
        } else {
            holder.setOnClickListeners(allContents[position], this)
            holder.setOnCheckedChangeListener(allContents[position], this)
        }
    }

    override fun getItemCount(): Int {
        return allContents.size
    }

    fun addAll(collection: Collection<FeedlyContent>) {
        collection.mapTo(allContents) { FeedlyContentDelegate(it) }
        notifyDataSetChanged()
    }

    fun clear() {
        allContents.clear()
        notifyDataSetChanged()
    }

    fun getItem(position: Int): FeedlyContent {
        return allContents[position]
    }

    /**
     * Sort the list using the last used sort method
     */
    fun sort() {
        currentSortable.sort()
    }

    private fun sort(sortable: AbsSortable) {
        currentSortable = sortable
        currentSortable.sort()
    }

    fun sortByTitleName() {
        sort(titleNameSortable)
    }

    fun sortBySavedTimestamp() {
        sort(saveTimestampSortable)
    }

    fun sortByLastPublishTimestamp() {
        sort(lastPublishTimestampSortable)
    }

    override fun onClick(v: View) {
        val position = v.tag as Int
        when (v.id) {
            R.id.list_row2 -> clickListener!!.onTitleClick(position)
        }
    }

    override fun onCheckedChanged(v: CompoundButton, checked: Boolean) {
        val position = v.tag as Int
        when (v.id) {
            android.R.id.checkbox -> {
                (getItem(position) as FeedlyContentDelegate).isChecked = checked
                clickListener!!.onToggleClick(position, checked)
            }
        }
    }
}