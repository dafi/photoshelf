package com.ternaryop.phototumblrshare.list;

import java.util.Collection;
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
    private ImageLoader imageLoader;
	private OnPhotoBrowseClick onPhotoBrowseClick;
	private boolean recomputeGroupIds;
 
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
			int groupId = post.getGroupId();
			if (groupId > 0) {
                vi.setBackgroundResource((groupId % 2) == 0 ? R.drawable.list_selector_post_group_even : R.drawable.list_selector_post_group_odd);
			} else {
                vi.setBackgroundResource(R.drawable.list_selector);
			}
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
	
	public void computeGroupIds() {
		if (getCount() == 0) {
			return;
		}
		PhotoSharePost post = getItem(0);
		String last = post.getTags().get(0);
		int groupId = 1;
		
		for (int i = 1; i < getCount(); i++) {
	        post = getItem(i);
	        String tag = post.getTags().get(0);
			if (tag.equals(last)) {
				getItem(i - 1).setGroupId(groupId);
				post.setGroupId(groupId);
				++i;
				for (; i < getCount(); i++) {
					tag = getItem(i).getTags().get(0);
					if (last.equals(tag)) {
						getItem(i).setGroupId(groupId);
					} else {
						last = tag;
						break;
					}
				}
				++groupId;
			} else {
				last = tag;
			}
		}
	}

	public boolean isRecomputeGroupIds() {
		return recomputeGroupIds;
	}

	public void setRecomputeGroupIds(boolean recomputeGroupIds) {
		this.recomputeGroupIds = recomputeGroupIds;
	}

	@Override
	public void add(PhotoSharePost object) {
		super.add(object);
		if (isRecomputeGroupIds()) {
			computeGroupIds();
		}
	}

	@Override
	public void addAll(Collection<? extends PhotoSharePost> collection) {
		super.addAll(collection);
		if (isRecomputeGroupIds()) {
			computeGroupIds();
		}
	}

	@Override
	public void addAll(PhotoSharePost... items) {
		super.addAll(items);
		if (isRecomputeGroupIds()) {
			computeGroupIds();
		}
	}

	@Override
	public void insert(PhotoSharePost object, int index) {
		super.insert(object, index);
		if (isRecomputeGroupIds()) {
			computeGroupIds();
		}
	}
}