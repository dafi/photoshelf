package com.ternaryop.photoshelf.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

/**
 * Created by dave on 30/11/17.
 * Handle Most Recently Used (MRU) list
 */

public class MRU {
    private static final String ITEM_SEPARATOR = "\n";
    private final SharedPreferences preferences;
    private final String key;
    private final int maxSize;
    private List<String> mruList;

    public MRU(Context context, String key, int maxSize) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.key = key;
        this.maxSize = maxSize;
    }

    public void add(List<String> items) {
        if (items == null) {
            return;
        }

        // every item is added at top so iterate in reverse order
        final ListIterator<String> iterator = items.listIterator(items.size());
        while (iterator.hasPrevious()) {
            add(iterator.previous());
        }
    }

    public boolean add(String item) {
        if (item == null || item.trim().isEmpty()) {
            return false;
        }
        String lowerCaseItem = item.toLowerCase(Locale.US);
        List<String> list = getList();
        if (!list.remove(lowerCaseItem) && list.size() == maxSize) {
            list.remove(list.size() - 1);
        }
        list.add(0, lowerCaseItem);
        return true;
    }

    public boolean remove(String item) {
        return getList().remove(item.toLowerCase(Locale.US));
    }

    public void save() {
        preferences
                .edit()
                .putString(key, TextUtils.join(ITEM_SEPARATOR, mruList))
                .apply();
    }

    public List<String> getList() {
        if (mruList == null) {
            mruList = new ArrayList<>(Arrays.asList(TextUtils.split(preferences.getString(key, ""), ITEM_SEPARATOR)));
        }
        return mruList;
    }
}
