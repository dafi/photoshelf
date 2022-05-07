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
import com.ternaryop.photoshelf.repository.tumblr.TumblrRepository
import com.ternaryop.photoshelf.tumblr.dialog.R
import com.ternaryop.tumblr.TumblrAltSize.Companion.IMAGE_AVATAR_WIDTH
import com.ternaryop.tumblr.getClosestByWidth
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface TumblrRepositoryEntryPoint {
    fun instance(): TumblrRepository
}

class BlogSpinnerAdapter(
    context: Context,
    blogs: List<String>
) : ArrayAdapter<String>(context, 0, blogs), SpinnerAdapter {
    private val tumblrRepository = EntryPointAccessors.fromApplication(
        context.applicationContext,
        TumblrRepositoryEntryPoint::class.java
    ).instance()

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

            val blog = tumblrRepository.blogByName(blogName)
            val url = blog.avatar.getClosestByWidth(IMAGE_AVATAR_WIDTH)?.url ?: blog.avatar.last().url
            holder.image.load(url) {
                placeholder(R.drawable.stub)
                error(R.drawable.stat_notify_error)
                transformations(CircleCropTransformation())
                // in case of error coil continues to call the url, the rate call limit is reached
                // very fast so we stop it adding this listener
                listener(
                    onStart = { holder.image.setImageDrawable(it.placeholder) },
                    onError = { _, result -> holder.image.setImageDrawable(result.drawable) },
                    onSuccess = { _, result -> holder.image.setImageDrawable(result.drawable) },
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
