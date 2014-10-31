package com.ternaryop.photoshelf.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.ternaryop.photoshelf.R;
import com.ternaryop.tumblr.Blog;
import com.ternaryop.utils.image.ScaleForDefaultDisplayTransformer;

public class BlogSpinnerAdapter extends ArrayAdapter<String> implements SpinnerAdapter {
    private static LayoutInflater inflater = null;

    public BlogSpinnerAdapter(Context context, List<String> blogNames) {
        super(context, 0, blogNames);
        inflater = LayoutInflater.from(context);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.blog_spinner_item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final String blogName = getItem(position);
        holder.title.setText(blogName);

        String imageUrl = Blog.getAvatarUrlBySize(blogName, 96);
        Picasso.with(getContext().getApplicationContext())
                .load(imageUrl)
                .placeholder(R.drawable.stub)
                .transform(new ScaleForDefaultDisplayTransformer(getContext().getApplicationContext()))
                .into(holder.image);

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    private class ViewHolder {
        final TextView title;
        final ImageView image;

        public ViewHolder(View vi) {
            title = (TextView)vi.findViewById(R.id.title1);
            image = (ImageView)vi.findViewById(R.id.image1);
        }
    }
}