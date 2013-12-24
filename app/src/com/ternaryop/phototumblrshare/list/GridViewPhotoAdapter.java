package com.ternaryop.phototumblrshare.list;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.fedorvlasov.lazylist.ImageLoader;
import com.ternaryop.phototumblrshare.widget.CheckableImageView;
import com.ternaryop.tumblr.TumblrPhotoPost;
 
public class GridViewPhotoAdapter extends ArrayAdapter<TumblrPhotoPost> {
    private ImageLoader imageLoader;
 
	public GridViewPhotoAdapter(Context context, String prefix) {
		super(context, 0);
        imageLoader = new ImageLoader(context.getApplicationContext(), prefix);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imageView;
		
		if (convertView == null) {
			// create the checkable view
			imageView = new CheckableImageView(getContext());
			imageView.setLayoutParams(new GridView.LayoutParams(250, 250));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 2, 2);
		} else {
			imageView = (ImageView)convertView;
		}
        imageLoader.displayImage(getItem(position).getClosestPhotoByWidth(250).getUrl(), imageView);

        return imageView;
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
}