package com.ternaryop.photoshelf.tumblr.ui.core.adapter.photo

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClick
import com.ternaryop.photoshelf.tumblr.ui.core.R
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.ViewType
import com.ternaryop.photoshelf.tumblr.ui.core.prefs.thumbnailWidth
import com.ternaryop.utils.recyclerview.AutofitGridLayoutManager

private const val PREF_VIEW_TYPE = "photo_adapter_view_type"

class PhotoAdapterSwitcher(
    val prefNamePrefix: String,
    val recyclerView: RecyclerView,
    var photoBrowseClickListener: OnPhotoBrowseClick?
) {
    var viewType: ViewType
        private set
    val photoAdapter: PhotoAdapter<out RecyclerView.ViewHolder>
        get() = recyclerView.adapter as PhotoAdapter<out RecyclerView.ViewHolder>

    // used to restore it if necessary
    private var itemAnimator = recyclerView.itemAnimator

    init {
        viewType = loadViewType(recyclerView.context, prefNamePrefix)
        setupAdapter(null)
    }

    fun switchView(viewType: ViewType) {
        if (viewType == this.viewType) {
            return
        }
        this.viewType = viewType
        setupAdapter(photoAdapter.allPosts)
    }

    private fun setupAdapter(posts: List<PhotoShelfPost>?) {
        val adapter = when (viewType) {
            ViewType.List -> createListAdapter(recyclerView.context)
            ViewType.Grid -> createGridAdapter(recyclerView.context)
        }
        adapter.onPhotoBrowseClick = photoBrowseClickListener
        if (posts != null) {
            adapter.addAll(posts)
        }
        adapter.sortSwitcher.loadSettings(
            prefNamePrefix, PreferenceManager.getDefaultSharedPreferences(recyclerView.context)
        )
        recyclerView.adapter = adapter
    }

    private fun createListAdapter(context: Context): PhotoListRowAdapter {
        val thumbnailWidth = PreferenceManager.getDefaultSharedPreferences(context)
            .thumbnailWidth(context.resources.getInteger(R.integer.thumbnail_width_value_default))
        val adapter = PhotoListRowAdapter(context, thumbnailWidth)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.itemAnimator = itemAnimator
        recyclerView.setPadding(0, 0, 0, 0)

        return adapter
    }

    private fun createGridAdapter(context: Context): PhotoGridAdapter {
        val adapter = PhotoGridAdapter(context)
        recyclerView.layoutManager = AutofitGridLayoutManager(
            context,
            context.resources.getDimension(R.dimen.grid_photo_thumb_width).toInt()
        )
        recyclerView.itemAnimator = null
        recyclerView.setPadding(
            context.resources.getDimension(R.dimen.photo_grid_padding_start).toInt(),
            context.resources.getDimension(R.dimen.photo_grid_padding_top).toInt(),
            context.resources.getDimension(R.dimen.photo_grid_padding_end).toInt(),
            context.resources.getDimension(R.dimen.photo_grid_padding_bottom).toInt())

        return adapter
    }

    fun saveSortSwitcher() {
        PreferenceManager.getDefaultSharedPreferences(recyclerView.context).edit().also {
            photoAdapter.sortSwitcher.saveSettings(prefNamePrefix, it)
        }.apply()
    }

    fun toggleView() {
        val viewType = if (viewType == ViewType.List) {
            ViewType.Grid
        } else {
            ViewType.List
        }
        saveViewType(recyclerView.context, prefNamePrefix, viewType)

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