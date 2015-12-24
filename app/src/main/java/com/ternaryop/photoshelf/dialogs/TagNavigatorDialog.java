package com.ternaryop.photoshelf.dialogs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.adapter.PhotoShelfPost;

/**
 * Created by dave on 17/05/15.
 * Allow to select tag
 */
public class TagNavigatorDialog extends DialogFragment {

    public static final String SELECTED_TAG = "selectedTag";
    public static final String ARG_TAG_LIST = "list";
    private static final int SORT_TAG_NAME = 0;
    private static final int SORT_TAG_COUNT = 1;
    public static final String PREF_NAME_TAG_SORT = "tagNavigatorSort";
    private ArrayAdapter<TagCounter> adapter;
    private Button sortButton;

    public static TagNavigatorDialog newInstance(List<PhotoShelfPost> photoList, Fragment target, int requestCode) {
        Bundle args = new Bundle();
        ArrayList<String> strings = new ArrayList<>();
        for (PhotoShelfPost photo : photoList) {
            strings.add(photo.getFirstTag());
        }
        args.putStringArrayList(TagNavigatorDialog.ARG_TAG_LIST, strings);

        TagNavigatorDialog fragment = new TagNavigatorDialog();
        fragment.setArguments(args);
        fragment.setTargetFragment(target, requestCode);
        return fragment;
    }

    /**
     * Helper method to use from dialog caller
     * @param photoList the list where to look for the tag
     * @param data the data returned by TagNavigatorDialog
     * @return the tag index if found, -1 otherwise
     */
    public static int findTagIndex(List<PhotoShelfPost> photoList, Intent data) {
        String tag = data.getStringExtra(TagNavigatorDialog.SELECTED_TAG);
        for (int i = 0; i < photoList.size(); i++) {
            PhotoShelfPost photo = photoList.get(i);
            if (photo.getFirstTag().compareToIgnoreCase(tag) == 0) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_tag_navigator, null);
        setupUI(view);
        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(getResources().getString(R.string.tag_navigator_title, adapter.getCount()))
                .setNegativeButton(getResources().getString(R.string.close), null)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.putExtra(SELECTED_TAG, adapter.getItem(which).tag);
                        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                    }
                })
                .create();
    }

    private void setupUI(View view) {
        adapter = createAdapter(getArguments().getStringArrayList(ARG_TAG_LIST));
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sortButton = (Button) view.findViewById(R.id.sort_tag);

        changeSortType(preferences.getInt(PREF_NAME_TAG_SORT, SORT_TAG_NAME));
        View.OnClickListener sortClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.sort_tag:
                        int sortType = preferences.getInt(PREF_NAME_TAG_SORT, SORT_TAG_NAME);
                        sortType = sortType == SORT_TAG_NAME ? SORT_TAG_COUNT : SORT_TAG_NAME;
                        preferences.edit().putInt(PREF_NAME_TAG_SORT, sortType).apply();
                        changeSortType(sortType);
                        break;
                }
            }
        };
        sortButton.setOnClickListener(sortClick);
    }

    private void changeSortType(int sortType) {
        switch (sortType) {
            case SORT_TAG_NAME:
                sortButton.setText(R.string.sort_by_count);
                sortByTagName();
                break;
            case SORT_TAG_COUNT:
                sortButton.setText(R.string.sort_by_name);
                sortByTagCount();
                break;
        }
    }

    public ArrayAdapter<TagCounter> createAdapter(List<String> tagList) {
        HashMap<String, TagCounter> map = new HashMap<>(tagList.size());
        for (String s : tagList) {
            String lower = s.toLowerCase();
            TagCounter tagCounter = map.get(lower);
            if (tagCounter == null) {
                tagCounter = new TagCounter(s);
                map.put(lower, tagCounter);
            } else {
                ++tagCounter.count;
            }
        }
        ArrayList<TagCounter> list = new ArrayList<>(map.values());

        return new ArrayAdapter<TagCounter>(getActivity(), android.R.layout.simple_list_item_1, new ArrayList<TagCounter>(list));
    }

    private void sortByTagCount() {
        adapter.sort(new Comparator<TagCounter>() {
            @Override
            public int compare(TagCounter lhs, TagCounter rhs) {
                // sort descending
                return rhs.count - lhs.count;
            }
        });
        adapter.notifyDataSetChanged();
    }

    private void sortByTagName() {
        adapter.sort(new Comparator<TagCounter>() {
            @Override
            public int compare(TagCounter lhs, TagCounter rhs) {
                return lhs.tag.compareToIgnoreCase(rhs.tag);
            }
        });
        adapter.notifyDataSetChanged();
    }

    private static class TagCounter {
        public final String tag;
        public int count;

        public TagCounter(String tag) {
            this.tag = tag;
            this.count = 1;
        }

        @Override
        public String toString() {
            if (count == 1) {
                return tag;
            }
            return tag + " (" + count + ")";
        }
    }
}
