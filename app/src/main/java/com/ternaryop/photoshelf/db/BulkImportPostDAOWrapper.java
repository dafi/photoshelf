package com.ternaryop.photoshelf.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

/**
 * Created by dave on 24/01/16.
 * Allow to make DAO operations on all tables used by Post
 */
public class BulkImportPostDAOWrapper extends BulkImportAbsDAO<PostTag> {
    private final Context context;
    private SQLiteStatement blogStatement;
    private SQLiteStatement tagStatement;
    private SQLiteStatement insertTagMatcherStatement;
    private SQLiteStatement selectTagMatcherStatement;

    private TagMatcherDAO tagMatcherDAO;

    public BulkImportPostDAOWrapper(SQLiteOpenHelper dbHelper, Context context) {
        super(dbHelper);
        this.context = context;
    }

    @Override
    public SQLiteStatement getCompiledInsertStatement(SQLiteDatabase db) {
        blogStatement = db.compileStatement("insert or ignore into blog (name) values (?)");
        tagStatement = db.compileStatement("insert or ignore into tag (name) values (?)");
        tagMatcherDAO = DBHelper.getInstance(context).getTagMatcherDAO();
        insertTagMatcherStatement = TagMatcherDAO.getInsertTagMatcherStatement(db);
        selectTagMatcherStatement = TagMatcherDAO.getSelectTagMatcherStatement(db);
        return db.compileStatement("insert into post(_id, tag_id, blog_id, publish_timestamp, show_order) values(" +
                "?," +
                "(SELECT _id FROM tag where name = ?)," +
                "(SELECT _id FROM blog where name = ?)," +
                "?," +
                "?);");
    }

    @Override
    public long insert(SQLiteStatement stmt, PostTag postTag) {
        insertBlog(postTag.getTumblrName());
        insertTag(postTag.getTag());
        insertTagMatcher(postTag.getTag());
        // insert a Post using the PostTag because the insert statement uses the blogName and tagName contained into PostTag
        return insertPost(stmt, postTag);
    }

    @Override
    protected void onCreate(SQLiteDatabase db) {
    }

    @Override
    protected void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @Override
    public ContentValues getContentValues(PostTag postTag) {
        return null;
    }

    @Override
    public String getTableName() {
        return null;
    }

    @Override
    public void removeAll() {
        DBHelper.getInstance(context).getPostDAO().removeAll();
        DBHelper.getInstance(context).getTagDAO().removeAll();
        DBHelper.getInstance(context).getBlogDAO().removeAll();
        DBHelper.getInstance(context).getTagMatcherDAO().removeAll();
    }

    private void insertBlog(String blogName) {
        int index = 0;
        blogStatement.bindString(++index, blogName);
        blogStatement.executeInsert();
        blogStatement.clearBindings();
    }

    private void insertTag(String tagName) {
        int index = 0;
        tagStatement.bindString(++index, tagName);
        tagStatement.executeInsert();
        tagStatement.clearBindings();
    }

    private long insertPost(SQLiteStatement stmt, PostTag postTag) {
        int index = 0;
        stmt.bindLong(++index, postTag.getId());
        stmt.bindString(++index, postTag.getTag());
        stmt.bindString(++index, postTag.getTumblrName());
        stmt.bindLong(++index, postTag.getPublishTimestamp());
        stmt.bindLong(++index, postTag.getShowOrder());

        return stmt.executeInsert();
    }

    private void insertTagMatcher(String tagName) {
        if (tagMatcherDAO.getMatchingTag(selectTagMatcherStatement, tagName) == null) {
            tagMatcherDAO.insert(insertTagMatcherStatement, tagName);
        }
    }
}
