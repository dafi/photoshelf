package com.ternaryop.phototumblrshare.db;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PostTagDatabaseHelper extends SQLiteOpenHelper {
	private static final String DB_NAME = "phototumblrshare.db";
 	private static final int SCHEMA_VERSION = 1;

 	private static PostTagDatabaseHelper instance = null;
 	 
	public static PostTagDatabaseHelper getInstance(Context context) {

		// Use the application context, which will ensure that you
		// don't accidentally leak an Activity's context.
		// See this article for more information: http://bit.ly/6LRzfx
		if (instance == null) {
			instance = new PostTagDatabaseHelper(
					context.getApplicationContext());
		}
		return instance;
	}

	/**
	 * Constructor should be private to prevent direct instantiation. make call
	 * to static factory method "getInstance()" instead.
	 */
	private PostTagDatabaseHelper(Context context) {
		super(context, DB_NAME, null, SCHEMA_VERSION);
	}
 	
	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "CREATE TABLE {0} ("
				+ "{1} INTEGER NOT NULL,"
				+ "{2} TEXT NOT NULL," 
				+ "{3} TEXT NOT NULL," 
				+ "{4} INTEGER NOT NULL,"
				+ "{5} INTEGER NOT NULL,"
				+ "{6} TEXT NOT NULL," 
				+ "PRIMARY KEY ({1}, {3}))";
			db.execSQL(MessageFormat.format(sql,
					PostTag.TABLE_NAME,
					PostTag.POST_ID,
					PostTag.TUMBLR_NAME,
					PostTag.TAG,
					PostTag.PUBLISH_TIMESTAMP,
					PostTag.SHOW_ORDER,
					PostTag.POST_ID_TYPE));
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + PostTag.TABLE_NAME);
        onCreate(db);
    }

	public long insert(PostTag postTag) {
		SQLiteDatabase db = getWritableDatabase();
		long rows = db.insert(PostTag.TABLE_NAME, null, postTag.getContentValues());
		
		return rows;
	}

	public long insertOrIgnore(PostTag postTag) {
		SQLiteDatabase db = getWritableDatabase();
		return db.insertWithOnConflict(
				PostTag.TABLE_NAME,
				null,
				postTag.getContentValues(),
				SQLiteDatabase.CONFLICT_IGNORE);
	}

	public void removeAll() {
		SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from " + PostTag.TABLE_NAME);
	}

	public int removeExpiredScheduledPosts(long expireTime) {
		SQLiteDatabase db = getWritableDatabase();
        String whereClause = MessageFormat.format("{0} = ? and {1} < ?",
        		PostTag.POST_ID_TYPE,
        		PostTag.PUBLISH_TIMESTAMP);
        return db.delete(PostTag.TABLE_NAME,
        		whereClause,
				new String[] { PostTag.POST_TYPE_SCHEDULED, "" + expireTime });
	}

	public PostTag getPostByTag(String tag, String tumblrName) {
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(
				PostTag.TABLE_NAME,
				PostTag.COLUMNS, 
				PostTag.TAG + " = ? and " + PostTag.TUMBLR_NAME + " = ?",
				new String[] { tag, tumblrName },
			null, 
			null, 
			null);

		ArrayList<PostTag> list = cursorToPostTagList(c);
		PostTag postTag = list.isEmpty() ? null : list.get(0);
		
		return postTag;
	}

	public Map<String, PostTag> getPostByTags(List<String> tags, String tumblrName) {
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
		
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(
				PostTag.TABLE_NAME,
				PostTag.COLUMNS, 
				PostTag.TAG + " in (" + inClause + ") and (" + PostTag.TUMBLR_NAME + " = ?)",
				args,
			null, 
			null, 
			null);

		Map<String, PostTag> map = cursorToPostTagMap(c);
		
		return map;
	}
	
	private ArrayList<PostTag> cursorToPostTagList(Cursor c) {
		ArrayList<PostTag> list = new ArrayList<PostTag>();
		try {
			while (c.moveToNext()) {
				PostTag postTag = new PostTag(
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

	private Map<String, PostTag> cursorToPostTagMap(Cursor c) {
		HashMap<String, PostTag> map = new HashMap<String, PostTag>();
		try {
			while (c.moveToNext()) {
				PostTag postTag = new PostTag(
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
}
