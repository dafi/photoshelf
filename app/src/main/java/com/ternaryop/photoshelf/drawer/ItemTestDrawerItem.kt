package com.ternaryop.photoshelf.drawer

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.ternaryop.photoshelf.EXTRA_URL
import com.ternaryop.photoshelf.R
import com.ternaryop.utils.drawer.adapter.DrawerItem

/**
 * Created by dave on 28/10/17.
 * Contain info about the item to test
 */

class ItemTestDrawerItem(itemId: Int, title: String, fragmentClass: Class<out Fragment>)
    : DrawerItem(itemId, title, fragmentClass) {
    override fun instantiateFragment(context: Context): Fragment {
        val fragment = super.instantiateFragment(context)
        val args = Bundle()
        args.putString(EXTRA_URL, context.getString(R.string.test_page_url))
        fragment.arguments = args

        return fragment
    }
}
