package com.ternaryop.photoshelf.view.snackbar

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar

open class SnackbarHolder : DefaultLifecycleObserver {
    private var snackbar: Snackbar? = null
    var backgroundColor = 0
    var textColor = 0
    var actionTextColor = 0

    init {
        defaultColors()
    }

    open fun show(
        view: View,
        t: Throwable?,
        actionText: String? = null,
        action: ((View?) -> (Unit))? = null
    ) = show(build(view, t?.localizedMessage ?: "", actionText, action))

    open fun show(snackbar: Snackbar, maxLines: Int = 3) {
        dismiss()
        this.snackbar = snackbar
        val sbView = snackbar.view
        sbView.setBackgroundColor(backgroundColor)
        val textView: TextView = sbView.findViewById(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(textColor)
        textView.maxLines = maxLines
        snackbar.show()
        resetColors()
    }

    open fun build(
        view: View,
        title: String?,
        actionText: String? = null,
        action: ((View?) -> (Unit))? = null
    ): Snackbar {
        val safeTitle = title ?: ""
        return if (action == null) {
            Snackbar.make(view, safeTitle, Snackbar.LENGTH_LONG)
        } else {
            Snackbar
                .make(view, safeTitle, Snackbar.LENGTH_INDEFINITE)
                .setActionTextColor(actionTextColor)
                .setAction(actionText, action)
        }
    }

    open fun dismiss() {
        snackbar?.dismiss()
        snackbar = null
    }

    open fun resetColors() = defaultColors()

    private fun defaultColors() {
        backgroundColor = Color.parseColor("#90000000")
        textColor = Color.WHITE
        actionTextColor = Color.parseColor("#ffdd00")
    }

    override fun onDestroy(owner: LifecycleOwner) = dismiss()
}
