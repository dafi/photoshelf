package com.ternaryop.photoshelf.db;

import java.util.Locale;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.Nullable;

/**
 * Created by dave on 07/01/18.
 * Searching into table Tag using functions (lower(), replace()) is very slow so we use a virtual table with FTS
 */

public class TagMatcherDAO extends AbsDAO<Tag> {
    public static final String MATCH_TAG = "match_tag";
    public static final String STRIPPED_TAG = "stripped_tag";
    public static final String TABLE_NAME = "tag_matcher";

    public TagMatcherDAO(DBHelper dbHelper) {
        super(dbHelper);
    }

    protected void onCreate(SQLiteDatabase db) {
        String sql = "CREATE VIRTUAL TABLE %1$s USING fts4(%2$s, %3$s)";
        db.execSQL(String.format(sql,
                TABLE_NAME,
                MATCH_TAG,
                STRIPPED_TAG));
    }

    protected void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            onNewVersion3(db);
        }
    }

    private void onNewVersion3(SQLiteDatabase db) {
        onCreate(db);
        String sql = "INSERT INTO %1$s (%2$s, %3$s) SELECT name, lower(replace(replace(replace(replace(name, '-', ''), '''', ''), ' ', ''), '.', '')) FROM tag";
        // populate the table with existing value into table Tag
        db.execSQL(String.format(sql,
                TABLE_NAME,
                MATCH_TAG,
                STRIPPED_TAG));
    }

    @Override
    public ContentValues getContentValues(Tag tag) {
        return null;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Nullable
    public String getMatchingTag(SQLiteStatement  stmt, String tag) {
        stmt.bindString(1, tag);

        try {
            return stmt.simpleQueryForString();
        } catch (SQLiteDoneException ex) {
            // zero rows
            return null;
        }
    }

    @Nullable
    public String getMatchingTag(String tag) {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        return getMatchingTag(getSelectTagMatcherStatement(db), cleanTag(tag));
    }

    public long insert(SQLiteStatement stmt, String tag) {
        int index = 0;
        stmt.bindString(++index, tag);
        stmt.bindString(++index, cleanTag(tag));

        return stmt.executeInsert();
    }

    public static String cleanTag(String tag) {
        return tag.replaceAll("[-' .]", "").toLowerCase(Locale.US);
    }

    public static SQLiteStatement getSelectTagMatcherStatement(SQLiteDatabase db) {
        return db.compileStatement(String.format("SELECT %2$s FROM %1$s WHERE %3$s match ?",
                TABLE_NAME,
                MATCH_TAG,
                STRIPPED_TAG));
    }

    public static SQLiteStatement getInsertTagMatcherStatement(SQLiteDatabase db) {
        return db.compileStatement(String.format("insert into %1$s (%2$s, %3$s) values (?, ?)",
                TABLE_NAME,
                MATCH_TAG,
                STRIPPED_TAG));
    }
}
