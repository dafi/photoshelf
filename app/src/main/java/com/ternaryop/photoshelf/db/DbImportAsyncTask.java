package com.ternaryop.photoshelf.db;

import java.util.Iterator;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.widget.TextView;

import com.ternaryop.photoshelf.R;
import com.ternaryop.utils.AbsProgressIndicatorAsyncTask;

public class DbImportAsyncTask<Pojo> extends AbsProgressIndicatorAsyncTask<Void, Integer, Void> {
    private final Iterator<Pojo> iterator;
    private final boolean removeAll;
    private final AbsDAO<Pojo> dao;

    public DbImportAsyncTask(Context context, TextView textView, Iterator<Pojo> iterator, AbsDAO<Pojo> dao, boolean removeAll) {
        super(context, context.getString(R.string.start_import_title), textView);
        this.iterator = iterator;
        this.dao = dao;
        this.removeAll = removeAll;
    }

    public DbImportAsyncTask(Context context, java.util.Iterator <Pojo> iterator, AbsDAO<Pojo> dao, boolean removeAll) {
        this(context, null, iterator, dao, removeAll);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        setProgressMessage(getContext().getString(R.string.import_progress_title, values[0]));
    }

    @Override
    protected Void doInBackground(Void... params) {
        SQLiteDatabase db = dao.getDbHelper().getWritableDatabase();
        try {
            db.beginTransaction();
            if (removeAll) {
                dao.removeAll();
            }
            int count = 1;
            while (iterator.hasNext()) {
                dao.insert(iterator.next());
                publishProgress(count);
                ++count;
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            setError(e);
        } finally {
            db.endTransaction();
        }
        return null;
    }
}
