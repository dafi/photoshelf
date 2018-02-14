package com.ternaryop.photoshelf.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement

/**
 * Created by dave on 24/01/16.
 * Allow to make DAO operations on all tables used by Post
 */
class BulkImportPostDAOWrapper(dbHelper: SQLiteOpenHelper, private val context: Context) : BulkImportAbsDAO<PostTag>(dbHelper) {
    private lateinit var blogStatement: SQLiteStatement
    private lateinit var tagStatement: SQLiteStatement
    private lateinit var insertTagMatcherStatement: SQLiteStatement
    private lateinit var selectTagMatcherStatement: SQLiteStatement

    private lateinit var tagMatcherDAO: TagMatcherDAO

    override val tableName: String
        get() = throw IllegalArgumentException("No table for this object")

    override fun getCompiledInsertStatement(db: SQLiteDatabase): SQLiteStatement {
        blogStatement = db.compileStatement("insert or ignore into blog (name) values (?)")
        tagStatement = db.compileStatement("insert or ignore into tag (name) values (?)")
        tagMatcherDAO = DBHelper.getInstance(context).tagMatcherDAO
        insertTagMatcherStatement = TagMatcherDAO.getInsertTagMatcherStatement(db)
        selectTagMatcherStatement = TagMatcherDAO.getSelectTagMatcherStatement(db)
        return db.compileStatement("insert into post(_id, tag_id, blog_id, publish_timestamp, show_order) values(" +
                "?," +
                "(SELECT _id FROM tag where name = ?)," +
                "(SELECT _id FROM blog where name = ?)," +
                "?," +
                "?);")
    }

    override fun insert(stmt: SQLiteStatement, pojo: PostTag): Long {
        insertBlog(pojo.tumblrName)
        insertTag(pojo.tag)
        insertTagMatcher(pojo.tag)
        // insert a Post using the PostTag because the insert statement uses the blogName and tagName contained into PostTag
        return insertPost(stmt, pojo)
    }

    override fun onCreate(db: SQLiteDatabase) {}

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }

    override fun getContentValues(pojo: PostTag): ContentValues {
        throw IllegalArgumentException("Invalid for this object")
    }

    override fun removeAll() {
        DBHelper.getInstance(context).postDAO.removeAll()
        DBHelper.getInstance(context).tagDAO.removeAll()
        DBHelper.getInstance(context).blogDAO.removeAll()
        DBHelper.getInstance(context).tagMatcherDAO.removeAll()
    }

    private fun insertBlog(blogName: String?) {
        var index = 0
        blogStatement.bindString(++index, blogName)
        blogStatement.executeInsert()
        blogStatement.clearBindings()
    }

    private fun insertTag(tagName: String?) {
        var index = 0
        tagStatement.bindString(++index, tagName)
        tagStatement.executeInsert()
        tagStatement.clearBindings()
    }

    private fun insertPost(stmt: SQLiteStatement, postTag: PostTag): Long {
        var index = 0
        stmt.bindLong(++index, postTag.id)
        stmt.bindString(++index, postTag.tag)
        stmt.bindString(++index, postTag.tumblrName)
        stmt.bindLong(++index, postTag.publishTimestamp)
        stmt.bindLong(++index, postTag.showOrder.toLong())

        return stmt.executeInsert()
    }

    private fun insertTagMatcher(tagName: String) {
        if (tagMatcherDAO.getMatchingTag(selectTagMatcherStatement, tagName) == null) {
            tagMatcherDAO.insert(insertTagMatcherStatement, tagName)
        }
    }
}
