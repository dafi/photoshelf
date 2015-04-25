package com.ternaryop.photoshelf.adapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ternaryop.lazyimageloader.ImageLoader;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.db.Birthday;
import com.ternaryop.tumblr.TumblrPhotoPost;

public class GridViewPhotoAdapter extends BaseAdapter implements View.OnClickListener {
    private final ImageLoader imageLoader;
    private final LayoutInflater inflater;
    private boolean showButtons;

    private OnPhotoBrowseClick onPhotoBrowseClick;
    private final ArrayList<Pair<Birthday, TumblrPhotoPost>> items;

    public GridViewPhotoAdapter(Context context, String prefix) {
        imageLoader = new ImageLoader(context.getApplicationContext(), prefix, R.drawable.stub);
        inflater = LayoutInflater.from(context);
        items = new ArrayList<>();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.gridview_photo_item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Pair<Birthday, TumblrPhotoPost> item = getItem(position);
        Birthday birthday = item.first;
        TumblrPhotoPost post = item.second;
        Calendar c = Calendar.getInstance();
        int age = c.get(Calendar.YEAR) - birthday.getBirthDateCalendar().get(Calendar.YEAR);
        holder.caption.setText(post.getTags().get(0) + ", " + age);
        imageLoader.displayImage(post.getClosestPhotoByWidth(250).getUrl(), holder.thumbImage);

        if (showButtons && onPhotoBrowseClick != null) {
            holder.showImageAction.setOnClickListener(this);
            holder.showImageAction.setTag(position);
        }
        holder.showImageAction.setVisibility(showButtons ? View.VISIBLE : View.INVISIBLE);
        holder.bgAction.setVisibility(showButtons ? View.VISIBLE : View.INVISIBLE);

        return convertView;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Pair<Birthday, TumblrPhotoPost> getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void clear() {
        items.clear();
    }

    public void addAll(List<Pair<Birthday, TumblrPhotoPost>> posts) {
        items.addAll(posts);
    }

    public void updatePostByTag(TumblrPhotoPost newPost, boolean notifyChange) {
        String name = newPost.getTags().get(0);

        for (int i = 0; i < getCount(); i++) {
            Pair<Birthday, TumblrPhotoPost> item = getItem(i);
            TumblrPhotoPost post = item.second;
            if (post.getTags().get(0).equalsIgnoreCase(name)) {
                items.set(i, Pair.create(item.first, newPost));

                if (notifyChange) {
                    notifyDataSetChanged();
                }
                break;
            }
        }
    }

    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.ic_show_image_action:
                onPhotoBrowseClick.onThumbnailImageClick((Integer) v.getTag());
                break;
        }
    }

    public void setOnPhotoBrowseClick(OnPhotoBrowseClick onPhotoBrowseClick) {
        this.onPhotoBrowseClick = onPhotoBrowseClick;
    }

    public boolean isShowButtons() {
        return showButtons;
    }

    public void setShowButtons(boolean showButtons) {
        this.showButtons = showButtons;
    }

    private class ViewHolder {
        final TextView caption;
        final ImageView thumbImage;
        final ImageView bgAction;
        final ImageView showImageAction;

        public ViewHolder(View vi) {
            caption = (TextView)vi.findViewById(R.id.caption);
            thumbImage = (ImageView)vi.findViewById(R.id.thumbnail_image);
            bgAction = (ImageView)vi.findViewById(R.id.bg_actions);
            showImageAction = (ImageView)vi.findViewById(R.id.ic_show_image_action);
        }
    }
}