package com.ternaryop.phototumblrshare.db;

import java.util.Iterator;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.utils.DialogUtils;

public class DbImportAsyncTask extends AsyncTask<Void, Integer, Void> {
	private Exception error;
	private ProgressDialog progressDialog;
	private Context context;
	private Iterator<PostTag> iterator;
	private boolean removeAll;
	
	public DbImportAsyncTask(Context context, Iterator<PostTag> iterator, boolean removeAll) {
		this.context = context;
		this.iterator = iterator;
		this.removeAll = removeAll;
	}

	protected void onPreExecute() {
		progressDialog = new ProgressDialog(context);
		progressDialog.setMessage(context.getResources().getString(R.string.start_import_title));
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
		DBHelper dbHelper = DBHelper.getInstance(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			db.beginTransaction();
			PostTagDAO postTagDAO = dbHelper.getPostTagDAO();
			if (removeAll) {
				postTagDAO.removeAll();
			}
	        int count = 1;
	        while (iterator.hasNext()) {
				postTagDAO.insert(iterator.next());
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
