package com.ternaryop.photoshelf.repository.tumblr

import android.app.Application
import android.net.Uri
import com.ternaryop.photoshelf.lifecycle.CommandMutableLiveData
import com.ternaryop.tumblr.Blog
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.android.TumblrManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TumblrRepository @Inject constructor(private val application: Application) {
    // user should be not logged when the manager is created so we create it lazily
    val tumblr: Tumblr by lazy { TumblrManager.getInstance(application) }
    private val _draftCount = CommandMutableLiveData<Int>()
    private val _scheduledCount = CommandMutableLiveData<Int>()
    val draftCount = _draftCount.asLiveData()
    val scheduledCount = _scheduledCount.asLiveData()

    private val _authenticate = CommandMutableLiveData<Boolean>()
    val authenticate = _authenticate.asLiveData()

    var blogs = emptyList<Blog>()
        private set

    fun updateDraftCount(count: Int) = _draftCount.setLastValue(count, true)

    fun updateScheduledCount(count: Int) = _scheduledCount.setLastValue(count, true)

    suspend fun handleCallbackUri(uri: Uri?) {
        if (TumblrManager.isLogged(application)) {
            return
        }
        _authenticate.post(false) { TumblrManager.handleOpenURI(application, uri) }
    }

    fun clearCount() {
        _draftCount.setLastValue(null, false)
        _scheduledCount.setLastValue(null, false)
    }

    fun fetchBlogs(): List<Blog> {
        blogs = tumblr.blogList
        return blogs
    }

    fun blogByName(blogName: String): Blog = blogs.first { it.name.equals(blogName, true) }
}
