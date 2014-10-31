package com.ternaryop.photoshelf.adapter;

import java.util.Calendar;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.db.Birthday;
import com.ternaryop.tumblr.TumblrPhotoPost;

public class GridViewPhotoAdapter extends ArrayAdapter<Pair<Birthday, TumblrPhotoPost>> {
    private final LayoutInflater inflater;

    public GridViewPhotoAdapter(Context context) {
        super(context, 0);
        inflater = LayoutInflater.from(context);
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
        Picasso.with(getContext().getApplicationContext())
                .load(post.getClosestPhotoByWidth(250).getUrl())
                .placeholder(R.drawable.stub)
                .into(holder.thumbImage);

        return convertView;
    }

    public void updatePostByTag(TumblrPhotoPost newPost, boolean notifyChange) {
        String name = newPost.getTags().get(0);

        for (int i = 0; i < getCount(); i++) {
            Pair<Birthday, TumblrPhotoPost> item = getItem(i);
            TumblrPhotoPost post = item.second;
            if (post.getTags().get(0).equalsIgnoreCase(name)) {
                remove(item);
                insert(Pair.create(item.first, newPost), i);

                if (notifyChange) {
                    notifyDataSetChanged();
                }
                break;
            }
        }
    }

    private class ViewHolder {
        final TextView caption;
        final ImageView thumbImage;

        public ViewHolder(View vi) {
            caption = (TextView)vi.findViewById(R.id.caption);
            thumbImage = (ImageView)vi.findViewById(R.id.thumbnail_image);
        }
    }
}