package com.ternaryop.photoshelf.fragment;

import java.text.DecimalFormat;
import java.util.Map;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
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

    private static final int STATS_DATA_OK = 1;

    private Handler handler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        refresh();

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            @SuppressWarnings("unchecked")
            public void handleMessage(Message msg) {
                if (msg.what == STATS_DATA_OK) {
                    fillStatsUI((Map<String, Long>) msg.obj);
                }
            }
        };

        return rootView;
    }

    private void fillStatsUI(Map<String, Long> statsMap) {
        DecimalFormat format = new DecimalFormat("###,###");
        getView().findViewById(R.id.home_container).setVisibility(View.VISIBLE);
        for (int i = 0; i < viewIdColumnMap.size(); i++) {
            TextView textView = (TextView) getView().findViewById(viewIdColumnMap.keyAt(i));
            Long count = statsMap.get(viewIdColumnMap.valueAt(i));
            textView.setText(format.format(count));
            textView.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade));
        }
    }

    private void refresh() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, Long> statsMap = DBHelper.getInstance(getActivity()).getPostTagDAO().getStatisticCounts(getBlogName());
                handler.obtainMessage(STATS_DATA_OK, statsMap).sendToTarget();
            }
        }).start();
    }
}
