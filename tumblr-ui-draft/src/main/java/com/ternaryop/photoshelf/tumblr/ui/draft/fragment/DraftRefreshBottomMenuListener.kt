package com.ternaryop.photoshelf.tumblr.ui.draft.fragment

import android.view.Menu
import android.view.MenuItem
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ternaryop.photoshelf.fragment.BottomMenuListener
import com.ternaryop.photoshelf.tumblr.ui.draft.R

class DraftRefreshBottomMenuListener(
    private val draftListFragment: DraftListFragment
) : BottomMenuListener {
    override val title: String
        get() = draftListFragment.resources.getString(com.ternaryop.photoshelf.core.R.string.refresh)
    override val menuId: Int
        get() = R.menu.draft_refresh

    override fun setupMenu(menu: Menu, sheet: BottomSheetDialogFragment) = Unit

    override fun onItemSelected(item: MenuItem, sheet: BottomSheetDialogFragment) {
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
