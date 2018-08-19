package com.ternaryop.photoshelf.adapter.feedly

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.feedly.FeedlyContent
import com.ternaryop.photoshelf.R

class FeedlyContentAdapter(private val context: Context) :
    RecyclerView.Adapter<FeedlyContentViewHolder>(), View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private val allContents = mutableListOf<FeedlyContentDelegate>()
    val sortSwitcher = FeedlyContentSortSwitcher(context)

    var clickListener: OnFeedlyContentClick? = null

    val uncheckedItems: List<FeedlyContent>
        get() = allContents.filterNot { it.isChecked }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedlyContentViewHolder {
        return FeedlyContentViewHolder(LayoutInflater.from(context).inflate(R.layout.list_row_2, parent, false))
    }

    override fun onBindViewHolder(holder: FeedlyContentViewHolder, position: Int) {
        holder.bindModel(allContents[position])
        setClickListeners(holder)
    }

    private fun setClickListeners(holder: FeedlyContentViewHolder) {
        if (clickListener == null) {
            holder.setOnClickListeners(null)
            holder.setOnCheckedChangeListener(null)
        } else {
            holder.setOnClickListeners(this)
            holder.setOnCheckedChangeListener(this)
        }
    }

    override fun getItemCount(): Int = allContents.size

    fun addAll(collection: Collection<FeedlyContentDelegate>) {
        allContents.addAll(collection)
        notifyDataSetChanged()
    }

    fun clear() {
        allContents.clear()
        notifyDataSetChanged()
    }

    fun getItem(position: Int): FeedlyContentDelegate = allContents[position]

    /**
     * Sort the list using the last used sort method
     */
    fun sort() = sortSwitcher.sort(allContents)

    fun sortBy(sortType: Int) {
        sortSwitcher.setType(sortType)
        sort()
    }

    override fun onClick(v: View) {
        val position = v.tag as Int
        when (v.id) {
            R.id.list_row2 -> clickListener!!.onTitleClick(position)
            R.id.tag -> clickListener!!.onTagClick(position)
        }
    }

    override fun onCheckedChanged(v: CompoundButton, checked: Boolean) {
        val position = v.tag as Int
        when (v.id) {
            android.R.id.checkbox -> {
                getItem(position).isChecked = checked
                clickListener!!.onToggleClick(position, checked)
            }
        }
    }
}