package com.ternaryop.photoshelf.adapter.tagnavigator

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.api.post.TagInfo

interface TagNavigatorListener {
    fun onClick(item: TagInfo)
}

class TagNavigatorAdapter(
    private val context: Context,
    list: List<TagInfo>,
    val blogName: String,
    private val listener: TagNavigatorListener)
    : RecyclerView.Adapter<TagNavigatorViewHolder>(),
    View.OnClickListener,
    Filterable,
    TagNavigatorFilterListener {

    val items = list.toMutableList()
    private var tagFilter: TagNavigatorFilter? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagNavigatorViewHolder {
        return TagNavigatorViewHolder(LayoutInflater.from(context)
            .inflate(R.layout.tag_navigator_row, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: TagNavigatorViewHolder, position: Int) {
        val item = items[position]

        holder.bindModel(item, tagFilter?.pattern)
        holder.setOnClickListeners(this)
    }

    override fun onClick(v: View) {
        listener.onClick(items[v.tag as Int])
    }

    fun sortByTagCount() {
        items.sortWith(Comparator { lhs, rhs ->
            // sort descending
            val sign = (rhs.postCount - lhs.postCount).toInt()
            if (sign == 0) lhs.compareTagTo(rhs) else sign
        })
        notifyDataSetChanged()
    }

    fun sortByTagName() {
        items.sortWith(Comparator { lhs, rhs -> lhs.compareTagTo(rhs) })
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        if (tagFilter == null) {
            tagFilter = TagNavigatorFilter(blogName, this)
        }
        return tagFilter!!
    }

    override fun onComplete(filter: TagNavigatorFilter, items: List<TagInfo>) {
        this.items.clear()
        this.items.addAll(items)

        notifyDataSetChanged()
    }

    override fun onError(filter: TagNavigatorFilter, throwable: Throwable) {
        Toast.makeText(context, throwable.message, Toast.LENGTH_LONG).show()
    }
}
