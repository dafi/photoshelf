package com.ternaryop.photoshelf.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.activity.TagPhotoBrowserActivity;
import com.ternaryop.photoshelf.db.PostTagDAO;
import com.ternaryop.photoshelf.db.TagCursorAdapter;

public class TagListFragment extends AbsPhotoShelfFragment implements OnItemClickListener {
    private ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list_tags, container, false);

        final TagCursorAdapter adapter = new TagCursorAdapter(
                getActivity(),
                android.R.layout.simple_list_item_1,
                getBlogName());
        
        listView = (ListView) rootView.findViewById(R.id.list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        listView.setTextFilterEnabled(true);
        // start with list filled
        adapter.getFilter().filter("");
        ((SearchView) rootView.findViewById(R.id.searchView1)).setOnQueryTextListener(new OnQueryTextListener() {
            
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
        return rootView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        Cursor cursor = (Cursor)parent.getItemAtPosition(position);
        String tag = cursor.getString(cursor.getColumnIndex(PostTagDAO.TAG));
        TagPhotoBrowserActivity.startPhotoBrowserActivity(getActivity(), getBlogName(), tag, false);
    }
}
