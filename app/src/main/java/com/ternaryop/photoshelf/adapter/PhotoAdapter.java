package com.ternaryop.photoshelf.adapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ternaryop.lazyimageloader.ImageLoader;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.adapter.PhotoShelfPost.ScheduleTime;
import com.ternaryop.tumblr.TumblrAltSize;
import com.ternaryop.utils.StringUtils;

public class PhotoAdapter extends BaseAdapter implements View.OnClickListener {
    private static LayoutInflater inflater = null;
    private final ImageLoader imageLoader;
    private final ArrayList<PhotoShelfPost> visiblePosts;
    private final ArrayList<PhotoShelfPost> allPosts;
    private final Context context;
    private OnPhotoBrowseClick onPhotoBrowseClick;
    private boolean recomputeGroupIds;
    private boolean isFiltering;
    private final int thumbnailWidth;

    public PhotoAdapter(Context context, String prefix) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        imageLoader = new ImageLoader(context.getApplicationContext(), prefix, R.drawable.stub);
        allPosts = new ArrayList<PhotoShelfPost>();
        visiblePosts = new ArrayList<PhotoShelfPost>();
        thumbnailWidth = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("thumbnail_width", "75"));
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
                holder.setColors(R.array.post_never);
                break;
            case POST_PUBLISH_FUTURE:
                holder.setColors(R.array.post_future);
                break;
            default:
                holder.setColors(post.getGroupId() % 2 == 0 ? R.array.post_even : R.array.post_odd);
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

        int noteCount = (int) post.getNoteCount();
        if (noteCount > 0) {
            holder.noteCount.setVisibility(View.VISIBLE);
            holder.noteCount.setText(getContext().getResources().getQuantityString(
                    R.plurals.note_title,
                    noteCount,
                    noteCount));
        } else {
            holder.noteCount.setVisibility(View.GONE);
        }

        holder.timeDesc.setText(post.getLastPublishedTimestampAsString());

        TumblrAltSize altSize = post.getClosestPhotoByWidth(thumbnailWidth);
        ViewGroup.LayoutParams imageLayoutParams = holder.thumbImage.getLayoutParams();
        int minThumbnainWidth = Math.max(thumbnailWidth, altSize.getWidth());
        // convert from pixel to DIP
        imageLayoutParams.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minThumbnainWidth, getContext().getResources().getDisplayMetrics());
        imageLayoutParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, altSize.getHeight(), getContext().getResources().getDisplayMetrics());

        imageLoader.displayImage(altSize.getUrl(), holder.thumbImage);
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

    @Override
    public PhotoShelfPost getItem(int position) {
        return visiblePosts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getCount() {
        return visiblePosts.size();
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

    public int getPosition(PhotoShelfPost post) {
        return visiblePosts.indexOf(post);
    }

    public void addAll(Collection<? extends PhotoShelfPost> collection) {
        visiblePosts.addAll(collection);
        if (!isFiltering) {
            allPosts.addAll(collection);
        }
        if (isRecomputeGroupIds()) {
            calcGroupIds();
        }
    }

    public void clear() {
        visiblePosts.clear();
        if (!isFiltering) {
            allPosts.clear();
        }
    }

    public void remove(PhotoShelfPost object) {
        visiblePosts.remove(object);
        if (!isFiltering) {
            allPosts.remove(object);
        }
    }

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

    public Context getContext() {
        return context;
    }

    private class ViewHolder {
        final TextView title;
        final TextView timeDesc;
        final TextView caption;
        final ImageView thumbImage;
        final ImageView menu;
        final View view;
        final TextView noteCount;

        public ViewHolder(View vi) {
            view = vi;
            title = (TextView)vi.findViewById(R.id.title_textview);
            timeDesc = (TextView)vi.findViewById(R.id.time_desc);
            caption = (TextView)vi.findViewById(R.id.caption);
            menu = (ImageView)vi.findViewById(R.id.menu);
            noteCount = (TextView)vi.findViewById(R.id.note_count);

            thumbImage = (ImageView)vi.findViewById(R.id.thumbnail_image);
        }

        public void setColors(int resArray) {
            TypedArray array = getContext().getResources().obtainTypedArray(resArray);
            view.setBackground(array.getDrawable(0));
            title.setTextColor(array.getColorStateList(1));
            timeDesc.setTextColor(array.getColorStateList(2));
            caption.setTextColor(array.getColorStateList(3));
            menu.setImageDrawable(array.getDrawable(4));
            noteCount.setTextColor(array.getColorStateList(3));
            array.recycle();
        }
    }
}