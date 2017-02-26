package com.ternaryop.photoshelf.adapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.ternaryop.feedly.FeedlyContent;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.util.sort.AbsSortable;
import com.ternaryop.photoshelf.util.sort.Sortable;

public class FeedlyContentAdapter extends RecyclerView.Adapter<FeedlyContentViewHolder> implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    public static final int SORT_TITLE_NAME = 1;
    public static final int SORT_SAVED_TIMESTAMP = 2;

    private final Context context;
    private final ArrayList<FeedlyContentDelegate> allContents;
    private AbsSortable titleNameSortable;
    private AbsSortable saveTimestampSortable;
    private AbsSortable currentSortable;
    private OnFeedlyContentClick clickListener;

    public FeedlyContentAdapter(Context context) {
        this.context = context;
        allContents = new ArrayList<>();
        currentSortable = getTitleNameSortable();
    }

    @Override
    public FeedlyContentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FeedlyContentViewHolder(LayoutInflater.from(context).inflate(R.layout.list_row_2, parent, false));
    }

    @Override
    public void onBindViewHolder(FeedlyContentViewHolder holder, int position) {
        holder.bindModel(allContents.get(position));
        setClickListeners(holder, position);
    }

    private void setClickListeners(FeedlyContentViewHolder holder, int position) {
        if (clickListener == null) {
            holder.setOnClickListeners(allContents.get(position), null);
            holder.setOnCheckedChangeListener(allContents.get(position), null);
        } else {
            holder.setOnClickListeners(allContents.get(position), this);
            holder.setOnCheckedChangeListener(allContents.get(position), this);
        }
    }

    @Override
    public int getItemCount() {
        return allContents.size();
    }

    public List<FeedlyContent> getUncheckedItems() {
        final ArrayList<FeedlyContent> list = new ArrayList<>();

        for (FeedlyContentDelegate fc: allContents) {
            if (!fc.checked) {
                list.add(fc);
            }
        }
        return list;
    }

    public void setClickListener(OnFeedlyContentClick listener) {
        this.clickListener = listener;
    }

    public void addAll(Collection<? extends FeedlyContent> collection) {
        for (FeedlyContent fc : collection) {
            allContents.add(new FeedlyContentDelegate(fc));
        }
        notifyDataSetChanged();
    }

    public void clear() {
        allContents.clear();
        notifyDataSetChanged();
    }

    public FeedlyContent getItem(int position) {
        return allContents.get(position);
    }

    /**
     * Sort the list using the last used sort method
     */
    public void sort() {
        currentSortable.sort();
    }

    public void setSortType(int sortType) {
        switch (sortType) {
            case SORT_TITLE_NAME:
                currentSortable = getTitleNameSortable();
                break;
            case SORT_SAVED_TIMESTAMP:
                saveTimestampSortable = getSaveTimestampSortable();
                break;
        }
    }

    public int getCurrentSort() {
        return currentSortable.getSortId();
    }

    public Sortable getCurrentSortable() {
        return currentSortable;
    }

    private void sort(AbsSortable sortable) {
        currentSortable = sortable;
        currentSortable.sort();
    }

    public void sortByTitleName() {
        sort(getTitleNameSortable());
    }

    public void sortBySavedTimestamp() {
        sort(getSaveTimestampSortable());
    }

    private AbsSortable getTitleNameSortable() {
        if (titleNameSortable == null) {
            titleNameSortable = new AbsSortable(true, SORT_TITLE_NAME) {
                @Override
                public void sort() {
                    Collections.sort(allContents, new Comparator<FeedlyContent>() {
                        @Override
                        public int compare(FeedlyContent c1, FeedlyContent c2) {
                            return c1.getTitle().compareToIgnoreCase(c2.getTitle());
                        }
                    });
                }
            };
        }
        return titleNameSortable;
    }

    private AbsSortable getSaveTimestampSortable() {
        if (saveTimestampSortable == null) {
            saveTimestampSortable = new AbsSortable(true, SORT_SAVED_TIMESTAMP) {
                @Override
                public void sort() {
                    Collections.sort(allContents, new Comparator<FeedlyContent>() {
                        @Override
                        public int compare(FeedlyContent c1, FeedlyContent c2) {
                            final long at1 = c1.getActionTimestamp();
                            final long at2 = c2.getActionTimestamp();
                            return at1 == at2 ? 0 : at1 < at2 ? 1 : -1;
                        }
                    });
                }
            };
        }
        return saveTimestampSortable;
    }

    @Override
    public void onClick(View v) {
        final Integer position = (Integer) v.getTag();
        switch (v.getId()) {
            case R.id.list_row2:
                clickListener.onTitleClick(position);
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton v, boolean checked) {
        final Integer position = (Integer) v.getTag();
        switch (v.getId()) {
            case android.R.id.checkbox:
                ((FeedlyContentDelegate)getItem(position)).checked = checked;
                clickListener.onToggleClick(position, checked);
                break;
        }
    }

    /**
     * Contains fields related to UI state
     */
    public static class FeedlyContentDelegate extends FeedlyContent {
        private FeedlyContent delegated;
        public boolean checked = true;

        public FeedlyContentDelegate(FeedlyContent delegated) {
            this.delegated = delegated;
        }

        @Override
        public String getId() {
            return delegated.getId();
        }

        @Override
        public String getTitle() {
            return delegated.getTitle();
        }

        @Override
        public String getOriginId() {
            return delegated.getOriginId();
        }

        @Override
        public long getActionTimestamp() {
            return delegated.getActionTimestamp();
        }

        @Override
        public void setId(String id) {
            delegated.setId(id);
        }

        @Override
        public void setTitle(String title) {
            delegated.setTitle(title);
        }

        @Override
        public void setOriginId(String originId) {
            delegated.setOriginId(originId);
        }

        @Override
        public void setActionTimestamp(long actionTimestamp) {
            delegated.setActionTimestamp(actionTimestamp);
        }

        @Override
        public Origin getOrigin() {
            return delegated.getOrigin();
        }

        @Override
        public void setOrigin(Origin origin) {
            delegated.setOrigin(origin);
        }
    }
}