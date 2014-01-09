package com.ternaryop.photoshelf.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.fedorvlasov.lazylist.ImageLoader;
import com.ternaryop.photoshelf.R;
import com.ternaryop.tumblr.TumblrPhotoPost;
 
public class GridViewPhotoAdapter extends ArrayAdapter<TumblrPhotoPost> {
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
        TumblrPhotoPost post = getItem(position);
        holder.caption.setText(post.getTags().get(0));
        imageLoader.displayImage(post.getClosestPhotoByWidth(250).getUrl(), holder.thumbImage);

        return convertView;
	}

	public void updatePostByTag(TumblrPhotoPost newPost, boolean notifyChange) {
	    String name = newPost.getTags().get(0);
	    
	    for (int i = 0; i < getCount(); i++) {
	        TumblrPhotoPost post = getItem(i);
	        if (post.getTags().get(0).equalsIgnoreCase(name)) {
	            remove(post);
	            insert(newPost, i);
	            
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