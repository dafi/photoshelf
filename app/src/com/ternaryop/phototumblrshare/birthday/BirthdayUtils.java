package com.ternaryop.phototumblrshare.birthday;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.activity.BirthdaysPublisherActivity;
import com.ternaryop.phototumblrshare.db.Birthday;
import com.ternaryop.phototumblrshare.db.BirthdayDAO;
import com.ternaryop.phototumblrshare.db.DBHelper;
import com.ternaryop.phototumblrshare.db.PostTag;
import com.ternaryop.phototumblrshare.db.PostTagDAO;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPhotoPost;

public class BirthdayUtils {
	private static final String BIRTHDAY_NOTIFICATION_TAG = "com.ternaryop.photoshare.bday";
	private static final int BIRTHDAY_NOTIFICATION_ID = 1;

	public static boolean notifyBirthday(Context context) {
		BirthdayDAO birthdayDatabaseHelper = DBHelper
				.getInstance(context.getApplicationContext())
				.getBirthdayDAO();
		Calendar now = Calendar.getInstance(Locale.US);
		List<Birthday> list = birthdayDatabaseHelper.getBirthdayByDate(now.getTime());
		if (list.isEmpty()) {
			return false;
		}

		long currYear = now.get(Calendar.YEAR);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				context.getApplicationContext())
				.setContentIntent(createPendingIntent(context))
				.setSmallIcon(R.drawable.stat_notify_bday);
		if (list.size() == 1) {
			Birthday birthday = list.get(0);
			builder.setContentTitle(context.getString(R.string.birthday_title_singular));
			Calendar cal = Calendar.getInstance(Locale.US);
			cal.setTime(birthday.getBirthDate());
			long years = currYear - cal.get(Calendar.YEAR);
			builder.setContentText(context.getString(R.string.birthday_years_old, birthday.getName(), years));
		} else {
			builder.setContentTitle(context.getString(R.string.birthday_title_plural, list.size()));
			NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
			inboxStyle.setBigContentTitle(context.getString(R.string.birthday_notification_title));
			for (Birthday birthday : list) {
				Calendar cal = Calendar.getInstance(Locale.US);
				cal.setTime(birthday.getBirthDate());
				long years = currYear - cal.get(Calendar.YEAR);
			    inboxStyle.addLine(context.getString(R.string.birthday_years_old, birthday.getName(), years));
			}
			builder.setStyle(inboxStyle);
		}

		Notification notification = builder.build();
		NotificationManager notificationManager = (NotificationManager)context.getApplicationContext()
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(BIRTHDAY_NOTIFICATION_TAG, BIRTHDAY_NOTIFICATION_ID, notification);

		return true;
	}

	private static PendingIntent createPendingIntent(Context context) {
		// Define Activity to start
		Intent resultIntent = new Intent(context, BirthdaysPublisherActivity.class);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		// Adds the back stack
		stackBuilder.addParentStack(BirthdaysPublisherActivity.class);
		// Adds the Intent to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		// Gets a PendingIntent containing the entire back stack
		PendingIntent resultPendingIntent =
		        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		return resultPendingIntent;
	}
	
	public static List<TumblrPhotoPost> getPhotoPosts(final Context context, Calendar birthDate) {
        DBHelper dbHelper = DBHelper
                .getInstance(context.getApplicationContext());
        List<Birthday> birthDays = dbHelper
                .getBirthdayDAO()
                .getBirthdayByDate(birthDate.getTime());
	    ArrayList<TumblrPhotoPost> posts = new ArrayList<TumblrPhotoPost>();

	    PostTagDAO postTagDAO = dbHelper.getPostTagDAO();
	    Map<String, String> params = new HashMap<String, String>(2);
	    params.put("type", "photo");
	    for (Birthday b : birthDays) {
	        PostTag postTag = postTagDAO.getRandomPostByTag(b.getName(), b.getTumblrName());
	        if (postTag != null) {
	            params.put("id", String.valueOf(postTag.getId()));
	            TumblrPhotoPost post = (TumblrPhotoPost)Tumblr.getSharedTumblr(context)
	                    .getPublicPosts(b.getTumblrName(), params);
                posts.add(post);
	        }
        }
	    return posts;
	}

	public static void bestByRange(Context context, int fromYear, int toYear, String postTags, String tumblrName) {
	    /*
	     Example
                Calendar now = Calendar.getInstance(Locale.US);
                int currYear = now.get(Calendar.YEAR);
                BirthdayUtils.bestByRange(getApplicationContext(), currYear - 30, currYear, "BestUnder30", getBlogName());

	     */
	    final int THUMBS_COUNT = 9;
        DBHelper dbHelper = DBHelper.getInstance(context);
        List<Birthday> birthdays = dbHelper.getBirthdayDAO().getBirthdayByYearRange(fromYear, toYear, tumblrName);
        Collections.shuffle(birthdays);
        int maxItems = Math.min(birthdays.size(), THUMBS_COUNT);
        
        ArrayList<String> names = new ArrayList<String>();
        for (int i = 0; i < maxItems; i++) {
            Birthday birthday = birthdays.get(i);
            names.add(birthday.getName());
        }
        List<PostTag> posts = dbHelper.getPostTagDAO().getListTagsLastPublishedTime(names, tumblrName);
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"breathwomen-thumb-250\">");

        Map<String, String> params = new HashMap<String, String>(2);
        TumblrPhotoPost post = null;
        params.put("type", "photo");
        for (PostTag postTag : posts) {
            params.put("id", String.valueOf(postTag.getId()));
            post = (TumblrPhotoPost)Tumblr.getSharedTumblr(context)
                        .getPublicPosts(tumblrName, params);
            String imageUrl = post.getFirstPhotoAltSize().get(TumblrPhotoPost.IMAGE_INDEX_250_PIXELS).getUrl();
            sb.append("<img src=\"" + imageUrl + "\"/>");
        }                
        sb.append("</div>");
        System.out.println(sb);
        if (post != null) {
            Tumblr.getSharedTumblr(context)
                .draftPhotoPost(tumblrName,
                    post.getFirstPhotoAltSize().get(TumblrPhotoPost.IMAGE_INDEX_500_PIXELS).getUrl(),
                    sb.toString(),
                    postTags);
        }
    }
    
}
