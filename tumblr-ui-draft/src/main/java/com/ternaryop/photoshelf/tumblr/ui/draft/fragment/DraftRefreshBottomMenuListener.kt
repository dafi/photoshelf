package com.ternaryop.photoshelf.tumblr.ui.draft.fragment

import android.view.Menu
import android.view.MenuItem
import com.ternaryop.photoshelf.fragment.BottomMenuListener
import com.ternaryop.photoshelf.tumblr.ui.draft.R

class DraftRefreshBottomMenuListener(
    private val draftListFragment: DraftListFragment
) : BottomMenuListener {
    override val title: String
        get() = draftListFragment.resources.getString(R.string.refresh)
    override val menuId: Int
        get() = R.menu.draft_refresh

    override fun setupMenu(menu: Menu) = Unit

    override fun onItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.clear_draft_cache -> {
                draftListFragment.draftCache.clear()
                draftListFragment.fetchPosts(false)
            }
            R.id.reload_draft -> {
                draftListFragment.fetchPosts(false)
            }
        }
    }
}
