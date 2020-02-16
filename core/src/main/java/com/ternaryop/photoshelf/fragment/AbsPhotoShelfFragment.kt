package com.ternaryop.photoshelf.fragment

import android.content.Context
import android.view.ActionMode
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.ternaryop.photoshelf.core.prefs.selectedBlogName
import com.ternaryop.photoshelf.view.snackbar.SnackbarHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

abstract class AbsPhotoShelfFragment : Fragment(), CoroutineScope {
    protected lateinit var fragmentActivityStatus: FragmentActivityStatus
    protected var actionMode: ActionMode? = null

    protected lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    val blogName: String?
        get() = PreferenceManager.getDefaultSharedPreferences(requireContext()).selectedBlogName

    val requireBlogName: String
        get() = checkNotNull(blogName)

    val supportActionBar: ActionBar?
        get() = (activity as AppCompatActivity).supportActionBar

    open val snackbarHolder: SnackbarHolder by lazy {
        SnackbarHolder().apply {
            lifecycle.addObserver(this)
        }
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    override fun onDetach() {
        job.cancel()
        super.onDetach()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // the job could be cancelled so we create it here instead of on onCreate
        job = Job()
        // all Activities must adhere to FragmentActivityStatus
        fragmentActivityStatus = context as FragmentActivityStatus
    }

    protected open fun refreshUI() {
    }
}
