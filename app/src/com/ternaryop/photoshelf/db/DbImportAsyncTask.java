package com.ternaryop.photoshelf.db;

import java.util.Iterator;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.ternaryop.photoshelf.R;
import com.ternaryop.utils.DialogUtils;

public class DbImportAsyncTask<Pojo> extends AsyncTask<Void, Integer, Void> {
	private Exception error;
	private ProgressDialog progressDialog;
	private Context context;
	private Iterator<Pojo> iterator;
	private boolean removeAll;
	private AbsDAO<Pojo> dao;
	
	public DbImportAsyncTask(Context context, Iterator<Pojo> iterator, AbsDAO<Pojo> dao, boolean removeAll) {
		this.context = context;
		this.iterator = iterator;
		this.dao = dao;
		this.removeAll = removeAll;
	}

	protected void onPreExecute() {
		progressDialog = new ProgressDialog(context);
		progressDialog.setMessage(context.getString(R.string.start_import_title));
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.show();
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		progressDialog.setMessage(context.getString(R.string.import_progress_title, values[0]));
	}
	
	@Override
	protected void onPostExecute(Void result) {
		progressDialog.dismiss();
		
		if (error != null) {
			DialogUtils.showErrorDialog(context, error);
		}
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
			error = e;
		} finally {
			db.endTransaction();
		}
		return null;
	}
}
