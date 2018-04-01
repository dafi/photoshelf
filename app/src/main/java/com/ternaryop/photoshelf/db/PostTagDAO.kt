@file:Suppress("MaxLineLength")
package com.ternaryop.photoshelf.db

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.text.TextUtils
import java.util.Locale

/**
 * Constructor is accessible only from package
 */
@Suppress("TooManyFunctions", "ObjectPropertyNaming")
class PostTagDAO internal constructor(dbHelper: SQLiteOpenHelper) : AbsDAO<PostTag>(dbHelper), BaseColumns {

    override val tableName: String
        get() = TABLE_NAME

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("create view vw_post_tag as" +
                " select p._id, t.name tag, b.name tumblr_name, tag_id, blog_id, publish_timestamp, show_order" +
                " from blog b, tag t, post p" +
                " where p.blog_id=b._id and p.tag_id=t._id")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    override fun getContentValues(pojo: PostTag): ContentValues {
        val v = ContentValues()

        v.put(BaseColumns._ID, pojo.id)
        v.put(TUMBLR_NAME, pojo.tumblrName)
        v.put(TAG, pojo.tag)
        v.put(PUBLISH_TIMESTAMP, pojo.publishTimestamp)
        v.put(SHOW_ORDER, pojo.showOrder)

        return v
    }

    fun getCursorByTag(tag: String, tumblrName: String): Cursor {
        val db = dbHelper.readableDatabase

        var sqlQuery = ("SELECT %2\$s, %3\$s, count(*) as " + POST_COUNT_COLUMN + " FROM %1\$s"
                + " WHERE %3\$s LIKE ? AND %4\$s=?"
                + " GROUP BY %3\$s ORDER BY %3\$s")
        sqlQuery = String.format(sqlQuery,
                TABLE_NAME,
                BaseColumns._ID,
                TAG,
                TUMBLR_NAME)
        return db.rawQuery(sqlQuery, arrayOf("%$tag%", tumblrName))
    }

    fun getRandomPostByTag(tag: String, tumblrName: String): PostTag? {
        val db = dbHelper.readableDatabase
        val sqlQuery = ("SELECT *"
                + " FROM " + TABLE_NAME
                + " WHERE lower(" + TAG + ") = lower(?)"
                + " and " + TUMBLR_NAME + " = ?"
                + " and " + SHOW_ORDER + " = 1"
                + " ORDER BY RANDOM() LIMIT 1")

        db.rawQuery(sqlQuery, arrayOf(tag, tumblrName)).use { c ->
            if (c.moveToNext()) {
                return postTagFromCursor(c)
            }
        }
        return null
    }

    fun getMapTagLastPublishedTime(tags: Collection<String>, tumblrName: String): Map<String, Long> {
        val map = HashMap<String, Long>()
        getCursorLastPublishedTime(tags, tumblrName, arrayOf(TAG, PUBLISH_TIMESTAMP)).use { c ->
            while (c.moveToNext()) {
                map[c.getString(c.getColumnIndex(TAG)).toLowerCase(Locale.US)] = c.getLong(c.getColumnIndex(PUBLISH_TIMESTAMP))
            }
        }
        return map
    }

    /**
     * @param tags the tags for which to find the publish timestamp
     * @param tumblrName the bloch name
     * @return the pair [last publish timestamp, tag name]
     */
    fun getListPairLastPublishedTimestampTag(tags: Collection<String>, tumblrName: String): List<Pair<Long, String>> {
        if (tags.isEmpty()) {
            return emptyList()
        }

        // the BETWEEN condition uses the index so the query execution is very fast compared to the LIKE expression
        val orClause = StringBuilder()
        for (i in tags.indices) {
            orClause.append(" ? between t.name and t.name || '{'")
            if (i < tags.size - 1) {
                orClause.append(" or")
            }
        }
        val db = dbHelper.readableDatabase

        val sqlQuery = ("SELECT max(p.publish_timestamp), t.name from post p, tag t, blog b"
                + " where t._id=p.tag_id"
                + " and p.blog_id=b._id"
                + " and b.name = ?"
                + " and t._id in ("
                + " select _id from tag t where " + orClause + ")"
                + " group by t.name")

        val list = mutableListOf<Pair<Long, String>>()
        db.rawQuery(sqlQuery, buildArguments(tags, tumblrName)).use { c ->
            while (c.moveToNext()) {
                list.add(Pair(c.getLong(0), c.getString(1)))
            }
        }
        return list
    }

    private fun buildArguments(tags: Collection<String>, tumblrName: String): Array<String> {
        // contains tumblrName, too
        val args = Array(tags.size + 1, { tumblrName })
        var pos = 1
        // make lowercase to match using ignore case
        for (tag in tags) {
            args[pos++] = tag.toLowerCase(Locale.US)
        }
        return args
    }

    private fun getCursorLastPublishedTime(tags: Collection<String>, tumblrName: String, selectArgs: Array<String>): Cursor {
        val db = dbHelper.readableDatabase

        return db.rawQuery("SELECT " + TextUtils.join(",", selectArgs)
                + " FROM vw_post_tag AS t"
                + " WHERE t.tumblr_name = ?"
                + " AND lower(t.tag) IN (" + inClauseParameters(tags.size) + ")"
                + " AND publish_timestamp = "
                + " (SELECT MAX(publish_timestamp)"
                + "   FROM vw_post_tag p"
                + "  WHERE p.tag = t.tag )",
                buildArguments(tags, tumblrName))
    }

    fun findLastPublishedPost(tumblrName: String): PostTag? {
        val db = dbHelper.readableDatabase

        val sqlQuery = ("SELECT " + TextUtils.join(",", COLUMNS) + ", max(" + PUBLISH_TIMESTAMP + ")"
                + " FROM " + TABLE_NAME + " WHERE " + TUMBLR_NAME + "=?")

        var postTag: PostTag? = null
        db.rawQuery(sqlQuery, arrayOf(tumblrName)).use { c ->
            // be sure at least one record is returned (table could be empty)
            if (c.moveToNext() && !c.isNull(c.getColumnIndex(BaseColumns._ID))) {
                postTag = postTagFromCursor(c)
            }
        }
        return postTag
    }

    private fun postTagFromCursor(c: Cursor): PostTag {
        return PostTag(
                c.getLong(c.getColumnIndex(BaseColumns._ID)),
                c.getString(c.getColumnIndex(TUMBLR_NAME)),
                c.getString(c.getColumnIndex(TAG)),
                c.getLong(c.getColumnIndex(PUBLISH_TIMESTAMP)),
                c.getInt(c.getColumnIndex(SHOW_ORDER)))
    }

    fun getStatisticCounts(tumblrName: String?): Map<String, Long> {
        // force to return all counter set to zero
        val filterTumblrName = tumblrName ?: ""
        val db = dbHelper.readableDatabase

        val sqlQuery = "select" +
                "(SELECT count(distinct(_id)) from vw_post_tag where tumblr_name=?) " + POST_COUNT_COLUMN + "," +
                "(SELECT count(*) FROM vw_post_tag where tumblr_name=?) " + RECORD_COUNT_COLUMN + "," +
                "(SELECT count(distinct(tag)) FROM vw_post_tag where tumblr_name=?) " + UNIQUE_TAGS_COUNT_COLUMN + "," +
                "(SELECT count(distinct(tag)) FROM vw_post_tag where tumblr_name=? and show_order=1) " + UNIQUE_FIRST_TAG_COUNT_COLUMN + "," +
                "(SELECT count(*) FROM birthday where tumblr_name=?) " + BIRTHDAYS_COUNT_COLUMN + "," +
                "(SELECT count(*) FROM VW_MISSING_BIRTHDAYS where tumblr_name=?) " + MISSING_BIRTHDAYS_COUNT_COLUMN

        val map = HashMap<String, Long>()
        db.rawQuery(sqlQuery, arrayOf(filterTumblrName, filterTumblrName, filterTumblrName, filterTumblrName, filterTumblrName, filterTumblrName)).use { c ->
            if (c.moveToNext()) {
                for (i in 0 until c.columnCount) {
                    map[c.getColumnName(i)] = c.getLong(i)
                }
            }
        }
        return map
    }

    fun cursorExport(): Cursor {
        val db = dbHelper.readableDatabase
        return db.query(
                TABLE_NAME,
                arrayOf(BaseColumns._ID, TUMBLR_NAME, TAG, PUBLISH_TIMESTAMP, SHOW_ORDER), null, null, null, null,
                BLOG_ID + ", " + BaseColumns._ID + ", " + SHOW_ORDER)
    }

    companion object {
        const val TABLE_NAME = "vw_post_tag"

        const val TAG = "tag"
        const val TUMBLR_NAME = "tumblr_name"
        const val PUBLISH_TIMESTAMP = "publish_timestamp"
        const val SHOW_ORDER = "show_order"
        const val BLOG_ID = "blog_id"
        const val _ID = BaseColumns._ID

        val COLUMNS = arrayOf(BaseColumns._ID, TUMBLR_NAME, TAG, PUBLISH_TIMESTAMP, SHOW_ORDER)
        const val POST_COUNT_COLUMN = "post_count"
        const val UNIQUE_TAGS_COUNT_COLUMN = "unique_tags_count"
        const val UNIQUE_FIRST_TAG_COUNT_COLUMN = "unique_first_tag_count"
        const val MISSING_BIRTHDAYS_COUNT_COLUMN = "missing_birthdays_count"
        const val BIRTHDAYS_COUNT_COLUMN = "birthdays_count"
        const val RECORD_COUNT_COLUMN = "record_count"
    }
}