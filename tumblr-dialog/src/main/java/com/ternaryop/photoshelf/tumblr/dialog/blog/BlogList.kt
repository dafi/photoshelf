package com.ternaryop.photoshelf.tumblr.dialog.blog

import android.content.Context
import android.widget.AdapterView
import android.widget.Spinner
import androidx.preference.PreferenceManager
import com.ternaryop.photoshelf.core.prefs.blogList
import com.ternaryop.photoshelf.core.prefs.clearBlogList
import com.ternaryop.photoshelf.core.prefs.fetchBlogNames
import com.ternaryop.photoshelf.core.prefs.selectedBlogName
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.utils.dialog.showErrorDialog

interface OnFetchBlogsListener {
    fun onStartFetchBlog(blogList: BlogList)
    fun onEndFetchBlog(blogList: BlogList)
    fun onErrorFetchBlog(blogList: BlogList, t: Throwable)
}

/**
 * Created by dave on 24/02/18.
 * Wrap the spinner containing the blog list
 */
class BlogList(
    val context: Context,
    private val spinner: Spinner,
    onBlogItemSelectedListener: OnBlogItemSelectedListener
) {

    private val blogPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    val selectedBlogName: String
        get() = spinner.selectedItem as String

    init {
        spinner.onItemSelectedListener = onBlogItemSelectedListener
    }

    private fun fillList(blogNames: List<String>) {
        val adapter = BlogSpinnerAdapter(context, blogNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val selectedName = blogPrefs.selectedBlogName
        if (selectedName != null) {
            val position = adapter.getPosition(selectedName)
            if (position >= 0) {
                spinner.setSelection(position)
            }
        }
    }

    suspend fun loadList(onFetchBlogsListener: OnFetchBlogsListener) {
        val blogSetNames = blogPrefs.blogList
        if (blogSetNames == null) {
            fetchBlogNames(onFetchBlogsListener)
        } else {
            onFetchBlogsListener.onStartFetchBlog(this)
            fillList(blogSetNames)
            onFetchBlogsListener.onEndFetchBlog(this)
        }
    }

    suspend fun fetchBlogNames(onFetchBlogsListener: OnFetchBlogsListener) {
        blogPrefs.clearBlogList()
        try {
            onFetchBlogsListener.onStartFetchBlog(this)
            fillList(blogPrefs.fetchBlogNames(TumblrManager.getInstance(context)))
        } catch (t: Throwable) {
            onFetchBlogsListener.onErrorFetchBlog(this, t)
            t.showErrorDialog(context)
        } finally {
            onFetchBlogsListener.onEndFetchBlog(this)
        }
    }

    fun saveBlogName() {
        blogPrefs.selectedBlogName = selectedBlogName
    }

    abstract class OnBlogItemSelectedListener : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
    }
}
