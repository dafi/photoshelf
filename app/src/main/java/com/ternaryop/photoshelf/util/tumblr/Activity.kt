package com.ternaryop.photoshelf.util.tumblr

import android.app.Activity
import android.app.Fragment
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AlertDialog
import android.widget.ArrayAdapter
import com.ternaryop.photoshelf.EXTRA_POST
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.activity.ImageViewerActivity
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.TumblrPost

/**
 * Created by dave on 23/02/18.
 * Extension to use TumblrPost with Activity
 */

fun TumblrPost.finishActivity(activity: Activity) {
    val data = Intent()
    data.putExtra(EXTRA_POST, this)
    activity.setResult(Activity.RESULT_OK, data)
    activity.finish()
}

fun TumblrPhotoPost.browseTagImageBySize(activity: Activity, tag: String = firstTag): Boolean {
    val photoAltSize = firstPhotoAltSize ?: return false
    val arrayAdapter = ArrayAdapter(
        activity,
        android.R.layout.select_dialog_item,
        photoAltSize)

    AlertDialog.Builder(activity)
        .setTitle(activity.getString(R.string.menu_header_show_image, tag))
        .setAdapter(arrayAdapter) { _, which ->
            arrayAdapter.getItem(which)?.let { ImageViewerActivity.startImageViewer(activity, it.url, this) }
        }
        .show()
    return true
}

fun TumblrPost.viewPost(fragment: Fragment) {
    val i = Intent(Intent.ACTION_VIEW)
    i.data = Uri.parse(postUrl)
    fragment.startActivity(i)
}
