package com.ternaryop.phototumblrshare.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public abstract class AbsDAO<Pojo> {
	private SQLiteOpenHelper dbHelper;

	public AbsDAO(SQLiteOpenHelper dbHelper) {
		this.dbHelper = dbHelper;
	}

	protected abstract void onCreate(SQLiteDatabase db);
	protected abstract void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);
	public abstract long insert(Pojo pojo);
	public abstract ContentValues getContentValues(Pojo pojo);

	public void removeAll() {
		
	}

	public SQLiteOpenHelper getDbHelper() {
		return dbHelper;
	}
}
