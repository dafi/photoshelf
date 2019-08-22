package com.ternaryop.photoshelf.activity

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.fragment.FragmentActivityStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

abstract class AbsPhotoShelfActivity : AppCompatActivity(), FragmentActivityStatus, CoroutineScope {
    open val blogName: String
        get() = appSupport.selectedBlogName!!

    override val appSupport: AppSupport by lazy { AppSupport(this) }
    protected lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

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

        job = Job()
        setContentView(contentViewLayoutId)
        setupActionBar()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val fragment = createFragment()
        if (fragment != null) {
            supportFragmentManager.beginTransaction().add(R.id.content_frame, fragment).commit()
        }
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
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
