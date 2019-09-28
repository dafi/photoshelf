package com.ternaryop.photoshelf.activity

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.ternaryop.photoshelf.R

abstract class AbsDrawerActionBarActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener,
    DrawerLayout.DrawerListener {
//    lateinit var drawerToggle: ActionBarDrawerToggle
//        private set
    // nav drawer title
//    private lateinit var drawerTitle: CharSequence
    // used to store app title
//    private var appTitle: CharSequence? = null
    private var subtitle: CharSequence? = null
    private var lastClickedMenuId = -1
    // used to update subtitle only if the drawer closed without change selected menu
    private var selectedMenuChanged: Boolean = false
    protected abstract val activityLayoutResId: Int
    abstract val toolbar: Toolbar
    abstract val topLevelDestinationIds: Set<Int>
    protected lateinit var appBarConfiguration: AppBarConfiguration
    protected lateinit var navController: NavController
    protected lateinit var navView: NavigationView

    private var lastSelectedBadgeActionView: View? = null

    protected var isDrawerIndicatorEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activityLayoutResId)

        setupActionBar()
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)

        appBarConfiguration = AppBarConfiguration(topLevelDestinationIds, drawerLayout)

        drawerLayout.openDrawer(GravityCompat.START)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

//        drawerTitle = title
//        appTitle = drawerTitle
//
//        drawerToggle = ActionBarDrawerToggle(
//            this,
//            drawerLayout,
//            R.string.drawer_open,
//            R.string.drawer_close)
        drawerLayout.addDrawerListener(this)
    }

//    override fun onPostCreate(savedInstanceState: Bundle?) {
//        super.onPostCreate(savedInstanceState)
//        // Sync the toggle state after onRestoreInstanceState has occurred.
//        drawerToggle.syncState()
//    }

//    override fun onConfigurationChanged(newConfig: Configuration) {
//        super.onConfigurationChanged(newConfig)
//        // Pass any configuration change to the drawer toggle
//        drawerToggle.onConfigurationChanged(newConfig)
//    }

    override fun onDrawerSlide(view: View, v: Float) {}

    override fun onDrawerOpened(view: View) {
        selectedMenuChanged = false
//        supportActionBar!!.title = drawerTitle
        // save current subtitle
        supportActionBar?.also { supportActionBar ->
            subtitle = supportActionBar.subtitle
            supportActionBar.subtitle = null
        }
        invalidateOptionsMenu()
    }

    override fun onDrawerClosed(view: View) {
        supportActionBar?.also { supportActionBar ->
//            supportActionBar.title = appTitle
            if (!selectedMenuChanged) {
                supportActionBar.subtitle = subtitle
            }
        }
        invalidateOptionsMenu()
    }

    override fun onDrawerStateChanged(i: Int) {}

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (!isDrawerIndicatorEnabled) {
            return true
        }
        return item.onNavDestinationSelected(findNavController(R.id.nav_host_fragment))
            || super.onOptionsItemSelected(item)
    }

    /**
     * Select clicked menu item
     * If the item differs from the current one then call onDrawerItemSelected()
     * @param menuItem The selected item
     * @return true to display the item as the selected item
     */
    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        val menuId = menuItem.itemId

        if (lastClickedMenuId == menuId) {
            return false
        }
        lastClickedMenuId = menuId
        selectedMenuChanged = true

        supportActionBar?.subtitle = null

        selectedBadge(menuItem)

        closeDrawer()
        return onDrawerItemSelected(menuItem)
    }

    protected open fun selectedBadge(menuItem: MenuItem) {
        lastSelectedBadgeActionView?.also {
            it.isSelected = false
        }
        lastSelectedBadgeActionView = menuItem.actionView
        lastSelectedBadgeActionView?.isSelected = true
    }

    /**
     * The drawer item selected
     * @param menuItem the selected item
     */
    abstract fun onDrawerItemSelected(menuItem: MenuItem): Boolean

//    override fun setTitle(title: CharSequence?) {
//        this.appTitle = title
//        supportActionBar?.title = title
//    }

    protected fun setupActionBar(): Toolbar {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        return toolbar
    }

    fun selectClickedItem(itemId: Int) {
       onNavigationItemSelected(navView.menu.findItem(itemId))
    }

    fun setBadgeText(itemId: Int, text: String?) {
        navView.menu.findItem(itemId).actionView?.also { actionView ->
            if (text.isNullOrBlank()) {
                actionView.visibility = GONE
            } else {
                actionView.findViewById<TextView>(R.id.badge)?.text = text
                actionView.visibility = VISIBLE
            }
        }
    }

    fun openDrawer() = appBarConfiguration.drawerLayout?.openDrawer(GravityCompat.START)
    fun closeDrawer() = appBarConfiguration.drawerLayout?.closeDrawer(GravityCompat.START)
    val isDrawerMenuOpen: Boolean
        get() = appBarConfiguration.drawerLayout?.isDrawerOpen(GravityCompat.START) ?: false
}
