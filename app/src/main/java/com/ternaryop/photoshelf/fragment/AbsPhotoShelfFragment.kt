package com.ternaryop.photoshelf.fragment

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.view.ActionMode
import android.view.View
import android.widget.TextView
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.dialogs.PostDialogData
import com.ternaryop.photoshelf.dialogs.TumblrPostDialog
import com.ternaryop.tumblr.TumblrPhotoPost
import io.reactivex.disposables.CompositeDisposable

abstract class AbsPhotoShelfFragment : Fragment() {
    protected lateinit var fragmentActivityStatus: FragmentActivityStatus
    protected var actionMode: ActionMode? = null

    protected lateinit var compositeDisposable: CompositeDisposable

    val blogName: String?
        get() = fragmentActivityStatus.appSupport.selectedBlogName

    val supportActionBar: ActionBar?
        get() = (activity as AppCompatActivity).supportActionBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        compositeDisposable = CompositeDisposable()
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    override fun onDetach() {
        compositeDisposable.clear()
        super.onDetach()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        // all Activities must adhere to FragmentActivityStatus
        fragmentActivityStatus = context as FragmentActivityStatus
    }

    protected open fun refreshUI() {
    }

    protected fun showEditDialog(item: TumblrPhotoPost, mode: ActionMode?) {
        actionMode = mode
        TumblrPostDialog.newInstance(PostDialogData(item), this).show(fragmentManager, "dialog")
    }

    protected fun showSnackbar(snackbar: Snackbar) {
        val sbView = snackbar.view
        sbView.setBackgroundColor(ContextCompat.getColor(context!!, R.color.image_picker_detail_text_bg))
        val textView = sbView.findViewById<TextView>(android.support.design.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(context!!, R.color.image_picker_detail_text_text))
        textView.maxLines = MAX_DETAIL_LINES
        snackbar.show()
    }

    protected open fun makeSnake(view: View, t: Throwable): Snackbar {
        return Snackbar.make(view, t.localizedMessage, Snackbar.LENGTH_LONG)
    }
}
