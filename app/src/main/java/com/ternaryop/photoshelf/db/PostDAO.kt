@file:Suppress("MaxLineLength")
package com.ternaryop.photoshelf.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.provider.BaseColumns._ID
import android.text.TextUtils
import com.ternaryop.tumblr.TumblrPost

/**
 * Constructor is accessible only from package
 */
@Suppress("TooManyFunctions", "ObjectPropertyNaming")
class PostDAO internal constructor(dbHelper: SQLiteOpenHelper) : AbsDAO<Post>(dbHelper), BaseColumns {

    override val tableName: String
        get() = TABLE_NAME

    override fun onCreate(db: SQLiteDatabase) {
        val sql = "CREATE TABLE post(" +
                "_id                 BIGINT UNSIGNED NOT NULL," +
                "tag_id              INTEGER NOT NULL," +
                "blog_id             INTEGER NOT NULL," +
                "publish_timestamp   INT UNSIGNED NOT NULL," +
                "show_order          UNSIGNED NOT NULL," +
                "PRIMARY KEY(_id, tag_id)," +
                "FOREIGN KEY(tag_id) REFERENCES tag(_id)" +
                "FOREIGN KEY(blog_id) REFERENCES blog(_id));"
        db.execSQL(sql)
        // lollipop warns about index problems so add it
        db.execSQL(String.format("CREATE INDEX %1\$s_%2\$s_IDX ON %1\$s(%2\$s)", TABLE_NAME, TAG_ID))
        // speedup getCursorLastPublishedTime
        db.execSQL(String.format("CREATE INDEX %1\$s_%2\$s_%3\$s_IDX ON %1\$s(%2\$s, %3\$s);", TABLE_NAME, TAG_ID, PUBLISH_TIMESTAMP))
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    override fun getContentValues(pojo: Post): ContentValues {
        val v = ContentValues()

        v.put(BaseColumns._ID, pojo.id)
        v.put(BLOG_ID, pojo.blogId)
        v.put(TAG_ID, pojo.tagId)
        v.put(PUBLISH_TIMESTAMP, pojo.publishTimestamp)
        v.put(SHOW_ORDER, pojo.showOrder)

        return v
    }

    fun update(context: Context, newValues: Map<String, String>) {
        val id = newValues["id"]
        val tumblrName = newValues["tumblrName"]
        val tags = newValues["tags"]

        if (id == null) {
            throw IllegalArgumentException("Post id is mandatory for update")
        }
        if (tumblrName == null || tumblrName.isEmpty()) {
            throw IllegalArgumentException("Tumblr name is mandatory for update")
        }
        if (tags == null || tags.isEmpty()) {
            throw IllegalArgumentException("Tag is mandatory for update")
        }

        val longId = java.lang.Long.parseLong(id)
        val posts = getPostsById(longId, tumblrName)
        if (posts.isEmpty()) {
            return
        }

        val db = dbHelper.writableDatabase
        try {
            db.beginTransaction()
            deleteById(longId)
            // insert using existing postTag so we can modify only some fields leaving the others with previous values
            val postTag = PostTag(posts[0])
            val bulkImportPostDAOWrapper = DBHelper.getInstance(context).bulkImportPostDAOWrapper
            val insertStatement = bulkImportPostDAOWrapper.getCompiledInsertStatement(db)
            TumblrPost.tagsFromString(tags).forEachIndexed { index: Int, tag: String ->
                postTag.tag = tag
                postTag.showOrder = index + 1
                postTag.tumblrName = tumblrName
                bulkImportPostDAOWrapper.insert(insertStatement, postTag)
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun getPostsById(id: Long, blogName: String): List<Post> {
        val db = dbHelper.readableDatabase

        val sqlQuery = ("select p." + TextUtils.join(",p.", COLUMNS)
                + " from " + TABLE_NAME + " p, " + BlogDAO.TABLE_NAME + " b"
                + " where p." + _ID + " = ? and p." + BLOG_ID + "=b." + BlogDAO._ID + " and b." + BlogDAO.NAME + "=?")

        return cursorToList(db.rawQuery(sqlQuery, arrayOf(id.toString(), blogName)))
    }

    private fun cursorToList(c: Cursor): List<Post> {
        val list = mutableListOf<Post>()
        try {
            while (c.moveToNext()) {
                val post = Post(
                        c.getLong(c.getColumnIndex(_ID)),
                        c.getLong(c.getColumnIndex(BLOG_ID)),
                        c.getLong(c.getColumnIndex(TAG_ID)),
                        c.getLong(c.getColumnIndex(PUBLISH_TIMESTAMP)),
                        c.getInt(c.getColumnIndex(SHOW_ORDER)))
                list.add(post)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            c.close()
        }
        return list
    }

    fun deleteById(id: Long): Int {
        return dbHelper.writableDatabase.delete(TABLE_NAME, BaseColumns._ID + "=?", arrayOf(id.toString()))
    }

    companion object {
        const val TABLE_NAME = "post"

        const val TAG_ID = "tag_id"
        const val BLOG_ID = "blog_id"
        const val PUBLISH_TIMESTAMP = "publish_timestamp"
        const val SHOW_ORDER = "show_order"

        val COLUMNS = arrayOf(BaseColumns._ID, BLOG_ID, TAG_ID, PUBLISH_TIMESTAMP, SHOW_ORDER)
    }
}