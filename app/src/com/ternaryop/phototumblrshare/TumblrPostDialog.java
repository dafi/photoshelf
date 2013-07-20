package com.ternaryop.phototumblrshare;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.ternaryop.tumblr.Blog;
import com.ternaryop.tumblr.Callback;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.utils.DialogUtils;

public class TumblrPostDialog extends Dialog {

	private static final String PREF_SELECTED_BLOG = "selectedBlog";
	private TextView imageUrl;
	private TextView title;
	private TextView tags;
	private Tumblr tumblr;
	private List<String> blogNames;
	private Activity activity;
	private Spinner blogList;
	private String PREFS_NAME = "tumblrShareImage";

	public TumblrPostDialog(Context context) {
		super(context);
		setContentView(R.layout.tumblr_post);
		setTitle(R.string.tumblr_post_title);

		imageUrl = (TextView)findViewById(R.id.imageUrl);
		title = (TextView)findViewById(R.id.title);
		tags = (TextView)findViewById(R.id.tags);
		blogList = (Spinner) findViewById(R.id.blog);
		
		activity = (Activity)context;
		((Button)findViewById(R.id.publishButton)).setOnClickListener(new OnClickPublishListener());
		((Button)findViewById(R.id.draftButton)).setOnClickListener(new OnClickPublishListener());
		((Button)findViewById(R.id.cancelButton)).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dismiss();
			}
			
		});
	}

	public String getImageUrl() {
		return imageUrl.getText().toString();
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl.setText(imageUrl);
	}

	public String getTitle() {
		return title.getText().toString();
	}

	public void setTitle(String title) {
		this.title.setText(title);
	}

	public String getTags() {
		return tags.getText().toString();
	}

	public void setTags(String tags) {
		this.tags.setText(tags);
	}
	
	@Override
	protected void onStart() {
		findViewById(R.id.publishButton).setEnabled(false);
		findViewById(R.id.draftButton).setEnabled(false);

		Tumblr.getTumblr(activity, new Callback<Void>() {
			@Override
			public void complete(Tumblr t, Void result) {
				tumblr = t;

				tumblr.getBlogList(new Callback<Blog[]>() {

					@Override
					public void complete(Tumblr tumblr, Blog[] result) {
						blogNames = new ArrayList<String>(result.length);
						for (int i = 0; i < result.length; i++) {
							blogNames.add(result[i].getName());
						}
						Collections.sort(blogNames);
						ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, blogNames);
						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						blogList.setAdapter(adapter);
						SharedPreferences preferences = activity.getSharedPreferences(PREFS_NAME, 0);
						String selectedBlog = preferences.getString(PREF_SELECTED_BLOG, null);
						if (selectedBlog != null) {
							int position = adapter.getPosition(selectedBlog);
							if (position >= 0) {
								blogList.setSelection(position);
							}
						}

						findViewById(R.id.publishButton).setEnabled(true);
						findViewById(R.id.draftButton).setEnabled(true);
					}

					@Override
					public void failure(Tumblr tumblr, Exception e) {
						DialogUtils.showErrorDialog(activity, e);
						dismiss();
					} 
				});
			}

			@Override
			public void failure(Tumblr tumblr, Exception ex) {
				DialogUtils.showErrorDialog(activity, ex);
			}
		});
	}

	private final class OnClickPublishListener implements View.OnClickListener {
		private final class PostCallback implements Callback<Long> {
			@Override
			public void failure(Tumblr tumblr, Exception ex) {
				progressDialog.dismiss();
				DialogUtils.showErrorDialog(activity, ex);
			}

			@Override
			public void complete(Tumblr tumblr, Long postId) {
				progressDialog.dismiss();
			}
		}

		ProgressDialog progressDialog;

		@Override
		public void onClick(final View v) {
			Tumblr.getTumblr(activity, new Callback<Void>() {

				@Override
				public void complete(Tumblr t, Void result) {
					boolean publish = v.getId() == R.id.publishButton;
					tumblr = t;
					progressDialog = new ProgressDialog(activity);
					progressDialog
							.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					progressDialog.setMessage(activity.getResources().getString(publish ? R.string.publishing_post : R.string.creating_a_post_in_draft));
					progressDialog.show();

					String selectedBlogName = (String) blogList
							.getSelectedItem();
					Editor edit = activity.getSharedPreferences(PREFS_NAME, 0)
							.edit();
					edit.putString(PREF_SELECTED_BLOG, selectedBlogName);
					edit.commit();

					if (publish) {
						tumblr.publishPhotoPost(selectedBlogName,
								getImageUrl(), getTitle(), getTags(),
								new PostCallback());
					} else {
						tumblr.draftPhotoPost(selectedBlogName, getImageUrl(),
								getTitle(), getTags(), new PostCallback());
					}
					dismiss();
				}

				@Override
				public void failure(Tumblr tumblr, Exception ex) {
					DialogUtils.showErrorDialog(activity, ex);
				}
			});
		}
	}

}
