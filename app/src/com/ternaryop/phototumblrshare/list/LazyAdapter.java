package com.ternaryop.phototumblrshare.list;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.fedorvlasov.lazylist.ImageLoader;
import com.ternaryop.phototumblrshare.R;
 
public class LazyAdapter extends BaseAdapter {
    public static final String KEY_ID = "id";
    public static final String KEY_TITLE = "title";
    public static final String KEY_TIME = "time";
    public static final String KEY_THUMB_URL = "thumbUrl";
    public static final String KEY_IMAGE_URL = "imageUrl";
    public static final String KEY_LAST_PUBLISH = "lastPublish";
    
    // possible values for KEY_LAST_PUBLISH
    public static final String KEY_PUBLISH_NEVER = "never";
    public static final String KEY_PUBLISH_PAST = "past";
    public static final String KEY_PUBLISH_FUTURE = "future";

    private Activity activity;
    private ArrayList<HashMap<String, String>> items;
    private static LayoutInflater inflater = null;
    public ImageLoader imageLoader;
 
    public LazyAdapter(Activity a) {
        activity = a;
        items = new ArrayList<HashMap<String, String>>();
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        imageLoader = new ImageLoader(activity.getApplicationContext());
    }
 
    public int getCount() {
        return items.size();
    }
 
    public Object getItem(int position) {
        return items.get(position);
    }
 
    public long getItemId(int position) {
        return position;
    }
 
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        if (convertView == null) {
            vi = inflater.inflate(R.layout.list_row, null);
        }

        HashMap<String, String> post = items.get(position);
        String lastPublish = post.get(KEY_LAST_PUBLISH);

        if (KEY_PUBLISH_NEVER.equals(lastPublish)) {
            vi.setBackgroundResource(R.drawable.list_selector_post_never);
        } else if (KEY_PUBLISH_FUTURE.equals(lastPublish)) {
            vi.setBackgroundResource(R.drawable.list_selector_post_future);
        } else {
            vi.setBackgroundResource(R.drawable.list_selector);
        }

        TextView title = (TextView)vi.findViewById(R.id.title);
        TextView timeDesc = (TextView)vi.findViewById(R.id.time_desc);
        ImageView thumbImage = (ImageView)vi.findViewById(R.id.list_image);
        title.setText(Html.fromHtml(post.get(KEY_TITLE)).toString().replaceAll("\n", ""));
        timeDesc.setText(post.get(KEY_TIME));
        imageLoader.DisplayImage(post.get(KEY_THUMB_URL), thumbImage);
        return vi;
    }
    
    public void addItem(HashMap<String, String> map) {
    	items.add(map);
    }
    
    public void remove(int position) {
    	items.remove(position);
    }
    
    public void clear() {
    	items.clear();
    }
}