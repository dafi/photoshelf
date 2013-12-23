package com.ternaryop.phototumblrshare.list;

import java.util.Collection;

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
import com.ternaryop.utils.StringUtils;
 
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

        holder.title.setText(post.getFirstTag());
        holder.caption.setText(Html.fromHtml(StringUtils.stripHtmlTags("a|img|p|br", post.getCaption())));

        // set the onclick listeners
        if (onPhotoBrowseClick != null) {
            if (post.getScheduleTimeType() == ScheduleTime.POST_PUBLISH_NEVER) {
                holder.title.setOnClickListener(null);
            } else {
                holder.title.setOnClickListener(this);
                holder.title.setTag(post);
            }
            
            holder.thumbImage.setOnClickListener(this);
            holder.thumbImage.setTag(post);
        }
        
        holder.timeDesc.setText(post.getLastPublishedTimestampAsString());

		String imageUrl = post.getClosestPhotoByWidth(75).getUrl();
        imageLoader.displayImage(imageUrl, holder.thumbImage);
        return vi;
    }
    
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.title_textview:
				onPhotoBrowseClick.onPhotoBrowseClick((PhotoSharePost)v.getTag());
				break;
			case R.id.thumbnail_image:
				onPhotoBrowseClick.onThumbnailImageClick((PhotoSharePost)v.getTag());
				break;
		}
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
        TextView caption;
		ImageView thumbImage;

		public ViewHolder(View vi) {
	        title = (TextView)vi.findViewById(R.id.title_textview);
	        timeDesc = (TextView)vi.findViewById(R.id.time_desc);
	        caption = (TextView)vi.findViewById(R.id.caption);

	        thumbImage = (ImageView)vi.findViewById(R.id.thumbnail_image);
		}
	}
}