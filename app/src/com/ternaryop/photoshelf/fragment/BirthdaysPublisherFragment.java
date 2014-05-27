package com.ternaryop.photoshelf.fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Environment;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.ternaryop.photoshelf.Constants;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.activity.TagPhotoBrowserActivity;
import com.ternaryop.photoshelf.adapter.GridViewPhotoAdapter;
import com.ternaryop.photoshelf.birthday.BirthdayUtils;
import com.ternaryop.photoshelf.db.Birthday;
import com.ternaryop.photoshelf.db.DBHelper;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.AbsProgressBarAsyncTask;
import com.ternaryop.utils.ImageUtils;

public class BirthdaysPublisherFragment extends AbsPhotoShelfFragment implements GridView.MultiChoiceModeListener, OnItemClickListener {
    private static final int PICK_IMAGE_REQUEST_CODE = 100;
    private static final String LOADER_PREFIX = "mediumThumb";

    private GridView gridView;
    private GridViewPhotoAdapter gridViewPhotoAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_birthdays_publisher, container, false);

        gridViewPhotoAdapter = new GridViewPhotoAdapter(getActivity(), LOADER_PREFIX);
        
        gridView = (GridView)rootView.findViewById(R.id.gridview);
        gridView.setAdapter(gridViewPhotoAdapter);
        gridView.setOnItemClickListener(this);
        gridView.setMultiChoiceModeListener(this);
        
        refresh();
        
        return rootView;
    }

    private void refresh() {
        new AbsProgressBarAsyncTask<Void, Void, List<Pair<Birthday, TumblrPhotoPost>>>(getActivity(), getString(R.string.shaking_images_title)) {

            @Override
            protected List<Pair<Birthday, TumblrPhotoPost>> doInBackground(Void... voidParams) {
                Calendar now = Calendar.getInstance(Locale.US);
                return BirthdayUtils.getPhotoPosts(getActivity(), now);
            }

            @Override
            protected void onPostExecute(List<Pair<Birthday, TumblrPhotoPost>> posts) {
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.birtdays_publisher, menu);
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
        if (select) {
            for (int i = 0; i < gridView.getCount(); i++) {
                gridView.setItemChecked(i, select);
            }
        } else {
            gridView.clearChoices();
        }
    }

    private void publish(final ActionMode mode, final boolean saveAsDraft) {
        new AbsProgressBarAsyncTask<Void, String, List<TumblrPhotoPost>>(getActivity(), "") {
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
                        createBirthdayPost(cakeImage, post, saveAsDraft);
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

    private void createBirthdayPost(Bitmap cakeImage, TumblrPhotoPost post, boolean saveAsDraft)
            throws IOException {
        String imageUrl = post.getClosestPhotoByWidth(400).getUrl();
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
        if (saveAsDraft) {
        	Tumblr.getSharedTumblr(getActivity()).draftPhotoPost(getBlogName(),
        			file,
        			getCaption(post),
        			"Birthday, " + post.getTags().get(0));
        } else {
        	Tumblr.getSharedTumblr(getActivity()).publishPhotoPost(getBlogName(),
        			file,
        			getCaption(post),
        			"Birthday, " + post.getTags().get(0));
        }
        file.delete();
    }

    private String getCaption(TumblrPost post) {
        DBHelper dbHelper = DBHelper
                .getInstance(getActivity().getApplicationContext());
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
    
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        TumblrPhotoPost post = gridViewPhotoAdapter.getItem(position).second;
        TagPhotoBrowserActivity.startPhotoBrowserActivityForResult(this, getBlogName(),
                post.getTags().get(0),
                PICK_IMAGE_REQUEST_CODE,
                false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE_REQUEST_CODE) {
            TumblrPhotoPost post = (TumblrPhotoPost) data.getSerializableExtra(Constants.EXTRA_POST);
            gridViewPhotoAdapter.updatePostByTag(post, true);
        }
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle(R.string.select_images);
        mode.setSubtitle(getResources().getQuantityString(R.plurals.selected_items, 1, 1));
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
            publish(mode, false);
            return true;
        case R.id.action_draft:
            publish(mode, true);
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
                list.add(gridViewPhotoAdapter.getItem(key).second);
            }
        }
        return list;
    }
    
    public void onDestroyActionMode(ActionMode mode) {
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position,
            long id, boolean checked) {
        int selectCount = gridView.getCheckedItemCount();
        mode.setSubtitle(getResources().getQuantityString(R.plurals.selected_items, 1, selectCount));
    }
}
