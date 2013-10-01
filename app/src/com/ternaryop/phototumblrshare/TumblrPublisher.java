package com.ternaryop.phototumblrshare;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import oauth.signpost.exception.OAuthException;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.helper.StringUtil;

import com.ternaryop.phototumblrshare.db.PostTag;
import com.ternaryop.phototumblrshare.db.PostTagDatabaseHelper;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.utils.JSONUtils;

public class TumblrPublisher {

	/**
	 * Return map where key is the first tag and value is the post
	 * @param draftPosts
	 * @return
	 * @throws JSONException
	 */
	public Map<String, List<JSONObject> > getTagsForDraftPosts(JSONArray draftPosts) throws JSONException {
	    HashMap<String, List<JSONObject> > map = new HashMap<String, List<JSONObject> >();

	    for (int i = 0; i < draftPosts.length(); i++) {
	    	JSONObject post = draftPosts.getJSONObject(i);
	    	String tag = post.getJSONArray("tags").getString(0);
	    	List<JSONObject> list = map.get(tag);
	    	if (list == null) {
	    		list = new ArrayList<JSONObject>();
	    		map.put(tag, list);
	    	}
	    	list.add(post);
	    }

	    return map;
	}

	/**
	 * Return map where key is the first tag and value the post
	 * @param queuedPosts
	 * @return
	 * @throws JSONException
	 */
	public Map<String, JSONObject> getTagsForQueuedPosts(JSONArray queuedPosts) throws JSONException {
	    HashMap<String, JSONObject> map = new HashMap<String, JSONObject>();

	    for (int i = 0; i < queuedPosts.length(); i++) {
	    	JSONObject post = queuedPosts.getJSONObject(i);
	    	String tag = post.getJSONArray("tags").getString(0);
	    	map.put(tag, post);
	    }

	    return map;
	}

	public Map<String, JSONObject> getLastPublishedPhotoByTags(List<String> tags, final String tumblrName) throws JSONException, ClientProtocolException, IllegalStateException, IOException, OAuthException {
		Map<String, JSONObject> lastPublish = new HashMap<String, JSONObject>();
		String url = "http://localhost/consolr/api/tags/lastPublished.php?";

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("tags", StringUtil.join(tags, ",")));
        params.add(new BasicNameValuePair("tumblrName", tumblrName));
        params.add(new BasicNameValuePair("api_key", ""));
		
		String paramString = URLEncodedUtils.format(params, "UTF-8");
        url += paramString;        	

        HttpContext context = new BasicHttpContext();
        HttpRequestBase request = new HttpGet(url);

        JSONObject jsonObject = JSONUtils.jsonFromInputStream(new DefaultHttpClient().execute(request, context).getEntity().getContent());
        System.out.println("TumblrPublisher.getLastPublishedPhotoByTags()" + jsonObject);

		return lastPublish;
	}

	public Map<String, PostTag> getLastPublishedPhotoByTags(List<String> tags, Tumblr tumblr, final String tumblrName, PostTagDatabaseHelper dbHelper)
			throws JSONException, ClientProtocolException, IllegalStateException, IOException, OAuthException {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("type", "photo");
		params.put("limit", "1");

		Map<String, PostTag> lastPublish = new HashMap<String, PostTag>();
		Map<String, PostTag> postByTags = dbHelper.getPostByTags(tags, tumblrName);

		for (Iterator<String> itr = tags.iterator(); itr.hasNext();) {
			String tag = itr.next();
			PostTag postTag = postByTags.get(tag);
			
			if (postTag == null) {
				params.put("tag", tag);
				JSONArray posts = tumblr.getPosts(tumblrName, params);
			    if (posts.length() > 0) {
			    	JSONObject post = posts.getJSONObject(0);
			    	PostTag newPostTag = new PostTag(post.getLong("id"), tumblrName, tag, post.getLong("timestamp"), 1);
			    	dbHelper.insert(newPostTag);
			    	lastPublish.put(tag, newPostTag);
			    }
			} else {
		    	lastPublish.put(tag, postTag);
			}
		}
		
		return lastPublish;
	}
	
	public List<JSONObject> getDraftPostSortedByPublishDate(
			Map<String, List<JSONObject> > draftPosts,
			Map<String, JSONObject> queuedPosts,
			Map<String, PostTag> lastPublished) throws JSONException {
		ArrayList<JSONObject> list = new ArrayList<JSONObject>();
		ArrayList<TimestampPosts> temp = new ArrayList<TimestampPosts>();

		for (String tag : draftPosts.keySet()) {
			List<JSONObject> draftPostList = draftPosts.get(tag);
		    long lastPublishedTimestamp = Long.MAX_VALUE;
		    long queuedTimestamp = 0;
		    
		    if (queuedPosts.get(tag) != null) {
		    	queuedTimestamp = queuedPosts.get(tag).getLong("scheduled_publish_time") * 1000; 
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
	    	for (JSONObject post : draftPostList) {
		    	post.put("photo-tumblr-share-timestamp", timestampToSave);
			}
	    	temp.add(new TimestampPosts(timestampToSave, draftPostList));
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

			    if (lhsTimestamp == Long.MAX_VALUE) {
			    	return -1;
			    }
			    if (rhsTimestamp == Long.MAX_VALUE) {
			    	return 1;
			    }
			    long ldiff = lhsTimestamp - rhsTimestamp;
			    int diff = ldiff == 0 ? 0 : ldiff < 0 ? -1 : 1;

			    if (diff == 0) {
					try {
						JSONObject lhsPost = lhs.posts.get(0);
						JSONObject rhsPost = rhs.posts.get(0);
						String lhsTag = lhsPost.getJSONArray("tags").getString(0);
				    	String rhsTag = rhsPost.getJSONArray("tags").getString(0);
				        diff = lhsTag.compareToIgnoreCase(rhsTag);
					} catch (JSONException e) {
						e.printStackTrace();
					}
			    }

			    return diff;
			}
		});
		for (TimestampPosts tsp : temp) {
	    	System.out
					.println("TumblrPublisher.getDraftPostSortedByPublishDate()" + tsp.timestamp + " tag " + tsp.posts.get(0).getJSONArray("tags").getString(0));
			list.addAll(tsp.posts);
		}
		return list;
	}

    public static String formatPublishDaysAgo(long timestamp) {
    	Calendar cal = Calendar.getInstance();
    	cal.set(Calendar.HOUR_OF_DAY, 0);
    	cal.set(Calendar.MINUTE, 0);
    	cal.set(Calendar.SECOND, 0);
    	cal.set(Calendar.MILLISECOND, 0);
    	long nowTime = cal.getTimeInMillis();
        long dayTime = 24 * 60 * 60 * 1000;
        String dayString = null;

        if (timestamp == Long.MAX_VALUE) {
            dayString = "Never Published";
        } else {
        	cal = Calendar.getInstance();
        	cal.setTime(new Date(timestamp));
        	cal.set(Calendar.HOUR_OF_DAY, 0);
        	cal.set(Calendar.MINUTE, 0);
        	cal.set(Calendar.SECOND, 0);
        	cal.set(Calendar.MILLISECOND, 0);

            long tsWithoutTime = cal.getTimeInMillis();
            long spanTime = nowTime - tsWithoutTime;
            long days = spanTime / dayTime;
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
    
    private class TimestampPosts {
		long timestamp;
    	List<JSONObject> posts;
    	public TimestampPosts(long timestamp, List<JSONObject> posts) {
			super();
			this.timestamp = timestamp;
			this.posts = posts;
		}
    }
}
