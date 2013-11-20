package com.ternaryop.phototumblrshare.db;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

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
				+ "{3} DATE NOT NULL," 
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
				+ " and (not(upper(t.TAG) in (select upper(BIRTHDAY.name) from BIRTHDAY)))"
				+ " and (t.TAG not in ('art')))");
	}

	protected void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        db.execSQL("DROP VIEW IF EXISTS VW_MISSING_BIRTHDAYS");
        onCreate(db);
    }

	public long insert(Birthday birthday) {
		SQLiteDatabase db = getDbHelper().getWritableDatabase();
		long rows = db.insertOrThrow(TABLE_NAME, null, getContentValues(birthday));
		
		return rows;
	}

	public List<Birthday> getBirthdayByDate(Date date, String tumblrName) {
		SQLiteDatabase db = getDbHelper().getReadableDatabase();
		// tumblrName is not used to filter rows
		
		String sqlQuery = "SELECT %2$s, %3$s, %4$s FROM %1$s WHERE strftime('%%m%%d', %3$s) = '%5$s' ORDER BY %2$s, strftime('%%d', %3$s)";
		sqlQuery = String.format(sqlQuery,
				TABLE_NAME,
				NAME,
				BIRTH_DATE,
				TUMBLR_NAME,
				MONTH_DAY_FORMAT.format(date),
				tumblrName);
		Cursor c = db.rawQuery(sqlQuery, null);
		List<Birthday> list = cursorToBirtdayList(c);

		return list;
	}

	public List<Birthday> getBirthdayByMonth(int month, String tumblrName) {
		SQLiteDatabase db = getDbHelper().getReadableDatabase();
		Cursor c = db.query(TABLE_NAME,
				new String[] {NAME,	BIRTH_DATE,	TUMBLR_NAME},
				String.format("strftime('%%m', %1$s) = ? and %2$s = ?", BIRTH_DATE, TUMBLR_NAME),
				new String[] {month < 10 ? "0" + month : "" + month, tumblrName},
				null,
				null,
				String.format("strftime('%%d', %1$s), %2$s", BIRTH_DATE, NAME));
		List<Birthday> list = cursorToBirtdayList(c);

		return list;
	}
	
	public boolean hasBirthdaysInDate(Date date, String tumblrName) {
		SQLiteDatabase db = getDbHelper().getReadableDatabase();
		
		return DatabaseUtils.queryNumEntries(db,
				TABLE_NAME,
				"strftime('%m%d', " + BIRTH_DATE + ") = ?"
				+ " and " + TUMBLR_NAME + " = ?",
				new String[] {MONTH_DAY_FORMAT.format(date), tumblrName}) > 0;
	}
	
    public List<String> getNameWithoutBirthDays(String tumblrName) {
		SQLiteDatabase db = getDbHelper().getReadableDatabase();
		Cursor c = db.query("VW_MISSING_BIRTHDAYS",
				new String[] {NAME},
				TUMBLR_NAME + " = ?",
				new String[] {tumblrName},
				null,
				null,
				NAME);
		ArrayList<String> list = new ArrayList<String>();
		try {
			while (c.moveToNext()) {
				list.add(c.getString(0));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			c.close();
		}
		return list;
    }
	
	private List<Birthday> cursorToBirtdayList(Cursor c) {
		ArrayList<Birthday> list = new ArrayList<Birthday>();
		try {
			while (c.moveToNext()) {
				Birthday birthday = new Birthday(
						c.getString(0),
						c.getString(1),
						c.getString(2));
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
		v.put(BIRTH_DATE, Birthday.ISO_DATE_FORMAT.format(birthday.getBirthDate()));
		
		return v;
	}
}