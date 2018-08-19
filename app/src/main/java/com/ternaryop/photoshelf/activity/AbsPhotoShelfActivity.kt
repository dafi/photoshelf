package com.ternaryop.photoshelf.activity

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.EXTRA_BLOG_NAME
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.fragment.FragmentActivityStatus

abstract class AbsPhotoShelfActivity : AppCompatActivity(), FragmentActivityStatus {
    private var _blogName: String? = null
    val blogName: String
        get() = _blogName ?: appSupport.selectedBlogName!!

    override val appSupport: AppSupport by lazy { AppSupport(this) }

    override val isDrawerMenuOpen: Boolean
        get() = false

    override val drawerToolbar: Toolbar
        get() = findViewById<View>(R.id.drawer_toolbar) as Toolbar

    /**
     * The subclass doesn't call setContentView directly to avoid side effects (action mode bar doesn't overlap the
     * toolbar but is shown above) but pass the layout id to use
     * @return the layout id to use
     */
    abstract val contentViewLayoutId: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentViewLayoutId)
        setupActionBar()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        _blogName = intent.extras?.getString(EXTRA_BLOG_NAME)
        val fragment = createFragment()
        if (fragment != null) {
            supportFragmentManager.beginTransaction().add(R.id.content_frame, fragment).commit()
        }
    }

    /**
     * The fragment is added programmatically because in many cases its creation from XML layout can collide with
     * the supportActionBar creation (eg the fragment needs the actionBar but it can't be created until
     * the xml is full instantiated so it will be null)
     * @return the fragment to use or null if no fragment must be added
     */
    abstract fun createFragment(): Fragment?

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // clicked the actionbar
                // close and return to caller
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupActionBar(): Toolbar {
        val toolbar = drawerToolbar
        setSupportActionBar(toolbar)

        return toolbar
    }
}
