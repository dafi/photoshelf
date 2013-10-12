package com.ternaryop.phototumblrshare;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.ternaryop.phototumblrshare.db.PostTag;
import com.ternaryop.phototumblrshare.db.PostTagDatabaseHelper;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.tumblr.TumblrPost;

public class DraftPostHelper {

	/**
	 * Return map where key is the first tag and value is the post
	 * @param draftPosts
	 * @return
	 */
	public Map<String, List<TumblrPost> > getTagsForDraftPosts(List<TumblrPost> draftPosts) {
	    HashMap<String, List<TumblrPost> > map = new HashMap<String, List<TumblrPost> >();

	    for (TumblrPost post : draftPosts) {
	    	if (post.getType().equals("photo")) {
		    	String tag = post.getTags().get(0);
		    	List<TumblrPost> list = map.get(tag);
		    	if (list == null) {
		    		list = new ArrayList<TumblrPost>();
		    		map.put(tag, list);
		    	}
		    	list.add(post);
	    	}
		}

	    return map;
	}

	/**
	 * Return map where key is the first tag and value the post
	 * @param queuedPosts
	 * @return
	 * @throws TumblrPost
	 */
	public Map<String, TumblrPost> getTagsForQueuedPosts(List<TumblrPost> queuedPosts) {
	    HashMap<String, TumblrPost> map = new HashMap<String, TumblrPost>();

	    for (TumblrPost post : queuedPosts) {
	    	if (post.getType().equals("photo")) {
		    	String tag = post.getTags().get(0);
		    	map.put(tag, post);
	    	}
		}

	    return map;
	}

	public Map<String, PostTag> getLastPublishedPhotoByTags(final Tumblr tumblr,
			final String tumblrName,
			final List<String> tags,
			final PostTagDatabaseHelper dbHelper)
			throws Exception {
		final Map<String, PostTag> lastPublish = new HashMap<String, PostTag>();
		Map<String, PostTag> postByTags = dbHelper.getPostByTags(tags, tumblrName);

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        ArrayList<Callable<Exception>> callables = new ArrayList<Callable<Exception>>();
		
		for (Iterator<String> itr = tags.iterator(); itr.hasNext();) {
			final String tag = itr.next();
			PostTag postTag = postByTags.get(tag);
			
			if (postTag == null) {
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
						    	PostTag newPostTag = new PostTag(
						    			post.getPostId(),
						    			tumblrName,
						    			tag,
						    			post.getTimestamp(),
						    			1,
						    			PostTag.POST_TYPE_PUBLISHED);
						    	dbHelper.insert(newPostTag);
						    	lastPublish.put(tag, newPostTag);
							}
						} catch (Exception e) {
							return e;
						}
						return null;
					}
				});
			} else {
		    	lastPublish.put(tag, postTag);
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
	
	public List<PhotoSharePost> getDraftPostSortedByPublishDate(
			Map<String, List<TumblrPost> > draftPosts,
			Map<String, TumblrPost> queuedPosts,
			Map<String, PostTag> lastPublished) {
		ArrayList<TimestampPosts> temp = new ArrayList<TimestampPosts>();

		for (String tag : draftPosts.keySet()) {
			List<TumblrPost> draftPostList = draftPosts.get(tag);
		    long lastPublishedTimestamp = Long.MAX_VALUE;
		    long queuedTimestamp = 0;
		    
		    if (queuedPosts.get(tag) != null) {
		    	queuedTimestamp = queuedPosts.get(tag).getScheduledPublishTime() * 1000; 
		    }
		    if (lastPublished.get(tag) != null) {
		    	lastPublishedTimestamp = lastPublished.get(tag).getPublishTimestamp() * 1000;
		    }
	    	long timestampToSave;
		    if (queuedTimestamp > 0) {
		    	timestampToSave = queuedTimestamp;
		    } else {
		    	timestampToSave = lastPublishedTimestamp;
		    }
		    if (timestampToSave != Long.MAX_VALUE) {
			    // remove time to allow sort only on date
	        	Calendar cal = Calendar.getInstance();
	        	cal.setTime(new Date(timestampToSave));
	        	cal.set(Calendar.HOUR_OF_DAY, 0);
	        	cal.set(Calendar.MINUTE, 0);
	        	cal.set(Calendar.SECOND, 0);
	        	cal.set(Calendar.MILLISECOND, 0);
	        	timestampToSave = cal.getTimeInMillis();
		    }
		    List<PhotoSharePost> photoShareList = new ArrayList<PhotoSharePost>(); 
	    	for (TumblrPost post : draftPostList) {
	    		photoShareList.add(new PhotoSharePost((TumblrPhotoPost) post, timestampToSave));
			}
	    	temp.add(new TimestampPosts(timestampToSave, photoShareList));
		}
		// sort following order from top to bottom
		// Never Published
		// Older published
		// In Queue
		Collections.sort(temp, new Comparator<TimestampPosts>() {

			@Override
			public int compare(TimestampPosts lhs, TimestampPosts rhs) {
			    long lhsTimestamp = lhs.timestamp;
			    long rhsTimestamp = rhs.timestamp;

			    long ldiff = lhsTimestamp - rhsTimestamp;
			    int diff = ldiff == 0 ? 0 : ldiff < 0 ? -1 : 1;

			    if (diff == 0) {
					TumblrPost lhsPost = lhs.posts.get(0);
					TumblrPost rhsPost = rhs.posts.get(0);
					String lhsTag = lhsPost.getTags().get(0);
			    	String rhsTag = rhsPost.getTags().get(0);
			        diff = lhsTag.compareToIgnoreCase(rhsTag);
			    } else {
				    if (lhsTimestamp == Long.MAX_VALUE) {
				    	return -1;
				    }
				    if (rhsTimestamp == Long.MAX_VALUE) {
				    	return 1;
				    }
			    }

			    return diff;
			}
		});
		ArrayList<PhotoSharePost> list = new ArrayList<PhotoSharePost>();
		for (TimestampPosts tsp : temp) {
			list.addAll(tsp.posts);
		}
		return list;
	}

	/**
	 * Determine days difference since timestamp
	 * if timestamp is equal to Long.MAX_VALUE then return Long.MAX_VALUE
	 * 
	 * @param timestamp
	 * @return numbers of days, if negative indicates days in the future beyond 
	 * passed timestamp
	 */
    public static long daysSinceTimestamp(long timestamp) {
    	Calendar cal = Calendar.getInstance();
    	cal.set(Calendar.HOUR_OF_DAY, 0);
    	cal.set(Calendar.MINUTE, 0);
    	cal.set(Calendar.SECOND, 0);
    	cal.set(Calendar.MILLISECOND, 0);
    	long nowTime = cal.getTimeInMillis();
        long dayTime = 24 * 60 * 60 * 1000;
        long days;

        if (timestamp == Long.MAX_VALUE) {
            days = Long.MAX_VALUE;
        } else {
        	cal = Calendar.getInstance();
        	cal.setTime(new Date(timestamp));
        	cal.set(Calendar.HOUR_OF_DAY, 0);
        	cal.set(Calendar.MINUTE, 0);
        	cal.set(Calendar.SECOND, 0);
        	cal.set(Calendar.MILLISECOND, 0);

            long tsWithoutTime = cal.getTimeInMillis();
            long spanTime = nowTime - tsWithoutTime;
            days = spanTime / dayTime;
        }
        return days;
    }
	
    public static String formatPublishDaysAgo(long timestamp) {
    	long days = daysSinceTimestamp(timestamp);
        String dayString;

        if (days == Long.MAX_VALUE) {
            dayString = "Never Published";
        } else {
            if (days < 0) {
            	dayString = "In " + (-days) + " days";
            } else if (days == 0) {
                dayString = "Today";
            } else if (days == 1) {
                dayString = "Yesterday";
            } else {
                dayString = days + " days ago";
            }
        }
        return dayString;
    }
    
    public class TimestampPosts {
		long timestamp;
    	List<PhotoSharePost> posts;
    	public TimestampPosts(long timestamp, List<PhotoSharePost> posts) {
			super();
			this.timestamp = timestamp;
			this.posts = posts;
		}
    }
   
    /**
     * Get in parallel tagsForDraftPosts and tagsForQueuedPosts, wait until all is retrieved
     * Expired scheduled posts are removed
     * @param tagsForDraftPosts on return contains value
     * @param queuedPosts on return contains value
     * @throws Exception 
     */
    public void getDraftAndQueueTags(
    		final Tumblr tumblr,
			final String tumblrName,
			final HashMap<String, List<TumblrPost> > tagsForDraftPosts,
    		final Map<String, TumblrPost> queuedPosts,
    		final PostTagDatabaseHelper dbHelper) throws Exception {
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
				try {
					dbHelper.removeExpiredScheduledPosts(System.currentTimeMillis());
					List<TumblrPost> posts = tumblr.getQueue(tumblrName);
					for (TumblrPost post : posts) {
				    	if (post.getType().equals("photo")) {
							for (String tag : post.getTags()) {
						    	PostTag newPostTag = new PostTag(
						    			post.getPostId(),
						    			tumblrName,
						    			tag,
						    			post.getScheduledPublishTime(),
						    			1,
						    			PostTag.POST_TYPE_SCHEDULED);
						    	dbHelper.insertOrIgnore(newPostTag);
							}
				    	}
					}
					queuedPosts.putAll(getTagsForQueuedPosts(posts));
				} catch (Exception e) {
					return e;
				}
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
