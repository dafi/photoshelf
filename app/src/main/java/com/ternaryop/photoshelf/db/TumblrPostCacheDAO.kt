@file:Suppress("MaxLineLength")
package com.ternaryop.photoshelf.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.ternaryop.tumblr.TumblrPost
import java.io.IOException
import java.util.Locale

@Suppress("TooManyFunctions", "ObjectPropertyNaming")
class TumblrPostCacheDAO internal constructor(dbHelper: SQLiteOpenHelper) : AbsDAO<TumblrPostCache>(dbHelper), BaseColumns {
    override val tableName: String
        get() = TABLE_NAME

    override fun onCreate(db: SQLiteDatabase) {
        val sql = "CREATE TABLE tumblr_post_cache(" +
                "_id                    TEXT NOT NULL," +
                "blog_name              TEXT NOT NULL," +
                "cache_type             INTEGER NOT NULL," +
                "post_timestamp         INT UNSIGNED NOT NULL," +
                "post_object            BLOB NOT NULL," +
                "PRIMARY KEY(_id))"
        db.execSQL(sql)
        db.execSQL(String.format("CREATE INDEX %1\$s_%2\$s_%3\$s_IDX ON %1\$s(%2\$s, %3\$s);", TABLE_NAME, BLOG_NAME, CACHE_TYPE))
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (newVersion == 2) {
            onCreate(db)
        }
    }

    override fun getContentValues(pojo: TumblrPostCache): ContentValues {
        val v = ContentValues()

        try {
            v.put(BaseColumns._ID, pojo.id)
            v.put(BLOG_NAME, pojo.blogName)
            v.put(CACHE_TYPE, pojo.cacheType)
            v.put(TIMESTAMP, pojo.timestamp)
            v.put(POST, AbsDAO.toBlob(pojo.post!!))
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        return v
    }

    fun deleteItem(item: TumblrPost): Int = dbHelper.writableDatabase.delete(TABLE_NAME, BaseColumns._ID + "=? and " + BLOG_NAME + "=?", arrayOf(item.reblogKey, item.blogName))

    fun insertItem(post: TumblrPost, cacheType: Int): Long {
        return insert(TumblrPostCache(post.reblogKey, post, cacheType))
    }

    fun updateItem(post: TumblrPost, cacheType: Int): Boolean {
        val cache = TumblrPostCache(post.reblogKey, post, cacheType)

        val v = getContentValues(cache)

        return dbHelper.writableDatabase.update(TABLE_NAME, v, BaseColumns._ID + "=?", arrayOf(cache.id)) == 1
    }

    fun clearCache(cacheType: Int) {
        dbHelper.writableDatabase.delete(TABLE_NAME, "$CACHE_TYPE=?", arrayOf(cacheType.toString()))
    }

    fun read(blogName: String, cacheType: Int): List<TumblrPost> {
        val db = dbHelper.readableDatabase
        val sqlQuery = ("SELECT " + POST
                + " FROM " + TABLE_NAME
                + " WHERE lower(" + BLOG_NAME + ") = ?"
                + " AND " + CACHE_TYPE + " =?")

        val list = mutableListOf<TumblrPost>()
        try {
            db.rawQuery(sqlQuery, arrayOf(blogName.toLowerCase(Locale.US), cacheType.toString())).use { c ->
                while (c.moveToNext()) {
                    list.add(AbsDAO.fromBlob(c.getBlob(0)) as TumblrPost)
                }
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        return list
    }

    fun write(posts: Collection<TumblrPost>, cacheType: Int) {
        for (post in posts) {
            insert(TumblrPostCache(post.reblogKey, post, cacheType))
        }
    }

    fun delete(posts: List<TumblrPost>, cacheType: Int) {
        if (posts.isEmpty()) {
            return
        }
        delete(getIds(posts), cacheType, posts[0].blogName)
    }

    fun delete(postIds: Collection<String>, cacheType: Int, blogName: String) {
        if (postIds.isEmpty()) {
            return
        }
        val whereClause = BaseColumns._ID + " in (" + inClauseParameters(postIds.size) + ") and " + BLOG_NAME + "=? and " + CACHE_TYPE + "=?"
        val args = arrayOfNulls<String>(postIds.size + 2)

        var i = 0
        for (id in postIds) {
            args[i++] = id
        }
        args[args.size - 2] = blogName
        args[args.size - 1] = cacheType.toString()

        dbHelper.writableDatabase.delete(TABLE_NAME, whereClause, args)
    }

    fun findMostRecentTimestamp(blogName: String, cacheType: Int): Long {
        val db = dbHelper.readableDatabase
        val sqlQuery = ("SELECT max(" + TIMESTAMP + ")"
                + " FROM " + TABLE_NAME
                + " WHERE lower(" + BLOG_NAME + ") = ?"
                + " AND " + CACHE_TYPE + " =?")

        db.rawQuery(sqlQuery, arrayOf(blogName.toLowerCase(Locale.US), cacheType.toString())).use { c ->
            if (c.moveToNext()) {
                return c.getLong(0)
            }
        }
        return 0
    }

    private fun getIds(posts: Collection<TumblrPost>): Collection<String> {
        val ids = HashSet<String>()
        for (post in posts) {
            ids.add(post.reblogKey)
        }
        return ids
    }

    companion object {
        const val TABLE_NAME = "tumblr_post_cache"

        const val BLOG_NAME = "blog_name"
        const val CACHE_TYPE = "cache_type"
        const val TIMESTAMP = "post_timestamp"
        const val POST = "post_object"
    }
}