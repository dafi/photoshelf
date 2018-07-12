package com.ternaryop.photoshelf.view

import android.content.Context
import android.util.AttributeSet
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

class PhotoShelfSwipe : WaitingResultSwipeRefreshLayout {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun setOnRefreshListener(listener: OnRefreshListener?) {
        if (listener == null) {
            isEnabled = false
        }
        super.setOnRefreshListener(listener)
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
                    .doOnSubscribe { setRefreshingAndWaitingResult(true) }
                    .doFinally { setRefreshingAndWaitingResult(false) }
        }
    }

    fun applyCompletableSwipe(): CompletableTransformer {
        return CompletableTransformer { upstream ->
            upstream
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { setRefreshingAndWaitingResult(true) }
                    .doFinally { setRefreshingAndWaitingResult(false) }
        }
    }
}
