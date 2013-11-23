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
import com.ternaryop.phototumblrshare.list.PhotoSharePost.ScheduleTime;
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
        ViewHolder holder;

        if (convertView == null) {
            vi = inflater.inflate(R.layout.list_row, null);
            holder = new ViewHolder(vi);
            vi.setTag(holder);
        } else {
        	holder = (ViewHolder) convertView.getTag();
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
            vi.setBackgroundResource((groupId % 2) == 0 ? R.drawable.list_selector_post_group_even : R.drawable.list_selector_post_group_odd);
			break;
        }

        String titleString = post.getFirstTag();
        holder.title.setText(Html.fromHtml(titleString).toString().replaceAll("\n", ""));
        holder.timeDesc.setText(post.getLastPublishedTimestampAsString());

        holder.browseImage.setTag(post);
        holder.browseImage.setOnClickListener(this);
        if (onPhotoBrowseClick == null || post.getScheduleTimeType() == ScheduleTime.POST_PUBLISH_NEVER) {
        	holder.browseImage.setVisibility(View.GONE);
        } else {
        	holder.browseImage.setVisibility(View.VISIBLE);
        }
        
		// TODO find 75x75 image url
		List<TumblrAltSize> altSizes = post.getPhotos().get(0).getAltSizes();
		TumblrAltSize smallestImage = altSizes.get(altSizes.size() - 1);
        imageLoader.displayImage(smallestImage.getUrl(), holder.thumbImage);
        return vi;
    }
    
	public void onClick(View v) {
		onPhotoBrowseClick.onPhotoBrowseClick((PhotoSharePost)v.getTag());
	}
	
	public void calcGroupIds() {
		int count = getCount();
		
		if (count > 0) {
			int groupId = 0;
			
			String last = getItem(0).getFirstTag();
			getItem(0).setGroupId(groupId);
			
			for (int i = 1; i < count; i++) {
				// set same groupId for all identical tags
				while (i < count && getItem(i).getFirstTag().equals(last)) {
					getItem(i++).setGroupId(groupId);
				}
				if (i < count) {
					++groupId;
					getItem(i).setGroupId(groupId);
					last = getItem(i).getFirstTag();
				}
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
			calcGroupIds();
		}
	}

	@Override
	public void addAll(Collection<? extends PhotoSharePost> collection) {
		super.addAll(collection);
		if (isRecomputeGroupIds()) {
			calcGroupIds();
		}
	}

	@Override
	public void addAll(PhotoSharePost... items) {
		super.addAll(items);
		if (isRecomputeGroupIds()) {
			calcGroupIds();
		}
	}

	@Override
	public void insert(PhotoSharePost object, int index) {
		super.insert(object, index);
		if (isRecomputeGroupIds()) {
			calcGroupIds();
		}
	}
	
	private class ViewHolder {
		TextView title;
		TextView timeDesc;
		ImageView thumbImage;
		ImageView browseImage;

		public ViewHolder(View vi) {
	        title = (TextView)vi.findViewById(R.id.title_textview);
	        timeDesc = (TextView)vi.findViewById(R.id.time_desc);

	        thumbImage = (ImageView)vi.findViewById(R.id.list_image);
	        browseImage = (ImageView)vi.findViewById(R.id.browse_images);
		}
	}
}