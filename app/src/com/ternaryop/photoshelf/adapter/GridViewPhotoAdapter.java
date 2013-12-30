package com.ternaryop.photoshelf.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.fedorvlasov.lazylist.ImageLoader;
import com.ternaryop.photoshelf.R;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.widget.CheckableImageView;
 
public class GridViewPhotoAdapter extends ArrayAdapter<TumblrPhotoPost> {
    private ImageLoader imageLoader;
 
	public GridViewPhotoAdapter(Context context, String prefix) {
		super(context, 0);
        imageLoader = new ImageLoader(context.getApplicationContext(), prefix);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imageView;
		
		if (convertView == null) {
            int layoutWidth = getContext().getResources().getDimensionPixelSize(R.dimen.grid_layout_thumb_width);
            int layoutHeight = getContext().getResources().getDimensionPixelSize(R.dimen.grid_layout_thumb_height);
			// create the checkable view
			imageView = new CheckableImageView(getContext());
			imageView.setLayoutParams(new GridView.LayoutParams(layoutWidth, layoutHeight));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
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