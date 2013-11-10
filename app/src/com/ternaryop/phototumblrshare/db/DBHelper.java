package com.ternaryop.phototumblrshare.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
	public static final String DB_NAME = "phototumblrshare.db";
 	private static final int SCHEMA_VERSION = 4;

 	private static DBHelper instance = null;

 	private LastPublishedPostCacheDAO lastPublishedPostCacheDAO;
 	private PostTagDAO postTagDAO;
 	private BirthdayDAO birthdayDAO;
 	 
	public static DBHelper getInstance(Context context) {

		// Use the application context, which will ensure that you
		// don't accidentally leak an Activity's context.
		// See this article for more information: http://bit.ly/6LRzfx
		if (instance == null) {
			instance = new DBHelper(
					context.getApplicationContext());
		}
		return instance;
	}

	/**
	 * Constructor should be private to prevent direct instantiation. make call
	 * to static factory method "getInstance()" instead.
	 */
	private DBHelper(Context context) {
		super(context, DB_NAME, null, SCHEMA_VERSION);
		lastPublishedPostCacheDAO = new LastPublishedPostCacheDAO(this);
		postTagDAO = new PostTagDAO(this);
		birthdayDAO = new BirthdayDAO(this);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		lastPublishedPostCacheDAO.onCreate(db);
		postTagDAO.onCreate(db);
		birthdayDAO.onCreate(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		lastPublishedPostCacheDAO.onUpgrade(db, oldVersion, newVersion);
		postTagDAO.onUpgrade(db, oldVersion, newVersion);
		birthdayDAO.onUpgrade(db, oldVersion, newVersion);
    }

	public LastPublishedPostCacheDAO getLastPublishedPostCacheDAO() {
		return lastPublishedPostCacheDAO;
	}
	
	public PostTagDAO getPostTagDAO() {
		return postTagDAO;
	}
	
	public BirthdayDAO getBirthdayDAO() {
		return birthdayDAO;
	}
}