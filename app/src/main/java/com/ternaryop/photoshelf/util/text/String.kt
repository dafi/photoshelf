package com.ternaryop.photoshelf.util.text

import android.text.Html
import android.text.Spanned

/**
 * Created by dave on 28/01/18.
 * Extensions to convert string to/from HTML
 */
fun String.fromHtml(): CharSequence = Html.fromHtml(this)

fun Spanned.toHtml(): String = Html.toHtml(this)
