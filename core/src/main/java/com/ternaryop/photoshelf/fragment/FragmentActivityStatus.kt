package com.ternaryop.photoshelf.fragment

import androidx.appcompat.widget.Toolbar

/**
 * Communicate activity status to fragment
 * @author dave
 */
interface FragmentActivityStatus {
    val isDrawerMenuOpen: Boolean
    val drawerToolbar: Toolbar
}
