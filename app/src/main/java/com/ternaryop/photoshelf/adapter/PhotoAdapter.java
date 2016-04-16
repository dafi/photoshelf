package com.ternaryop.photoshelf.adapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;

import com.ternaryop.lazyimageloader.ImageLoader;
import com.ternaryop.photoshelf.R;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoViewHolder> implements View.OnClickListener, View.OnLongClickListener {
    public static final int SORT_NONE = 0;
    public static final int SORT_TAG_NAME = 1;
    public static final int SORT_LAST_PUBLISHED_TAG = 2;
    public static final int SORT_UPLOAD_TIME = 3;

    private final ImageLoader imageLoader;
    private ArrayList<PhotoShelfPost> visiblePosts;
    private final ArrayList<PhotoShelfPost> allPosts;
    private final Context context;
    private OnPhotoBrowseClick onPhotoBrowseClick;
    private final int thumbnailWidth;

    private int currentSort = SORT_LAST_PUBLISHED_TAG;
    private boolean sortAscending = true;

    final SelectionArrayViewHolder<PhotoViewHolder> selection = new SelectionArrayViewHolder<>(this);

    public PhotoAdapter(Context context, String prefix) {
        this.context = context;
        imageLoader = new ImageLoader(context.getApplicationContext(), prefix, R.drawable.stub);
        allPosts = new ArrayList<>();
        visiblePosts = allPosts;
        thumbnailWidth = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("thumbnail_width", "75"));
    }

    public void setOnPhotoBrowseClick(OnPhotoBrowseClick onPhotoBrowseClick) {
        this.onPhotoBrowseClick = onPhotoBrowseClick;
    }

    @Override
    public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PhotoViewHolder(LayoutInflater.from(context).inflate(R.layout.list_row, parent, false));
    }

    @Override
    public void onBindViewHolder(PhotoViewHolder holder, int position) {
        View.OnClickListener listener = onPhotoBrowseClick == null ? null : this;
        holder.bindModel(visiblePosts.get(position), imageLoader, thumbnailWidth);
        holder.setOnClickListeners(visiblePosts.get(position), listener);
        if (onPhotoBrowseClick instanceof OnPhotoBrowseClickMultiChoice) {
            holder.setOnClickMultiChoiceListeners(listener, this);
        }
        holder.itemView.setSelected(selection.isSelected(position));
    }

    @Override
    public int getItemCount() {
        return visiblePosts.size();
    }

    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.title_textview:
                onPhotoBrowseClick.onTagClick((Integer) v.getTag());
                break;
            case R.id.thumbnail_image:
                onPhotoBrowseClick.onThumbnailImageClick((Integer) v.getTag());
                break;
            case R.id.menu:
                onPhotoBrowseClick.onOverflowClick(v, (Integer) v.getTag());
                break;
            case R.id.list_row:
                ((OnPhotoBrowseClickMultiChoice)onPhotoBrowseClick).onItemClick((Integer) v.getTag());
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        ((OnPhotoBrowseClickMultiChoice)onPhotoBrowseClick).onItemLongClick((Integer) v.getTag());
        return true;
    }

    public PhotoShelfPost getItem(int position) {
        return visiblePosts.get(position);
    }

    public void calcGroupIds() {
        int count = getItemCount();

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
        notifyDataSetChanged();
    }

    public void clear() {
        visiblePosts.clear();
        allPosts.clear();
        notifyDataSetChanged();
    }

    public void remove(PhotoShelfPost object) {
        allPosts.remove(object);
        int position = visiblePosts.indexOf(object);
        if (position >= 0) {
            visiblePosts.remove(position);
            notifyItemRemoved(position);
        }
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
                    ArrayList<PhotoShelfPost> filteredPosts = new ArrayList<>();

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

    public List<PhotoShelfPost> getPhotoList() {
        return visiblePosts;
    }

    public List<PhotoShelfPost> getSelectedPosts() {
        ArrayList<PhotoShelfPost> list = new ArrayList<>(getSelection().getItemCount());
        for (int pos : getSelection().getSelectedPositions()) {
            list.add(getItem(pos));
        }
        return list;
    }


    public Selection getSelection() {
        return selection;
    }

    public void setEmptyView(final View view) {
        if (view != null) {
            registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    view.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
                }
            });
        }
    }
}