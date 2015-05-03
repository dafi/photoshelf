package com.ternaryop.photoshelf.db;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.text.TextUtils;

public class BirthdayDAO extends AbsDAO<Birthday> implements BaseColumns {
    public static final String NAME = "NAME";
    public static final String BIRTH_DATE = "BIRTH_DATE";
    public static final String TUMBLR_NAME = "TUMBLR_NAME";
    public static final String TABLE_NAME = "BIRTHDAY";
    
    public static final String[] COLUMNS = new String[] { _ID, TUMBLR_NAME, NAME, BIRTH_DATE };

    private static final SimpleDateFormat MONTH_DAY_FORMAT = new SimpleDateFormat("MMdd", Locale.US);
 
    /**
     * Constructor is accessible only from package
     */
    BirthdayDAO(SQLiteOpenHelper dbHelper) {
        super(dbHelper);
    }
     
    protected void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE {0} ("
                + "{1} TEXT UNIQUE,"
                + "{2} INTEGER PRIMARY KEY," 
                + "{3} DATE," 
                + "{4} TEXT NOT NULL)";
        db.execSQL(MessageFormat.format(sql,
                TABLE_NAME,
                NAME,
                _ID,
                BIRTH_DATE,
                TUMBLR_NAME));
        
        // create views
        db.execSQL("CREATE VIEW VW_MISSING_BIRTHDAYS AS"
                + " select distinct t.TAG AS name, t.tumblr_name from POST_TAG t"
                + " where ((t.SHOW_ORDER = 1)"
                + " and (not(upper(t.TAG) in (select upper(BIRTHDAY.name) from BIRTHDAY))))");
        // lollipop warns about index problems so add it
        db.execSQL("CREATE INDEX TUMBLR_NAME_IDX ON BIRTHDAY(TUMBLR_NAME)");
    }

    protected void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion == 3) {
            db.execSQL("CREATE INDEX TUMBLR_NAME_IDX ON BIRTHDAY(TUMBLR_NAME)");
            return;
        }
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        db.execSQL("DROP VIEW IF EXISTS VW_MISSING_BIRTHDAYS");
        onCreate(db);
    }

    public List<Birthday> getBirthdayByDate(Date date) {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        
        // exclude row with an invalid date
        String sqlQuery = "SELECT %2$s, %3$s, %4$s FROM %1$s WHERE strftime('%%m%%d', %3$s) = '%5$s' ORDER BY %2$s, strftime('%%d', %3$s)";
        sqlQuery = String.format(sqlQuery,
                TABLE_NAME,
                NAME,
                BIRTH_DATE,
                TUMBLR_NAME,
                MONTH_DAY_FORMAT.format(date));
        Cursor c = db.rawQuery(sqlQuery, null);

        return cursorToBirtdayList(c);
    }

    public List<Birthday> getBirthdayByMonth(int month, String tumblrName) {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        Cursor c = db.query(TABLE_NAME,
                new String[] {NAME,    BIRTH_DATE,    TUMBLR_NAME},
                String.format("strftime('%%m', %1$s) = ? and %2$s = ?", BIRTH_DATE, TUMBLR_NAME),
                new String[] {month < 10 ? "0" + month : "" + month, tumblrName},
                null,
                null,
                String.format("strftime('%%d', %1$s), %2$s", BIRTH_DATE, NAME));

        return cursorToBirtdayList(c);
    }
    
    public long getBirthdaysCountInDate(Date date, String tumblrName) {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        
        return DatabaseUtils.queryNumEntries(db,
                TABLE_NAME,
                "strftime('%m%d', " + BIRTH_DATE + ") = ?"
                + " and " + TUMBLR_NAME + " = ?",
                new String[] {MONTH_DAY_FORMAT.format(date), tumblrName});
    }
    
    public List<String> getNameWithoutBirthDays(String tumblrName) {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        ArrayList<String> list = new ArrayList<String>();
        try (Cursor c = db.query("VW_MISSING_BIRTHDAYS",
                new String[]{NAME},
                TUMBLR_NAME + " = ?",
                new String[]{tumblrName},
                null,
                null,
                NAME)) {
            while (c.moveToNext()) {
                list.add(c.getString(0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public Cursor getMissingBirthDaysCursor(String patternName, String tumblrName) {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        return db.rawQuery("select name " + NAME + ", " + TUMBLR_NAME + ", -1 " + _ID + ", null " + BIRTH_DATE + " from VW_MISSING_BIRTHDAYS" +
                        " where " + NAME + " like ?" +
                        " and " + TUMBLR_NAME + "= ?" +
                        " order by " + NAME,
                new String[]{"%" + patternName + "%", tumblrName});
    }

    private List<Birthday> cursorToBirtdayList(Cursor c) {
        ArrayList<Birthday> list = new ArrayList<Birthday>();
        try {
            while (c.moveToNext()) {
                Birthday birthday = new Birthday(
                        c.getString(c.getColumnIndex(NAME)),
                        c.getString(c.getColumnIndex(BIRTH_DATE)),
                        c.getString(c.getColumnIndex(TUMBLR_NAME)));
                list.add(birthday);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
        return list;
    }    

    public ContentValues getContentValues(Birthday birthday) {
        ContentValues v = new ContentValues();

        v.put(TUMBLR_NAME, birthday.getTumblrName());
        v.put(NAME, birthday.getName());
        if (birthday.getBirthDate() != null) {
            v.put(BIRTH_DATE, Birthday.ISO_DATE_FORMAT.format(birthday.getBirthDate()));
        }
        
        return v;
    }
    
    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    public Birthday getBirthdayByName(String name, String tumblrName) {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();

        try (Cursor c = db.query(TABLE_NAME,
                new String[]{BIRTH_DATE},
                "lower(" + NAME + ") = lower(?) and " + TUMBLR_NAME + "=?",
                new String[]{name, tumblrName},
                null, null, null)) {
            if (c.moveToNext()) {
                return new Birthday(
                        name,
                        c.getString(0),
                        tumblrName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Map<String, String>> getBirthdayByAgeRange(int fromAge, int toAge, int daysPeriod, String tumblrName) {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();

        // integers are replaced directly inside query because rawQuery bounds them to strings
        String dateQuery = "select" +
                    " t._id," +
                    " t.tag," +
                    " (strftime('%Y', 'now') - strftime('%Y', b.birth_date)) - (strftime('%m-%d', 'now') < strftime('%m-%d', b.birth_date)) age" +
                    " from post_tag t," +
                    " birthday b" +
                    " where" +
                    " t.tag=b.name" +
                    " and datetime(publish_timestamp,  'unixepoch') >= date('now', '" + (-daysPeriod) + " days')" +
                    " and tag in (" +
                    " select b.name from birthday b" +
                    " where" +
                    " (strftime('%Y', 'now') - strftime('%Y', b.birth_date))" +
                    "     - (strftime('%m-%d', 'now') < strftime('%m-%d', b.birth_date) ) between " + fromAge + " and " + toAge + ")" +
                    " and t.show_order=1" +
                    " and t.TUMBLR_NAME=?" +
                    " order by tag";

        ArrayList<Map<String, String>> list = new ArrayList<Map<String, String>>();
        try (Cursor c = db.rawQuery(dateQuery,
                new String[]{tumblrName})) {
            while (c.moveToNext()) {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("postId", c.getString(0));
                map.put("tag", c.getString(1));
                map.put("age", c.getString(2));
                list.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public Cursor getBirthdayCursorByName(String name, int month, String tumblrName) {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        if (month > 0) {
            return db.query(TABLE_NAME,
                    COLUMNS,
                    String.format("%1$s like ? and %2$s = ? and strftime('%%m', %3$s) = ?", NAME, TUMBLR_NAME, BIRTH_DATE),
                    new String[]{"%" + name + "%", tumblrName, month < 10 ? "0" + month : "" + month},
                    null,
                    null,
                    String.format("strftime('%%d', %1$s), %2$s", BIRTH_DATE, NAME));
        } else {
            return db.query(TABLE_NAME,
                    COLUMNS,
                    String.format("%3$s is not null and %1$s like ? and %2$s = ?", NAME, TUMBLR_NAME, BIRTH_DATE),
                    new String[]{"%" + name + "%", tumblrName},
                    null,
                    null,
                    NAME);
        }
    }

    public Cursor getIgnoredBirthdayCursor(String name, String tumblrName) {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        return db.query(TABLE_NAME,
                COLUMNS,
                String.format("%3$s is null and %1$s like ? and %2$s = ?", NAME, TUMBLR_NAME, BIRTH_DATE),
                new String[]{"%" + name + "%", tumblrName},
                null,
                null,
                NAME);
    }

    public static Birthday getBirthday(Cursor c) {
        try {
            Birthday birthday = new Birthday(
                    c.getString(c.getColumnIndex(NAME)),
                    c.getString(c.getColumnIndex(BIRTH_DATE)),
                    c.getString(c.getColumnIndex(TUMBLR_NAME)));
            birthday.setId(c.getLong(c.getColumnIndex(_ID)));
            return birthday;
        } catch (Exception ex) {
            return null;
        }
    }

    public void remove(long id) {
        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        db.delete(getTableName(), _ID + "=?", new String[]{String.valueOf(id)});
    }

    public boolean markAsIgnored(long id) {
        ContentValues v = new ContentValues();

        v.put(BIRTH_DATE, (String)null);
        return getDbHelper().getWritableDatabase().update(TABLE_NAME, v, _ID + "=?", new String[] {String.valueOf(id)}) == 1;
    }

    public boolean update(Birthday birthday) {
        ContentValues v = getContentValues(birthday);
        return getDbHelper().getWritableDatabase().update(TABLE_NAME, v, _ID + "=?", new String[] {String.valueOf(birthday.getId())}) == 1;
    }

    public Cursor getBirthdaysInSameDay(String pattern, String tumblrName) {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        String likeClause = "%" + pattern + "%";

        String query = "select " + TextUtils.join(",", COLUMNS) + " from birthday where name like ?" +
                " and TUMBLR_NAME = ?" +
                " and strftime('%m%d', birth_date) in" +
                " (SELECT strftime('%m%d', birth_date) FROM birthday where name like ?" +
                " and TUMBLR_NAME = ? and birth_date is not null group by strftime('%m%d', birth_date) having count(*) > 1 order by count(*) ) order by strftime('%m%d', birth_date), name";

        return db.rawQuery(query,
                new String[]{likeClause, tumblrName, likeClause, tumblrName});
    }
}