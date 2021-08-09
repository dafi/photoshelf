package com.ternaryop.photoshelf.tumblr.ui.core.adapter.switcher

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.ViewType

private const val PREF_VIEW_TYPE = "photo_adapter_view_type"

class AdapterSwitcher<T: RecyclerView.Adapter<RecyclerView.ViewHolder>>(
    val context: Context,
    val adapterGroup: AdapterGroup<T>,
    val onSwitchView: OnSwitchView<T>?
) {
    var viewType: ViewType
        private set

    init {
        viewType = loadViewType(context, adapterGroup.config.prefNamePrefix)
        onSwitched(null)
    }

    fun switchView(viewType: ViewType) {
        if (viewType == this.viewType) {
            return
        }
        this.viewType = viewType
        onSwitched(adapterGroup.adapter)
    }

    private fun onSwitched(adapter: T?) {
        when (viewType) {
            ViewType.List -> adapterGroup.createListAdapter(context, adapter)
            ViewType.Grid -> adapterGroup.createGridAdapter(context, adapter)
        }
        onSwitchView?.onSwitched(this, viewType)
    }

    fun toggleView() {
        val viewType = if (viewType == ViewType.List) {
            ViewType.Grid
        } else {
            ViewType.List
        }
        saveViewType(context, adapterGroup.config.prefNamePrefix, viewType)

        switchView(viewType)
    }

    companion object {
        fun loadViewType(context: Context, prefNamePrefix: String): ViewType {
            return ViewType.load(
                PreferenceManager.getDefaultSharedPreferences(context),
                prefNamePrefix + PREF_VIEW_TYPE
            )
        }

        fun saveViewType(context: Context, prefNamePrefix: String, viewType: ViewType) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().also {
                viewType.save(it, prefNamePrefix + PREF_VIEW_TYPE)
            }.apply()
        }
    }
}