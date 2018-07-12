package com.ternaryop.photoshelf.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Constructor should be private to prevent direct instantiation. make call
 * to static factory method "getInstance()" instead.
 */
class DBHelper private constructor(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, SCHEMA_VERSION) {

    val tagMatcherDAO = TagMatcherDAO(this)
    val tumblrPostCacheDAO = TumblrPostCacheDAO(this)

    override fun onCreate(db: SQLiteDatabase) {
        tumblrPostCacheDAO.onCreate(db)
        tagMatcherDAO.onCreate(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        tumblrPostCacheDAO.onUpgrade(db, oldVersion, newVersion)
        tagMatcherDAO.onUpgrade(db, oldVersion, newVersion)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    companion object {
        private const val DB_NAME = "photoshelf.db"
        private const val SCHEMA_VERSION = 3

        private var instance: DBHelper? = null

        fun getInstance(context: Context): DBHelper {

            // Use the application context, which will ensure that you
            // don't accidentally leak an Activity's context.
            // See this article for more information: http://bit.ly/6LRzfx
            if (instance == null) {
                instance = DBHelper(context.applicationContext)
            }
            return instance!!
        }
    }
}