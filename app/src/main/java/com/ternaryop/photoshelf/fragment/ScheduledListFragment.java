package com.ternaryop.photoshelf.fragment;

import java.util.HashMap;
import java.util.List;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.adapter.PhotoShelfPost;
import com.ternaryop.photoshelf.view.PhotoShelfSwipe;
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

public class ScheduledListFragment extends AbsPostsListFragment {
    protected PhotoShelfSwipe photoShelfSwipe;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        photoShelfSwipe = new PhotoShelfSwipe(rootView, R.id.swipe_container, new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                resetAndReloadPhotoPosts();
            }
        });
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        photoAdapter.setOnPhotoBrowseClick(this);
        
        if (getBlogName() != null) {
            resetAndReloadPhotoPosts();
        }
    }

    @Override
    protected int getActionModeMenuId() {
        return R.menu.scheduled_context;
    }
    
    @Override
    protected void readPhotoPosts() {
        if (isScrolling) {
            return;
        }
        refreshUI();
        isScrolling = true;

        final HashMap<String, String> params = new HashMap<>();
        params.put("offset", String.valueOf(offset));

        // we assume all returned items are photos, (we handle only photos)
        // if other posts type are returned, the getQueue() list size may be greater than photo list size
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
                                .getQueue(getBlogName(), params));
                    }
                })
                .map(new Function<TumblrPost, PhotoShelfPost>() {
                    @Override
                    public PhotoShelfPost apply(TumblrPost tumblrPost) throws Exception {
                        return new PhotoShelfPost((TumblrPhotoPost) tumblrPost,
                                tumblrPost.getScheduledPublishTime() * 1000);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.scheduler, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                resetAndReloadPhotoPosts();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
