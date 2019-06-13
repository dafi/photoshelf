package com.ternaryop.tumblr.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.TumblrPost

/**
 * Created by dave on 23/02/18.
 * Extension to use TumblrPost with Activity
 */

fun TumblrPost.finishActivity(activity: Activity, extraPostName: String) {
    val data = Intent()
    data.putExtra(extraPostName, this)
    activity.setResult(Activity.RESULT_OK, data)
    activity.finish()
}

fun TumblrPhotoPost.browseImageBySize(
    Context: Context,
    title: String,
    startImageViewer: (String, TumblrPhotoPost) -> Unit) {
    val photoAltSize = firstPhotoAltSize ?: return
    val arrayAdapter = ArrayAdapter(
        Context,
        android.R.layout.select_dialog_item,
        photoAltSize)

    AlertDialog.Builder(Context)
        .setTitle(title)
        .setAdapter(arrayAdapter) { _, which ->
            arrayAdapter.getItem(which)?.let { startImageViewer(it.url, this) }
        }
        .show()
    return
}

fun TumblrPost.viewPost(fragment: Fragment) {
    val i = Intent(Intent.ACTION_VIEW)
    i.data = Uri.parse(postUrl)
    fragment.startActivity(i)
}
