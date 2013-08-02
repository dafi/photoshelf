package com.ternaryop.phototumblrshare;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class TumblrActionModeCallback implements ActionMode.Callback {
	private ActionMode actionMode;
	private ImageAdapter imageAdapter;
	private final Activity activity;
	private String title = "<<NO TITLE>>";

	public TumblrActionModeCallback(Activity activity, ImageAdapter imageAdapter) {
		this.activity = activity;
		this.imageAdapter = imageAdapter;
	}

	public ActionMode startActionMode() {
		if (actionMode == null) {
			actionMode = activity.startActionMode(this);
		}
		return actionMode;
	}

	public void invalidate() {
		if (imageAdapter.getSelectedItems().size() == 0) {
			actionMode.finish();
		} else {
			actionMode.invalidate();
		}
	}
	
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.action_context, menu);
		return true;
	}

	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		menu.findItem(R.id.counter).setTitle(imageAdapter.getSelectedItems().size() + " urls");
		return true;
	}

	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
			case R.id.showDialog:
				new ImageLinkScraper(activity, title, imageAdapter)
					.execute(createUrlCSSSelectorMap());
				mode.finish();
				return true;
			default:
				return false;
		}
	}

	private Map<String, String> createUrlCSSSelectorMap() {
		Map<String, String> urlCSSSelectorMap = new HashMap<String, String>();
		ImageDOMSelectorFinder finder = new ImageDOMSelectorFinder(activity);
		
		for (ImageInfo imageInfo : imageAdapter.getSelectedItems()) {
			String selector = finder.getSelectorFromUrl(imageInfo.imageURL);
			if (selector != null) {
				urlCSSSelectorMap.put(imageInfo.imageURL, selector);
			}
		}

		return urlCSSSelectorMap;
	}

	// Called when the user exits the action mode
	public void onDestroyActionMode(ActionMode mode) {
		actionMode = null;
	}
}
