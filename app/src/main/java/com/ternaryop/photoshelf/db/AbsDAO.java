package com.ternaryop.photoshelf.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

@SuppressWarnings("unused")
public abstract class AbsDAO<Pojo> {
    private final SQLiteOpenHelper dbHelper;

    public AbsDAO(SQLiteOpenHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    protected abstract void onCreate(SQLiteDatabase db);
    protected abstract void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);
    public abstract ContentValues getContentValues(Pojo pojo);
    public abstract String getTableName();

    public long insert(Pojo pojo) {
        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        return db.insertOrThrow(getTableName(), null, getContentValues(pojo));
    }

    public void removeAll() {
        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        db.delete(getTableName(), null, null);
    }

    public SQLiteOpenHelper getDbHelper() {
        return dbHelper;
    }

    protected StringBuilder inClauseParameters(int parametersCount) {
        StringBuilder inClause = new StringBuilder();
        boolean firstTime = true;

        for (int i = 0; i < parametersCount; i++) {
            if (firstTime) {
                firstTime = false;
            } else {
                inClause.append(",");
            }
            inClause.append("?");
        }
        return inClause;
    }

    public static byte[] toBlob(Object o) throws IOException {
        try (final ByteArrayOutputStream byteStream = new ByteArrayOutputStream(); ObjectOutput output = new ObjectOutputStream(byteStream)) {
            output.writeObject(o);
            return byteStream.toByteArray();
        }
    }

    public static Object fromBlob(byte[] b) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(b); ObjectInput input = new ObjectInputStream(bis)) {
            return input.readObject();
        }
    }
}
