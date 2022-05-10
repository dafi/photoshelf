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
    fun setupMenu(menu: Menu, sheet: BottomSheetDialogFragment)
    fun onItemSelected(item: MenuItem, sheet: BottomSheetDialogFragment)
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
            setupTitle(view, listener)
            setupSubtitle(view)
            setupNavigationView(view, listener)
        }
    }

    private fun setupTitle(
        view: View,
        listener: BottomMenuListener
    ) {
        val title = view.findViewById<TextView>(R.id.title)
        title.text = listener.title ?: arguments?.getString(ARG_TITLE) ?: ""
    }

    private fun setupSubtitle(view: View) {
        val subtitle = arguments?.getString(ARG_SUBTITLE)
        val subtitleView = view.findViewById<TextView>(R.id.subtitle)

        if (subtitle == null) {
            subtitleView.visibility = View.GONE
        } else {
            subtitleView.visibility = View.VISIBLE
            subtitleView.text = subtitle
        }
    }

    private fun setupNavigationView(
        view: View,
        listener: BottomMenuListener
    ) {
        val navigationView = view.findViewById<NavigationView>(R.id.navigation_view)
        navigationView.inflateMenu(listener.menuId)
        listener.setupMenu(navigationView.menu, this)
        navigationView.setNavigationItemSelectedListener { menu ->
            listener.onItemSelected(menu, this)
            dismiss()
            true
        }
    }

    companion object {
        const val ARG_TITLE = "bottom.menu.sheet.title"
        const val ARG_SUBTITLE = "bottom.menu.sheet.subtitle"
    }
}
