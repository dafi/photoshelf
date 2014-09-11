package com.ternaryop.photoshelf.counter;

import java.util.Calendar;

import android.content.Context;
import android.widget.BaseAdapter;

import com.ternaryop.photoshelf.db.DBHelper;

public class BirthdaysCountRetriever extends AbsCountRetriever {

    public BirthdaysCountRetriever(Context context, String blogName, BaseAdapter adapter) {
        super(context, blogName, adapter);
    }

    @Override
    protected Long getCount() {
        return DBHelper
                .getInstance(getContext())
                .getBirthdayDAO()
                .getBirthdaysCountInDate(Calendar.getInstance().getTime(),
                        getBlogName());
    }
}
