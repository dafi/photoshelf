package com.ternaryop.phototumblrshare.db;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class LastPublishedPostCacheDAO implements BaseColumns {
	public static final String TABLE_NAME = "LAST_PUBLISHED_POSTS_CACHE";
	
	public static final String POST_ID = "POST_ID";
	public static final String TUMBLR_NAME = "TUMBLR_NAME";
	public static final String TAG = "TAG";
	public static final String PUBLISH_TIMESTAMP = "PUBLISH_TIMESTAMP";
	public static final String SHOW_ORDER = "SHOW_ORDER";
	public static final String POST_ID_TYPE = "POST_ID_TYPE";
	
	public static final String[] COLUMNS = new String[] { POST_ID, TUMBLR_NAME, TAG, PUBLISH_TIMESTAMP, SHOW_ORDER, POST_ID_TYPE };

	public static final String POST_TYPE_PUBLISHED = "p";
	public static final String POST_TYPE_SCHEDULED = "s";
		
	private SQLiteOpenHelper dbHelper;

	/**
	 * Constructor is accessible only from package
	 */
	LastPublishedPostCacheDAO(SQLiteOpenHelper dbHelper) {
		this.dbHelper = dbHelper;
	}
 	
	void onCreate(SQLiteDatabase db) {
		String sql = "CREATE TABLE {0} ("
				+ "{1} INTEGER NOT NULL,"
				+ "{2} TEXT NOT NULL," 
				+ "{3} TEXT NOT NULL," 
				+ "{4} INTEGER NOT NULL,"
				+ "{5} INTEGER NOT NULL,"
				+ "{6} TEXT NOT NULL," 
				+ "PRIMARY KEY ({1}, {3}))";
			db.execSQL(MessageFormat.format(sql,
					TABLE_NAME,
					POST_ID,
					TUMBLR_NAME,
					TAG,
					PUBLISH_TIMESTAMP,
					SHOW_ORDER,
					POST_ID_TYPE));
	}

	void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

	public ContentValues getContentValues(LastPublishedPostCache postTag) {
		ContentValues v = new ContentValues();

		v.put(POST_ID, postTag.getPostId());
		v.put(TUMBLR_NAME, postTag.getTumblrName());
		v.put(TAG, postTag.getTag());
		v.put(PUBLISH_TIMESTAMP, postTag.getPublishTimestamp());
		v.put(SHOW_ORDER, postTag.getShowOrder());
		v.put(POST_ID_TYPE, postTag.getPostIdType());
		
		return v;
	}
	public long insert(LastPublishedPostCache postTag) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rows = db.insert(TABLE_NAME, null, getContentValues(postTag));
		
		return rows;
	}

	public long insertOrIgnore(LastPublishedPostCache postTag) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		return db.insertWithOnConflict(
				TABLE_NAME,
				null,
				getContentValues(postTag),
				SQLiteDatabase.CONFLICT_IGNORE);
	}

	public void removeAll() {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("delete from " + TABLE_NAME);
	}

	public int removeExpiredScheduledPosts(long expireTime) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
        String whereClause = MessageFormat.format("{0} = ? and {1} < ?",
        		POST_ID_TYPE,
        		PUBLISH_TIMESTAMP);
        return db.delete(TABLE_NAME,
        		whereClause,
				new String[] { POST_TYPE_SCHEDULED, "" + expireTime });
	}

	public LastPublishedPostCache getPostByTag(String tag, String tumblrName) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = db.query(
				TABLE_NAME,
				COLUMNS, 
				TAG + " = ? and " + TUMBLR_NAME + " = ?",
				new String[] { tag, tumblrName },
			null, 
			null, 
			null);

		ArrayList<LastPublishedPostCache> list = cursorToPostTagList(c);
		LastPublishedPostCache postTag = list.isEmpty() ? null : list.get(0);
		
		return postTag;
	}

	public Map<String, LastPublishedPostCache> getPostByTags(List<String> tags, String tumblrName) {
		// contains tumblrName, too
		String args[] = new String[tags.size() + 1];
		tags.toArray(args);
		args[args.length - 1] = tumblrName;
		
		StringBuilder inClause = new StringBuilder();
		for (int i = 0; i < tags.size(); i++) {
			inClause.append("?");
			if (i < (tags.size() - 1)) {
				inClause.append(",");
			}
		}
		
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = db.query(
				TABLE_NAME,
				COLUMNS, 
				TAG + " in (" + inClause + ") and (" + TUMBLR_NAME + " = ?)",
				args,
			null, 
			null, 
			null);

		Map<String, LastPublishedPostCache> map = cursorToPostTagMap(c);
		
		return map;
	}
	
	private ArrayList<LastPublishedPostCache> cursorToPostTagList(Cursor c) {
		ArrayList<LastPublishedPostCache> list = new ArrayList<LastPublishedPostCache>();
		try {
			while (c.moveToNext()) {
				LastPublishedPostCache postTag = new LastPublishedPostCache(
						c.getLong(0),
						c.getString(1),
						c.getString(2),
						c.getLong(3),
						c.getLong(4),
						c.getString(5));
				list.add(postTag);
			}
		} finally {
			c.close();
		}
		return list;
	}	

	private Map<String, LastPublishedPostCache> cursorToPostTagMap(Cursor c) {
		HashMap<String, LastPublishedPostCache> map = new HashMap<String, LastPublishedPostCache>();
		try {
			while (c.moveToNext()) {
				LastPublishedPostCache postTag = new LastPublishedPostCache(
						c.getLong(0),
						c.getString(1),
						c.getString(2),
						c.getLong(3),
						c.getLong(4),
						c.getString(5));
				map.put(postTag.getTag(), postTag);
			}
		} finally {
			c.close();
		}
		return map;
	}

	public SQLiteOpenHelper getDbHelper() {
		return dbHelper;
	}	
}
