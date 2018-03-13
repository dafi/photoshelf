@file:Suppress("MaxLineLength")
package com.ternaryop.photoshelf.db

import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement
import android.provider.BaseColumns
import android.text.TextUtils
import java.text.MessageFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Constructor is accessible only from package
 */
@Suppress("TooManyFunctions", "ObjectPropertyNaming")
class BirthdayDAO internal constructor(dbHelper: SQLiteOpenHelper) : BulkImportAbsDAO<Birthday>(dbHelper), BaseColumns {

    override val tableName: String
        get() = TABLE_NAME

    override fun onCreate(db: SQLiteDatabase) {
        val sql = ("CREATE TABLE {0} ("
                + "{1} TEXT UNIQUE,"
                + "{2} INTEGER PRIMARY KEY,"
                + "{3} DATE,"
                + "{4} TEXT NOT NULL)")
        db.execSQL(MessageFormat.format(sql,
                TABLE_NAME,
                NAME,
                BaseColumns._ID,
                BIRTH_DATE,
                TUMBLR_NAME))

        // create views
        db.execSQL("CREATE VIEW vw_missing_birthdays AS"
                + " select distinct t.tag as name, t.tumblr_name from vw_post_tag t"
                + " where ((t.show_order = 1)"
                + " and (not(upper(t.tag) in (select upper(birthday.name) from birthday))))")
        // lollipop warns about index problems so add it
        db.execSQL("CREATE INDEX tumblr_name_idx ON birthday(tumblr_name)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun getBirthdayByDate(date: Date): List<Birthday> {
        val db = dbHelper.readableDatabase

        // exclude row with an invalid date
        var sqlQuery = "SELECT %2\$s, %3\$s, %4\$s FROM %1\$s WHERE strftime('%%m%%d', %3\$s) = '%5\$s' ORDER BY %2\$s, strftime('%%d', %3\$s)"
        sqlQuery = String.format(sqlQuery,
                TABLE_NAME,
                NAME,
                BIRTH_DATE,
                TUMBLR_NAME,
                MONTH_DAY_FORMAT.format(date))
        return cursorToBirthdayList(db.rawQuery(sqlQuery, null))
    }

    fun getBirthdayByMonth(month: Int, tumblrName: String): List<Birthday> {
        val db = dbHelper.readableDatabase
        return cursorToBirthdayList(db.query(TABLE_NAME,
                arrayOf(NAME, BIRTH_DATE, TUMBLR_NAME),
                String.format("strftime('%%m', %1\$s) = ? and %2\$s = ?", BIRTH_DATE, TUMBLR_NAME),
                arrayOf(String.format("%02d", month), tumblrName), null, null,
                String.format("strftime('%%d', %1\$s), %2\$s", BIRTH_DATE, NAME)))
    }

    fun getBirthdaysCountInDate(date: Date, tumblrName: String): Long {
        val db = dbHelper.readableDatabase

        return DatabaseUtils.queryNumEntries(db,
                TABLE_NAME,
                "strftime('%m%d', " + BIRTH_DATE + ") = ?"
                        + " and " + TUMBLR_NAME + " = ?",
                arrayOf(MONTH_DAY_FORMAT.format(date), tumblrName))
    }

    fun getNameWithoutBirthDays(tumblrName: String): List<String> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<String>()
        try {
            db.query("vw_missing_birthdays",
                    arrayOf(NAME),
                    TUMBLR_NAME + " = ?",
                    arrayOf(tumblrName), null, null,
                    NAME).use { c ->
                while (c.moveToNext()) {
                    list.add(c.getString(0))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list
    }

    fun getMissingBirthDaysCursor(patternName: String, tumblrName: String): Cursor {
        val db = dbHelper.readableDatabase
        return db.rawQuery("select name " + NAME + ", " + TUMBLR_NAME + ", -1 " + BaseColumns._ID + ", null " + BIRTH_DATE + " from vw_missing_birthdays" +
                " where " + NAME + " like ?" +
                " and " + TUMBLR_NAME + "= ?" +
                " order by " + NAME,
                arrayOf("%$patternName%", tumblrName))
    }

    private fun cursorToBirthdayList(c: Cursor): List<Birthday> {
        val list = mutableListOf<Birthday>()
        try {
            while (c.moveToNext()) {
                val birthday = Birthday(
                        c.getString(c.getColumnIndex(NAME)),
                        c.getString(c.getColumnIndex(BIRTH_DATE)),
                        c.getString(c.getColumnIndex(TUMBLR_NAME)))
                list.add(birthday)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            c.close()
        }
        return list
    }

    override fun getContentValues(pojo: Birthday): ContentValues {
        val v = ContentValues()

        v.put(TUMBLR_NAME, pojo.tumblrName)
        v.put(NAME, pojo.name)
        val birthDate = pojo.birthDate
        if (birthDate != null) {
            v.put(BIRTH_DATE, Birthday.toIsoFormat(birthDate))
        }

        return v
    }

    fun getBirthdayByName(name: String, tumblrName: String): Birthday? {
        val db = dbHelper.readableDatabase

        try {
            db.query(TABLE_NAME,
                    arrayOf(BIRTH_DATE),
                    "lower($NAME) = lower(?) and $TUMBLR_NAME=?",
                    arrayOf(name, tumblrName), null, null, null).use { c ->
                if (c.moveToNext()) {
                    return Birthday(
                            name,
                            c.getString(0),
                            tumblrName)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun getBirthdayByAgeRange(fromAge: Int, toAge: Int, daysPeriod: Int, tumblrName: String): List<Map<String, String>> {
        val db = dbHelper.readableDatabase

        // integers are replaced directly inside query because rawQuery bounds them to strings
        val dateQuery = "select" +
                " t._id," +
                " t.tag," +
                " (strftime('%Y', 'now') - strftime('%Y', b.birth_date)) - (strftime('%m-%d', 'now') < strftime('%m-%d', b.birth_date)) age" +
                " from vw_post_tag t," +
                " birthday b" +
                " where" +
                " t.tag=b.name" +
                " and datetime(publish_timestamp,  'unixepoch') >= date('now', '" + -daysPeriod + " days')" +
                " and tag in (" +
                " select b.name from birthday b" +
                " where" +
                " (strftime('%Y', 'now') - strftime('%Y', b.birth_date))" +
                "     - (strftime('%m-%d', 'now') < strftime('%m-%d', b.birth_date) ) between " + fromAge + " and " + toAge + ")" +
                " and t.show_order=1" +
                " and t.tumblr_name=?" +
                " order by tag"

        val list = mutableListOf<Map<String, String>>()
        try {
            db.rawQuery(dateQuery,
                    arrayOf(tumblrName)).use { c ->
                while (c.moveToNext()) {
                    val map = HashMap<String, String>()
                    map["postId"] = c.getString(0)
                    map["tag"] = c.getString(1)
                    map["age"] = c.getString(2)
                    list.add(map)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list
    }

    fun getBirthdayCursorByName(name: String, month: Int, tumblrName: String): Cursor {
        val db = dbHelper.readableDatabase
        return if (month > 0) {
            db.query(TABLE_NAME,
                    COLUMNS,
                    String.format("%1\$s like ? and %2\$s = ? and strftime('%%m', %3\$s) = ?", NAME, TUMBLR_NAME, BIRTH_DATE),
                    arrayOf("%$name%", tumblrName, String.format("%02d", month)), null, null,
                    String.format("strftime('%%d', %1\$s), %2\$s", BIRTH_DATE, NAME))
        } else {
            db.query(TABLE_NAME,
                    COLUMNS,
                    String.format("%3\$s is not null and %1\$s like ? and %2\$s = ?", NAME, TUMBLR_NAME, BIRTH_DATE),
                    arrayOf("%$name%", tumblrName), null, null,
                    NAME)
        }
    }

    fun getIgnoredBirthdayCursor(name: String, tumblrName: String): Cursor {
        val db = dbHelper.readableDatabase
        return db.query(TABLE_NAME,
                COLUMNS,
                String.format("%3\$s is null and %1\$s like ? and %2\$s = ?", NAME, TUMBLR_NAME, BIRTH_DATE),
                arrayOf("%$name%", tumblrName), null, null,
                NAME)
    }

    fun getBirthdaysWithoutPostsCursor(tumblrName: String): Cursor {
        val db = dbHelper.readableDatabase
        return db.query(TABLE_NAME,
                COLUMNS,
                "name NOT IN (SELECT name FROM tag)", null, null, null,
                NAME)
    }

    fun remove(id: Long) {
        val db = dbHelper.writableDatabase
        db.delete(tableName, BaseColumns._ID + "=?", arrayOf(id.toString()))
    }

    fun markAsIgnored(id: Long): Boolean {
        val v = ContentValues()

        v.put(BIRTH_DATE, null as String?)
        return dbHelper.writableDatabase.update(TABLE_NAME, v, BaseColumns._ID + "=?", arrayOf(id.toString())) == 1
    }

    fun update(birthday: Birthday): Boolean {
        val v = getContentValues(birthday)
        return dbHelper.writableDatabase.update(TABLE_NAME, v, BaseColumns._ID + "=?", arrayOf(birthday.id.toString())) == 1
    }

    fun getBirthdaysInSameDay(pattern: String, tumblrName: String): Cursor {
        val db = dbHelper.readableDatabase
        val likeClause = "%$pattern%"

        val query = "select " + TextUtils.join(",", COLUMNS) + " from birthday where name like ?" +
                " and tumblr_name = ?" +
                " and strftime('%m%d', birth_date) in" +
                " (SELECT strftime('%m%d', birth_date) FROM birthday where name like ?" +
                " and tumblr_name = ? and birth_date is not null group by strftime('%m%d', birth_date) having count(*) > 1 order by count(*) ) order by strftime('%m%d', birth_date), name"

        return db.rawQuery(query,
                arrayOf(likeClause, tumblrName, likeClause, tumblrName))
    }

    override fun getCompiledInsertStatement(db: SQLiteDatabase): SQLiteStatement {
        return db.compileStatement("insert into $TABLE_NAME(tumblr_name, name, birth_date) values (?, ?, ?)")
    }

    override fun insert(stmt: SQLiteStatement, pojo: Birthday): Long {
        var index = 0
        stmt.bindString(++index, pojo.tumblrName)
        stmt.bindString(++index, pojo.name)
        val birthDate = pojo.birthDate
        if (birthDate == null) {
            stmt.bindNull(++index)
        } else {
            stmt.bindString(++index, Birthday.toIsoFormat(birthDate))
        }

        return stmt.executeInsert()
    }

    companion object {
        const val NAME = "name"
        const val BIRTH_DATE = "birth_date"
        const val TUMBLR_NAME = "tumblr_name"
        const val TABLE_NAME = "birthday"

        val COLUMNS = arrayOf(BaseColumns._ID, TUMBLR_NAME, NAME, BIRTH_DATE)

        private val MONTH_DAY_FORMAT = SimpleDateFormat("MMdd", Locale.US)

        fun getBirthday(c: Cursor): Birthday? {
            return try {
                val birthday = Birthday(
                        c.getString(c.getColumnIndex(NAME)),
                        c.getString(c.getColumnIndex(BIRTH_DATE)),
                        c.getString(c.getColumnIndex(TUMBLR_NAME)))
                birthday.id = c.getLong(c.getColumnIndex(BaseColumns._ID))
                birthday
            } catch (ex: Exception) {
                null
            }
        }
    }
}