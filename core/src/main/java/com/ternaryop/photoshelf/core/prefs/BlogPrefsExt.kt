package com.ternaryop.photoshelf.core.prefs

import android.content.SharedPreferences
import com.ternaryop.tumblr.Blog
import com.ternaryop.tumblr.Tumblr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val PREF_SELECTED_BLOG = "selectedBlog"
const val PREF_BLOG_NAMES = "blogNames"

var SharedPreferences.selectedBlogName: String?
    get() = getString(PREF_SELECTED_BLOG, null)
    set(blogName) = edit().putString(PREF_SELECTED_BLOG, blogName).apply()

val SharedPreferences.blogList: List<String>?
    get() {
        val blogSet = getStringSet(PREF_BLOG_NAMES, null) ?: return null
        val list = blogSet.toMutableList()
        list.sort()
        return list
    }

fun SharedPreferences.clearBlogList() = edit().remove(PREF_BLOG_NAMES).apply()

suspend fun SharedPreferences.fetchBlogNames(tumblr: Tumblr): List<String> = withContext(Dispatchers.IO) {
    fetchBlogNames(tumblr.blogList)
}

fun SharedPreferences.fetchBlogNames(blogs: List<Blog>): List<String> =
    blogList ?: setupBlogs(blogs)

private fun SharedPreferences.setupBlogs(blogs: List<Blog>): List<String> {
    val blogNames = HashSet<String>(blogs.size)
    var primaryBlog: String? = null
    for (blog in blogs) {
        blogNames.add(blog.name)
        if (blog.isPrimary) {
            primaryBlog = blog.name
        }
    }
    edit().putStringSet(PREF_BLOG_NAMES, blogNames).apply()
    if (primaryBlog != null) {
        selectedBlogName = primaryBlog
    }
    return blogList ?: emptyList()
}
