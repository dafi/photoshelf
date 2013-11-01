package com.ternaryop.phototumblrshare.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.db.PostTagDAO;
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
				getBlogName());
		
		listView = (ListView) findViewById(R.id.list);
		listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        listView.setTextFilterEnabled(true);
        // start with list filled
        adapter.getFilter().filter("");
        ((SearchView) findViewById(R.id.searchView1)).setOnQueryTextListener(new OnQueryTextListener() {
			
			@Override
			public boolean onQueryTextSubmit(String query) {
				return false;
			}
			
			@Override
			public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);                           
				return true;
			}
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
		String tag = cursor.getString(cursor.getColumnIndex(PostTagDAO.TAG));
		TagPhotoBrowserActivity.startPhotoBrowserActivity(this, getBlogName(), tag);
	}
}
