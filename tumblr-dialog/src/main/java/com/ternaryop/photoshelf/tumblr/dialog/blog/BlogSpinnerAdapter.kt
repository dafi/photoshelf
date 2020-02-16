package com.ternaryop.photoshelf.tumblr.dialog.blog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.SpinnerAdapter
import android.widget.TextView
import coil.api.load
import coil.transform.CircleCropTransformation
import com.ternaryop.photoshelf.tumblr.dialog.R
import com.ternaryop.tumblr.Blog
import com.ternaryop.tumblr.TumblrAltSize
import com.ternaryop.tumblr.android.coil.CoilTumblrOAuth

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

            holder.image.load(
                Blog.getAvatarUrlBySize(blogName, TumblrAltSize.IMAGE_AVATAR_WIDTH),
                CoilTumblrOAuth.get(context)) {
                placeholder(R.drawable.stub)
                transformations(CircleCropTransformation())
            }
        }

        return inflatedView
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup) =
        getView(position, convertView, parent)

    private inner class ViewHolder(vi: View) {
        internal val title = vi.findViewById<View>(R.id.title1) as TextView
        internal val image = vi.findViewById<View>(R.id.image1) as ImageView
    }
}
