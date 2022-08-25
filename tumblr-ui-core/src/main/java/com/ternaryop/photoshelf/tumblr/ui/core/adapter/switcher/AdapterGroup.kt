package com.ternaryop.photoshelf.tumblr.ui.core.adapter.switcher

import android.content.Context
import androidx.recyclerview.widget.RecyclerView

/**
 * Create the adapters used to switch the view from list to grid an vice versa
 */
interface AdapterGroup<A : RecyclerView.Adapter<RecyclerView.ViewHolder>> {
    val config: AdapterSwitcherConfig
    val recyclerView: RecyclerView
    val adapter: A

    fun createListAdapter(
        context: Context,
        currentAdapter: A?
    ): RecyclerView.Adapter<out RecyclerView.ViewHolder>

    fun createGridAdapter(
        context: Context,
        currentAdapter: A?
    ): RecyclerView.Adapter<out RecyclerView.ViewHolder>
}
