package com.ternaryop.photoshelf.counter;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.TextView;

public abstract class AbsCountRetriever implements CountRetriever, CountChangedListener {
    private Context context;
    private String blogName;
    private AsyncTask<Void, Void, Long> task;
    private Long lastCount;
    private BaseAdapter adapter;

    public AbsCountRetriever(Context context, String blogName, BaseAdapter adapter) {
        this.context = context;
        this.blogName = blogName;
        this.adapter = adapter;
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

    /**
     * This method will be executed always asynchronously
     * @return the number of items for counter or null if error occurred
     */
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
                } catch (Exception ignored) {
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
    
    @Override
    public void onChangeCount(CountProvider provider, long newCount) {
        if (lastCount != null && lastCount == newCount) {
            return;
        }
        lastCount = newCount;
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public BaseAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(BaseAdapter adapter) {
        this.adapter = adapter;
    }
}
