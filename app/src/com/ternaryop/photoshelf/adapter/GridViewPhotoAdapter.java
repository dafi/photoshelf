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

import com.fedorvlasov.lazylist.ImageLoader;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.db.Birthday;
import com.ternaryop.tumblr.TumblrPhotoPost;
 
public class GridViewPhotoAdapter extends ArrayAdapter<Pair<Birthday, TumblrPhotoPost>> {
    private ImageLoader imageLoader;
 
	public GridViewPhotoAdapter(Context context, String prefix) {
		super(context, 0);
        imageLoader = new ImageLoader(context.getApplicationContext(), prefix);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
	    ViewHolder holder;
	    
		if (convertView == null) {
	        LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        convertView = inflater.inflate(R.layout.gridview_photo_item, null);
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
        TextView caption;
        ImageView thumbImage;

        public ViewHolder(View vi) {
            caption = (TextView)vi.findViewById(R.id.caption);
            thumbImage = (ImageView)vi.findViewById(R.id.thumbnail_image);
        }
    }
}