package com.ternaryop.phototumblrshare.activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Environment;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.birthday.BirthdayUtils;
import com.ternaryop.phototumblrshare.db.Birthday;
import com.ternaryop.phototumblrshare.db.DBHelper;
import com.ternaryop.phototumblrshare.list.GridViewPhotoAdapter;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.AbsProgressBarAsyncTask;
import com.ternaryop.utils.ImageUtils;

public class BirthdaysPublisherActivity extends PhotoTumblrActivity implements GridView.MultiChoiceModeListener, OnItemClickListener {
    private static final int PICK_IMAGE_REQUEST_CODE = 100;
    private static final String LOADER_PREFIX = "mediumThumb";

    private GridView gridView;
    private GridViewPhotoAdapter gridViewPhotoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_birthdays_publisher);

        gridViewPhotoAdapter = new GridViewPhotoAdapter(this, LOADER_PREFIX);
        
        gridView = (GridView)findViewById(R.id.gridview);
        gridView.setAdapter(gridViewPhotoAdapter);
        gridView.setOnItemClickListener(this);
        gridView.setMultiChoiceModeListener(this);
        
        refresh();
    }

    private void refresh() {
        new AbsProgressBarAsyncTask<Void, Void, List<TumblrPhotoPost>>(this, getString(R.string.shaking_images_title)) {

            @Override
            protected List<TumblrPhotoPost> doInBackground(Void... voidParams) {
                Calendar now = Calendar.getInstance(Locale.US);
                return BirthdayUtils.getPhotoPosts(BirthdaysPublisherActivity.this, now);
            }

            protected void onPostExecute(List<TumblrPhotoPost> posts) {
                super.onPostExecute(null);
                if (getError() == null) {
                    gridViewPhotoAdapter.clear();
                    gridViewPhotoAdapter.addAll(posts);
                    gridViewPhotoAdapter.notifyDataSetChanged();
                }
            }
        }.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.birtdays_publisher, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_refresh:
            refresh();
            return true;
        case R.id.action_selectall:
            selectAll(true);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void selectAll(boolean select) {
        for (int i = 0; i < gridView.getCount(); i++) {
            gridView.setItemChecked(i, select);
        }
    }

    private void publish(final ActionMode mode) {
        new AbsProgressBarAsyncTask<Void, String, List<TumblrPhotoPost>>(this, "") {
            @Override
            protected void onProgressUpdate(String... values) {
                getProgressDialog().setMessage(values[0]);
            }

            @Override
            protected List<TumblrPhotoPost> doInBackground(Void... voidParams) {
                try {
                    InputStream is = getContext().getAssets().open("cake.png");
                    Bitmap cakeImage = BitmapFactory.decodeStream(is);
                    is.close();

                    for (TumblrPhotoPost post : getCheckedPosts()) {
                        String name = post.getTags().get(0);
                        publishProgress(getContext().getString(R.string.sending_cake_title, name));
                        createBirthdayPost(cakeImage, post);
                    }
                } catch (Exception e) {
                    setError(e);
                }
                return null;
            }
            
            protected void onPostExecute(List<TumblrPhotoPost> result) {
                super.onPostExecute(null);
                if (getError() == null) {
                    mode.finish();
                }
            }
        }.execute();
    }

    private void createBirthdayPost(Bitmap cakeImage, TumblrPhotoPost post)
            throws IOException {
        String imageUrl = post.getFirstPhotoAltSize().get(TumblrPhotoPost.IMAGE_INDEX_400_PIXELS).getUrl();
        Bitmap image = ImageUtils.readImage(imageUrl);
        
        final int IMAGE_SEPARATOR_HEIGHT = 10;
        int canvasWidth = image.getWidth();
        int canvasHeight = cakeImage.getHeight() + IMAGE_SEPARATOR_HEIGHT + image.getHeight();
        
        Config config = image.getConfig() == null ? Bitmap.Config.ARGB_8888 : image.getConfig();
        Bitmap destBmp = Bitmap.createBitmap(canvasWidth, canvasHeight, config);
        Canvas canvas = new Canvas(destBmp);
        
        canvas.drawBitmap(cakeImage, (image.getWidth() - cakeImage.getWidth()) / 2, 0, null);
        canvas.drawBitmap(image, 0, cakeImage.getHeight() + IMAGE_SEPARATOR_HEIGHT, null);
        File file = saveImage(destBmp, "birth-" + post.getTags().get(0));
        Tumblr.getSharedTumblr(this).draftPhotoPost(getBlogName(),
                file,
                getCaption(post),
                "Birthday, " + post.getTags().get(0));
        file.delete();
    }

    private String getCaption(TumblrPost post) {
        DBHelper dbHelper = DBHelper
                .getInstance(getApplicationContext());
        String name = post.getTags().get(0);
        Birthday birthDay = dbHelper
                .getBirthdayDAO()
                .getBirthdayByName(name, getBlogName());
        Calendar birthDate = Calendar.getInstance();
        birthDate.setTime(birthDay.getBirthDate());
        int age = Calendar.getInstance().get(Calendar.YEAR) - birthDate.get(Calendar.YEAR);
        // caption must not be localized
        String caption = "Happy %1$dth Birthday, %2$s!!";
        return String.format(caption, age, name);
    }
    
    private File saveImage(Bitmap image, String fileName) throws IOException {
        File imageFile = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), fileName + ".png");
        OutputStream fout = new FileOutputStream(imageFile);

        image.compress(Bitmap.CompressFormat.PNG, 100, fout);

        fout.flush();
        fout.close();
        
        return imageFile;
    }
    
    public static void startBirthdayPublisherActivity(Context context, String blogName) {
        Intent intent = new Intent(context, BirthdaysPublisherActivity.class);
        Bundle bundle = new Bundle();

        bundle.putString(BLOG_NAME, blogName);
        intent.putExtras(bundle);

        context.startActivity(intent);
    }

    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        TumblrPhotoPost post = gridViewPhotoAdapter.getItem(position);
        TagPhotoBrowserActivity.startPhotoBrowserActivityForResult(this, getBlogName(),
                post.getTags().get(0),
                PICK_IMAGE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_REQUEST_CODE) {
            TumblrPhotoPost post = (TumblrPhotoPost) data.getSerializableExtra("post");
            gridViewPhotoAdapter.updatePostByTag(post, true);
        }
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle(R.string.select_images);
        mode.setSubtitle(R.string.selected_singular);
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.birtdays_publisher_action, menu);
        return true;
    }
    
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_publish:
            publish(mode);
            return true;
        default:
            return false;
        }
    }

    private List<TumblrPhotoPost> getCheckedPosts() {
        SparseBooleanArray checkedItemPositions = gridView.getCheckedItemPositions();
        ArrayList<TumblrPhotoPost> list = new ArrayList<TumblrPhotoPost>();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            int key = checkedItemPositions.keyAt(i);
            if (checkedItemPositions.get(key)) {
                list.add(gridViewPhotoAdapter.getItem(key));
            }
        }
        return list;
    }
    
    public void onDestroyActionMode(ActionMode mode) {
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position,
            long id, boolean checked) {
        int selectCount = gridView.getCheckedItemCount();
        mode.setSubtitle(getString(R.string.selected_singular, selectCount));
    }
}
