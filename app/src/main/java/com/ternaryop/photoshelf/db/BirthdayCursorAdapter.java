package com.ternaryop.photoshelf.db;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.Filter;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.activity.TagPhotoBrowserActivity;
import com.ternaryop.utils.DateTimeUtils;
import com.ternaryop.utils.StringUtils;

/**
 * Used by searchView in actionBar
 * @author dave
 *
 */
public class BirthdayCursorAdapter extends SimpleCursorAdapter implements FilterQueryProvider, ViewBinder {
    public static final int SHOW_BIRTHDAYS_ALL = 1 << 0;
    public static final int SHOW_BIRTHDAYS_IGNORED = 1 << 1;
    public static final int SHOW_BIRTHDAYS_IN_SAME_DAY = 1 << 2;

    private final DBHelper dbHelper;
    private final Context context;
    private final SimpleDateFormat dateFormat;
    private int showFlags = SHOW_BIRTHDAYS_ALL;
    private String blogName;
    private String pattern = "";
    private int month;

    public BirthdayCursorAdapter(Context context, String blogName) {
        super(context,
                R.layout.list_row_2,
                null,
                new String[] { BirthdayDAO.NAME, BirthdayDAO.BIRTH_DATE },
                new int[] { android.R.id.text1, android.R.id.text2 },
                0);
        this.context = context;
        this.blogName = blogName;
        dbHelper = DBHelper.getInstance(context);
        dateFormat = new SimpleDateFormat("d MMMM, yyyy", Locale.US);

        setViewBinder(this);
        setFilterQueryProvider(this);
    }

    @Override
    public Cursor runQuery(CharSequence constraint) {
        this.pattern = constraint == null ? "" : constraint.toString().trim();
        if (isShowIgnored()) {
            return dbHelper.getBirthdayDAO().getIgnoredBirthdayCursor(pattern, blogName);
        } else if (isShowInSameDay()) {
            return dbHelper.getBirthdayDAO().getBirthdaysInSameDay(pattern, blogName);
        }
        return dbHelper.getBirthdayDAO().getBirthdayCursorByName(pattern, month, blogName);
    }

    public String convertToString(final Cursor cursor) {
        final int columnIndex = cursor.getColumnIndexOrThrow(BirthdayDAO.NAME);
        return cursor.getString(columnIndex);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);
        Calendar now = Calendar.getInstance();
        int dayOfMonth = now.get(Calendar.DAY_OF_MONTH);
        int month = now.get(Calendar.MONTH);
        Calendar date = Calendar.getInstance();

        try {
            String isoDate = cursor.getString(cursor.getColumnIndex(BirthdayDAO.BIRTH_DATE));
            if (isoDate == null) {
                view.setBackgroundResource(R.drawable.list_selector_post_group_even);
            } else {
                date.setTime(Birthday.ISO_DATE_FORMAT.parse(isoDate));
                if (date.get(Calendar.DAY_OF_MONTH) == dayOfMonth && date.get(Calendar.MONTH) == month) {
                    view.setBackgroundResource(R.drawable.list_selector_post_never);
                } else {
                    view.setBackgroundResource(R.drawable.list_selector_post_group_even);
                }
            }
        } catch (ParseException ignored) {
        }
    }

    @Override
    public boolean setViewValue(final View view, final Cursor cursor, final int columnIndex) {
        if (columnIndex == cursor.getColumnIndexOrThrow(BirthdayDAO.NAME)) {
            if (pattern.length() > 0) {
                final String htmlHighlightPattern = StringUtils.htmlHighlightPattern(
                        pattern, cursor.getString(columnIndex));
                final Spanned spanned = Html.fromHtml(htmlHighlightPattern);
                ((TextView) view).setText(spanned);
            } else {
                ((TextView) view).setText(cursor.getString(columnIndex));
            }
        } else if (columnIndex == cursor.getColumnIndexOrThrow(BirthdayDAO.BIRTH_DATE)) {
            try {
                String isoDate = cursor.getString(columnIndex);
                if (isoDate == null) {
                    view.setVisibility(View.GONE);
                } else {
                    Calendar c = Calendar.getInstance();
                    c.setTime(Birthday.ISO_DATE_FORMAT.parse(isoDate));

                    String age = String.valueOf(DateTimeUtils.yearsBetweenDates(c, Calendar.getInstance()));
                    String dateStr = dateFormat.format(c.getTime());

                    view.setVisibility(View.VISIBLE);
                    ((TextView) view).setText(context.getString(R.string.name_with_age, dateStr, age));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public String getBlogName() {
        return blogName;
    }

    public void setBlogName(String blogName) {
        this.blogName = blogName;
    }

    public void browsePhotos(Activity activity, int position) {
        String tag = getCursor().getString(getCursor().getColumnIndex(BirthdayDAO.NAME));
        TagPhotoBrowserActivity.startPhotoBrowserActivity(activity, getBlogName(), tag, false);

    }

    public Birthday getBirthdayItem(int index) {
        return BirthdayDAO.getBirthday((Cursor)getItem(index));
    }

    public void refresh(Filter.FilterListener filterListener) {
        getFilter().filter(pattern, filterListener);
        notifyDataSetChanged();
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int findDayPosition(int day) {
        if (day < 0 || day > 31) {
            return -1;
        }
        for (int i = 0; i < getCount(); i++) {
            Cursor cursor = (Cursor) getItem(i);
            String isoDate = cursor.getString(cursor.getColumnIndex(BirthdayDAO.BIRTH_DATE));

            if (isoDate == null) {
                continue;
            }
            int bday = Integer.parseInt(isoDate.substring(isoDate.length() - 2));

            // move to bday or closest one
            if (bday >= day) {
                return i;
            }
        }
        return -1;
    }

    public boolean isShowIgnored() {
        return (showFlags & SHOW_BIRTHDAYS_IGNORED) != 0;
    }

    public boolean isShowInSameDay() {
        return (showFlags & SHOW_BIRTHDAYS_IN_SAME_DAY) != 0;
    }

    public boolean isShowFlag(int value) {
        return (showFlags & value) != 0;
    }

    public void setShow(int value, boolean show) {
        if ((value & SHOW_BIRTHDAYS_ALL) != 0) {
            // SHOW_BIRTHDAYS_ALL is the default value and it can't be hidden
            showFlags = SHOW_BIRTHDAYS_ALL;
        } else if ((value & SHOW_BIRTHDAYS_IGNORED) != 0) {
            showFlags = show ? SHOW_BIRTHDAYS_IGNORED : SHOW_BIRTHDAYS_ALL;
        } else if ((value & SHOW_BIRTHDAYS_IN_SAME_DAY) != 0) {
            showFlags = show ? SHOW_BIRTHDAYS_IN_SAME_DAY : SHOW_BIRTHDAYS_ALL;
        }
    }

}
