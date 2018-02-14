package com.ternaryop.photoshelf.db

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement

/**
 * Created by dave on 22/01/16.
 * Allow bulk operations
 */
abstract class BulkImportAbsDAO<in Pojo>(dbHelper: SQLiteOpenHelper) : AbsDAO<Pojo>(dbHelper) {

    abstract fun getCompiledInsertStatement(db: SQLiteDatabase): SQLiteStatement

    abstract fun insert(stmt: SQLiteStatement, pojo: Pojo): Long
}
