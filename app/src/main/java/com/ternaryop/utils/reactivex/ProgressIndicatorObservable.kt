package com.ternaryop.utils.reactivex

import android.view.View
import android.widget.ProgressBar
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by dave on 03/09/17.
 * Show a progress dialog and return an Rx Observable
 */

object ProgressIndicatorObservable {

    fun <T> apply(progressBar: ProgressBar, max: Int): ObservableTransformer<T, T> {
        // TODO Use Observable.using instead of doFinally (doOnTerminate isn't called when take() is used)
        // https://github.com/tranngoclam/rx-progress-dialog/blob/master/library/src/main/java/io/github/lamtran/rpd/RxProgressDialog.java
        // https://github.com/ReactiveX/RxJava/issues/3124#issuecomment-126210874
        // https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#miscellaneous-changes

        return ObservableTransformer { upstream ->
            upstream
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally { progressBar.visibility = View.GONE }
                    .doOnSubscribe { show(progressBar, max) }
                    .doOnNext { progressBar.incrementProgressBy(1) }
        }
    }

    internal fun show(progressBar: ProgressBar, max: Int) {
        progressBar.progress = 0
        progressBar.max = max
        progressBar.visibility = View.VISIBLE
    }
}
