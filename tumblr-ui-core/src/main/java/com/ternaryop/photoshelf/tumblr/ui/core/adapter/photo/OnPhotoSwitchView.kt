package com.ternaryop.photoshelf.tumblr.ui.core.adapter.photo

import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.ViewType
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.switcher.AdapterSwitcher
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.switcher.OnSwitchView

class OnPhotoSwitchView : OnSwitchView<PhotoAdapter<RecyclerView.ViewHolder>> {
    override fun onSwitched(
        adapterSwitcher: AdapterSwitcher<PhotoAdapter<RecyclerView.ViewHolder>>,
        newViewType: ViewType
    ) {
        adapterSwitcher.adapterGroup.adapter.sortSwitcher.loadSettings(
            adapterSwitcher.adapterGroup.config.prefNamePrefix,
            PreferenceManager.getDefaultSharedPreferences(adapterSwitcher.adapterGroup.recyclerView.context)
        )
    }
}