package com.ternaryop.photoshelf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
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
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

public class DraftPostHelper {
    private final Context context;
    private final String blogName;
    private final Tumblr tumblr;
    private final TumblrPostCache draftCache;

    public DraftPostHelper(Context context, String blogName) {
        this.context = context;
        this.blogName = blogName;
        draftCache = new TumblrPostCache(context, blogName + "Draft");
        tumblr = Tumblr.getSharedTumblr(context);
    }

    public TumblrPostCache getDraftCache() {
        return draftCache;
    }

    /**
     * Return the map where the key is the first tag and value contains the posts for that tag
     * @param posts posts to group by tag
     * @return the (tag, posts) map
     */
    private Map<String, List<TumblrPost>> groupPostByTag(Collection<TumblrPost> posts) {
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
                .fromCallable(new Callable<List<TumblrPhotoPost>>() {
                    @Override
                    public List<TumblrPhotoPost> call() throws Exception {
                        final HashMap<String, String> params = new HashMap<>();
                        params.put("type", "photo");
                        params.put("limit", "1");
                        params.put("tag", tag);

                        return tumblr.getPhotoPosts(blogName, params);
                    }
                })
                .filter(new Predicate<List<TumblrPhotoPost>>() {
                    @Override
                    public boolean test(List<TumblrPhotoPost> posts) throws Exception {
                        return !posts.isEmpty();
                    }
                })
                .map(new Function<List<TumblrPhotoPost>, TumblrPhotoPost>() {
                    @Override
                    public TumblrPhotoPost apply(List<TumblrPhotoPost> posts) throws Exception {
                        return posts.get(0);
                    }
                });
    }

    public Single<Map<String, Long>> getTagLastPublishedMap(final Set<String> tags) {
        final PostTagDAO postTagDAO = DBHelper.getInstance(context).getPostTagDAO();
        final Map<String, Long> postByTags = postTagDAO.getMapTagLastPublishedTime(tags, blogName);
        final ExecutorService executorService = Executors.newFixedThreadPool(5);

        return Observable
                .fromIterable(tags)
                .flatMap(new Function<String, ObservableSource<TumblrPhotoPost>>() {
                    @Override
                    public ObservableSource<TumblrPhotoPost> apply(String tag) throws Exception {
                        Long lastPublishedTime = postByTags.get(tag);
                        if (lastPublishedTime == null) {
                            return findLastPublishedPost(tag)
                                    .subscribeOn(Schedulers.from(executorService)).toObservable();
                        }
                        final TumblrPhotoPost post = new TumblrPhotoPost();
                        post.setTags(tag);
                        post.setTimestamp(lastPublishedTime);
                        return Observable.just(post);
                    }
                })
                .toMap(new Function<TumblrPhotoPost, String>() {
                    @Override
                    public String apply(TumblrPhotoPost post) throws Exception {
                        return post.getTags().get(0).toLowerCase(Locale.US);
                    }
                }, new Function<TumblrPhotoPost, Long>() {
                    @Override
                    public Long apply(TumblrPhotoPost post) throws Exception {
                        return post.getTimestamp();
                    }
                });
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

    public Single< Map<String, List<TumblrPost> > > getDraftTags() {
        return Single.fromCallable(new Callable<Map<String, List<TumblrPost>>>() {
            @Override
            public Map<String, List<TumblrPost>> call() throws Exception {
                List<TumblrPost> draftPosts = draftCache.read();
                List<TumblrPost> newerPosts = tumblr.getDraftPosts(blogName, findMostRecentTimestamp(draftPosts));

                if (newerPosts.size() > 0) {
                    ArrayList<TumblrPost> arr = new ArrayList<>(newerPosts);
                    arr.addAll(draftPosts);
                    draftPosts = arr;
                    draftCache.write(arr, true);
                }
                return groupPostByTag(draftPosts);
            }
        });
    }

    private long findMostRecentTimestamp(List<TumblrPost> posts) {
        long maxTimestamp = 0;

        for (TumblrPost p : posts) {
            if (p.getTimestamp() > maxTimestamp) {
                maxTimestamp = p.getTimestamp();
            }
        }
        return maxTimestamp;
    }

    public Single<Map<String, List<TumblrPost>>> getQueueTags() {
        return Single.fromCallable(new Callable<Map<String, List<TumblrPost>>>() {
            @Override
            public Map<String, List<TumblrPost>> call() throws Exception {
                return groupPostByTag(TumblrUtils.getQueueAll(tumblr, blogName));
            }
        });
    }
}
