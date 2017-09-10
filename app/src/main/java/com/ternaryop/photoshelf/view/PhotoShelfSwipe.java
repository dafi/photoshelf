package com.ternaryop.photoshelf.view;

import android.support.annotation.IdRes;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

import com.ternaryop.photoshelf.R;
import com.ternaryop.widget.WaitingResultSwipeRefreshLayout;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by dave on 08/09/17.
 *
 * Hold the WaitingResultSwipeRefreshLayout used in many classes
 */

public class PhotoShelfSwipe {
    private final WaitingResultSwipeRefreshLayout swipeLayout;

    public PhotoShelfSwipe(View rootView, @IdRes int id) {
        this(rootView, id, null);
    }

    public PhotoShelfSwipe(View rootView, @IdRes int id, SwipeRefreshLayout.OnRefreshListener listener) {
        swipeLayout = (WaitingResultSwipeRefreshLayout) rootView.findViewById(id);
        swipeLayout.setColorScheme(R.array.progress_swipe_colors);
        if (listener == null) {
            swipeLayout.setEnabled(false);
        } else {
            swipeLayout.setOnRefreshListener(listener);
        }
    }

    /**
     * Create a transformer that show/hide the swipe refresh
     * @param <T> the Upstream
     * @return the transformer
     */
    public <T> SingleTransformer<T, T> applySwipe() {
        return new SingleTransformer<T, T>() {
            @Override
            public SingleSource<T> apply(Single<T> upstream) {
                return upstream
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(new Consumer<Disposable>() {
                            @Override
                            public void accept(Disposable disposable) throws Exception {
                                swipeLayout.setRefreshingAndWaintingResult(true);
                            }
                        })
                        .doFinally(new Action() {
                            @Override
                            public void run() throws Exception {
                                swipeLayout.setRefreshingAndWaintingResult(false);
                            }
                        });
            }
        };
    }

    public WaitingResultSwipeRefreshLayout getSwipe() {
        return swipeLayout;
    }
}
