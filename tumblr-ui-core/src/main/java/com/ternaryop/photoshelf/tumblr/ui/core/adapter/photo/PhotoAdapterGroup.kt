package com.ternaryop.photoshelf.tumblr.ui.core.adapter.photo

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClick
import com.ternaryop.photoshelf.tumblr.ui.core.R
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.switcher.AdapterGroup
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.switcher.AdapterSwitcher
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.switcher.AdapterSwitcherConfig
import com.ternaryop.photoshelf.tumblr.ui.core.prefs.thumbnailWidth
import com.ternaryop.utils.recyclerview.AutofitGridLayoutManager

typealias PhotoAdapterSwitcher = AdapterSwitcher<PhotoAdapter<RecyclerView.ViewHolder>>

class PhotoAdapterGroup(
    override val config: AdapterSwitcherConfig,
    override val recyclerView: RecyclerView,
    var photoBrowseClickListener: OnPhotoBrowseClick?
) : AdapterGroup<PhotoAdapter<RecyclerView.ViewHolder>> {
    // used to restore it if necessary
    private var itemAnimator = recyclerView.itemAnimator
    override val adapter: PhotoAdapter<RecyclerView.ViewHolder>
        get() = recyclerView.adapter as PhotoAdapter<RecyclerView.ViewHolder>

    override fun createListAdapter(
        context: Context,
        currentAdapter: PhotoAdapter<RecyclerView.ViewHolder>?
    ): RecyclerView.Adapter<out RecyclerView.ViewHolder> {
        val thumbnailWidth = PreferenceManager.getDefaultSharedPreferences(context)
            .thumbnailWidth(context.resources.getInteger(R.integer.thumbnail_width_value_default))
        val adapter = PhotoListRowAdapter(context, thumbnailWidth)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.itemAnimator = itemAnimator
        recyclerView.setPadding(0, 0, 0, 0)

        setupAdapter(adapter, currentAdapter)

        return adapter
    }

    override fun createGridAdapter(
        context: Context,
        currentAdapter: PhotoAdapter<RecyclerView.ViewHolder>?
    ): RecyclerView.Adapter<out RecyclerView.ViewHolder> {
        val adapter = PhotoGridAdapter(context, config.colorCellByScheduleTimeType)
        recyclerView.layoutManager = AutofitGridLayoutManager(
            context,
            context.resources.getDimension(R.dimen.grid_photo_thumb_width).toInt()
        )
        recyclerView.itemAnimator = null
        recyclerView.setPadding(
            context.resources.getDimension(R.dimen.photo_grid_padding_start).toInt(),
            context.resources.getDimension(R.dimen.photo_grid_padding_top).toInt(),
            context.resources.getDimension(R.dimen.photo_grid_padding_end).toInt(),
            context.resources.getDimension(R.dimen.photo_grid_padding_bottom).toInt()
        )

        setupAdapter(adapter, currentAdapter)

        return adapter
    }

    private fun setupAdapter(
        adapter: PhotoAdapter<out RecyclerView.ViewHolder>,
        currentAdapter: PhotoAdapter<out RecyclerView.ViewHolder>?
    ) {
        adapter.onPhotoBrowseClick = photoBrowseClickListener
        if (currentAdapter != null) {
            adapter.addAll(currentAdapter.allPosts)
        }
        recyclerView.adapter = adapter
    }
}
