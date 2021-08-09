@file:Suppress("MaxLineLength")
package com.ternaryop.photoshelf.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDoneException
import android.database.sqlite.SQLiteStatement
import java.util.Locale

/**
 * Created by dave on 07/01/18.
 * Searching into table Tag using functions (lower(), replace()) is very slow so we use a virtual table with FTS
 */

@Suppress("TooManyFunctions")
class TagMatcherDAO(dbHelper: DBHelper) : AbsDAO<Long>(dbHelper) {

    override val tableName: String
        get() = TABLE_NAME

    override fun onCreate(db: SQLiteDatabase) {
        val sql = "CREATE VIRTUAL TABLE %1\$s USING fts4(%2\$s, %3\$s)"
        db.execSQL(String.format(sql,
                TABLE_NAME,
                MATCH_TAG,
                STRIPPED_TAG))
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    override fun getContentValues(pojo: Long): ContentValues {
        throw IllegalArgumentException("Invalid for this object")
    }

    fun getMatchingTag(stmt: SQLiteStatement, tag: String): String? {
        stmt.bindString(1, cleanTag(tag))

        return try {
            stmt.simpleQueryForString()
        } catch (ex: SQLiteDoneException) {
            // zero rows
            null
        }
    }

    fun getMatchingTag(tag: String): String? {
        val db = dbHelper.readableDatabase
        return getMatchingTag(getSelectTagMatcherStatement(db), tag)
    }

    fun insert(name: String) {
        val db = dbHelper.writableDatabase
        insert(getInsertTagMatcherStatement(db), name)
    }

    fun insert(stmt: SQLiteStatement, tag: String): Long {
        var index = 0
        stmt.bindString(++index, tag)
        stmt.bindString(++index, cleanTag(tag))

        return stmt.executeInsert()
    }

    companion object {
        const val MATCH_TAG = "match_tag"
        const val STRIPPED_TAG = "stripped_tag"
        const val TABLE_NAME = "tag_matcher"

        fun cleanTag(tag: String): String {
            return tag.replace("[-' .]".toRegex(), "").lowercase(Locale.US)
        }

        fun getSelectTagMatcherStatement(db: SQLiteDatabase): SQLiteStatement {
            return db.compileStatement(String.format("SELECT %2\$s FROM %1\$s WHERE %3\$s match ?",
                    TABLE_NAME,
                    MATCH_TAG,
                    STRIPPED_TAG))
        }

        fun getInsertTagMatcherStatement(db: SQLiteDatabase): SQLiteStatement {
            return db.compileStatement(String.format("insert into %1\$s (%2\$s, %3\$s) values (?, ?)",
                    TABLE_NAME,
                    MATCH_TAG,
                    STRIPPED_TAG))
        }
    }
}
