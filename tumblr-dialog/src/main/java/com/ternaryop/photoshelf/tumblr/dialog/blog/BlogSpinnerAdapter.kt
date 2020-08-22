package com.ternaryop.photoshelf.tumblr.dialog.blog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.SpinnerAdapter
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.ternaryop.photoshelf.tumblr.dialog.R
import com.ternaryop.tumblr.Blog
import com.ternaryop.tumblr.TumblrAltSize
import com.ternaryop.tumblr.android.TumblrManager

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

            Glide
                .with(inflatedView)
                .load(oauthGlideUrl(blogName))
                .placeholder(R.drawable.stub)
                .circleCrop()
                .into(holder.image)
        }

        return inflatedView
    }

    private fun oauthGlideUrl(blogName: String): GlideUrl {
        val url = Blog.getAvatarUrlBySize(blogName, TumblrAltSize.IMAGE_AVATAR_WIDTH)
        val signedRequest = TumblrManager.getInstance(context)
            .consumer
            .getSignedGetAuthRequest(url)

        val headers = LazyHeaders.Builder().apply {
            signedRequest.headers.forEach { (k, v) -> this.addHeader(k, v) }
        }.build()
        return GlideUrl(signedRequest.completeUrl, headers)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup) =
        getView(position, convertView, parent)

    private class ViewHolder(vi: View) {
        val title = vi.findViewById<View>(R.id.title1) as TextView
        val image = vi.findViewById<View>(R.id.image1) as ImageView
    }
}
