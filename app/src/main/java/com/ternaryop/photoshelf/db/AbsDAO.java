package com.ternaryop.photoshelf.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public abstract class AbsDAO<Pojo> {
    private final SQLiteOpenHelper dbHelper;

    public AbsDAO(SQLiteOpenHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    protected abstract void onCreate(SQLiteDatabase db);
    protected abstract void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);
    public abstract ContentValues getContentValues(Pojo pojo);
    public abstract String getTableName();

    public long insert(Pojo pojo) {
        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        return db.insertOrThrow(getTableName(), null, getContentValues(pojo));
    }

    public void removeAll() {
        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        db.delete(getTableName(), null, null);
    }

    public SQLiteOpenHelper getDbHelper() {
        return dbHelper;
    }
}
