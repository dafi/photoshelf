package com.ternaryop.photoshelf.fragment

import androidx.appcompat.widget.Toolbar

import com.ternaryop.photoshelf.AppSupport

/**
 * Communicate activity status to fragment
 * @author dave
 */
interface FragmentActivityStatus {
    val isDrawerMenuOpen: Boolean
    val appSupport: AppSupport
    val drawerToolbar: Toolbar
}
