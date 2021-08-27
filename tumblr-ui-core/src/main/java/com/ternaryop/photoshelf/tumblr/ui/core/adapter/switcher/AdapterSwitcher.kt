package com.ternaryop.photoshelf.tumblr.ui.core.adapter.switcher

import android.content.Context
import android.os.Parcelable
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

    private val layoutStateMap = HashMap<ViewType, Parcelable?>()

    init {
        viewType = loadViewType(context, adapterGroup.config.prefNamePrefix)
        onSwitched(null)
    }

    fun switchView(viewType: ViewType) {
        if (viewType == this.viewType) {
            return
        }
        saveState()
        this.viewType = viewType
        onSwitched(adapterGroup.adapter)
        restoreState()
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

    private fun saveState() {
        layoutStateMap[viewType] = adapterGroup.recyclerView.layoutManager?.onSaveInstanceState()
    }

    private fun restoreState() {
        adapterGroup.recyclerView.layoutManager?.onRestoreInstanceState(layoutStateMap[viewType])
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