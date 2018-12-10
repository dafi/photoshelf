package com.ternaryop.photoshelf.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ternaryop.photoshelf.R
import kotlinx.android.synthetic.main.fragment_bottom_menu.navigation_view
import kotlinx.android.synthetic.main.fragment_bottom_menu.title

interface BottomMenuListener {
    val title: String?
    val menuId: Int
    fun setupMenu(menu: Menu)
    fun onItemSelected(item: MenuItem)
}

class BottomMenuSheetDialogFragment: BottomSheetDialogFragment() {
    var menuListener: BottomMenuListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater
            .cloneInContext(ContextThemeWrapper(activity, R.style.Theme_PhotoShelf))
            .inflate(R.layout.fragment_bottom_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        menuListener?.also { listener ->
            title.text = listener.title ?: ""
            navigation_view.inflateMenu(listener.menuId)
            listener.setupMenu(navigation_view.menu)
            navigation_view.setNavigationItemSelectedListener { menu ->
                listener.onItemSelected(menu)
                dismiss()
            true
            }
        }
    }
}
