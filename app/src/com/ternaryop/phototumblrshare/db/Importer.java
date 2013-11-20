package com.ternaryop.phototumblrshare.db;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.importer.CSVIterator;
import com.ternaryop.phototumblrshare.importer.CSVIterator.CSVBuilder;
import com.ternaryop.tumblr.Callback;
import com.ternaryop.tumblr.PostRetriever;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.AbsProgressBarAsyncTask;
import com.ternaryop.utils.DialogUtils;

public class Importer {
	public static void importPostsFromCSV(final Context context, final String importPath) {
		try {
			new DbImportAsyncTask<PostTag>(context,
					new CSVIterator<PostTag>(importPath, new PostTagCSVBuilder()),
					DBHelper.getInstance(context).getPostTagDAO(),
					true).execute();
		} catch (Exception error) {
			DialogUtils.showErrorDialog(context, error);
		}
	}

	public static void exportPostsToCSV(final Context context, final String exportPath) {
		try {
			new AbsProgressBarAsyncTask<Void, Void, Void>(context, context.getString(R.string.exporting_to_csv)) {
				@Override
				protected Void doInBackground(Void... voidParams) {
					SQLiteDatabase db = DBHelper.getInstance(context).getReadableDatabase();
					Cursor c = db.query(PostTagDAO.TABLE_NAME, null, null, null, null, null, PostTagDAO._ID);
					try {
						PrintWriter pw = new PrintWriter(exportPath);
						while (c.moveToNext()) {
							pw.println(String.format("%1$d;%2$s;%3$s;%4$d;%5$d",
									c.getLong(c.getColumnIndex(PostTagDAO._ID)),
									c.getString(c.getColumnIndex(PostTagDAO.TUMBLR_NAME)),
									c.getString(c.getColumnIndex(PostTagDAO.TAG)),
									c.getLong(c.getColumnIndex(PostTagDAO.PUBLISH_TIMESTAMP)),
									c.getLong(c.getColumnIndex(PostTagDAO.SHOW_ORDER))
									));
						}
						pw.flush();
						pw.close();
					} catch (Exception e) {
						setError(e);
					} finally {
						c.close();
					}	
					
					return null;
				}
			}.execute();
		} catch (Exception error) {
			DialogUtils.showErrorDialog(context, error);
		}
	}
	
	public static void importFromTumblr(final Context context, final String blogName) {
		PostTag post = DBHelper.getInstance(context).getPostTagDAO().findLastPublishedPost(blogName);
		PostRetriever postRetriever = new PostRetriever(context, post.getPublishTimestamp(), new Callback<List<TumblrPost>>() {
			
			@Override
			public void failure(Exception error) {
				if (error != null) {
					DialogUtils.showErrorDialog(context, error);
				}
			}
			
			@Override
			public void complete(List<TumblrPost> allPosts) {
				List<PostTag> allPostTags = new ArrayList<PostTag>();
				for (TumblrPost tumblrPost : allPosts) {
					allPostTags.addAll(PostTag.postTagsFromTumblrPost(tumblrPost));
				}
				new DbImportAsyncTask<PostTag>(context,
						allPostTags.iterator(),
						DBHelper.getInstance(context).getPostTagDAO(),
						false).execute();
			}
		});
		Tumblr.getSharedTumblr(context).readPublicPhotoPosts(blogName, null, postRetriever);
	}

	public static void importDOMFilters(Context context, String importPath) {
		InputStream in = null;
		OutputStream out = null;

		try {
			in = new FileInputStream(importPath);
		    out = context.openFileOutput("domSelectors.json", 0);

		    byte[] buf = new byte[1024];
		    int len;
		    while ((len = in.read(buf)) > 0) {
		        out.write(buf, 0, len);
		    }
			Toast.makeText(context, context.getString(R.string.importSuccess), Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		} finally {
			if (in != null) try { in.close(); } catch (Exception ex) {}
			if (out != null) try { out.close(); } catch (Exception ex) {}
		}
	}

	public static void importBirtdays(final Context context, final String importPath) {
		try {
			new DbImportAsyncTask<Birthday>(context,
					new CSVIterator<Birthday>(importPath, new CSVBuilder<Birthday>() {

						@Override
						public Birthday parseCSVFields(String[] fields) throws ParseException {
							// id is skipped
							return new Birthday(fields[1], fields[2], fields[3]);
						}
					}),
					DBHelper.getInstance(context).getBirthdayDAO(),
					true).execute();
		} catch (Exception error) {
			DialogUtils.showErrorDialog(context, error);
		}
	}
	
	public static void exportBirthdaysToCSV(final Context context, final String exportPath) {
		try {
			new AbsProgressBarAsyncTask<Void, Void, Void>(context, context.getString(R.string.exporting_to_csv)) {
				@Override
				protected Void doInBackground(Void... voidParams) {
					SQLiteDatabase db = DBHelper.getInstance(context).getReadableDatabase();
					Cursor c = db.query(BirthdayDAO.TABLE_NAME, null, null, null, null, null, BirthdayDAO.NAME);
					try {
						PrintWriter pw = new PrintWriter(exportPath);
						while (c.moveToNext()) {
							pw.println(String.format("%1$d;%2$s;%3$s;%4$s",
									c.getLong(c.getColumnIndex(BirthdayDAO._ID)),
									c.getString(c.getColumnIndex(BirthdayDAO.NAME)),
									c.getString(c.getColumnIndex(BirthdayDAO.BIRTH_DATE)),
									c.getString(c.getColumnIndex(BirthdayDAO.TUMBLR_NAME))
									));
						}
						pw.flush();
						pw.close();
					} catch (Exception e) {
						setError(e);
					} finally {
						c.close();
					}	
					
					return null;
				}
			}.execute();
		} catch (Exception error) {
			DialogUtils.showErrorDialog(context, error);
		}
	}

	static class PostTagCSVBuilder implements CSVBuilder<PostTag> {
		@Override
		public PostTag parseCSVFields(String[] fields) {
			return new PostTag(
					Long.parseLong(fields[0]),
					fields[1],
					fields[2],
					Long.parseLong(fields[3]),
					Long.parseLong(fields[4]));
		}
	}
}
