package com.ternaryop.photoshelf.dialogs.mru

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by dave on 15/12/17.
 * The adapter used to selected and remove MRU items
 */

class MRUAdapter(private val dialog: MRUDialog, private val items: MutableList<String>)
    : RecyclerView.Adapter<MRUViewHolder>(), View.OnClickListener {
    var onMRUListener: OnMRUListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MRUViewHolder {
        return MRUViewHolder(LayoutInflater.from(dialog.activity)
            .inflate(android.R.layout.simple_list_item_1, parent, false))
    }

    override fun onBindViewHolder(holder: MRUViewHolder, position: Int) {
        val item = items[position]
        holder.bindModel(item)

        holder.setOnClickListeners(item, if (onMRUListener == null) null else this)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun getItem(position: Int): String {
        return items[position]
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun getPosition(item: String): Int {
        return items.indexOf(item)
    }

    override fun onClick(v: View) {
        val positions = intArrayOf(getPosition(v.tag as String))
        onMRUListener!!.onItemsSelected(dialog, positions)
    }

    fun removeAt(position: Int) {
        onMRUListener!!.onItemDelete(dialog, position)
        items.removeAt(position)
        notifyItemRemoved(position)
    }
}
