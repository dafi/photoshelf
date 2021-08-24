package com.ternaryop.photoshelf.tumblr.dialog.blog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.SpinnerAdapter
import android.widget.TextView
import coil.load
import coil.transform.CircleCropTransformation
import com.ternaryop.photoshelf.tumblr.dialog.R
import com.ternaryop.tumblr.Blog
import com.ternaryop.tumblr.TumblrAltSize
import com.ternaryop.tumblr.android.TumblrManager
import okhttp3.Headers

class BlogSpinnerAdapter(
    context: Context,
    blogNames: List<String>
) : ArrayAdapter<String>(context, 0, blogNames), SpinnerAdapter {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val inflatedView: View

        if (convertView == null) {
            inflatedView = LayoutInflater.from(context).inflate(R.layout.blog_spinner_item, parent, false)
            holder = ViewHolder(inflatedView)
            inflatedView.tag = holder
        } else {
            inflatedView = convertView
            holder = convertView.tag as ViewHolder
        }

        getItem(position)?.also { blogName ->
            holder.title.text = blogName

            val url = Blog.getAvatarUrlBySize(blogName, TumblrAltSize.IMAGE_AVATAR_WIDTH)
            val signedRequest = TumblrManager.getInstance(context)
                    .consumer
                    .getSignedGetAuthRequest(url)
            val oauthHeaders = Headers.Builder().apply {
                signedRequest.headers.forEach { (k, v) -> this.add(k, v) }
            }.build()
            holder.image.load(signedRequest.completeUrl) {
                headers(oauthHeaders)
                placeholder(R.drawable.stub)
                error(R.drawable.stat_notify_error)
                transformations(CircleCropTransformation())
                // in case of error coil continues to call the url, the rate call limit is reached
                // very fast so we stop it adding this listener
                target(
                    onStart = { holder.image.setImageDrawable(it) },
                    onSuccess = { holder.image.setImageDrawable(it) },
                    onError = { holder.image.setImageDrawable(it) }
                )
            }
        }

        return inflatedView
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup) =
        getView(position, convertView, parent)

    private class ViewHolder(vi: View) {
        val title = vi.findViewById<View>(R.id.title1) as TextView
        val image = vi.findViewById<View>(R.id.image1) as ImageView
    }
}
