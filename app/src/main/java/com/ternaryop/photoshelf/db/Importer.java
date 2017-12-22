package com.ternaryop.photoshelf.db;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.annotation.PluralsRes;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.v2.files.WriteMode;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.birthday.BirthdayUtils;
import com.ternaryop.photoshelf.dropbox.DropboxManager;
import com.ternaryop.photoshelf.importer.CSVIterator;
import com.ternaryop.photoshelf.importer.CSVIterator.CSVBuilder;
import com.ternaryop.photoshelf.importer.PostRetriever;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.IOUtils;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class Importer {
    public static final String CSV_FILE_NAME = "tags.csv";
    public static final String TITLE_PARSER_FILE_NAME = "titleParser.json";
    public static final String BIRTHDAYS_FILE_NAME = "birthdays.csv";
    public static final String MISSING_BIRTHDAYS_FILE_NAME = "missingBirthdays.csv";
    public static final String TOTAL_USERS_FILE_NAME = "totalUsers.csv";

    private static final SimpleDateFormat ISO_8601_DATE = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final ObservableTransformer<List<TumblrPost>, List<TumblrPost>> DO_NOTHING_TRANSFORMER = upstream -> upstream;

    private final Context context;
    private final DropboxManager dropboxManager;

    public Importer(final Context context) {
        this(context, null);
    }

    public Importer(final Context context, DropboxManager dropboxManager) {
        this.context = context;
        this.dropboxManager = dropboxManager;
    }

    public Observable<Integer> importPostsFromCSV(final String importPath) throws IOException {
        return new DbImport<>(DBHelper.getInstance(context).getBulkImportPostDAOWrapper())
                .importer(new CSVIterator<>(importPath, new PostTagCSVBuilder()), true);
    }

    public int exportPostsToCSV(final String exportPath) throws Exception {
        try (Cursor c = DBHelper.getInstance(context).getPostTagDAO().cursorExport()) {
            PrintWriter pw = fastPrintWriter(exportPath);
            int count = 0;
            while (c.moveToNext()) {
                pw.println(String.format(Locale.US, "%1$d;%2$s;%3$s;%4$d;%5$d",
                        c.getLong(c.getColumnIndex(PostTagDAO._ID)),
                        c.getString(c.getColumnIndex(PostTagDAO.TUMBLR_NAME)),
                        c.getString(c.getColumnIndex(PostTagDAO.TAG)),
                        c.getLong(c.getColumnIndex(PostTagDAO.PUBLISH_TIMESTAMP)),
                        c.getLong(c.getColumnIndex(PostTagDAO.SHOW_ORDER))
                ));
                ++count;
            }
            pw.flush();
            pw.close();

            copyFileToDropbox(exportPath);
            return count;
        }
    }

    /**
     * Create the Observable to import newer posts and inserted them into database
     * @param blogName the blog name
     * @param transformer used to set the schedulers to used
     * @param textView the textView used to show the progress, can be null
     * @return the Observable
     */
    public Observable<List<TumblrPost>> importFromTumblr(final String blogName,
                                                @Nullable final ObservableTransformer<List<TumblrPost>, List<TumblrPost>> transformer,
                                                @Nullable final TextView textView) {
        PostTag post = DBHelper.getInstance(context).getPostTagDAO().findLastPublishedPost(blogName);

        if (textView != null) {
            textView.setText(context.getString(R.string.start_import_title));
        }

        final PostRetriever postRetriever = new PostRetriever(context);

        return postRetriever
                .readPhotoPosts(blogName, post == null ? 0 : post.getPublishTimestamp())
                .compose(transformer == null ? DO_NOTHING_TRANSFORMER : transformer)
                .doOnNext(posts -> updateText(textView, postRetriever.getTotal(), R.plurals.posts_read_count))
                .observeOn(Schedulers.computation())
                .flatMap(postTags -> importToDB(postTags, textView));
    }

    private Observable<List<TumblrPost>> importToDB(List<TumblrPost> postTags, @Nullable final TextView textView) {
        return new DbImport<>(DBHelper.getInstance(context).getBulkImportPostDAOWrapper())
                .importer(PostTag.from(postTags).iterator(), false)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(total -> updateText(textView, total, R.plurals.imported_items))
                .takeLast(1)
                .flatMap(total -> Observable.just(postTags));
    }

    private void updateText(TextView textView, int total, @PluralsRes int id) {
        if (textView != null) {
            String message = context.getResources().getQuantityString(
                    id,
                    total,
                    total);
            textView.setText(message);
        }
    }

    public static ObservableTransformer<List<TumblrPost>, List<TumblrPost>> schedulers() {
        return upstream -> upstream
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public void importFile(String importPath, String contextFileName) {
        try {
            copyFileToContext(importPath, contextFileName);
            Toast.makeText(context, context.getString(R.string.importSuccess), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void copyFileToContext(String fullPath, String contextFileName) throws IOException {
        try (InputStream in = new FileInputStream(fullPath); OutputStream out = context.openFileOutput(contextFileName, 0)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    public Observable<Integer> importBirthdays(final String importPath) throws IOException {
        return new DbImport<>(DBHelper.getInstance(context).getBirthdayDAO())
                .importer(new CSVIterator<>(importPath, fields -> {
                    // id is skipped
                    return new Birthday(fields[1], fields[2], fields[3]);
                }), true);
    }

    public int exportBirthdaysToCSV(final String exportPath) throws Exception {
        SQLiteDatabase db = DBHelper.getInstance(context).getReadableDatabase();
        try (Cursor c = db.query(BirthdayDAO.TABLE_NAME, null, null, null, null, null, BirthdayDAO.NAME)) {
            PrintWriter pw = fastPrintWriter(exportPath);
            long id = 1;
            int count = 0;
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
                ++count;
            }
            pw.flush();
            pw.close();

            copyFileToDropbox(exportPath);
            return count;
        }
    }

    public Observable<ImportProgressInfo<Birthday>> importMissingBirthdaysFromWeb(final String blogName) {
        BirthdayDAO birthdayDAO = DBHelper.getInstance(context).getBirthdayDAO();
        List<String> names = birthdayDAO.getNameWithoutBirthDays(blogName);
        SimpleImportProgressInfo<Birthday> info = new SimpleImportProgressInfo<>(names.size());
        Iterator<String> nameIterator = names.iterator();

        return Observable.generate(emitter -> {
            if (nameIterator.hasNext()) {
                ++info.progress;
                String name = nameIterator.next();
                try {
                    Birthday birthday = BirthdayUtils.searchBirthday(context, name, blogName);
                    if (birthday != null) {
                        info.items.add(birthday);
                    }
                } catch (Exception e) {
                    // simply skip
                }
                emitter.onNext(info);
            } else {
                saveBirthdaysToDatabase(info.items);
                saveBirthdaysToFile(info.items);
                emitter.onNext(info);
                emitter.onComplete();
            }
        });
    }

    private void saveBirthdaysToDatabase(List<Birthday> birthdays) {
        BirthdayDAO birthdayDAO = DBHelper.getInstance(context).getBirthdayDAO();
        SQLiteDatabase db = birthdayDAO.getDbHelper().getWritableDatabase();
        try {
            db.beginTransaction();
            for (Birthday birthday : birthdays) {
                birthdayDAO.insert(birthday);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void saveBirthdaysToFile(List<Birthday> birthdays) throws IOException {
        String fileName = "birthdays-" + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()) + ".csv";
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + fileName;

        try (PrintWriter pw = fastPrintWriter(path)) {
            for (Birthday birthday : birthdays) {
                pw.println(String.format(Locale.US, "%1$d;%2$s;%3$s;%4$s",
                        1L,
                        birthday.getName(),
                        Birthday.ISO_DATE_FORMAT.format(birthday.getBirthDate()),
                        birthday.getTumblrName()));
            }
            pw.flush();
        }
    }

    public int exportMissingBirthdaysToCSV(final String exportPath, final String tumblrName) throws Exception {
        try (PrintWriter pw = fastPrintWriter(exportPath)) {
            List<String> list = DBHelper.getInstance(context).getBirthdayDAO().getNameWithoutBirthDays(tumblrName);
            for (String name : list) {
                pw.println(name);
            }
            pw.flush();

            copyFileToDropbox(exportPath);
            return list.size();
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

    private void copyFileToDropbox(final String exportPath) throws Exception {
        if (dropboxManager.isLinked()) {
            File exportFile = new File(exportPath);
            try (InputStream in = new FileInputStream(exportFile)) {
                // Autorename = true and Mode = OVERWRITE allow to overwrite the file if it exists or create it if doesn't
                dropboxManager.getClient()
                        .files()
                        .uploadBuilder(dropboxPath(exportFile))
                        .withAutorename(true)
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(in);
            }
        }
    }

    private String dropboxPath(File exportFile) {
        return "/" + exportFile.getName();
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

    public static String getTitleParserPath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + TITLE_PARSER_FILE_NAME;
    }

    public static String getBirthdaysPath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + BIRTHDAYS_FILE_NAME;
    }

    public static String getTotalUsersPath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + TOTAL_USERS_FILE_NAME;
    }

    public void syncExportTotalUsersToCSV(final String exportPath, final String blogName) throws Exception {
        // do not overwrite the entire file but append to the existing one
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(exportPath, true)))) {
            String time = ISO_8601_DATE.format(Calendar.getInstance().getTimeInMillis());
            long totalUsers = Tumblr.getSharedTumblr(context)
                    .getFollowers(blogName, null, null)
                    .getTotalUsers();
            pw.println(time + ";" + blogName + ";" + totalUsers);
            pw.flush();
            copyFileToDropbox(exportPath);
        }
    }

    /**
     * Create a PrintWriter disabling the flush to speedup writing
     * @param path the destination path
     * @return the created PrintWriter
     * @throws IOException the thrown exception
     */
    public static PrintWriter fastPrintWriter(String path) throws IOException {
        return new PrintWriter(new BufferedWriter(new FileWriter(path)), false);
    }

    public interface ImportProgressInfo<T> {
        int getProgress();
        int getMax();
        Collection<T> getItems();
    }

    public class SimpleImportProgressInfo<T> implements ImportProgressInfo<T> {
        int progress;
        final int max;
        final ArrayList<T> items;

        public SimpleImportProgressInfo(int max) {
            this.max = max;
            items = new ArrayList<>();
        }

        @Override
        public int getProgress() {
            return progress;
        }

        @Override
        public int getMax() {
            return max;
        }

        @Override
        public Collection<T> getItems() {
            return items;
        }

        @Override
        public String toString() {
            return "progress " + progress + " max " + max + " items " + items;
        }
    }
}
