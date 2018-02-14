package com.ternaryop.photoshelf.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import java.text.MessageFormat

class TagDAO
/**
 * Constructor is accessible only from package
 */
internal constructor(dbHelper: SQLiteOpenHelper) : AbsDAO<Tag>(dbHelper), BaseColumns {

    override val tableName: String
        get() = TABLE_NAME

    override fun onCreate(db: SQLiteDatabase) {
        val sql = ("CREATE TABLE {0} ("
                + "{1} INTEGER PRIMARY KEY,"
                + "{2} TEXT UNIQUE);")
        db.execSQL(MessageFormat.format(sql,
                TABLE_NAME,
                BaseColumns._ID,
                NAME))

        // lollipop warns about index problems so add it
        db.execSQL(String.format("CREATE INDEX %1\$s_%2\$s_IDX ON %1\$s(%2\$s)", TABLE_NAME, NAME))
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    override fun getContentValues(pojo: Tag): ContentValues {
        val v = ContentValues()

        v.put(NAME, pojo.name)

        return v
    }

    companion object {
        const val TABLE_NAME = "tag"
        const val NAME = "name"

        val COLUMNS = arrayOf(BaseColumns._ID, NAME)
    }
}