package com.ternaryop.photoshelf.counter;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;

public abstract class AbsCountRetriever implements CountRetriever {
    private Context context;
    private String blogName;
    private AsyncTask<Void, Void, Long> task;
    private Long lastCount;

    public AbsCountRetriever(Context context, String blogName) {
        this.context = context;
        this.blogName = blogName;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public String getBlogName() {
        return blogName;
    }

    @Override
    public void setBlogName(String blogName) {
        this.blogName = blogName;
    }

    protected abstract Long getCount();
    
    @Override
    public void updateCount(final TextView textView) {
        // if available use the cached value
        if (lastCount != null) {
            textView.setText(lastCount.toString());
            textView.setVisibility(View.VISIBLE);
            return;
        }
        if (task != null && task.getStatus().equals(AsyncTask.Status.RUNNING)) {
            return;
        }
        task = new AsyncTask<Void, Void, Long>() {

            @Override
            protected Long doInBackground(Void... params) {
                try {
                    return getCount();
                } catch (Exception e) {
                }
                return null;
            }
            
            @Override
            protected void onPostExecute(Long count) {
                if (count == null || count.intValue() == 0) {
                    lastCount = null;
                    textView.setVisibility(View.GONE);
                } else {
                    lastCount = count;
                    textView.setText(count.toString());
                    textView.setVisibility(View.VISIBLE);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
