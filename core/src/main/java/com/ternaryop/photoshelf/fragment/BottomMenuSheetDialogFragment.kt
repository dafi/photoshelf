package com.ternaryop.photoshelf.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.navigation.NavigationView
import com.ternaryop.photoshelf.core.R

interface BottomMenuListener {
    val title: String?
    val menuId: Int
    fun setupMenu(menu: Menu)
    fun onItemSelected(item: MenuItem)
}

class BottomMenuSheetDialogFragment : BottomSheetDialogFragment() {
    var menuListener: BottomMenuListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater
            .inflate(R.layout.fragment_bottom_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        menuListener?.also { listener ->
            val title = view.findViewById<TextView>(R.id.title)
            title.text = listener.title ?: ""
            val navigationView = view.findViewById<NavigationView>(R.id.navigation_view)
            navigationView.inflateMenu(listener.menuId)
            listener.setupMenu(navigationView.menu)
            navigationView.setNavigationItemSelectedListener { menu ->
                listener.onItemSelected(menu)
                dismiss()
            true
            }
        }
    }
}
