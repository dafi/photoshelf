package com.ternaryop.photoshelf.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.provider.BaseColumns;
import android.text.TextUtils;

public class PostDAO extends AbsDAO<Post> implements BaseColumns {
    public static final String TABLE_NAME = "post";

    public static final String TAG_ID = "tag_id";
    public static final String BLOG_ID = "blog_id";
    public static final String PUBLISH_TIMESTAMP = "publish_timestamp";
    public static final String SHOW_ORDER = "show_order";

    public static final String[] COLUMNS = new String[] { _ID, BLOG_ID, TAG_ID, PUBLISH_TIMESTAMP, SHOW_ORDER };

    /**
     * Constructor is accessible only from package
     */
    PostDAO(SQLiteOpenHelper dbHelper) {
        super(dbHelper);
    }

    protected void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE post(" +
                "_id                 BIGINT UNSIGNED NOT NULL," +
                "tag_id              INTEGER NOT NULL," +
                "blog_id             INTEGER NOT NULL," +
                "publish_timestamp   INT UNSIGNED NOT NULL," +
                "show_order          UNSIGNED NOT NULL," +
                "PRIMARY KEY(_id, tag_id)," +
                "FOREIGN KEY(tag_id) REFERENCES tag(_id)" +
                "FOREIGN KEY(blog_id) REFERENCES blog(_id));";
        db.execSQL(sql);
        // lollipop warns about index problems so add it
        db.execSQL(String.format("CREATE INDEX %1$s_%2$s_IDX ON %1$s(%2$s)", TABLE_NAME, TAG_ID));
        // speedup getCursorLastPublishedTime
        db.execSQL(String.format("CREATE INDEX %1$s_%2$s_%3$s_IDX ON %1$s(%2$s, %3$s);", TABLE_NAME, TAG_ID, PUBLISH_TIMESTAMP));
    }

    protected void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public ContentValues getContentValues(Post post) {
        ContentValues v = new ContentValues();

        v.put(_ID,  post.getId());
        v.put(BLOG_ID, post.getBlogId());
        v.put(TAG_ID, post.getTagId());
        v.put(PUBLISH_TIMESTAMP, post.getPublishTimestamp());
        v.put(SHOW_ORDER, post.getShowOrder());
        
        return v;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    public void update(Map<String, String> newValues, Context context) {
        String id = newValues.get("id");
        String tumblrName = newValues.get("tumblrName");
        String tags = newValues.get("tags");

        if (id == null) {
            throw new IllegalArgumentException("Post id is mandatory for update");
        }
        if (tumblrName == null || tumblrName.isEmpty()) {
            throw new IllegalArgumentException("Tumblr name is mandatory for update");
        }
        if (tags == null || tags.isEmpty()) {
            throw new IllegalArgumentException("Tag is mandatory for update");
        }

        long longId = Long.parseLong(id);
        List<Post> posts = getPostsById(longId, tumblrName);
        if (posts.isEmpty()) {
            return;
        }

        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        try {
            db.beginTransaction();
            deleteById(longId);
            // insert using existing postTag so we can modify only some fields leaving the others with previous values
            PostTag postTag = new PostTag(posts.get(0));
            int showOrder = 1;
            BulkImportPostDAOWrapper bulkImportPostDAOWrapper = DBHelper.getInstance(context).getBulkImportPostDAOWrapper();
            SQLiteStatement insertStatement = bulkImportPostDAOWrapper.getCompiledInsertStatement(db);
            for (String tagString : tags.split(",")) {
                String trimmedTag = tagString.trim();
                if (!trimmedTag.isEmpty()) {
                    postTag.setTag(trimmedTag);
                    postTag.setShowOrder(showOrder++);
                    postTag.setTumblrName(tumblrName);
                    bulkImportPostDAOWrapper.insert(insertStatement, postTag);
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    public List<Post> getPostsById(long id, String blogName) {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();

        String sqlQuery = "select p." + TextUtils.join(",p.", COLUMNS)
                + " from " + TABLE_NAME + " p, " + BlogDAO.TABLE_NAME + " b"
                + " where p." + _ID + " = ? and p." + BLOG_ID + "=b." + BlogDAO._ID + " and b." + BlogDAO.NAME + "=?";

        return cursorToList(db.rawQuery(sqlQuery, new String[] {String.valueOf(id), blogName}));
    }

    private List<Post> cursorToList(Cursor c) {
        ArrayList<Post> list = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                Post post = new Post(
                        c.getLong(c.getColumnIndex(_ID)),
                        c.getLong(c.getColumnIndex(BLOG_ID)),
                        c.getLong(c.getColumnIndex(TAG_ID)),
                        c.getLong(c.getColumnIndex(PUBLISH_TIMESTAMP)),
                        c.getLong(c.getColumnIndex(SHOW_ORDER)));
                list.add(post);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
        return list;
    }

    public int deleteById(long id) {
        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        return db.delete(TABLE_NAME, _ID + "=?", new String[] {String.valueOf(id)});
    }
}