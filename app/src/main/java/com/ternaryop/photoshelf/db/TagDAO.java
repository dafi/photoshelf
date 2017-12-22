package com.ternaryop.photoshelf.db;

import java.text.MessageFormat;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class TagDAO extends AbsDAO<Tag> implements BaseColumns {
    public static final String TABLE_NAME = "tag";
    public static final String NAME = "name";

    public static final String[] COLUMNS = new String[] { _ID, NAME };

    /**
     * Constructor is accessible only from package
     */
    TagDAO(SQLiteOpenHelper dbHelper) {
        super(dbHelper);
    }
     
    protected void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE {0} ("
                + "{1} INTEGER PRIMARY KEY,"
                + "{2} TEXT UNIQUE);";
        db.execSQL(MessageFormat.format(sql,
                TABLE_NAME,
                _ID,
                NAME));
        
        // lollipop warns about index problems so add it
        db.execSQL(String.format("CREATE INDEX %1$s_%2$s_IDX ON %1$s(%2$s)", TABLE_NAME, NAME));
    }

    protected void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public ContentValues getContentValues(Tag tag) {
        ContentValues v = new ContentValues();

        v.put(NAME, tag.getName());

        return v;
    }
}