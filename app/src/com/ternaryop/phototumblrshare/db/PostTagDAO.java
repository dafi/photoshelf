package com.ternaryop.phototumblrshare.db;

import java.text.MessageFormat;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class PostTagDAO implements BaseColumns {
	public static final String TABLE_NAME = "POST_TAG";
	
	public static final String TAG = "TAG";
	public static final String TUMBLR_NAME = "TUMBLR_NAME";
	public static final String PUBLISH_TIMESTAMP = "PUBLISH_TIMESTAMP";
	public static final String SHOW_ORDER = "SHOW_ORDER";
	
	public static final String[] COLUMNS = new String[] { _ID, TUMBLR_NAME, TAG, PUBLISH_TIMESTAMP, SHOW_ORDER };

	private SQLiteOpenHelper dbHelper;

	/**
	 * Constructor is accessible only from package
	 */
	PostTagDAO(SQLiteOpenHelper dbHelper) {
		this.dbHelper = dbHelper;
	}

	void onCreate(SQLiteDatabase db) {
		String sql = "CREATE TABLE {0} ("
				+ "{1} BIGINT UNSIGNED NOT NULL,"
				+ "{2} TEXT NOT NULL,"
				+ "{3} TEXT NOT NULL,"
				+ "{4} INT UNSIGNED NOT NULL,"
				+ "{5} UNSIGNED NOT NULL,"
				+ "PRIMARY KEY ( {1}, {2}));";
		db.execSQL(MessageFormat.format(sql,
				TABLE_NAME,
				_ID,
				TAG,
				TUMBLR_NAME,
				PUBLISH_TIMESTAMP,
				SHOW_ORDER));
	}

	void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

	public ContentValues getContentValues(PostTag postTag) {
		ContentValues v = new ContentValues();

		v.put(_ID,  postTag.getId());
		v.put(TUMBLR_NAME, postTag.getTumblrName());
		v.put(TAG, postTag.getTag());
		v.put(PUBLISH_TIMESTAMP, postTag.getPublishTimestamp());
		v.put(SHOW_ORDER, postTag.getShowOrder());
		
		return v;
	}

	public long insert(PostTag postTag) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rows = db.insertOrThrow(TABLE_NAME, null, getContentValues(postTag));
		
		return rows;
	}

	public void removeAll() {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("delete from " + TABLE_NAME);
	}

	public Cursor getCursorByTag(String tag, String tumblrName) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();

		String sqlQuery = "SELECT %2$s, %3$s, count(*) as post_count FROM %1$s WHERE %3$s LIKE '%%%5$s%%' AND %4$s='%6$s' GROUP BY %3$s ORDER BY %3$s";
		sqlQuery = String.format(sqlQuery,
				TABLE_NAME,
				_ID,
				TAG,
				TUMBLR_NAME,
				tag,
				tumblrName);
		 return db.rawQuery(sqlQuery, null);
	}
	
	public PostTag findLastPublishedPost(String tumblrName) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		String sqlQuery = "SELECT * FROM %1$s WHERE %2$s = '%3$s' ORDER BY %4$s DESC LIMIT 1";
		sqlQuery = String.format(sqlQuery,
				TABLE_NAME,
				TUMBLR_NAME,
				tumblrName,
				PUBLISH_TIMESTAMP);
		
		System.out.println("PostTagDAO.findLastPublishedPost()" + sqlQuery);
		Cursor c = db.rawQuery(sqlQuery, null);
		PostTag postTag = null;
		try {
			if (c.moveToNext()) {
				postTag = new PostTag(
						c.getLong(c.getColumnIndex(_ID)),
						c.getString(c.getColumnIndex(TUMBLR_NAME)),
						c.getString(c.getColumnIndex(TAG)),
						c.getLong(c.getColumnIndex(PUBLISH_TIMESTAMP)),
						c.getLong(c.getColumnIndex(SHOW_ORDER))
						);
			}
		} finally {
			c.close();
		}	
		return postTag;
	}
}