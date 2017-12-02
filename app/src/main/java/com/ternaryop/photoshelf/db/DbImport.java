package com.ternaryop.photoshelf.db;

import java.util.Iterator;

import android.database.sqlite.SQLiteDatabase;

import io.reactivex.Observable;

public class DbImport<Pojo> {
    private final BulkImportAbsDAO<Pojo> dao;
    private int total;

    public DbImport(BulkImportAbsDAO<Pojo> dao) {
        this.dao = dao;
    }

    public Observable<Integer> importer(final Iterator<Pojo> iterator, final boolean removeAll) {
        SQLiteDatabase db = dao.getDbHelper().getWritableDatabase();
        total = 0;
        return Observable.generate(
                () -> {
                    db.beginTransaction();
                    if (removeAll) {
                        dao.removeAll();
                    }
                    return dao.getCompiledInsertStatement(db);
                },
                (statement, emitter) -> {
                    try {
                        if (iterator.hasNext()) {
                            dao.insert(statement, iterator.next());
                            statement.clearBindings();
                            emitter.onNext(++total);
                        } else {
                            db.setTransactionSuccessful();
                            // subscribe() is never called without a call to onNext()
                            emitter.onNext(total);
                            emitter.onComplete();
                        }
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                },
                total -> db.endTransaction()
        );
    }
}
