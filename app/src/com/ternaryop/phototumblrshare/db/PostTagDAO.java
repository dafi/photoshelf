package com.ternaryop.phototumblrshare.db;

import java.text.MessageFormat;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PostTagDAO {

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
				PostTag.TABLE_NAME,
				PostTag._ID,
				PostTag.TAG,
				PostTag.TUMBLR_NAME,
				PostTag.PUBLISH_TIMESTAMP,
				PostTag.SHOW_ORDER));
	}

	void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + PostTag.TABLE_NAME);
        onCreate(db);
    }

	public long insert(PostTag postTag) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rows = db.insertOrThrow(PostTag.TABLE_NAME, null, postTag.getContentValues());
		
		return rows;
	}

	public void removeAll() {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("delete from " + PostTag.TABLE_NAME);
	}

	public Cursor getCursorByTag(String tag, String tumblrName) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();

		String sqlQuery = "SELECT %2$s, %3$s, count(*) as post_count FROM %1$s WHERE %3$s LIKE '%%%5$s%%' AND %4$s='%6$s' GROUP BY %3$s ORDER BY %3$s";
		sqlQuery = String.format(sqlQuery,
				PostTag.TABLE_NAME,
				PostTag._ID,
				PostTag.TAG,
				PostTag.TUMBLR_NAME,
				tag,
				tumblrName);
		 return db.rawQuery(sqlQuery, null);
	}
	
	public PostTag findLastPublishedPost(String tumblrName) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		String sqlQuery = "SELECT * FROM %1$s WHERE %2$s = '%3$s' ORDER BY %4$s DESC LIMIT 1";
		sqlQuery = String.format(sqlQuery,
				PostTag.TABLE_NAME,
				PostTag.TUMBLR_NAME,
				tumblrName,
				PostTag.PUBLISH_TIMESTAMP);
		
		System.out.println("PostTagDAO.findLastPublishedPost()" + sqlQuery);
		Cursor c = db.rawQuery(sqlQuery, null);
		PostTag postTag = null;
		try {
			if (c.moveToNext()) {
				postTag = new PostTag(
						c.getLong(c.getColumnIndex(PostTag._ID)),
						c.getString(c.getColumnIndex(PostTag.TUMBLR_NAME)),
						c.getString(c.getColumnIndex(PostTag.TAG)),
						c.getLong(c.getColumnIndex(PostTag.PUBLISH_TIMESTAMP)),
						c.getLong(c.getColumnIndex(PostTag.SHOW_ORDER))
						);
			}
		} finally {
			c.close();
		}	
		return postTag;
	}
}