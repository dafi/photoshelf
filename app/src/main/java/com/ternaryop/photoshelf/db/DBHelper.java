package com.ternaryop.photoshelf.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "photoshelf.db";
    private static final int SCHEMA_VERSION = 1;

    private static DBHelper instance = null;

    private final PostDAO postDAO;
    private final PostTagDAO postTagDAO;
    private final BirthdayDAO birthdayDAO;

    private final BlogDAO blogDAO;
    private final TagDAO tagDAO;

    private final BulkImportPostDAOWrapper bulkImportPostDAOWrapper;

    public static DBHelper getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (instance == null) {
            instance = new DBHelper(
                    context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Constructor should be private to prevent direct instantiation. make call
     * to static factory method "getInstance()" instead.
     */
    private DBHelper(Context context) {
        super(context, DB_NAME, null, SCHEMA_VERSION);
        blogDAO = new BlogDAO(this);
        tagDAO = new TagDAO(this);
        postDAO = new PostDAO(this);
        postTagDAO = new PostTagDAO(this);
        birthdayDAO = new BirthdayDAO(this);
        bulkImportPostDAOWrapper = new BulkImportPostDAOWrapper(this, context);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        blogDAO.onCreate(db);
        tagDAO.onCreate(db);
        postDAO.onCreate(db);
        postTagDAO.onCreate(db);
        birthdayDAO.onCreate(db);
        bulkImportPostDAOWrapper.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        blogDAO.onUpgrade(db, oldVersion, newVersion);
        tagDAO.onUpgrade(db, oldVersion, newVersion);
        postDAO.onUpgrade(db, oldVersion, newVersion);
        postTagDAO.onUpgrade(db, oldVersion, newVersion);
        birthdayDAO.onUpgrade(db, oldVersion, newVersion);
        bulkImportPostDAOWrapper.onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    public PostDAO getPostDAO() {
        return postDAO;
    }

    public PostTagDAO getPostTagDAO() {
        return postTagDAO;
    }
    
    public BirthdayDAO getBirthdayDAO() {
        return birthdayDAO;
    }

    public BlogDAO getBlogDAO() {
        return blogDAO;
    }

    public TagDAO getTagDAO() {
        return tagDAO;
    }

    public BulkImportPostDAOWrapper getBulkImportPostDAOWrapper() {
        return bulkImportPostDAOWrapper;
    }
}