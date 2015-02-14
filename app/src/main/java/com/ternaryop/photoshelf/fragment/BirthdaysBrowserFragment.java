package com.ternaryop.photoshelf.fragment;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
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
import android.widget.DatePicker;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.db.Birthday;
import com.ternaryop.photoshelf.db.BirthdayCursorAdapter;
import com.ternaryop.photoshelf.db.DBHelper;

public class BirthdaysBrowserFragment extends AbsPhotoShelfFragment implements AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener, AdapterView.OnItemSelectedListener {

    private Spinner toolbarSpinner;
    private int currentSelectedItemId = R.id.action_show_all;
    private final static int[] MISSING_BIRTHDAYS_ITEMS = new int[] {R.id.item_edit};

    protected enum ITEM_ACTION {
        MARK_AS_IGNORED,
        DELETE
    }

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
            case R.id.item_mark_as_ignored:
                showConfirmDialog(ITEM_ACTION.MARK_AS_IGNORED, mode);
                return true;
            case R.id.item_edit:
                showEditBirthdateDialog(mode);
                return true;
            default:
                return false;
        }
    }

    private void showEditBirthdateDialog(final ActionMode mode) {
        final List<Birthday> birthdays = getSelectedPosts();
        if (birthdays.size() != 1) {
            return;
        }
        final Calendar c = Calendar.getInstance();
        final Birthday birthday = birthdays.get(0);
        final Date date = birthday.getBirthDate();
        if (date != null) {
            c.setTime(date);
        }
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                c.set(Calendar.YEAR, year);
                c.set(Calendar.MONTH, month);
                c.set(Calendar.DAY_OF_MONTH, day);

                birthday.setBirthDate(c.getTime());
                if (birthday.getId() < 0) {
                    DBHelper.getInstance(getActivity()).getBirthdayDAO().insert(birthday);
                } else {
                    DBHelper.getInstance(getActivity()).getBirthdayDAO().update(birthday);
                }
                birthdayAdapter.refresh(null);
                mode.finish();
            }
        }, year, month, day).show();
    }

    public void onDestroyActionMode(ActionMode mode) {
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position,
                                          long id, boolean checked) {
        int selectCount = listView.getCheckedItemCount();
        boolean singleSelection = selectCount == 1;

        Menu menu = mode.getMenu();
        if (birthdayAdapter.isShowMissing()) {
            for (int i = 0; i < menu.size(); i++) {
                int itemId = menu.getItem(i).getItemId();
                boolean showMenu = false;
                for (int mitemId : MISSING_BIRTHDAYS_ITEMS) {
                    if (mitemId == itemId) {
                        showMenu = true;
                        break;
                    }
                }
                menu.getItem(i).setVisible(showMenu);
            }
        }

        for (int itemId : getSingleSelectionMenuIds()) {
            MenuItem item = menu.findItem(itemId);
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
            singleSelectionMenuIds = new int[] {R.id.item_edit};
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
                                deleteBirthdays(birthdays, mode);
                                break;
                            case MARK_AS_IGNORED:
                                markAsIgnored(birthdays, mode);
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
            case MARK_AS_IGNORED:
                message = getResources().getQuantityString(R.plurals.update_items_confirm,
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

    private void markAsIgnored(List<Birthday> birthdays, ActionMode mode) {
        for (Birthday b : birthdays) {
            DBHelper.getInstance(getActivity()).getBirthdayDAO().markAsIgnored(b.getId());
        }
        birthdayAdapter.refresh(null);
        mode.finish();
    }

    private void deleteBirthdays(List<Birthday> list, final ActionMode mode) {
        for (Birthday b : list) {
            DBHelper.getInstance(getActivity()).getBirthdayDAO().remove(b.getId());
        }
        birthdayAdapter.refresh(null);
        mode.finish();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.birthdays_browser, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // if selected item is already selected don't change anything
        if (currentSelectedItemId == item.getItemId()) {
            return true;
        }
        boolean isChecked = !item.isChecked();
        int showFlag;

        currentSelectedItemId = item.getItemId();

        CharSequence subTitle;
        switch (item.getItemId()) {
            case R.id.action_show_all:
                setSpinnerVisibility(true);
                showFlag = BirthdayCursorAdapter.SHOW_BIRTHDAYS_ALL;
                subTitle = null;
                break;
            case R.id.action_show_ignored:
                setSpinnerVisibility(false);
                subTitle = item.getTitle();
                showFlag = BirthdayCursorAdapter.SHOW_BIRTHDAYS_IGNORED;
                break;
            case R.id.action_show_birthdays_in_same_day:
                setSpinnerVisibility(false);
                subTitle = item.getTitle();
                showFlag = BirthdayCursorAdapter.SHOW_BIRTHDAYS_IN_SAME_DAY;
                break;
            case R.id.action_show_birthdays_missing:
                setSpinnerVisibility(false);
                subTitle = item.getTitle();
                showFlag = BirthdayCursorAdapter.SHOW_BIRTHDAYS_MISSING;
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        getSupportActionBar().setSubtitle(subTitle);
        item.setChecked(isChecked);
        birthdayAdapter.setShow(showFlag, isChecked);
        birthdayAdapter.refresh(null);
        return true;
    }

    private void setSpinnerVisibility(boolean visible) {
        if (visible) {
            toolbarSpinner.setVisibility(View.VISIBLE);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        } else {
            toolbarSpinner.setVisibility(View.GONE);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        ActionBar actionBar = getSupportActionBar();

        if (fragmentActivityStatus.isDrawerOpen()) {
            fragmentActivityStatus.getToolbar().removeView(toolbarSpinner);
            actionBar.setDisplayShowTitleEnabled(true);
        } else {
            // check if view is already added (eg when the overflow menu is opened)
            if (birthdayAdapter.isShowFlag(BirthdayCursorAdapter.SHOW_BIRTHDAYS_ALL) && fragmentActivityStatus.getToolbar().indexOfChild(toolbarSpinner) == -1) {
                fragmentActivityStatus.getToolbar().addView(toolbarSpinner);
                actionBar.setDisplayShowTitleEnabled(false);
            }
            menu.findItem(currentSelectedItemId).setChecked(true);
        }
        super.onPrepareOptionsMenu(menu);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);

        String months[] = new String[13];
        months[0] = getString(R.string.all);
        System.arraycopy(new DateFormatSymbols().getMonths(), 0, months, 1, 12);
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<String>(
                actionBar.getThemedContext(),
                android.R.layout.simple_spinner_item,
                months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        toolbarSpinner = (Spinner) LayoutInflater.from(actionBar.getThemedContext())
                .inflate(R.layout.toolbar_spinner,
                        fragmentActivityStatus.getToolbar(),
                        false);
        toolbarSpinner.setAdapter(monthAdapter);
        toolbarSpinner.setSelection(Calendar.getInstance().get(Calendar.MONTH) + 1);
        toolbarSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        birthdayAdapter.setMonth(pos);
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
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
