package com.ternaryop.photoshelf.view

import android.support.annotation.IdRes
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import com.ternaryop.photoshelf.R
import com.ternaryop.widget.WaitingResultSwipeRefreshLayout
import io.reactivex.CompletableTransformer
import io.reactivex.SingleTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by dave on 08/09/17.
 *
 * Hold the WaitingResultSwipeRefreshLayout used in many classes
 */

class PhotoShelfSwipe constructor(rootView: View, @IdRes id: Int, listener: SwipeRefreshLayout.OnRefreshListener? = null) {
    val swipe = rootView.findViewById<View>(id) as WaitingResultSwipeRefreshLayout

    init {
        swipe.setColorScheme(R.array.progress_swipe_colors)
        if (listener == null) {
            swipe.isEnabled = false
        } else {
            swipe.setOnRefreshListener(listener)
        }
    }

    /**
     * Create a transformer that show/hide the swipe refresh
     * @param <T> the Upstream
     * @return the transformer
     */
    fun <T> applySwipe(): SingleTransformer<T, T> {
        return SingleTransformer { upstream ->
            upstream
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { swipe.setRefreshingAndWaintingResult(true) }
                    .doFinally { swipe.setRefreshingAndWaintingResult(false) }
        }
    }

    fun <T> applyCompletableSwipe(): CompletableTransformer {
        return CompletableTransformer { upstream ->
            upstream
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { swipe.setRefreshingAndWaintingResult(true) }
                    .doFinally { swipe.setRefreshingAndWaintingResult(false) }
        }
    }
}
