package com.ternaryop.phototumblrshare.dialogs;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.ternaryop.phototumblrshare.AppSupport;
import com.ternaryop.phototumblrshare.ImageInfo;
import com.ternaryop.phototumblrshare.R;
import com.ternaryop.tumblr.Blog;
import com.ternaryop.tumblr.Callback;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.utils.DialogUtils;

public class TumblrPostDialog extends Dialog implements View.OnClickListener {

	private EditText postTitle;
	private EditText postTags;
	private Tumblr tumblr;
	private Activity activity;
	private Spinner blogList;
	private String[] imageUrls;
	private AppSupport appSupport;

	public TumblrPostDialog(Context context) {
		super(context);
		setContentView(R.layout.dialog_publish_post);

		postTitle = (EditText)findViewById(R.id.post_title);
		postTags = (EditText)findViewById(R.id.post_tags);
		blogList = (Spinner) findViewById(R.id.blog);
		
		activity = (Activity)context;
		appSupport = new AppSupport(context);
		((Button)findViewById(R.id.publish_button)).setOnClickListener(new OnClickPublishListener());
		((Button)findViewById(R.id.draft_button)).setOnClickListener(new OnClickPublishListener());
		((ImageButton)findViewById(R.id.refreshBlogList)).setOnClickListener(this);
		((Button)findViewById(R.id.cancelButton)).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.cancelButton:
				dismiss();
				return;
			case R.id.refreshBlogList:
				fetchBlogNames();
				return;
		}
	}
	
	@Override
	public void show() {
		if (imageUrls.length == 1) {
			setTitle(R.string.tumblr_post_title);
		} else {
			setTitle(getContext().getResources().getString(R.string.tumblr_multiple_post_title, imageUrls.length));
		}
		super.show();
	}
	
	public String[] getImageUrls() {
		return imageUrls;
	}

	public void setImageUrls(List<String> imageUrls) {
		this.imageUrls = imageUrls.toArray(new String[imageUrls.size()]);
	}

	public void setImageUrlsFromImageInfo(List<ImageInfo> imageList) {
		this.imageUrls = new String[imageList.size()];

		for (int i = 0; i < imageList.size(); i++) {
			this.imageUrls[i] = imageList.get(0).imageURL;
		}
	}

	public String getPostTitle() {
		return Html.toHtml(postTitle.getText());
	}

	public void setPostTitle(String title) {
		this.postTitle.setText(Html.fromHtml(title));
	}

	public String getPostTags() {
		return postTags.getText().toString();
	}

	public void setPostTags(List<String> tags) {
		// show only first tag
		this.postTags.setText(tags.isEmpty() ? "" : tags.get(0));
	}
	
	@Override
	protected void onStart() {
		findViewById(R.id.publish_button).setEnabled(false);
		findViewById(R.id.draft_button).setEnabled(false);

		List<String> blogSetNames = appSupport.getBlogList();
		if (blogSetNames == null) {
			fetchBlogNames();
		} else {
			fillBlogList(blogSetNames);
			findViewById(R.id.publish_button).setEnabled(true);
			findViewById(R.id.draft_button).setEnabled(true);
		}
	}

	private void fillBlogList(List<String> blogNames) {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, blogNames);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		blogList.setAdapter(adapter);

		String selectedName = appSupport.getSelectedBlogName();
		if (selectedName != null) {
			int position = adapter.getPosition(selectedName);
			if (position >= 0) {
				blogList.setSelection(position);
			}
		}
	}
	
	private void fetchBlogNames() {
		findViewById(R.id.publish_button).setEnabled(false);
		findViewById(R.id.draft_button).setEnabled(false);
		Tumblr.getTumblr(activity, new Callback<Void>() {
			@Override
			public void complete(Tumblr t, Void result) {
				tumblr = t;

				tumblr.getBlogList(new Callback<Blog[]>() {

					@Override
					public void complete(Tumblr tumblr, Blog[] result) {
						List<String> blogNames = new ArrayList<String>(result.length);
						for (int i = 0; i < result.length; i++) {
							blogNames.add(result[i].getName());
						}
						appSupport.setBlogList(blogNames);
						fillBlogList(blogNames);
						findViewById(R.id.publish_button).setEnabled(true);
						findViewById(R.id.draft_button).setEnabled(true);
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
		private ProgressDialog progressDialog;

		private final class PostCallback implements Callback<Long> {
			public PostCallback (int max, boolean publish) {
				progressDialog = new ProgressDialog(activity);
				progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				progressDialog.setMessage(activity.getResources().getString(publish ? R.string.publishing_post : R.string.creating_a_post_in_draft));
				progressDialog.setMax(max);
				progressDialog.show();
			}

			@Override
			public void failure(Tumblr tumblr, Exception ex) {
				progressDialog.dismiss();
				DialogUtils.showErrorDialog(activity, ex);
			}

			@Override
			public void complete(Tumblr tumblr, Long postId) {
				progressDialog.incrementProgressBy(1);
				if ((progressDialog.getProgress()) >= progressDialog.getMax()) {
					progressDialog.dismiss();
				}
			}
		}

		@Override
		public void onClick(final View v) {
			Tumblr.getTumblr(activity, new Callback<Void>() {

				@Override
				public void complete(final Tumblr t, Void result) {
					boolean publish = v.getId() == R.id.publish_button;
					tumblr = t;
					String selectedBlogName = (String) blogList
							.getSelectedItem();
					appSupport.setSelectedBlogName(selectedBlogName);

					String[] urls = getImageUrls();
					final PostCallback callback = new PostCallback(urls.length, publish);
					if (publish) {
						for (String url : urls) {
							tumblr.publishPhotoPost(selectedBlogName,
									url, getPostTitle(), getPostTags(),
									callback);
						}
					} else {
						for (String url : urls) {
							tumblr.draftPhotoPost(selectedBlogName,
									url, getPostTitle(), getPostTags(),
									callback);
						}
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
