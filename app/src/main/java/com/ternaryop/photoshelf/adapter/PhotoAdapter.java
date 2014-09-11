package com.ternaryop.photoshelf.adapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.fedorvlasov.lazylist.ImageLoader;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.adapter.PhotoShelfPost.ScheduleTime;
import com.ternaryop.utils.StringUtils;

public class PhotoAdapter extends ArrayAdapter<PhotoShelfPost> implements View.OnClickListener {
    private static LayoutInflater inflater = null;
    private final ImageLoader imageLoader;
    private final ArrayList<PhotoShelfPost> allPosts;
    private OnPhotoBrowseClick onPhotoBrowseClick;
    private boolean recomputeGroupIds;
    private boolean isFiltering;

    public PhotoAdapter(Context context, String prefix) {
        super(context, 0);
        inflater = LayoutInflater.from(context);
        imageLoader = new ImageLoader(context.getApplicationContext(), prefix);
        allPosts = new ArrayList<PhotoShelfPost>();
    }

    public void setOnPhotoBrowseClick(OnPhotoBrowseClick onPhotoBrowseClick) {
        this.onPhotoBrowseClick = onPhotoBrowseClick;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        ViewHolder holder;

        if (convertView == null) {
            vi = inflater.inflate(R.layout.list_row, parent, false);
            holder = new ViewHolder(vi);
            vi.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final PhotoShelfPost post = getItem(position);

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

            holder.menu.setOnClickListener(this);
            holder.menu.setTag(post);
        }

        holder.timeDesc.setText(post.getLastPublishedTimestampAsString());

        String imageUrl = post.getClosestPhotoByWidth(75).getUrl();
        imageLoader.displayImage(imageUrl, holder.thumbImage);
        return vi;
    }

    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.title_textview:
                onPhotoBrowseClick.onPhotoBrowseClick((PhotoShelfPost)v.getTag());
                break;
            case R.id.thumbnail_image:
                onPhotoBrowseClick.onThumbnailImageClick((PhotoShelfPost)v.getTag());
                break;
            case R.id.menu:
                onPhotoBrowseClick.onOverflowClick(v, (PhotoShelfPost) v.getTag());
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
                while (i < count && getItem(i).getFirstTag().equalsIgnoreCase(last)) {
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
    public void add(PhotoShelfPost object) {
        super.add(object);
        if (!isFiltering) {
            allPosts.add(object);
        }
        if (isRecomputeGroupIds()) {
            calcGroupIds();
        }
    }

    @Override
    public void addAll(Collection<? extends PhotoShelfPost> collection) {
        super.addAll(collection);
        if (!isFiltering) {
            allPosts.addAll(collection);
        }
        if (isRecomputeGroupIds()) {
            calcGroupIds();
        }
    }

    @Override
    public void addAll(PhotoShelfPost... items) {
        super.addAll(items);
        if (!isFiltering) {
            Collections.addAll(allPosts, items);
        }
        if (isRecomputeGroupIds()) {
            calcGroupIds();
        }
    }

    @Override
    public void insert(PhotoShelfPost object, int index) {
        super.insert(object, index);
        if (!isFiltering) {
            allPosts.add(index, object);
        }
        if (isRecomputeGroupIds()) {
            calcGroupIds();
        }
    }

    @Override
    public void clear() {
        super.clear();
        if (!isFiltering) {
            allPosts.clear();
        }
    }

    @Override
    public void remove(PhotoShelfPost object) {
        super.remove(object);
        if (!isFiltering) {
            allPosts.remove(object);
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                isFiltering = true;
                clear();
                addAll((List<PhotoShelfPost>)results.values);
                isFiltering = false;
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                ArrayList<PhotoShelfPost> filteredPosts = new ArrayList<PhotoShelfPost>();

                String pattern = constraint.toString().toLowerCase(Locale.US);
                for (PhotoShelfPost post : allPosts) {
                    if (post.getFirstTag().toLowerCase(Locale.US).contains(pattern))  {
                        filteredPosts.add(post);
                    }
                }

                FilterResults results = new FilterResults();
                results.count = filteredPosts.size();
                results.values = filteredPosts;

                return results;
            }
        };
    }

    public void removeAndRecalcGroups(PhotoShelfPost item, Calendar lastPublishDateTime) {
        remove(item);
        ArrayList<PhotoShelfPost> list = new ArrayList<PhotoShelfPost>(getCount());
        boolean isRegroupNeeded = false;
        String tag = item.getFirstTag();

        for (int i = 0; i < getCount(); i++) {
            PhotoShelfPost post = getItem(i);
            list.add(post);
            if (post.getFirstTag().equalsIgnoreCase(tag)) {
                isRegroupNeeded = true;
                post.setLastPublishedTimestamp(lastPublishDateTime.getTimeInMillis());
            }
        }
        if (isRegroupNeeded) {
            Collections.sort(list, new LastPublishedTimestampComparator());
            clear();
            addAll(list);
            calcGroupIds();
        }
    }

    private class ViewHolder {
        final TextView title;
        final TextView timeDesc;
        final TextView caption;
        final ImageView thumbImage;
        final ImageView menu;

        public ViewHolder(View vi) {
            title = (TextView)vi.findViewById(R.id.title_textview);
            timeDesc = (TextView)vi.findViewById(R.id.time_desc);
            caption = (TextView)vi.findViewById(R.id.caption);
            menu = (ImageView)vi.findViewById(R.id.menu);

            thumbImage = (ImageView)vi.findViewById(R.id.thumbnail_image);
        }
    }
}