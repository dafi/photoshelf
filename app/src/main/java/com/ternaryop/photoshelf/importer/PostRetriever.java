package com.ternaryop.photoshelf.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.support.annotation.NonNull;

import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPost;
import io.reactivex.Observable;

public class PostRetriever {
    private final Tumblr sharedTumblr;
    private int offset;
    private int total;

    public PostRetriever(Context context) {
        sharedTumblr = Tumblr.getSharedTumblr(context);
    }

    public Observable<List<TumblrPost>> readPhotoPosts(final String tumblrName, final long lastPublishTimestamp) {
        return readPhotoPosts(tumblrName, lastPublishTimestamp, null);
    }

    public Observable<List<TumblrPost>> readPhotoPosts(final String tumblrName, final long lastPublishTimestamp, final String tag) {
        HashMap<String, String> params = buildParams(tag);

        offset = 0;
        total = 0;
        return Observable.generate(emitter -> {
            params.put("offset", String.valueOf(offset));
            List<TumblrPost> postsList = sharedTumblr.getPublicPosts(tumblrName, params);
            boolean loadNext = postsList.size() > 0;
            offset += postsList.size();

            final ArrayList<TumblrPost> newerPosts = new ArrayList<>();
            for (TumblrPost tumblrPost : postsList) {
                if (lastPublishTimestamp < tumblrPost.getTimestamp()) {
                    newerPosts.add(tumblrPost);
                } else {
                    loadNext = false;
                    break;
                }
            }

            total += newerPosts.size();

            emitter.onNext(newerPosts);
            if (!loadNext) {
                emitter.onComplete();
            }
        });
    }

    @NonNull
    private HashMap<String, String> buildParams(String tag) {
        HashMap<String, String> params = new HashMap<>();
        params.put("type", "photo");
        if (tag != null && !tag.trim().isEmpty()) {
            params.put("tag", tag);
        }
        return params;
    }

    public int getTotal() {
        return total;
    }
}