package com.ternaryop.utils.reactivex;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

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

    public static <T> ObservableTransformer<T, T> apply(@NonNull Context context, @StringRes final int titleId, final int max) {
        return apply(context, context.getResources().getString(titleId), max);
    }

    public static <T> ObservableTransformer<T, T> apply(@NonNull Context context, final String title, final int max) {
        final ProgressDialog progressDialog = new ProgressDialog(context);

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
                                System.out.println("ProgressIndicatorObservable.run doFinally");
                                progressDialog.dismiss();
                            }
                        })
                        .doOnSubscribe(new Consumer<Disposable>() {
                            @Override
                            public void accept(Disposable disposable) throws Exception {
                                show(progressDialog, title, max, disposable);
                            }
                        })
                        .doOnNext(new Consumer<T>() {
                            @Override
                            public void accept(T obj) throws Exception {
                                progressDialog.incrementProgressBy(1);
                            }
                        });
            }
        };
    }

    protected static void show(final ProgressDialog progressDialog, final String title, final int max, final Disposable disposable) {
        progressDialog.setMessage(title);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(max);
        progressDialog.show();
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (disposable != null && !disposable.isDisposed()) {
                    disposable.dispose();
                }
            }
        });
    }
}
