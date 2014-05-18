package com.ternaryop.photoshelf.fragment;

import java.text.DecimalFormat;
import java.util.Map;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.db.DBHelper;
import com.ternaryop.photoshelf.db.PostTagDAO;

public class HomeFragment extends AbsPhotoShelfFragment {
    private final static SparseArray<String> viewIdColumnMap = new SparseArray<String>();

    static {
        viewIdColumnMap.put(R.id.total_records, PostTagDAO.RECORD_COUNT_COLUMN);
        viewIdColumnMap.put(R.id.total_posts, PostTagDAO.POST_COUNT_COLUMN);
        viewIdColumnMap.put(R.id.total_unique_tags, PostTagDAO.UNIQUE_TAGS_COUNT_COLUMN);
        viewIdColumnMap.put(R.id.total_unique_first_tags, PostTagDAO.UNIQUE_FIRST_TAG_COUNT_COLUMN);
        viewIdColumnMap.put(R.id.birthdays_count, PostTagDAO.BIRTHDAYS_COUNT_COLUMN);
        viewIdColumnMap.put(R.id.missing_birthdays_count, PostTagDAO.MISSING_BIRTHDAYS_COUNT_COLUMN);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        refresh(rootView);

        return rootView;
    }

    private void refresh(View rootView) {
        Map<String, Long> statsMap = DBHelper.getInstance(getActivity()).getPostTagDAO().getStatisticCounts(getBlogName());
        DecimalFormat format = new DecimalFormat("###,###");
        for (int i = 0; i < viewIdColumnMap.size(); i++) {
            TextView textView = (TextView) rootView.findViewById(viewIdColumnMap.keyAt(i));
            Long count = statsMap.get(viewIdColumnMap.valueAt(i));
            textView.setText(format.format(count));
        }
    }
}
