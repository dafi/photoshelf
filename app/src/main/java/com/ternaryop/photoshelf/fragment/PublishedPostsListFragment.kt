package com.ternaryop.photoshelf.fragment

import android.text.format.DateUtils.SECOND_IN_MILLIS
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.util.post.OnScrollPostFetcher
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.android.TumblrManager
import io.reactivex.Observable
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class PublishedPostsListFragment : ScheduledListFragment() {
    override val actionModeMenuId: Int
        get() = R.menu.published_context

    override fun fetchPosts(listener: OnScrollPostFetcher) {
        refreshUI()

        val params = HashMap<String, String>()
        params["offset"] = postFetcher.offset.toString()
        params["type"] = "photo"
        params["notes_info"] = "true"

        Observable
            .just(params)
            .doFinally { postFetcher.isScrolling = false }
            .flatMap { Observable.fromIterable(TumblrManager.getInstance(context!!).getPublicPosts(blogName!!, it)) }
            .map { PhotoShelfPost(it as TumblrPhotoPost, it.timestamp * SECOND_IN_MILLIS) }
            .toList()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .compose(photoShelfSwipe.applySwipe())
            .subscribe(object : SingleObserver<List<PhotoShelfPost>> {
                override fun onSubscribe(d: Disposable) { compositeDisposable.add(d) }

                override fun onSuccess(photoList: List<PhotoShelfPost>) {
                    postFetcher.incrementReadPostCount(photoList.size)
                    photoAdapter.addAll(photoList)
                    refreshUI()
                }

                override fun onError(t: Throwable) { showSnackbar(makeSnake(recyclerView, t)) }
            })
    }
}
