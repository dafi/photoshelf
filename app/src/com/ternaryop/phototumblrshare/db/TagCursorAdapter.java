package com.ternaryop.phototumblrshare.db;

import android.content.Context;
import android.database.Cursor;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.utils.StringUtils;

/**
 * Used by searchView in actionBar
 * @author dave
 *
 */
public class TagCursorAdapter extends SimpleCursorAdapter implements FilterQueryProvider, ViewBinder {
	private String tumblrName;
	private DBHelper dbHelper;
	private String pattern = "";
	private Context context;

	public TagCursorAdapter(Context context, int resId, String tumblrName) {
		super(context,
				resId,
				null,
				new String[] { PostTag.TAG },
				new int[] { android.R.id.text1 },
				0);
		this.context = context;
		this.tumblrName = tumblrName;
		dbHelper = DBHelper.getInstance(context);

		setViewBinder(this);
		setFilterQueryProvider(this);
	}

	@Override
	public Cursor runQuery(CharSequence constraint) {
		this.pattern = constraint == null ? "" : constraint.toString().trim();
		return dbHelper.getPostTagDAO().getCursorByTag(pattern, tumblrName);
	}

	public String convertToString(final Cursor cursor) {
		final int columnIndex = cursor.getColumnIndexOrThrow(PostTag.TAG);
		return cursor.getString(columnIndex);
	}

	@Override
	public boolean setViewValue(final View view, final Cursor cursor, final int columnIndex) {
		final int countColumnIndex = cursor.getColumnIndexOrThrow("post_count");
		final int postCount = cursor.getInt(countColumnIndex);
		if (pattern.length() > 0) {
			final String htmlHighlightPattern = StringUtils.htmlHighlightPattern(
					pattern, cursor.getString(columnIndex));
			final Spanned spanned = Html.fromHtml(context.getString(R.string.tag_with_post_count, htmlHighlightPattern, postCount));
			((TextView) view).setText(spanned);
		} else {
			((TextView) view).setText(context.getString(R.string.tag_with_post_count, cursor.getString(columnIndex), postCount));
		}
		return true;
	}

}
