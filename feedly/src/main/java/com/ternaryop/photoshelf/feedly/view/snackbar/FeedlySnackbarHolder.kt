package com.ternaryop.photoshelf.feedly.view.snackbar

import android.view.View
import com.ternaryop.feedly.TokenExpiredException
import com.ternaryop.photoshelf.feedly.R
import com.ternaryop.photoshelf.view.snackbar.SnackbarHolder

class FeedlySnackbarHolder(
    private val expiredTokenAction: ((View?) -> (Unit)),
    private val refreshAction: ((View?) -> (Unit))
) : SnackbarHolder() {

    override fun show(
        view: View,
        t: Throwable?,
        actionText: String?,
        action: ((View?) -> (Unit))?
    ) = show(build(view, t))

    fun build(view: View, t: Throwable?) =
        if (t is TokenExpiredException) {
            build(
                view,
                view.context.getString(R.string.token_expired),
                view.context.getString(com.ternaryop.photoshelf.core.R.string.refresh),
                expiredTokenAction
            )
        } else {
            build(
                view,
                t?.localizedMessage,
                view.context.getString(com.ternaryop.photoshelf.core.R.string.refresh),
                refreshAction
            )
        }
}
