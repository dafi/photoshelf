package com.ternaryop.photoshelf.dialogs

import android.content.Context
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.utils.dialog.showErrorDialog

/**
 * Created by dave on 24/02/18.
 * Wrap the spinner containing the blog list
 */
class BlogList(
    val context: Context,
    private val spinner: Spinner,
    onBlogItemSelectedListener: OnBlogItemSelectedListener) {

    private val appSupport = AppSupport(context)
    val selectedBlogName: String
        get() = spinner.selectedItem as String

    init {
        spinner.onItemSelectedListener = onBlogItemSelectedListener
    }

    private fun fillList(blogNames: List<String>) {
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

    suspend fun loadList(dialog: AlertDialog) {
        val blogSetNames = appSupport.blogList
        if (blogSetNames == null) {
            fetchBlogNames(dialog)
        } else {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = false
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

            fillList(blogSetNames)

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = true
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
        }

    }

    suspend fun fetchBlogNames(dialog: AlertDialog) {
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = false
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

        appSupport.clearBlogList()
        try {
            val blogNames = appSupport.fetchBlogNames(appSupport)
            fillList(blogNames)
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = true
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
        } catch (t: Throwable) {
            dialog.dismiss()
            t.showErrorDialog(appSupport)
        }
    }

    fun saveBlogName() {
        appSupport.selectedBlogName = selectedBlogName
    }

    abstract class OnBlogItemSelectedListener : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
}