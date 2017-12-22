package com.ternaryop.photoshelf.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import com.ternaryop.tumblr.TumblrPost;

public class TumblrPostCacheDAO extends AbsDAO<TumblrPostCache> implements BaseColumns {
    public static final String TABLE_NAME = "tumblr_post_cache";

    public static final String BLOG_NAME = "blog_name";
    public static final String CACHE_TYPE = "cache_type";
    public static final String TIMESTAMP = "post_timestamp";
    public static final String POST = "post_object";

    public static final String[] COLUMNS = new String[] { _ID, BLOG_NAME, CACHE_TYPE, TIMESTAMP, POST };

    /**
     * Constructor is accessible only from package
     */
    TumblrPostCacheDAO(SQLiteOpenHelper dbHelper) {
        super(dbHelper);
    }

    protected void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE tumblr_post_cache(" +
                "_id                    TEXT NOT NULL," +
                "blog_name              TEXT NOT NULL," +
                "cache_type             INTEGER NOT NULL," +
                "post_timestamp         INT UNSIGNED NOT NULL," +
                "post_object            BLOB NOT NULL," +
                "PRIMARY KEY(_id))";
        db.execSQL(sql);
        db.execSQL(String.format("CREATE INDEX %1$s_%2$s_%3$s_IDX ON %1$s(%2$s, %3$s);", TABLE_NAME, BLOG_NAME, CACHE_TYPE));
    }

    protected void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion == 2) {
            onCreate(db);
        }
    }

    public ContentValues getContentValues(TumblrPostCache post) {
        ContentValues v = new ContentValues();

        try {
            v.put(_ID,  post.getId());
            v.put(BLOG_NAME, post.getBlogName());
            v.put(CACHE_TYPE, post.getCacheType());
            v.put(TIMESTAMP, post.getTimestamp());
            v.put(POST, toBlob(post.getPost()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return v;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    public int deleteItem(TumblrPost item) {
        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        return db.delete(TABLE_NAME, _ID + "=? and " + BLOG_NAME + "=?", new String[] {item.getReblogKey(), item.getBlogName()});
    }

    public boolean updateItem(TumblrPost post, int cacheType) {
        final TumblrPostCache cache = new TumblrPostCache(post.getReblogKey(), post, cacheType);

        ContentValues v = getContentValues(cache);

        return getDbHelper().getWritableDatabase().update(TABLE_NAME, v, _ID + "=?", new String[] {cache.getId()}) == 1;
    }

    public void clearCache(int cacheType) {
        getDbHelper().getWritableDatabase().delete(TABLE_NAME, CACHE_TYPE + "=?", new String[] {String.valueOf(cacheType)});
    }

    public List<TumblrPost> read(String blogName, int cacheType) {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        String sqlQuery = "SELECT " + POST
                + " FROM " + TABLE_NAME
                + " WHERE lower(" + BLOG_NAME +") = ?"
                + " AND " + CACHE_TYPE + " =?";

        ArrayList<TumblrPost> list = new ArrayList<>();
        try (Cursor c = db.rawQuery(sqlQuery, new String[]{blogName.toLowerCase(Locale.US), String.valueOf(cacheType)})) {
            while (c.moveToNext()) {
                list.add((TumblrPost) fromBlob(c.getBlob(0)));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public void write(Collection<TumblrPost> posts, int cacheType) {
        for (TumblrPost post : posts) {
            insert(new TumblrPostCache(post.getReblogKey(), post, cacheType));
        }
    }

    public void delete(List<TumblrPost> posts, int cacheType) {
        if (posts.isEmpty()) {
            return;
        }
        final Collection<String> postIds = getIds(posts);

        String whereClause = _ID + " in (" + inClauseParameters(postIds.size()) + ") and " + BLOG_NAME + "=? and " + CACHE_TYPE + "=?";
        String[] args = new String[postIds.size() + 2];
        postIds.toArray(args);
        args[args.length - 2] = posts.get(0).getBlogName();
        args[args.length - 1] = String.valueOf(cacheType);

        getDbHelper().getWritableDatabase().delete(TABLE_NAME, whereClause, args);
    }

    public long findMostRecentTimestamp(String blogName, int cacheType) {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        String sqlQuery = "SELECT max(" + TIMESTAMP + ")"
                + " FROM " + TABLE_NAME
                + " WHERE lower(" + BLOG_NAME +") = ?"
                + " AND " + CACHE_TYPE + " =?";

        try (Cursor c = db.rawQuery(sqlQuery, new String[]{blogName.toLowerCase(Locale.US), String.valueOf(cacheType)})) {
            if (c.moveToNext()) {
                return c.getLong(0);
            }
        }
        return 0;
    }

    @NonNull
    private Collection<String> getIds(Collection<TumblrPost> posts) {
        HashSet<String> ids = new HashSet<>();
        for (TumblrPost post : posts) {
            ids.add(post.getReblogKey());
        }
        return ids;
    }
}