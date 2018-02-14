package com.ternaryop.photoshelf.db

import android.database.sqlite.SQLiteStatement
import io.reactivex.Emitter
import io.reactivex.Observable
import io.reactivex.functions.BiConsumer
import io.reactivex.functions.Consumer
import java.util.concurrent.Callable

class DbImport<in Pojo>(private val dao: BulkImportAbsDAO<Pojo>) {
    private var total: Int = 0

    fun importer(iterator: Iterator<Pojo>, removeAll: Boolean): Observable<Int> {
        val db = dao.dbHelper.writableDatabase
        total = 0

        return Observable.generate<Int, SQLiteStatement>(
                Callable {
                    db.beginTransaction()
                    if (removeAll) {
                        dao.removeAll()
                    }
                    dao.getCompiledInsertStatement(db)
                },
                BiConsumer { statement: SQLiteStatement, emitter: Emitter<Int> ->
                    try {
                        if (iterator.hasNext()) {
                            dao.insert(statement, iterator.next())
                            statement.clearBindings()
                            emitter.onNext(++total)
                        } else {
                            db.setTransactionSuccessful()
                            // subscribe() is never called without a call to onNext()
                            emitter.onNext(total)
                            emitter.onComplete()
                        }
                    } catch (e: Exception) {
                        emitter.onError(e)
                    }
                },
                Consumer { db.endTransaction() }
        )
    }
}
