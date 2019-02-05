package com.ternaryop.photoshelf.fragment.draft

import android.view.Menu
import android.view.MenuItem
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.photo.PhotoSortSwitcher
import com.ternaryop.photoshelf.fragment.BottomMenuListener

class DraftSortBottomMenuListener(
    private val draftListFragment: DraftListFragment,
    private val sortSwitcher: PhotoSortSwitcher) : BottomMenuListener {
    override val title: String?
        get() = draftListFragment.resources.getString(R.string.sort_by)
    override val menuId: Int
        get() = R.menu.draft_sort

    private val uploadTimeId =
        if (sortSwitcher.sortable.isAscending) R.id.sort_upload_time_date_oldest else R.id.sort_upload_time_date_newest

    private val tagNameId =
        if (sortSwitcher.sortable.isAscending) R.id.sort_tag_name_a_z else R.id.sort_tag_name_z_a

    override fun setupMenu(menu: Menu) {
        when (sortSwitcher.sortable.sortId) {
            PhotoSortSwitcher.TAG_NAME -> menu.findItem(tagNameId).isChecked = true
            PhotoSortSwitcher.LAST_PUBLISHED_TAG -> menu.findItem(R.id.sort_published_tag).isChecked = true
            PhotoSortSwitcher.UPLOAD_TIME -> menu.findItem(uploadTimeId).isChecked = true
        }
    }

    override fun onItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.sort_tag_name_a_z -> draftListFragment.sortBy(PhotoSortSwitcher.TAG_NAME, true)
            R.id.sort_tag_name_z_a -> draftListFragment.sortBy(PhotoSortSwitcher.TAG_NAME, false)
            R.id.sort_published_tag -> draftListFragment.sortBy(PhotoSortSwitcher.LAST_PUBLISHED_TAG, true)
            R.id.sort_upload_time_date_newest -> draftListFragment.sortBy(PhotoSortSwitcher.UPLOAD_TIME, false)
            R.id.sort_upload_time_date_oldest -> draftListFragment.sortBy(PhotoSortSwitcher.UPLOAD_TIME, true)
        }
    }
}