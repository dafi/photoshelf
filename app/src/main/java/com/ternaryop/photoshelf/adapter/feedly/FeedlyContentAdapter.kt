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

private const val PREFIX_FAVICON = "favicon"

class FeedlyContentAdapter(private val context: Context, tumblrName: String) :
    RecyclerView.Adapter<FeedlyContentViewHolder>(), View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private val allContents = mutableListOf<FeedlyContentDelegate>()
    private val imageLoader = ImageLoader(context.applicationContext, PREFIX_FAVICON, R.drawable.stub)
    val sortSwitcher = FeedlyContentSortSwitcher(context, tumblrName)

    var clickListener: OnFeedlyContentClick? = null

    val uncheckedItems: List<FeedlyContent>
        get() = allContents.filterNot { it.isChecked }

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
        sortSwitcher.sort(allContents)
    }

    fun sortBy(sortType: Int) {
        sortSwitcher.setType(sortType)
        sort()
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