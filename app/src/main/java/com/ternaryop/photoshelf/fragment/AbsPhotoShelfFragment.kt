package com.ternaryop.photoshelf.fragment

import android.content.Context
import android.os.Bundle
import android.view.ActionMode
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
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

    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        compositeDisposable = CompositeDisposable()
    }

    override fun onDestroy() {
        snackbar?.dismiss()
        compositeDisposable.clear()
        super.onDestroy()
    }

    override fun onDetach() {
        compositeDisposable.clear()
        super.onDetach()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // all Activities must adhere to FragmentActivityStatus
        fragmentActivityStatus = context as FragmentActivityStatus
    }

    protected open fun refreshUI() {
    }

    protected fun showEditDialog(item: TumblrPhotoPost, mode: ActionMode?) {
        fragmentManager?.also {
            actionMode = mode
            TumblrPostDialog.newInstance(PostDialogData(item), this).show(it, "dialog")
        }
    }

    protected fun showSnackbar(snackbar: Snackbar) {
        this.snackbar = snackbar
        val sbView = snackbar.view
        sbView.setBackgroundColor(ContextCompat.getColor(context!!, R.color.image_picker_detail_text_bg))
        val textView = sbView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(context!!, R.color.image_picker_detail_text_text))
        textView.maxLines = MAX_DETAIL_LINES
        snackbar.show()
    }

    protected open fun makeSnake(view: View, t: Throwable): Snackbar {
        return Snackbar.make(view, t.localizedMessage ?: "", Snackbar.LENGTH_LONG)
    }

    protected open fun makeSnack(view: View,
        errorMessage: String,
        action: ((View?) -> (Unit))? = null): Snackbar {
        return if (action == null) {
            Snackbar.make(view, errorMessage, Snackbar.LENGTH_LONG)
        } else {
            Snackbar
                .make(view, errorMessage, Snackbar.LENGTH_INDEFINITE)
                .setActionTextColor(ContextCompat.getColor(context!!, R.color.snack_error_color))
                .setAction(resources.getString(R.string.refresh), action)
        }
    }
}
