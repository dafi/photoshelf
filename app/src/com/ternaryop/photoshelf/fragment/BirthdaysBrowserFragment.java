package com.ternaryop.photoshelf.fragment;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.SearchView;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.db.Birthday;
import com.ternaryop.photoshelf.db.BirthdayCursorAdapter;
import com.ternaryop.photoshelf.db.DBHelper;

public class BirthdaysBrowserFragment extends AbsPhotoShelfFragment implements AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener, ActionBar.OnNavigationListener {
    protected enum ITEM_ACTION {
        DELETE
    };
    private ListView listView;
    private BirthdayCursorAdapter birthdayAdapter;
    private int[] singleSelectionMenuIds;
    private boolean alreadyScrolledToFirstBirthday;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_birthdays_by_name, container, false);

        birthdayAdapter = new BirthdayCursorAdapter(
                getActivity(),
                fragmentActivityStatus.getAppSupport().getSelectedBlogName());
        // listView is filled from onNavigationItemSelected
        listView = (ListView) rootView.findViewById(R.id.list);
        listView.setAdapter(birthdayAdapter);
        listView.setTextFilterEnabled(true);
        listView.setOnItemClickListener(this);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(this);

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
        rootView.findViewById(R.id.searchView1).requestFocus();

        setupActionBar();
        setHasOptionsMenu(true);

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

    protected int getActionModeMenuId() {
        return R.menu.birthdays_context;
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle(R.string.select_items);
        mode.setSubtitle(getResources().getQuantityString(R.plurals.selected_items, 1, 1));
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(getActionModeMenuId(), menu);
        return true;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    protected List<Birthday> getSelectedPosts() {
        SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();
        ArrayList<Birthday> list = new ArrayList<Birthday>();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            int key = checkedItemPositions.keyAt(i);
            if (checkedItemPositions.get(key)) {
                list.add(birthdayAdapter.getBirthdayItem(key));
            }
        }
        return list;
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_delete:
                showConfirmDialog(ITEM_ACTION.DELETE, mode);
                return true;
            default:
                return false;
        }
    }
    public void onDestroyActionMode(ActionMode mode) {
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position,
                                          long id, boolean checked) {
        int selectCount = listView.getCheckedItemCount();
        boolean singleSelection = selectCount == 1;

        for (int itemId : getSingleSelectionMenuIds()) {
            MenuItem item = mode.getMenu().findItem(itemId);
            if (item != null) {
                item.setVisible(singleSelection);
            }
        }

        mode.setSubtitle(getResources().getQuantityString(
                R.plurals.selected_items,
                selectCount,
                selectCount));
    }

    protected int[] getSingleSelectionMenuIds() {
        if (singleSelectionMenuIds == null) {
            singleSelectionMenuIds = new int[] {};
        }
        return singleSelectionMenuIds;
    }

    private void showConfirmDialog(final ITEM_ACTION postAction, final ActionMode mode) {
        final List<Birthday> birthdays = getSelectedPosts();
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        switch (postAction) {
                            case DELETE:
                                deletePost(birthdays, mode);
                                break;
                        }
                        break;
                }
            }
        };

        String message = null;
        switch (postAction) {
            case DELETE:
                message = getResources().getQuantityString(R.plurals.delete_items_confirm,
                        birthdays.size(),
                        birthdays.size(),
                        birthdays.get(0).getName());
                break;
        }

        new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, dialogClickListener)
                .setNegativeButton(android.R.string.no, dialogClickListener)
                .show();
    }

    private void deletePost(List<Birthday> list, final ActionMode mode) {
        for (Birthday b : list) {
            DBHelper.getInstance(getActivity()).getBirthdayDAO().remove(b.getId());
        }
        birthdayAdapter.refresh(null);
        mode.finish();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        ActionBar actionBar = getActivity().getActionBar();

        if (fragmentActivityStatus.isDrawerOpen()) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayShowTitleEnabled(true);
        } else {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        super.onPrepareOptionsMenu(menu);
    }

    private void setupActionBar() {
        ActionBar actionBar = getActivity().getActionBar();
        String months[] = new String[13];
        months[0] = getString(R.string.all);
        System.arraycopy(new DateFormatSymbols().getMonths(), 0, months, 1, 12);
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<String>(
                actionBar.getThemedContext(),
                android.R.layout.simple_spinner_item,
                months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setListNavigationCallbacks(monthAdapter, this);
        actionBar.setSelectedNavigationItem(Calendar.getInstance().get(Calendar.MONTH) + 1);
    }

    @Override
    public boolean onNavigationItemSelected(final int itemPosition, long itemId) {
        birthdayAdapter.setMonth(itemPosition);
        birthdayAdapter.refresh(new Filter.FilterListener() {
            public void onFilterComplete(int count) {
                // when month changes scroll to first item unless must be scroll to first birthday item
                if (!alreadyScrolledToFirstBirthday) {
                    alreadyScrolledToFirstBirthday = true;
                    int pos = birthdayAdapter.findDayPosition(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
                    if (pos >= 0) {
                        listView.setSelection(pos);
                    }
                } else {
                    listView.setSelection(0);
                }
            }
        });

        return true;
    }
}
