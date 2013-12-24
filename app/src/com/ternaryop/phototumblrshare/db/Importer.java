package com.ternaryop.phototumblrshare.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.widget.Toast;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.importer.CSVIterator;
import com.ternaryop.phototumblrshare.importer.CSVIterator.CSVBuilder;
import com.ternaryop.phototumblrshare.importer.PostRetriever;
import com.ternaryop.tumblr.Callback;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.AbsProgressBarAsyncTask;
import com.ternaryop.utils.DialogUtils;
import com.ternaryop.utils.StringUtils;

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
		importFromTumblr(context, blogName, null);
	}
	
	public static void importFromTumblr(final Context context, final String blogName, final ImportCompleteCallback callback) {
		PostTag post = DBHelper.getInstance(context).getPostTagDAO().findLastPublishedPost(blogName);
		long publishTimestamp = post == null ? 0 : post.getPublishTimestamp();
		PostRetriever postRetriever = new PostRetriever(context, publishTimestamp, new Callback<List<TumblrPost>>() {
			
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
						false) {
					@Override
					protected void onPostExecute(Void result) {
						super.onPostExecute(result);
						if (callback != null) {
							callback.complete();
						}
					}
				}.execute();
			}
		});
		postRetriever.readPhotoPosts(blogName, null);
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
							String birthdate = c.getString(c.getColumnIndex(BirthdayDAO.BIRTH_DATE));
                            pw.println(String.format("%1$d;%2$s;%3$s;%4$s",
									c.getLong(c.getColumnIndex(BirthdayDAO._ID)),
									c.getString(c.getColumnIndex(BirthdayDAO.NAME)),
									birthdate == null ? "" : birthdate,
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

    public static void importMissingBirthdaysFromWikipedia(final Context context, final String blogName) {
        new AbsProgressBarAsyncTask<Void, String, String>(context,
                context.getString(R.string.import_missing_birthdays_from_wikipedia_title)) {
            @Override
            protected void onProgressUpdate(String... values) {
                getProgressDialog().setMessage(values[0]);
            }

            private Birthday getBirthdayFromGoogle(String name, String blogName) throws IOException, ParseException {
                String cleanName = name
                        .replaceAll(" ", "+")
                        .replaceAll("\"", "");
                String url = "https://www.google.com/search?hl=en&q=" + cleanName;
                String text = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:25.0) Gecko/20100101 Firefox/25.0")
                        .get()
                        .text();
                // match only dates in expected format (ie. "Born: month_name day, year")
                Pattern pattern = Pattern.compile("Born: ([a-zA-Z]+ \\d{1,2}, \\d{4})");
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String textDate = matcher.group(1);
                    
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
                    Date date = dateFormat.parse(textDate);
                    return new Birthday(name, date, blogName);
                }
                return null;
            }

            private Birthday getBirthdayFromWikipedia(String name, String blogName) throws IOException, ParseException {
                String cleanName = StringUtils
                        .capitalize(name)
                        .replaceAll(" ", "_")
                        .replaceAll("\"", "");
                String url = "http://en.wikipedia.org/wiki/" + cleanName;
                Document document = Jsoup.connect(url).get();
                // protect against redirect
                if (document.title().toLowerCase(Locale.US).contains(name)) {
                    Elements el = document.select(".bday");
                    if (el.size() > 0) {
                        String birthDate = el.get(0).text();
                        return new Birthday(name, birthDate, blogName);
                    }
                }
                return null;
            }
            
            @Override
            protected String doInBackground(Void... params) {
                BirthdayDAO birthdayDAO = DBHelper.getInstance(context).getBirthdayDAO();
                List<String> names = birthdayDAO.getNameWithoutBirthDays(blogName);
                List<Birthday> birthdays = new ArrayList<Birthday>();
                int curr = 1;
                int size = names.size();

                
                for (final String name : names) {
                    publishProgress(name + " (" + curr + "/" + size + ")");
                    try {
                        Birthday birthday = getBirthdayFromGoogle(name, blogName);
                        if (birthday == null) {
                            birthday = getBirthdayFromWikipedia(name, blogName);
                        }
                        if (birthday != null) {
                            birthdays.add(birthday);
                        }
                    } catch (Exception e) {
                        // simply skip
                    }
                    ++curr;
                }
                // store to db and write the csv file
                SQLiteDatabase db = birthdayDAO.getDbHelper().getWritableDatabase();
                try {
                    db.beginTransaction();
                    String fileName = "birthdays-" + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()) + ".csv";
                    String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + fileName;
                    PrintWriter pw = new PrintWriter(path);
                    for (Birthday birthday : birthdays) {
                        pw.println(String.format("%1$d;%2$s;%3$s;%4$s",
                                1L,
                                birthday.getName(),
                                Birthday.ISO_DATE_FORMAT.format(birthday.getBirthDate()),
                                blogName));
                        birthdayDAO.insert(birthday);
                    }
                    pw.flush();
                    pw.close();
                    db.setTransactionSuccessful();
                } catch (Exception e) {
                    setError(e);
                } finally {
                    db.endTransaction();
                }
                return getContext().getString(R.string.import_progress_title, birthdays.size());
            }
            
            protected void onPostExecute(String message) {
                super.onPostExecute(null);
                
                if (getError() == null) {
                    DialogUtils.showSimpleMessageDialog(getContext(),
                            R.string.import_missing_birthdays_from_wikipedia_title,
                            message);
                }
            }
        }.execute();
    }

    public static void exportMissingBirthdaysToCSV(final Context context, final String exportPath, final String tumblrName) {
        try {
            new AbsProgressBarAsyncTask<Void, Void, Void>(context, context.getString(R.string.exporting_to_csv)) {
                @Override
                protected Void doInBackground(Void... voidParams) {
                    List<String> list = DBHelper.getInstance(context).getBirthdayDAO().getNameWithoutBirthDays(tumblrName);
                    try {
                        PrintWriter pw = new PrintWriter(exportPath);
                        for (String name : list) {
                            pw.println(name);
                        }
                        pw.flush();
                        pw.close();
                    } catch (Exception e) {
                        setError(e);
                    } finally {
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

	public interface ImportCompleteCallback {
		void complete();
	}
}
