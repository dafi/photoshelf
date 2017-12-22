package com.ternaryop.photoshelf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;

import com.ternaryop.photoshelf.adapter.PhotoShelfPost;
import com.ternaryop.photoshelf.db.DBHelper;
import com.ternaryop.photoshelf.db.PostTagDAO;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.tumblr.TumblrUtils;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class DraftPostHelper {
    private final Context context;
    private final String blogName;
    private final Tumblr tumblr;

    public DraftPostHelper(Context context, String blogName) {
        this.context = context;
        this.blogName = blogName;
        tumblr = Tumblr.getSharedTumblr(context);
    }

    /**
     * Return the map where the key is the first tag and value contains the posts for that tag
     * @param posts posts to group by tag
     * @return the (tag, posts) map
     */
    public Map<String, List<TumblrPost>> groupPostByTag(Collection<TumblrPost> posts) {
        HashMap<String, List<TumblrPost>> map = new HashMap<>();

        for (TumblrPost post : posts) {
            if (post.getType().equals("photo") && post.getTags().size() > 0) {
                final String tag = post.getTags().get(0).toLowerCase(Locale.US);
                List<TumblrPost> list = map.get(tag);
                if (list == null) {
                    list = new ArrayList<>();
                    map.put(tag, list);
                }
                list.add(post);
            }
        }

        return map;
    }

    private Maybe<TumblrPhotoPost> findLastPublishedPost(final String tag) {
        return Single
                .fromCallable(() -> {
                    final HashMap<String, String> params = new HashMap<>();
                    params.put("type", "photo");
                    params.put("limit", "1");
                    params.put("tag", tag);

                    return tumblr.getPhotoPosts(blogName, params);
                })
                .filter(posts -> !posts.isEmpty())
                .map(posts -> posts.get(0));
    }

    public Single<Map<String, Long>> getTagLastPublishedMap(final Set<String> tags) {
        final PostTagDAO postTagDAO = DBHelper.getInstance(context).getPostTagDAO();
        final Map<String, Long> postByTags = postTagDAO.getMapTagLastPublishedTime(tags, blogName);
        final ExecutorService executorService = Executors.newFixedThreadPool(5);

        return Observable
                .fromIterable(tags)
                .flatMap(tag -> {
                    Long lastPublishedTime = postByTags.get(tag);
                    if (lastPublishedTime == null) {
                        return findLastPublishedPost(tag)
                                .subscribeOn(Schedulers.from(executorService)).toObservable();
                    }
                    final TumblrPhotoPost post = new TumblrPhotoPost();
                    post.setTags(tag);
                    post.setTimestamp(lastPublishedTime);
                    return Observable.just(post);
                })
                .toMap(post -> post.getTags().get(0).toLowerCase(Locale.US), TumblrPost::getTimestamp);
    }

    public List<PhotoShelfPost> getPhotoShelfPosts(
            Map<String, List<TumblrPost>> draftPosts,
            Map<String, List<TumblrPost>> queuedPosts,
            Map<String, Long> lastPublished) {
        ArrayList<PhotoShelfPost> list = new ArrayList<>();

        for (String tag : draftPosts.keySet()) {
            long lastPublishedTimestamp = getLastPublishedTimestampByTag(tag, lastPublished);
            long queuedTimestamp = getNextScheduledPublishTimeByTag(tag, queuedPosts);
            long timestampToSave = queuedTimestamp > 0 ? queuedTimestamp : lastPublishedTimestamp;

            for (TumblrPost post : draftPosts.get(tag)) {
                // preserve schedule time when present
                post.setScheduledPublishTime(queuedTimestamp / 1000);
                list.add(new PhotoShelfPost((TumblrPhotoPost) post, timestampToSave));
            }
        }
        return list;
    }

    private long getNextScheduledPublishTimeByTag(final String tag, final Map<String, List<TumblrPost>> queuedPosts) {
        final List<TumblrPost> list = queuedPosts.get(tag);

        if (list == null) {
            return 0;
        }
        // posts are sorted by schedule date so it's sufficient to get the first item
        return list.get(0).getScheduledPublishTime() * 1000;
    }

    private long getLastPublishedTimestampByTag(final String tag, final Map<String, Long> lastPublished) {
        Long lastPublishedTimestamp = lastPublished.get(tag);

        if (lastPublishedTimestamp == null) {
            return Long.MAX_VALUE;
        }
        return lastPublishedTimestamp * 1000;
    }

    public Single<List<TumblrPost>> getNewerDraftPosts(long maxTimestamp) {
        return Single.fromCallable(() -> tumblr.getDraftPosts(blogName, maxTimestamp));
    }

    public Single<List<TumblrPost>> getQueuePosts() {
        return Single.fromCallable(() -> TumblrUtils.getQueueAll(tumblr, blogName));
    }
}
