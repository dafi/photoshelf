package com.ternaryop.photoshelf.tumblr.dialog.editor

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.ActionBar
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ternaryop.photoshelf.mru.adapter.MRUHolder
import com.ternaryop.photoshelf.tumblr.dialog.NewPostEditorData
import com.ternaryop.photoshelf.tumblr.dialog.NewPostEditorResult
import com.ternaryop.photoshelf.tumblr.dialog.PostEditorResult
import com.ternaryop.photoshelf.tumblr.dialog.R
import com.ternaryop.photoshelf.tumblr.dialog.blog.BlogList
import com.ternaryop.photoshelf.tumblr.dialog.blog.OnFetchBlogsListener
import com.ternaryop.photoshelf.tumblr.dialog.editor.viewholder.TagsHolder
import com.ternaryop.photoshelf.tumblr.dialog.editor.viewholder.TitleHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class NewTumblrPostEditor(
    private val newPostDialogData: NewPostEditorData,
    titleHolder: TitleHolder,
    tagsHolder: TagsHolder,
    mruHolder: MRUHolder
) : DefaultLifecycleObserver,
    AbsTumblrPostEditor(titleHolder, tagsHolder, mruHolder), CoroutineScope {
    private val job = Job()
    private lateinit var blogList: BlogList
    private var menuItems: Array<MenuItem>? = null
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun setupUI(actionBar: ActionBar?, view: View) {
        blogList = BlogList(view.context, view.findViewById(R.id.blog),
            object : BlogList.OnBlogItemSelectedListener() {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    tagsHolder.updateBlogName(blogList.selectedBlogName)
                    actionBar?.subtitle = blogList.selectedBlogName
                }
            })

        val size = newPostDialogData.imageUrls.size
        actionBar?.title = view.context.resources.getQuantityString(
            R.plurals.post_image,
            size,
            size)

        val fetchBlogsListener = object : OnFetchBlogsListener {
            override fun onStartFetchBlog(blogList: BlogList) = enableMenuItems(false)
            override fun onEndFetchBlog(blogList: BlogList) = enableMenuItems(true)
            override fun onErrorFetchBlog(blogList: BlogList, t: Throwable) = Unit
        }

        view.findViewById<View>(R.id.refreshBlogList)
            .setOnClickListener {
                launch {
                    blogList.fetchBlogNames(fetchBlogsListener)
                }
            }

        launch { blogList.loadList(fetchBlogsListener) }
    }

    override fun onPrepareMenu(menu: Menu) {
        menuItems = arrayOf(
            menu.findItem(R.id.draft),
            menu.findItem(R.id.publish),
            menu.findItem(R.id.toggle_blog_list)
        )

        menuItems?.forEach { it.isVisible = true }
    }

    override fun canExecute(item: MenuItem): Boolean = item.itemId == R.id.draft || item.itemId == R.id.publish

    override fun execute(item: MenuItem): PostEditorResult? {
        if (!canExecute(item)) {
            return null
        }
        blogList.saveBlogName()
        updateMruList()

        return NewPostEditorResult(
            item.itemId == R.id.publish,
            newPostDialogData.imageUrls,
            blogList.selectedBlogName,
            titleHolder.htmlTitle,
            tagsHolder.tags)
    }

    private fun enableMenuItems(enable: Boolean) {
        menuItems?.forEach { it.isEnabled = enable }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        job.cancel()
    }
}
