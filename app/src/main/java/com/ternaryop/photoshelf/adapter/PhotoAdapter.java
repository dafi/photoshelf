package com.ternaryop.photoshelf.adapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
    public static final int SORT_NONE = 0;
    public static final int SORT_TAG_NAME = 1;
    public static final int SORT_LAST_PUBLISHED_TAG = 2;
    public static final int SORT_UPLOAD_TIME = 3;

    private static LayoutInflater inflater = null;
    private final ImageLoader imageLoader;
    private ArrayList<PhotoShelfPost> visiblePosts;
    private final ArrayList<PhotoShelfPost> allPosts;
    private final Context context;
    private OnPhotoBrowseClick onPhotoBrowseClick;
    private final int thumbnailWidth;

    private int currentSort = SORT_LAST_PUBLISHED_TAG;
    private boolean sortAscending = true;

    public PhotoAdapter(Context context, String prefix) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        imageLoader = new ImageLoader(context.getApplicationContext(), prefix, R.drawable.stub);
        allPosts = new ArrayList<PhotoShelfPost>();
        visiblePosts = allPosts;
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
                holder.title.setTag(position);
            }

            holder.thumbImage.setOnClickListener(this);
            holder.thumbImage.setTag(position);

            holder.menu.setOnClickListener(this);
            holder.menu.setTag(position);
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
                onPhotoBrowseClick.onPhotoBrowseClick((Integer) v.getTag());
                break;
            case R.id.thumbnail_image:
                onPhotoBrowseClick.onThumbnailImageClick((Integer) v.getTag());
                break;
            case R.id.menu:
                onPhotoBrowseClick.onOverflowClick(v, (Integer) v.getTag());
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

    public int getPosition(PhotoShelfPost post) {
        return visiblePosts.indexOf(post);
    }

    public void addAll(Collection<? extends PhotoShelfPost> collection) {
        allPosts.addAll(collection);
    }

    public void clear() {
        visiblePosts.clear();
        allPosts.clear();
    }

    public void remove(PhotoShelfPost object) {
        visiblePosts.remove(object);
        allPosts.remove(object);
    }

    public Filter getFilter() {
        return new Filter() {

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results.values == allPosts) {
                    visiblePosts = allPosts;
                } else {
                    visiblePosts = (ArrayList<PhotoShelfPost>) results.values;
                    calcGroupIds();
                }
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                String pattern = constraint.toString().trim();

                if (pattern.length() == 0) {
                    results.count = allPosts.size();
                    results.values = allPosts;
                } else {
                    ArrayList<PhotoShelfPost> filteredPosts = new ArrayList<PhotoShelfPost>();

                    pattern = pattern.toLowerCase(Locale.US);
                    for (PhotoShelfPost post : allPosts) {
                        if (post.getFirstTag().toLowerCase(Locale.US).contains(pattern)) {
                            filteredPosts.add(post);
                        }
                    }

                    results.count = filteredPosts.size();
                    results.values = filteredPosts;
                }

                return results;
            }
        };
    }

    public void removeAndRecalcGroups(PhotoShelfPost item, Calendar lastPublishDateTime) {
        remove(item);
        boolean isSortNeeded = false;
        String tag = item.getFirstTag();

        for (PhotoShelfPost post : visiblePosts) {
            if (post.getFirstTag().equalsIgnoreCase(tag)) {
                isSortNeeded = true;
                post.setLastPublishedTimestamp(lastPublishDateTime.getTimeInMillis());
            }
        }
        if (isSortNeeded && currentSort == SORT_LAST_PUBLISHED_TAG) {
            sort();
        } else {
            calcGroupIds();
        }
    }

    public Context getContext() {
        return context;
    }

    private void updateCurrentSortType(int type) {
        if (currentSort == type) {
            sortAscending = !sortAscending;
        } else {
            sortAscending = true;
            currentSort = type;
        }
    }

    public void sortByTagName() {
        updateCurrentSortType(SORT_TAG_NAME);
        Collections.sort(visiblePosts, new Comparator<PhotoShelfPost>() {
            @Override
            public int compare(PhotoShelfPost lhs, PhotoShelfPost rhs) {
                return LastPublishedTimestampComparator.compareTag(lhs, rhs, sortAscending);
            }
        });
        calcGroupIds();
    }

    public void sortByLastPublishedTag() {
        if (currentSort == SORT_LAST_PUBLISHED_TAG) {
            return;
        }
        currentSort = SORT_LAST_PUBLISHED_TAG;
        Collections.sort(visiblePosts, new LastPublishedTimestampComparator());
        calcGroupIds();
    }

    public void sortByUploadTime() {
        updateCurrentSortType(SORT_UPLOAD_TIME);
        Collections.sort(visiblePosts, new Comparator<PhotoShelfPost>() {
            @Override
            public int compare(PhotoShelfPost lhs, PhotoShelfPost rhs) {
                long diff = lhs.getTimestamp() - rhs.getTimestamp();
                int compare = diff < -1 ? -1 : diff > 1 ? 1 : 0;
                if (compare == 0) {
                    return lhs.getFirstTag().compareToIgnoreCase(rhs.getFirstTag());
                }
                return sortAscending ? compare : -compare;
            }
        });
        calcGroupIds();
    }

    /**
     * Sort the list using the last used sort method
     */
    public void sort() {
        int temp = currentSort;

        // reset the flag to force the sort
        currentSort = SORT_NONE;

        switch (temp) {
            case SORT_TAG_NAME:
                sortByTagName();
                break;
            case SORT_LAST_PUBLISHED_TAG:
                sortByLastPublishedTag();
                break;
            case SORT_UPLOAD_TIME:
                sortByUploadTime();
                break;
        }
    }

    public int getCurrentSort() {
        return currentSort;
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
            title = (TextView) vi.findViewById(R.id.title_textview);
            timeDesc = (TextView) vi.findViewById(R.id.time_desc);
            caption = (TextView) vi.findViewById(R.id.caption);
            menu = (ImageView) vi.findViewById(R.id.menu);
            noteCount = (TextView) vi.findViewById(R.id.note_count);

            thumbImage = (ImageView) vi.findViewById(R.id.thumbnail_image);
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