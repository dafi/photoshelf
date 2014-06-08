package com.ternaryop.photoshelf.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.db.BirthdayCursorAdapter;

public class BirthdaysByNameFragment extends AbsPhotoShelfFragment implements AdapterView.OnItemClickListener {
    private ListView listView;
    private BirthdayCursorAdapter birthdayAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_birthdays_by_name, container, false);

        birthdayAdapter = new BirthdayCursorAdapter(
                getActivity(),
                fragmentActivityStatus.getAppSupport().getSelectedBlogName());
        listView = (ListView) rootView.findViewById(R.id.list);
        listView.setAdapter(birthdayAdapter);
        listView.setTextFilterEnabled(true);
        listView.setOnItemClickListener(this);
        // start with list filled
        birthdayAdapter.getFilter().filter("");
        ((SearchView) rootView.findViewById(R.id.searchView1)).setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                birthdayAdapter.getFilter().filter(newText);
                return true;
            }
        });

        return rootView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        birthdayAdapter.browsePhotos(getActivity(), position);
    }

    @Override
    public void setRetainInstance(boolean retain) {
        // retain can't be set to true when calling nested fragment like tabs
        // so we ignore any call
    }
}
