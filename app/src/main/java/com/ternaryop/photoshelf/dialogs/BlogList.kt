package com.ternaryop.photoshelf.dialogs

import android.support.v7.app.AlertDialog
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.utils.DialogUtils
import io.reactivex.SingleObserver
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

/**
 * Created by dave on 24/02/18.
 * Wrap the spinner containing the blog list
 */
class BlogList(val appSupport: AppSupport,
    val spinner: Spinner,
    onBlogItemSelectedListener: BlogList.OnBlogItemSelectedListener) {
    val selectedBlogName: String
        get() = spinner.selectedItem as String

    init {
        spinner.onItemSelectedListener = onBlogItemSelectedListener
    }

    fun fillList(blogNames: List<String>) {
        val adapter = ArrayAdapter(appSupport, android.R.layout.simple_spinner_item, blogNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val selectedName = appSupport.selectedBlogName
        if (selectedName != null) {
            val position = adapter.getPosition(selectedName)
            if (position >= 0) {
                spinner.setSelection(position)
            }
        }
    }

    fun fetchBlogNames(dialog: AlertDialog, compositeDisposable: CompositeDisposable) {
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = false
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

        appSupport.clearBlogList()
        appSupport.fetchBlogNames(appSupport)
            .subscribe(object : SingleObserver<List<String>> {
                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onSuccess(blogNames: List<String>) {
                    fillList(blogNames)
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = true
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                }

                override fun onError(e: Throwable) {
                    dialog.dismiss()
                    DialogUtils.showErrorDialog(appSupport, e)
                }
            })
    }

    abstract class OnBlogItemSelectedListener : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
}