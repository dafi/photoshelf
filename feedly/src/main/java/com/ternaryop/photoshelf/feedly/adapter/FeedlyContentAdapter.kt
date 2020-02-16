package com.ternaryop.photoshelf.feedly.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.feedly.FeedlyContent
import com.ternaryop.photoshelf.feedly.R

class FeedlyContentAdapter(private val context: Context) :
    RecyclerView.Adapter<FeedlyContentViewHolder>(), View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    val allContents = mutableListOf<FeedlyContentDelegate>()
    val sortSwitcher = FeedlyContentSortSwitcher(context)

    var clickListener: OnFeedlyContentClick? = null

    val uncheckedItems: List<FeedlyContent>
        get() = allContents.filterNot { it.isChecked }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedlyContentViewHolder {
        return FeedlyContentViewHolder(LayoutInflater.from(context).inflate(R.layout.list_row_2, parent, false))
    }

    override fun onBindViewHolder(holder: FeedlyContentViewHolder, position: Int) {
        val content = allContents[position]
        holder.bindModel(content)
        setClickListeners(content, holder)
    }

    private fun setClickListeners(content: FeedlyContentDelegate, holder: FeedlyContentViewHolder) {
        if (clickListener == null) {
            holder.setOnClickListeners(content, null)
            holder.setOnCheckedChangeListener(content, null)
        } else {
            holder.setOnClickListeners(content, this)
            holder.setOnCheckedChangeListener(content, this)
        }
    }

    override fun getItemCount(): Int = allContents.size

    fun addAll(collection: Collection<FeedlyContentDelegate>) {
        allContents.addAll(collection)
    }

    fun clear() {
        allContents.clear()
        notifyDataSetChanged()
    }

    fun getItem(position: Int): FeedlyContentDelegate = allContents[position]

    fun getPositionById(id: String): Int = allContents.indexOfFirst { it.id == id }

    /**
     * Sort the list using the last used sort method
     */
    fun sort() {
        sortSwitcher.sort(allContents)
        notifyDataSetChanged()
    }

    fun sortBy(sortType: Int) {
        sortSwitcher.setType(sortType)
        sort()
    }

    override fun onClick(v: View) {
        val position = getPositionById(v.tag as String)

        if (position == -1) {
            return
        }

        when (v.id) {
            R.id.list_row2 -> clickListener?.onTitleClick(position)
            R.id.tag -> clickListener?.onTagClick(position)
        }
    }

    override fun onCheckedChanged(v: CompoundButton, checked: Boolean) {
        val position = getPositionById(v.tag as String)

        if (position == -1) {
            return
        }
        when (v.id) {
            android.R.id.checkbox -> {
                getItem(position).isChecked = checked
                clickListener?.onToggleClick(position, checked)
            }
        }
    }

    fun moveToBottom(position: Int) {
        if (position == (allContents.size - 1)) {
            return
        }
        allContents.add(allContents.removeAt(position))
        // notifyItemMoved scrolls to bottom so we use the pair notifyItemRemoved/notifyItemInserted
        // to remain on item position
        notifyItemRemoved(position)
        notifyItemInserted(allContents.size - 1)
    }
}
