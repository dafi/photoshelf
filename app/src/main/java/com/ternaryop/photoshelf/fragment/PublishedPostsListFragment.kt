package com.ternaryop.photoshelf.fragment

import android.text.format.DateUtils
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.util.post.OnScrollPostFetcher
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.android.TumblrManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PublishedPostsListFragment : ScheduledListFragment() {
    override val actionModeMenuId: Int
        get() = R.menu.published_context

    override fun fetchPosts(listener: OnScrollPostFetcher) {
        refreshUI()

        val params = HashMap<String, String>()
        params["offset"] = postFetcher.offset.toString()
        params["type"] = "photo"
        params["notes_info"] = "true"

        photoShelfSwipe.setRefreshingAndWaitingResult(true)
        launch {
            try {
                val photoList = withContext(Dispatchers.IO) {
                    TumblrManager.getInstance(context!!).getPublicPosts(blogName!!, params).map {
                        PhotoShelfPost(it as TumblrPhotoPost, it.timestamp * DateUtils.SECOND_IN_MILLIS)
                    }
                }
                photoShelfSwipe.setRefreshingAndWaitingResult(false)
                postFetcher.incrementReadPostCount(photoList.size)
                photoAdapter.addAll(photoList)
                refreshUI()
            } catch (t: Throwable) {
                photoShelfSwipe.setRefreshingAndWaitingResult(false)
                showSnackbar(makeSnake(recyclerView, t))
            }
            postFetcher.isScrolling = false
        }
    }
}
