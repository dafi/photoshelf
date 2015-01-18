package com.ternaryop.photoshelf.db;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.birthday.BirthdayUtils;
import com.ternaryop.photoshelf.importer.CSVIterator;
import com.ternaryop.photoshelf.importer.CSVIterator.CSVBuilder;
import com.ternaryop.photoshelf.importer.PostRetriever;
import com.ternaryop.tumblr.Callback;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.AbsProgressIndicatorAsyncTask;
import com.ternaryop.utils.DialogUtils;
import com.ternaryop.utils.IOUtils;

public class Importer {
    private static final String CSV_FILE_NAME = "tags.csv";
    private static final String DOM_FILTERS_FILE_NAME = "domSelectors.json";
    private static final String BIRTHDAYS_FILE_NAME = "birthdays.csv";
    private static final String MISSING_BIRTHDAYS_FILE_NAME = "missingBirthdays.csv";
    private static final String TOTAL_USERS_FILE_NAME = "totalUsers.csv";

    private static final SimpleDateFormat ISO_8601_DATE = new SimpleDateFormat("yyyy-MM-dd");

    private final Context context;
    private final DbxAccountManager dropboxManager;

    public Importer(final Context context, DbxAccountManager dropboxManager) {
        this.context = context;
        this.dropboxManager = dropboxManager;
    }

    public void importPostsFromCSV(final String importPath) {
        try {
            new DbImportAsyncTask<PostTag>(context,
                    new CSVIterator<PostTag>(importPath, new PostTagCSVBuilder()),
                    DBHelper.getInstance(context).getPostTagDAO(),
                    true).execute();
//            if (dropboxManager.hasLinkedAccount()) {
//                DbxFileSystem dbxFs = DbxFileSystem.forAccount(dropboxManager.getLinkedAccount());
//                File exportFile = new File(importPath);
//                final DbxFile file = dbxFs.open(new DbxPath(exportFile.getName()));
//                new DbImportAsyncTask<PostTag>(context,
//                        new CSVIterator<PostTag>(importPath, new PostTagCSVBuilder()),
//                        DBHelper.getInstance(context).getPostTagDAO(),
//                        true) {
//                    protected void onPostExecute(Void result) {
//                        super.onPostExecute(result);
//                        file.close();
//                    }
//                }.execute();
//            }
        } catch (Exception error) {
            DialogUtils.showErrorDialog(context, error);
        }
    }

    public void exportPostsToCSV(final String exportPath) {
        try {
            new AbsProgressIndicatorAsyncTask<Void, Void, Void>(context, context.getString(R.string.exporting_to_csv)) {
                @Override
                protected Void doInBackground(Void... voidParams) {
                    try {
                        syncExportPostsToCSV(exportPath);
                    } catch (Exception e) {
                        setError(e);
                    }

                    return null;
                }
            }.execute();
        } catch (Exception error) {
            DialogUtils.showErrorDialog(context, error);
        }
    }

    public void syncExportPostsToCSV(final String exportPath) throws Exception {
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

            copyFileToDropbox(exportPath);
        } finally {
            c.close();
        }
    }

    public void importFromTumblr(final String blogName) {
        importFromTumblr(blogName, null, null);
    }

    public PostRetriever importFromTumblr(final String blogName, final TextView textView, final ImportCompleteCallback callback) {
        PostTag post = DBHelper.getInstance(context).getPostTagDAO().findLastPublishedPost(blogName);
        long publishTimestamp = post == null ? 0 : post.getPublishTimestamp();
        Callback<List<TumblrPost>> wrapperCallback = new Callback<List<TumblrPost>>() {

            @Override
            public void failure(Exception error) {
                if (error != null) {
                    DialogUtils.showErrorDialog(context, error);
                }
            }

            @Override
            public void complete(List<TumblrPost> allPosts) {
                if (allPosts.isEmpty()) {
                    if (callback != null) {
                        callback.complete();
                    }
                    return;
                }
                List<PostTag> allPostTags = new ArrayList<PostTag>();
                for (TumblrPost tumblrPost : allPosts) {
                    allPostTags.addAll(PostTag.postTagsFromTumblrPost(tumblrPost));
                }
                new DbImportAsyncTask<PostTag>(context,
                        textView,
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
        };
        PostRetriever postRetriever = new PostRetriever(context, publishTimestamp, textView, wrapperCallback);
        postRetriever.readPhotoPosts(blogName, null);
        return postRetriever;
    }

    public void importDOMFilters(String importPath) {
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
            if (in != null) try { in.close(); } catch (Exception ignored) {}
            if (out != null) try { out.close(); } catch (Exception ignored) {}
        }
    }

    public void importBirthdays(final String importPath) {
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

    public void exportBirthdaysToCSV(final String exportPath) {
        try {
            new AbsProgressIndicatorAsyncTask<Void, Void, Void>(context, context.getString(R.string.exporting_to_csv)) {
                @Override
                protected Void doInBackground(Void... voidParams) {
                    try {
                        syncExportBirthdaysToCSV(exportPath);
                    } catch (Exception e) {
                        setError(e);
                    }

                    return null;
                }
            }.execute();
        } catch (Exception error) {
            DialogUtils.showErrorDialog(context, error);
        }
    }

    public void syncExportBirthdaysToCSV(final String exportPath) throws Exception {
        SQLiteDatabase db = DBHelper.getInstance(context).getReadableDatabase();
        Cursor c = db.query(BirthdayDAO.TABLE_NAME, null, null, null, null, null, BirthdayDAO.NAME);
        try {
            PrintWriter pw = new PrintWriter(exportPath);
            long id = 1;
            while (c.moveToNext()) {
                String birthdate = c.getString(c.getColumnIndex(BirthdayDAO.BIRTH_DATE));
                // ids are recomputed
                String csvLine = String.format(Locale.US,
                        "%1$d;%2$s;%3$s;%4$s",
                        id++,
                        c.getString(c.getColumnIndex(BirthdayDAO.NAME)),
                        birthdate == null ? "" : birthdate,
                        c.getString(c.getColumnIndex(BirthdayDAO.TUMBLR_NAME))
                );
                pw.println(csvLine);
            }
            pw.flush();
            pw.close();

            copyFileToDropbox(exportPath);
        } finally {
            c.close();
        }
    }

    public void importMissingBirthdaysFromWikipedia(final String blogName) {
        new AbsProgressIndicatorAsyncTask<Void, String, String>(context,
                context.getString(R.string.import_missing_birthdays_from_wikipedia_title)) {
            @Override
            protected void onProgressUpdate(String... values) {
                setProgressMessage(values[0]);
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
                        Birthday birthday = BirthdayUtils.searchBirthday(name, blogName);
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

                if (!hasError()) {
                    DialogUtils.showSimpleMessageDialog(getContext(),
                            R.string.import_missing_birthdays_from_wikipedia_title,
                            message);
                }
            }
        }.execute();
    }

    public void exportMissingBirthdaysToCSV(final String exportPath, final String tumblrName) {
        try {
            new AbsProgressIndicatorAsyncTask<Void, Void, Void>(context, context.getString(R.string.exporting_to_csv)) {
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

                        copyFileToDropbox(exportPath);
                    } catch (Exception e) {
                        setError(e);
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

    private void copyFileToDropbox(final String exportPath)
            throws IOException {
        if (dropboxManager.hasLinkedAccount()) {
            DbxFileSystem dbxFs = DbxFileSystem.forAccount(dropboxManager.getLinkedAccount());
            File exportFile = new File(exportPath);
            DbxPath dbxPath = new DbxPath(exportFile.getName());
            DbxFile file = null;

            // This will block until we can sync metadata the first time
            dbxFs.listFolder(DbxPath.ROOT);

            try {
                if (dbxFs.exists(dbxPath)) {
                    file = dbxFs.open(dbxPath);
                    // sync file so it's updated then remove it
                    file.readString();
                    file.getSyncStatus();
                    file.update();
                } else {
                    file = dbxFs.create(dbxPath);
                }
                file.writeFromExistingFile(exportFile, false);
            } finally {
                if (file != null) {
                    try { file.close(); } catch (Exception ignored) {}
                }
            }
        }
    }

    /**
     * If necessary rename exportPath
     * @param exportPath the export path to use as prefix
     * @return the original passed parameter if exportPath doesn't exist or a new unique path
     */
    public static String getExportPath(String exportPath) {
        String newPath = IOUtils.generateUniqueFileName(exportPath);
        if (!newPath.equals(exportPath)) {
            new File(exportPath).renameTo(new File(newPath));
        }
        return exportPath;
    }

    public static String getMissingBirthdaysPath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + MISSING_BIRTHDAYS_FILE_NAME;
    }

    public static String getPostsPath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + CSV_FILE_NAME;
    }

    public static String getDOMFiltersPath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + DOM_FILTERS_FILE_NAME;
    }

    public static String getBirthdaysPath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + BIRTHDAYS_FILE_NAME;
    }

    public static String getTotalUsersPath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + TOTAL_USERS_FILE_NAME;
    }

    public void syncExportTotalUsersToCSV(final String exportPath, final String blogName) throws IOException {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(exportPath, true)));
            String time = ISO_8601_DATE.format(Calendar.getInstance().getTimeInMillis());
            long totalUsers = Tumblr.getSharedTumblr(context)
                    .getFollowers(blogName, null, null)
                    .getTotalUsers();
            pw.println(time + ";" + blogName + ";" + totalUsers);
            pw.flush();
            copyFileToDropbox(exportPath);
        } finally {
            if (pw != null) try { pw.close(); } catch (Exception ignored) {}
        }
    }

}
