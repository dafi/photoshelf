package com.ternaryop.photoshelf.util.menu

import android.view.Menu

fun Menu.enableAll(isEnabled: Boolean) {
    for (i in 0 until size()) {
        getItem(i).isEnabled = isEnabled
    }
}
