package com.ternaryop.photoshelf.service;

import java.util.Calendar;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ternaryop.photoshelf.db.DBHelper;
import com.ternaryop.photoshelf.event.CounterEvent;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrUtils;
import org.greenrobot.eventbus.EventBus;

/**
 * Created by dave on 28/10/17.
 * Handle counters retrieval
 */
public class CounterIntentService extends IntentService implements PhotoShelfIntentExtra {
    public CounterIntentService() {
        super("counterIntent");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        String selectedBlogName = intent.getStringExtra(EXTRA_BLOG_NAME);
        int type = intent.getIntExtra(EXTRA_TYPE, CounterEvent.NONE);
        String action = intent.getStringExtra(EXTRA_ACTION);

        if (ACTION_FETCH_COUNTER.equals(action)) {
            fetchCounterByType(type, selectedBlogName);
        }
    }

    public static void fetchCounter(@NonNull Context context,
                                    @NonNull String blogName,
                                    @CounterEvent.CounterType int type) {
        Intent intent = new Intent(context, CounterIntentService.class);
        intent.putExtra(EXTRA_TYPE, type);
        intent.putExtra(EXTRA_BLOG_NAME, blogName);
        intent.putExtra(EXTRA_ACTION, ACTION_FETCH_COUNTER);

        context.startService(intent);
    }

    private void fetchCounterByType(final @CounterEvent.CounterType int type, final String blogName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (EventBus.getDefault().hasSubscriberForEvent(CounterEvent.class)) {
                    EventBus.getDefault().post(new CounterEvent(type, getCount(type, blogName)));
                }
            }
        }).start();
    }

    private long getCount(final @CounterEvent.CounterType int type, final String blogName) {
        try {
            switch (type) {
                case CounterEvent.BIRTHDAY:
                    return DBHelper
                            .getInstance(getApplicationContext())
                            .getBirthdayDAO()
                            .getBirthdaysCountInDate(Calendar.getInstance().getTime(), blogName);
                case CounterEvent.DRAFT:
                    return TumblrUtils.getDraftCount(Tumblr.getSharedTumblr(getApplicationContext()), blogName);
                case CounterEvent.SCHEDULE:
                    return TumblrUtils.getQueueCount(Tumblr.getSharedTumblr(getApplicationContext()), blogName);
                case CounterEvent.NONE:
                    break;
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}
