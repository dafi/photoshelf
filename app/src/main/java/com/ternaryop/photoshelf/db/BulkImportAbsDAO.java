package com.ternaryop.photoshelf.db;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

/**
 * Created by dave on 22/01/16.
 * Allow bulk operations
 */
public abstract class BulkImportAbsDAO<Pojo> extends AbsDAO<Pojo> {
    public BulkImportAbsDAO(SQLiteOpenHelper dbHelper) {
        super(dbHelper);
    }

    public abstract SQLiteStatement getCompiledInsertStatement(SQLiteDatabase db);

    @SuppressWarnings("UnusedReturnValue")
    public abstract long insert(SQLiteStatement stmt, Pojo pojo);

}
