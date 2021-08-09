package com.ternaryop.photoshelf.tumblr.ui.core.adapter.switcher

import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.ViewType

interface OnSwitchView <A: RecyclerView.Adapter<RecyclerView.ViewHolder>> {
    fun onSwitched(adapterSwitcher: AdapterSwitcher<A>, newViewType: ViewType)
}