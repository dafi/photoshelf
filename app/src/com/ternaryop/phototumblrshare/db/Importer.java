package com.ternaryop.phototumblrshare.db;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.utils.DialogUtils;

public class Importer {
	public static void importPosts(final Context context, final String importPath) {
		new AsyncTask<Void, Integer, Void>() {
			private Exception error;
			ProgressDialog progressDialog;

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
				BufferedReader br = null;
				DBHelper dbHelper = DBHelper.getInstance(context);
				SQLiteDatabase db = dbHelper.getWritableDatabase();
				try {
					db.beginTransaction();
					PostTagDAO postTagDAO = dbHelper.getPostTagDAO();
					postTagDAO.removeAll();
			        br = new BufferedReader(new InputStreamReader(new FileInputStream(importPath)));
			        String line;
			        int count = 1;
			        while ((line = br.readLine()) != null) {
			        	String[] args = line.split(";");
						postTagDAO.insert(new PostTag(
								Long.parseLong(args[0]),
								args[1],
								args[2],
								Long.parseLong(args[3]),
								Long.parseLong(args[4])));
						publishProgress(count);
						++count;
			        }
					db.setTransactionSuccessful();
				} catch (Exception e) {
					e.printStackTrace();
					error = e;
				} finally {
					if (br != null) try { br.close(); } catch (Exception ex) {}
					db.endTransaction();
				}
				return null;
			}
		}.execute();
	}

}
