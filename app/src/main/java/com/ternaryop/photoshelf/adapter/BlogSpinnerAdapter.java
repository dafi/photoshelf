package com.ternaryop.photoshelf.adapter;

import java.util.List;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.ternaryop.lazyimageloader.ImageLoader;
import com.ternaryop.photoshelf.R;
import com.ternaryop.tumblr.Blog;

public class BlogSpinnerAdapter extends ArrayAdapter<String> implements SpinnerAdapter {
    private static LayoutInflater inflater = null;
    private final ImageLoader imageLoader;

    public BlogSpinnerAdapter(Context context, String prefix, List<String> blogNames) {
        super(context, 0, blogNames);
        inflater = LayoutInflater.from(context);
        imageLoader = new ImageLoader(context.getApplicationContext(), prefix, R.drawable.stub);
    }

    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
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
        imageLoader.displayImage(imageUrl, holder.image, true);

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
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