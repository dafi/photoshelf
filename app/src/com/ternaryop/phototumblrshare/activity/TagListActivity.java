package com.ternaryop.phototumblrshare.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.db.PostTag;
import com.ternaryop.phototumblrshare.db.TagCursorAdapter;

public class TagListActivity extends PhotoTumblrActivity implements OnItemClickListener {
	private ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_tags);
		final TagCursorAdapter adapter = new TagCursorAdapter(
				this,
				android.R.layout.simple_list_item_1,
				appSupport.getSelectedBlogName());
		
		listView = (ListView) findViewById(R.id.list);
		listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        listView.setTextFilterEnabled(true);
        // start with list filled
        adapter.getFilter().filter("");
        ((EditText)findViewById(R.id.text1)).addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s.toString());                           
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}
        });
	}

	public static void startTagListActivity(Context context, String blogName, String postTag) {
		Intent intent = new Intent(context, TagListActivity.class);
		Bundle bundle = new Bundle();

		intent.putExtras(bundle);

		context.startActivity(intent);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		Cursor cursor = (Cursor)parent.getItemAtPosition(position);
		String tag = cursor.getString(cursor.getColumnIndex(PostTag.TAG));
		TagPhotoBrowserActivity.startPhotoBrowserActivity(this, appSupport.getSelectedBlogName(), tag);
	}
}
