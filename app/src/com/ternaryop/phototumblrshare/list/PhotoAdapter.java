package com.ternaryop.phototumblrshare.list;

import java.util.List;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.fedorvlasov.lazylist.ImageLoader;
import com.ternaryop.phototumblrshare.R;
import com.ternaryop.tumblr.TumblrAltSize;
 
public class PhotoAdapter extends ArrayAdapter<PhotoSharePost> implements View.OnClickListener {
    private static LayoutInflater inflater = null;
    public ImageLoader imageLoader;
	private OnPhotoBrowseClick onPhotoBrowseClick;
 
    public PhotoAdapter(Context context, String prefix) {
		super(context, 0);
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        imageLoader = new ImageLoader(context.getApplicationContext(), prefix);
    }
 
    public void setOnPhotoBrowseClick(OnPhotoBrowseClick onPhotoBrowseClick) {
		this.onPhotoBrowseClick = onPhotoBrowseClick;
    }
    
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        if (convertView == null) {
            vi = inflater.inflate(R.layout.list_row, null);
        }

        final PhotoSharePost post = getItem(position);

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
    
	public void onClick(View v) {
		onPhotoBrowseClick.onPhotoBrowseClick((PhotoSharePost)v.getTag());
	}
}