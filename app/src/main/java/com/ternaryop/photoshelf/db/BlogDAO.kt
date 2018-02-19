package com.ternaryop.photoshelf.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import java.text.MessageFormat

/**
 * Constructor is accessible only from package
 */
@Suppress("TooManyFunctions", "ObjectPropertyNaming")
class BlogDAO internal constructor(dbHelper: SQLiteOpenHelper) : AbsDAO<Blog>(dbHelper), BaseColumns {

    override val tableName: String
        get() = TABLE_NAME

    override fun onCreate(db: SQLiteDatabase) {
        val sql = ("CREATE TABLE {0} ("
                + "{1} INTEGER PRIMARY KEY,"
                + "{2} TEXT UNIQUE)")
        db.execSQL(MessageFormat.format(sql,
                TABLE_NAME,
                BaseColumns._ID,
                NAME))

        // lollipop warns about index problems so add it
        db.execSQL(String.format("CREATE INDEX %1\$s_%2\$s_IDX ON %1\$s(%2\$s)", TABLE_NAME, NAME))
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    override fun getContentValues(pojo: Blog): ContentValues {
        val v = ContentValues()

        v.put(NAME, pojo.name)

        return v
    }

    companion object {
        const val NAME = "name"
        const val TABLE_NAME = "blog"
        // BaseColumns as Java interface isn't visible from other classes so we 'redeclare' the _ID
        const val _ID = BaseColumns._ID

        val COLUMNS = arrayOf(_ID, NAME)
    }
}