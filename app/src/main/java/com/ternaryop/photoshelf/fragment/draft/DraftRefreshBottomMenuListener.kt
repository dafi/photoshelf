package com.ternaryop.photoshelf.fragment.draft

import android.view.Menu
import android.view.MenuItem
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.db.TumblrPostCache
import com.ternaryop.photoshelf.fragment.BottomMenuListener

class DraftRefreshBottomMenuListener(
    private val draftListFragment: DraftListFragment) : BottomMenuListener {
    override val title: String?
        get() = draftListFragment.resources.getString(R.string.refresh)
    override val menuId: Int
        get() = R.menu.draft_refresh

    override fun setupMenu(menu: Menu) {
    }

    override fun onItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.clear_draft_cache -> {
                draftListFragment.draftCache.clearCache(TumblrPostCache.CACHE_TYPE_DRAFT)
                draftListFragment.fetchPosts(false)
            }
            R.id.reload_draft -> {
                draftListFragment.fetchPosts(false)
            }
        }
    }
}