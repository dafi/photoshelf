package com.ternaryop.photoshelf.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by dave on 10/03/18.
 * Base Adapter
 */
abstract class AbsBaseAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
    fun setEmptyView(view: View, onAdapterChanged: ((RecyclerView.Adapter<VH>) -> Boolean)? = null) {
        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                if (onAdapterChanged == null || onAdapterChanged(this@AbsBaseAdapter)) {
                    view.visibility = if (itemCount == 0) View.VISIBLE else View.GONE
                }
            }
        })
    }
}
