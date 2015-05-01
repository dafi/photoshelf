package com.ternaryop.photoshelf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.ternaryop.photoshelf.adapter.PhotoShelfPost;
import com.ternaryop.photoshelf.db.PostTagDAO;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.tumblr.TumblrUtils;

public class DraftPostHelper {

    /**
     * Return map where key is the first tag and value is the post
     * @param draftPosts draft posts to convert to map (tag, posts)
     * @return the tag, posts map
     */
    public Map<String, List<TumblrPost> > getTagsForDraftPosts(List<TumblrPost> draftPosts) {
        HashMap<String, List<TumblrPost> > map = new HashMap<String, List<TumblrPost> >();

        for (TumblrPost post : draftPosts) {
            if (post.getType().equals("photo") && post.getTags().size() > 0) {
                String tag = post.getTags().get(0).toLowerCase(Locale.US);
                List<TumblrPost> list = map.get(tag);
                if (list == null) {
                    list = new ArrayList<TumblrPost>();
                    map.put(tag.toLowerCase(Locale.US), list);
                }
                list.add(post);
            }
        }

        return map;
    }

    /**
     * Return map where key is the first tag and value the post
     * @param queuedPosts queued posts to convert to map (tag, posts)
     * @return the tag, posts map
     */
    public Map<String, TumblrPost> getTagsForQueuedPosts(List<TumblrPost> queuedPosts) {
        HashMap<String, TumblrPost> map = new HashMap<String, TumblrPost>();

        for (TumblrPost post : queuedPosts) {
            if (post.getType().equals("photo") && post.getTags().size() > 0) {
                String tag = post.getTags().get(0);
                map.put(tag.toLowerCase(Locale.US), post);
            }
        }

        return map;
    }

    public Map<String, Long> getLastPublishedPhotoByTags(final Tumblr tumblr,
            final String tumblrName,
            final List<String> tags,
            final PostTagDAO postTagDAO)
            throws Exception {
        final Map<String, Long> lastPublish = new HashMap<String, Long>();
        Map<String, Long> postByTags = postTagDAO.getMapTagLastPublishedTime(tags, tumblrName);

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        ArrayList<Callable<Exception>> callables = new ArrayList<Callable<Exception>>();

        for (final String tag : tags) {
            Long lastPublishedTime = postByTags.get(tag);

            if (lastPublishedTime == null) {
                // every thread receives its own parameters map
                final HashMap<String, String> params = new HashMap<String, String>();
                params.put("type", "photo");
                params.put("limit", "1");
                params.put("tag", tag);
                callables.add(new Callable<Exception>() {

                    @Override
                    public Exception call() {
                        try {
                            List<TumblrPhotoPost> posts = tumblr.getPhotoPosts(tumblrName, params);
                            for (TumblrPhotoPost post : posts) {
                                lastPublish.put(tag.toLowerCase(Locale.US), post.getTimestamp());
                            }
                        } catch (Exception e) {
                            return e;
                        }
                        return null;
                    }
                });
            } else {
                lastPublish.put(tag.toLowerCase(Locale.US), lastPublishedTime);
            }
        }
        for (Future<Exception> result : executorService.invokeAll(callables)) {
            Exception error = result.get();
            if (error != null) {
                throw error;
            }
        }
        return lastPublish;
    }
    
    public List<PhotoShelfPost> getDraftPosts(
            Map<String, List<TumblrPost>> draftPosts,
            Map<String, TumblrPost> queuedPosts,
            Map<String, Long> lastPublished) {
        ArrayList<PhotoShelfPost> list = new ArrayList<PhotoShelfPost>();

        for (String tag : draftPosts.keySet()) {
            List<TumblrPost> draftPostList = draftPosts.get(tag);
            long lastPublishedTimestamp = Long.MAX_VALUE;
            long queuedTimestamp = 0;
            
            if (queuedPosts.get(tag) != null) {
                queuedTimestamp = queuedPosts.get(tag).getScheduledPublishTime() * 1000; 
            }
            if (lastPublished.get(tag) != null) {
                lastPublishedTimestamp = lastPublished.get(tag) * 1000;
            }
            long timestampToSave;
            if (queuedTimestamp > 0) {
                timestampToSave = queuedTimestamp;
            } else {
                timestampToSave = lastPublishedTimestamp;
            }
            for (TumblrPost post : draftPostList) {
                // preserve schedule time when present
                post.setScheduledPublishTime(queuedTimestamp / 1000);
                list.add(new PhotoShelfPost((TumblrPhotoPost) post, timestampToSave));
            }
        }
        return list;
    }

    /**
     * Get in parallel tagsForDraftPosts and tagsForQueuedPosts, wait until all is retrieved
     * Expired scheduled posts are removed
     * @param tumblr the tumblr instance
     * @param tumblrName the blog name
     * @param tagsForDraftPosts on return contains value
     * @param queuedPosts on return contains value
     * @throws Exception 
     */
    public void getDraftAndQueueTags(
            final Tumblr tumblr,
            final String tumblrName,
            final HashMap<String, List<TumblrPost> > tagsForDraftPosts,
            final Map<String, TumblrPost> queuedPosts) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        ArrayList<Callable<Exception>> callables = new ArrayList<Callable<Exception>>();

        callables.add(new Callable<Exception>() {

            @Override
            public Exception call() throws Exception {
                try {
                    tagsForDraftPosts.putAll(getTagsForDraftPosts(tumblr.getDraftPosts(tumblrName)));
                } catch (Exception e) {
                    return e;
                }
                return null;
            }
        });
        
        callables.add(new Callable<Exception>() {

            @Override
            public Exception call() throws Exception {
                List<TumblrPost> posts = TumblrUtils.getQueueAll(tumblr, tumblrName);
                queuedPosts.putAll(getTagsForQueuedPosts(posts));
                return null;
            }
        });

        // throw the first exception found
        for (Future<Exception> result : executorService.invokeAll(callables)) {
            Exception error = result.get();
            if (error != null) {
                throw error;
            }
        }
    }
}
