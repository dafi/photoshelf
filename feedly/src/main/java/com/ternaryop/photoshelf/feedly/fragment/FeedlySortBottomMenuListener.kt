package com.ternaryop.photoshelf.feedly.fragment

import android.view.Menu
import android.view.MenuItem
import com.ternaryop.photoshelf.feedly.R
import com.ternaryop.photoshelf.feedly.adapter.FeedlyContentSortSwitcher
import com.ternaryop.photoshelf.fragment.BottomMenuListener

class FeedlySortBottomMenuListener(
    private val feedlyListFragment: FeedlyListFragment,
    private val sortSwitcher: FeedlyContentSortSwitcher
) : BottomMenuListener {
    override val title: String?
        get() = feedlyListFragment.resources.getString(R.string.sort_by)
    override val menuId: Int
        get() = R.menu.feedly_sort

    override fun setupMenu(menu: Menu) {
        when (sortSwitcher.currentSortable.sortId) {
            FeedlyContentSortSwitcher.TITLE_NAME -> menu.findItem(R.id.sort_title_name).isChecked = true
            FeedlyContentSortSwitcher.SAVED_TIMESTAMP -> menu.findItem(R.id.sort_saved_time).isChecked = true
            FeedlyContentSortSwitcher.LAST_PUBLISH_TIMESTAMP -> menu.findItem(R.id.sort_published_tag).isChecked = true
        }
    }

    override fun onItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.sort_title_name -> feedlyListFragment.sortBy(FeedlyContentSortSwitcher.TITLE_NAME)
            R.id.sort_saved_time -> feedlyListFragment.sortBy(FeedlyContentSortSwitcher.SAVED_TIMESTAMP)
            R.id.sort_published_tag -> feedlyListFragment.sortBy(FeedlyContentSortSwitcher.LAST_PUBLISH_TIMESTAMP)
        }
    }
}
