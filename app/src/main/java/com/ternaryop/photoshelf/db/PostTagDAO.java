package com.ternaryop.photoshelf.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.text.TextUtils;

public class PostTagDAO extends AbsDAO<PostTag> implements BaseColumns {
    public static final String TABLE_NAME = "vw_post_tag";
    
    public static final String TAG = "tag";
    public static final String TUMBLR_NAME = "tumblr_name";
    public static final String PUBLISH_TIMESTAMP = "publish_timestamp";
    public static final String SHOW_ORDER = "show_order";
    public static final String TAG_ID = "tag_id";
    public static final String BLOG_ID = "blog_id";

    public static final String[] COLUMNS = new String[] { _ID, TUMBLR_NAME, TAG, PUBLISH_TIMESTAMP, SHOW_ORDER };
    public static final String POST_COUNT_COLUMN = "post_count";
    public static final String UNIQUE_TAGS_COUNT_COLUMN = "unique_tags_count";
    public static final String UNIQUE_FIRST_TAG_COUNT_COLUMN = "unique_first_tag_count";
    public static final String MISSING_BIRTHDAYS_COUNT_COLUMN = "missing_birthdays_count";
    public static final String BIRTHDAYS_COUNT_COLUMN = "birthdays_count";
    public static final String RECORD_COUNT_COLUMN = "record_count";

    /**
     * Constructor is accessible only from package
     */
    PostTagDAO(SQLiteOpenHelper dbHelper) {
        super(dbHelper);
    }

    protected void onCreate(SQLiteDatabase db) {
        db.execSQL("create view vw_post_tag as" +
                " select p._id, t.name tag, b.name tumblr_name, tag_id, blog_id, publish_timestamp, show_order" +
                " from blog b, tag t, post p" +
                " where p.blog_id=b._id and p.tag_id=t._id");
    }

    protected void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP VIEW IF EXISTS " + TABLE_NAME);
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

    public Cursor getCursorByTag(String tag, String tumblrName) {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();

        String sqlQuery = "SELECT %2$s, %3$s, count(*) as " + POST_COUNT_COLUMN + " FROM %1$s"
                + " WHERE %3$s LIKE ? AND %4$s=?"
                + " GROUP BY %3$s ORDER BY %3$s";
        sqlQuery = String.format(sqlQuery,
                TABLE_NAME,
                _ID,
                TAG,
                TUMBLR_NAME);
         return db.rawQuery(sqlQuery, new String[] {"%" + tag + "%", tumblrName});
    }

    public PostTag getRandomPostByTag(String tag, String tumblrName) {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        String sqlQuery = "SELECT *"
                            + " FROM " + TABLE_NAME
                            + " WHERE lower(" + TAG +") = lower(?)"
                            + " and " + TUMBLR_NAME + " = ?"
                            + " and " + SHOW_ORDER + " = 1"
                            + " ORDER BY RANDOM() LIMIT 1";

        try (Cursor c = db.rawQuery(sqlQuery, new String[]{tag, tumblrName})) {
            if (c.moveToNext()) {
                return new PostTag(
                        c.getLong(c.getColumnIndex(_ID)),
                        tumblrName,
                        c.getString(c.getColumnIndex(TAG)),
                        c.getLong(c.getColumnIndex(PUBLISH_TIMESTAMP)),
                        c.getLong(c.getColumnIndex(SHOW_ORDER))
                );
            }
        }
        return null;
    }
    
    public Map<String, Long> getMapTagLastPublishedTime(List<String> tags, String tumblrName) {

        HashMap<String, Long> map = new HashMap<>();
        try (Cursor c = getCursorLastPublishedTime(tags, tumblrName, new String[]{TAG, PUBLISH_TIMESTAMP})) {
            while (c.moveToNext()) {
                map.put(c.getString(c.getColumnIndex(TAG)).toLowerCase(Locale.US),
                        c.getLong(c.getColumnIndex(PUBLISH_TIMESTAMP)));
            }
        }
        return map;
    }

    public List<PostTag> getListTagsLastPublishedTime(List<String> tags, String tumblrName) {
        return cursorToList(getCursorLastPublishedTime(tags, tumblrName, COLUMNS));
    }

    private Cursor getCursorLastPublishedTime(List<String> tags, String tumblrName, String[] selectArgs) {
        // contains tumblrName, too
        String args[] = new String[tags.size() + 1];
        args[0] = tumblrName;
        // make lowecase to match using ignorecase
        for (int i = 0; i < tags.size(); i++) {
            args[i + 1] = tags.get(i).toLowerCase(Locale.US);
        }
        
        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < tags.size(); i++) {
            inClause.append("?");
            if (i < (tags.size() - 1)) {
                inClause.append(",");
            }
        }
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        
        return db.rawQuery("SELECT " + TextUtils.join(",", selectArgs) 
                + " FROM vw_post_tag AS t"
                + " WHERE t.tumblr_name = ?"
                + " AND lower(t.tag) IN (" + inClause + ")"
                + " AND publish_timestamp = "
                + " (SELECT MAX(publish_timestamp)"
                + "   FROM vw_post_tag p"
                + "  WHERE p.tag = t.tag )",
                args);
    }

    private List<PostTag> cursorToList(Cursor c) {
        ArrayList<PostTag> list = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                PostTag postTag = new PostTag(
                        c.getLong(c.getColumnIndex(_ID)),
                        c.getString(c.getColumnIndex(TUMBLR_NAME)),
                        c.getString(c.getColumnIndex(TAG)),
                        c.getLong(c.getColumnIndex(PUBLISH_TIMESTAMP)),
                        c.getLong(c.getColumnIndex(SHOW_ORDER)));
                list.add(postTag);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
        return list;
    }   
    
    public PostTag findLastPublishedPost(String tumblrName) {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();

        String sqlQuery = "SELECT " + TextUtils.join(",", COLUMNS) + ", max(" + PUBLISH_TIMESTAMP + ")"
                + " FROM " + TABLE_NAME + " WHERE " + TUMBLR_NAME + "=?";

        PostTag postTag = null;
        try (Cursor c = db.rawQuery(sqlQuery, new String[]{tumblrName})) {
            // be sure at least one record is returned (table could be empty)
            if (c.moveToNext() && !c.isNull(c.getColumnIndex(_ID))) {
                postTag = new PostTag(
                        c.getLong(c.getColumnIndex(_ID)),
                        c.getString(c.getColumnIndex(TUMBLR_NAME)),
                        c.getString(c.getColumnIndex(TAG)),
                        c.getLong(c.getColumnIndex(PUBLISH_TIMESTAMP)),
                        c.getLong(c.getColumnIndex(SHOW_ORDER))
                );
            }
        }    
        return postTag;
    }

    public Map<String, Long> getStatisticCounts(String tumblrName) {
        if (tumblrName == null) {
            // force to return all counter set to zero
            tumblrName = "";
        }
        SQLiteDatabase db = getDbHelper().getReadableDatabase();

        String sqlQuery = "select" +
                "(SELECT count(distinct(_id)) from vw_post_tag where tumblr_name=?) " + POST_COUNT_COLUMN + "," +
                "(SELECT count(*) FROM vw_post_tag where tumblr_name=?) " + RECORD_COUNT_COLUMN + "," +
                "(SELECT count(distinct(tag)) FROM vw_post_tag where tumblr_name=?) " + UNIQUE_TAGS_COUNT_COLUMN + "," +
                "(SELECT count(distinct(tag)) FROM vw_post_tag where tumblr_name=? and show_order=1) " + UNIQUE_FIRST_TAG_COUNT_COLUMN + "," +
                "(SELECT count(*) FROM birthday where tumblr_name=?) " + BIRTHDAYS_COUNT_COLUMN + "," +
                "(SELECT count(*) FROM VW_MISSING_BIRTHDAYS where tumblr_name=?) " + MISSING_BIRTHDAYS_COUNT_COLUMN;

        HashMap<String, Long> map = new HashMap<>();
        try (Cursor c = db.rawQuery(sqlQuery, new String[]{tumblrName, tumblrName, tumblrName, tumblrName, tumblrName, tumblrName})) {
            if (c.moveToNext()) {
                for (int i = 0; i < c.getColumnCount(); i++) {
                    map.put(c.getColumnName(i), c.getLong(i));
                }
            }
        }
        return map;
    }

    public long getPostCountByTag(String tag, String tumblrName) {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();

        return DatabaseUtils.queryNumEntries(db,
                TABLE_NAME,
                "lower(" + TAG + ") = lower(?)"
                + " and " + TUMBLR_NAME + " = ?",
                new String[]{tag, tumblrName});
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    public Cursor cursorExport() {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        return db.query(
                TABLE_NAME,
                new String[] {_ID, TUMBLR_NAME, TAG, PUBLISH_TIMESTAMP, SHOW_ORDER},
                null,
                null,
                null,
                null,
                _ID + ", " + BLOG_ID + ", " + TAG_ID);
    }
}