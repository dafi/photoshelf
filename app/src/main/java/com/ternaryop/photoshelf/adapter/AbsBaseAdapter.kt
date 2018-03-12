package com.ternaryop.photoshelf.adapter

import android.support.v7.widget.RecyclerView
import android.view.View

/**
 * Created by dave on 10/03/18.
 * Base Adapter
 */
abstract class AbsBaseAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
    fun setEmptyView(view: View) {
        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                view.visibility = if (itemCount == 0) View.VISIBLE else View.GONE
            }
        })
    }
}