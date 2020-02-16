package com.ternaryop.photoshelf.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import android.database.sqlite.SQLiteOpenHelper
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

abstract class AbsDAO<in Pojo>(val dbHelper: SQLiteOpenHelper) {
    abstract val tableName: String

    abstract fun onCreate(db: SQLiteDatabase)
    abstract fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)
    abstract fun getContentValues(pojo: Pojo): ContentValues

    fun insert(pojo: Pojo): Long {
        return dbHelper.writableDatabase.insertOrThrow(tableName, null, getContentValues(pojo))
    }

    fun insertWithOnConflict(pojo: Pojo, conflictAlgorithm: Int = CONFLICT_REPLACE): Long =
        dbHelper.writableDatabase.insertWithOnConflict(tableName, null, getContentValues(pojo), conflictAlgorithm)

    open fun removeAll() {
        dbHelper.writableDatabase.delete(tableName, null, null)
    }

    protected fun inClauseParameters(parametersCount: Int): StringBuilder {
        val inClause = StringBuilder()
        var firstTime = true

        for (i in 0 until parametersCount) {
            if (firstTime) {
                firstTime = false
            } else {
                inClause.append(",")
            }
            inClause.append("?")
        }
        return inClause
    }

    companion object {

        @Throws(IOException::class)
        fun toBlob(o: Any): ByteArray {
            ByteArrayOutputStream().use { byteStream ->
                ObjectOutputStream(byteStream).use { output ->
                    output.writeObject(o)
                    return byteStream.toByteArray()
                }
            }
        }

        @Throws(IOException::class, ClassNotFoundException::class)
        fun fromBlob(b: ByteArray): Any {
            ByteArrayInputStream(b).use { bis -> ObjectInputStream(bis).use { input -> return input.readObject() } }
        }
    }
}
