package com.ternaryop.photoshelf.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView

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