package com.ternaryop.photoshelf.fragment;

import java.util.HashMap;
import java.util.List;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.adapter.PhotoShelfPost;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.tumblr.TumblrPost;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class PublishedPostsListFragment extends ScheduledListFragment {
    @Override
    protected void readPhotoPosts() {
        if (isScrolling) {
            return;
        }
        refreshUI();
        isScrolling = true;

        HashMap<String, String> params = new HashMap<>();
        params.put("offset", String.valueOf(offset));
        params.put("type", "photo");
        params.put("notes_info", "true");

        Observable
                .just(params)
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        isScrolling = false;
                    }
                })
                .flatMap(new Function<HashMap<String, String>, ObservableSource<TumblrPost>>() {
                    @Override
                    public ObservableSource<TumblrPost> apply(HashMap<String, String> params) throws Exception {
                        return Observable.fromIterable(Tumblr.getSharedTumblr(getActivity())
                                .getPublicPosts(getBlogName(), params));
                    }
                })
                .map(new Function<TumblrPost, PhotoShelfPost>() {
                    @Override
                    public PhotoShelfPost apply(TumblrPost tumblrPost) throws Exception {
                        return new PhotoShelfPost((TumblrPhotoPost)tumblrPost, tumblrPost.getTimestamp() * 1000);
                    }
                })
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(photoShelfSwipe.<List<PhotoShelfPost>>applySwipe())
                .subscribe(new SingleObserver<List<PhotoShelfPost>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onSuccess(List<PhotoShelfPost> photoList) {
                        totalPosts += photoList.size();
                        hasMorePosts = photoList.size() == Tumblr.MAX_POST_PER_REQUEST;
                        photoAdapter.addAll(photoList);
                        refreshUI();
                    }

                    @Override
                    public void onError(Throwable t) {
                        showSnackbar(makeSnake(recyclerView, t));
                    }
                });
    }

    @Override
    protected int getActionModeMenuId() {
        return R.menu.published_context;
    }
}
