package com.ternaryop.photoshelf.adapter.mru

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.R

/**
 * Created by dave on 15/12/17.
 * The adapter used to selected and remove MRU items
 */

class MRUAdapter(private val context: Context, private val items: MutableList<String>, val maxTopItems: Int)
    : RecyclerView.Adapter<MRUViewHolder>(), View.OnClickListener {
    var onMRUListener: OnMRUListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MRUViewHolder {
        return MRUViewHolder(LayoutInflater.from(context)
            .inflate(R.layout.mru_list_row, parent, false))
    }

    override fun onBindViewHolder(holder: MRUViewHolder, position: Int) {
        val item = items[position]
        holder.bindModel(item, maxTopItems)

        holder.setOnClickListeners(item, if (onMRUListener == null) null else this)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun getItem(position: Int): String {
        return items[position]
    }

    override fun onClick(v: View) {
        onMRUListener!!.onItemSelect(v.tag as String)
    }

    fun removeAt(position: Int) {
        onMRUListener!!.onItemDelete(items[position])
        items.removeAt(position)
        notifyItemRemoved(position)
    }
}
