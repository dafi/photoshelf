package com.ternaryop.phototumblrshare.list;

import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.fedorvlasov.lazylist.ImageLoader;
import com.ternaryop.phototumblrshare.R;
import com.ternaryop.tumblr.TumblrAltSize;
 
public class PhotoAdapter extends BaseAdapter implements View.OnClickListener {
    private Activity activity;
    private static LayoutInflater inflater = null;
    public ImageLoader imageLoader;
    private List<PhotoSharePost> items = Collections.emptyList();
	private OnPhotoBrowseClick onPhotoBrowseClick;
 
    public PhotoAdapter(Activity a, String prefix) {
        activity = a;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        imageLoader = new ImageLoader(activity.getApplicationContext(), prefix);
    }
 
    public int getCount() {
        return items.size();
    }
 
    public Object getItem(int position) {
        return items.get(position);
    }
 
    public long getItemId(int position) {
        return position;
    }
 
    public void setOnPhotoBrowseClick(OnPhotoBrowseClick onPhotoBrowseClick) {
		this.onPhotoBrowseClick = onPhotoBrowseClick;
    }
    
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        if (convertView == null) {
            vi = inflater.inflate(R.layout.list_row, null);
        }

        final PhotoSharePost post = items.get(position);

        switch (post.getScheduleTimeType()) {
        	case POST_PUBLISH_NEVER:
                vi.setBackgroundResource(R.drawable.list_selector_post_never);
                break;
        	case POST_PUBLISH_FUTURE:
        		vi.setBackgroundResource(R.drawable.list_selector_post_future);
        		break;
        	default:
                vi.setBackgroundResource(R.drawable.list_selector);
                break;
        }

        TextView title = (TextView)vi.findViewById(R.id.title_textview);
        TextView timeDesc = (TextView)vi.findViewById(R.id.time_desc);
        ImageView thumbImage = (ImageView)vi.findViewById(R.id.list_image);
        String titleString = post.getTags().get(0);
        title.setText(Html.fromHtml(titleString).toString().replaceAll("\n", ""));
        timeDesc.setText(post.getLastPublishedTimestampAsString());

        ImageView browseImage = (ImageView)vi.findViewById(R.id.browse_images);
        browseImage.setTag(post);
        browseImage.setOnClickListener(this);
        if (onPhotoBrowseClick == null) {
        	browseImage.setVisibility(View.GONE);
        } else {
        	browseImage.setVisibility(View.VISIBLE);
        }
        
		// TODO find 75x75 image url
		List<TumblrAltSize> altSizes = post.getPhotos().get(0).getAltSizes();
		TumblrAltSize smallestImage = altSizes.get(altSizes.size() - 1);
        imageLoader.displayImage(smallestImage.getUrl(), thumbImage);
        return vi;
    }
    
    public void remove(int position) {
    	items.remove(position);
    }
    
    public void clear() {
    	items.clear();
    }

	public List<PhotoSharePost> getItems() {
		return items;
	}

	public void addItems(List<PhotoSharePost> items) {
		this.items.addAll(items);
	}
	
	public void setItems(List<PhotoSharePost> items) {
		this.items = items;
	}

	public void onClick(View v) {
		onPhotoBrowseClick.onClick((PhotoSharePost)v.getTag());
	}
}