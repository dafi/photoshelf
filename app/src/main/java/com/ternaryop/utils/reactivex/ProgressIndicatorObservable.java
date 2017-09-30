package com.ternaryop.utils.reactivex;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ProgressBar;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by dave on 03/09/17.
 * Show a progress dialog and return an Rx Observable
 */

public class ProgressIndicatorObservable {
    private ProgressIndicatorObservable() {
    }

    public static <T> ObservableTransformer<T, T> apply(@NonNull final ProgressBar progressBar, final int max) {
        // TODO Use Observable.using instead of doFinally (doOnTerminate isn't called when take() is used)
        // https://github.com/tranngoclam/rx-progress-dialog/blob/master/library/src/main/java/io/github/lamtran/rpd/RxProgressDialog.java
        // https://github.com/ReactiveX/RxJava/issues/3124#issuecomment-126210874
        // https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#miscellaneous-changes

        return new ObservableTransformer<T, T>() {
            @Override
            public ObservableSource<T> apply(Observable<T> upstream) {
                return upstream
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally(new Action() {
                            @Override
                            public void run() throws Exception {
                                progressBar.setVisibility(View.GONE);
                            }
                        })
                        .doOnSubscribe(new Consumer<Disposable>() {
                            @Override
                            public void accept(Disposable disposable) throws Exception {
                                show(progressBar, max);
                            }
                        })
                        .doOnNext(new Consumer<T>() {
                            @Override
                            public void accept(T obj) throws Exception {
                                progressBar.incrementProgressBy(1);
                            }
                        });
            }
        };
    }

    protected static void show(final ProgressBar progressBar, final int max) {
        progressBar.setProgress(0);
        progressBar.setMax(max);
        progressBar.setVisibility(View.VISIBLE);
    }
}
