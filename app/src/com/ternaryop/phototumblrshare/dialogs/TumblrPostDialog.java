package com.ternaryop.phototumblrshare.dialogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

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
import com.ternaryop.phototumblrshare.parsers.TitleData;
import com.ternaryop.phototumblrshare.parsers.TitleParser;
import com.ternaryop.tumblr.Blog;
import com.ternaryop.tumblr.Callback;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.utils.DialogUtils;

public class TumblrPostDialog extends Dialog implements View.OnClickListener {

	private EditText postTitle;
	private EditText postTags;
	private Spinner blogList;
	private String[] imageUrls;
	private AppSupport appSupport;
	private long postId;
	private OnClickListener dialogClickListener;

	public TumblrPostDialog(Context context) {
		this(context, 0);
	}
	
	/**
	 * If posdtId is greater that 0 that edit the post
	 * @param context
	 * @param postId
	 */
	public TumblrPostDialog(Context context, long postId) {
		super(context);
		setContentView(R.layout.dialog_publish_post);

		this.postId = postId;

		postTitle = (EditText)findViewById(R.id.post_title);
		postTags = (EditText)findViewById(R.id.post_tags);
		blogList = (Spinner) findViewById(R.id.blog);
		
		appSupport = new AppSupport(context);
		((Button)findViewById(R.id.cancelButton)).setOnClickListener(this);
		((Button)findViewById(R.id.parse_title_button)).setOnClickListener(this);
		
		if (postId > 0) {
			findViewById(R.id.publish_button).setVisibility(View.GONE);
			findViewById(R.id.draft_button).setVisibility(View.GONE);
			findViewById(R.id.blog_list).setVisibility(View.GONE);
			findViewById(R.id.edit_button).setVisibility(View.VISIBLE);
			((Button)findViewById(R.id.edit_button)).setOnClickListener(this);
		} else {
			((Button)findViewById(R.id.publish_button)).setOnClickListener(new OnClickPublishListener());
			((Button)findViewById(R.id.draft_button)).setOnClickListener(new OnClickPublishListener());
			((ImageButton)findViewById(R.id.refreshBlogList)).setOnClickListener(this);
		}
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
			case R.id.edit_button:
	            editPost();
	            return;
			case R.id.parse_title_button:
	            parseTitle();
	            return;
		}
	}
	
	@Override
	public void show() {
		if (postId > 0) {
			setTitle(R.string.edit_post_title);
		} else {
			if (imageUrls.length == 1) {
				setTitle(R.string.tumblr_post_title);
			} else {
				setTitle(getContext().getResources().getString(R.string.tumblr_multiple_post_title, imageUrls.length));
			}
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
			this.imageUrls[i] = imageList.get(0).getDestinationDocumentURL();
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
		if (postId <= 0) {
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
	}

	private void fillBlogList(List<String> blogNames) {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, blogNames);
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

		Tumblr.getSharedTumblr(getContext()).getBlogList(new Callback<Blog[]>() {

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
				dismiss();
				DialogUtils.showErrorDialog(getContext(), e);
			} 
		});
	}

	private final class OnClickPublishListener implements View.OnClickListener {
		private ProgressDialog progressDialog;

		private final class PostCallback implements Callback<Long> {
			public PostCallback (int max, boolean publish) {
				progressDialog = new ProgressDialog(getContext());
				progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				if (publish) {
					progressDialog.setMessage(getContext().getResources().getString(R.string.publishing_post));
				} else {
					int resId = max == 1 ? R.string.saving_post_in_draft_singular : R.string.saving_post_in_draft_plural;
					progressDialog.setMessage(getContext().getResources().getString(resId, max));
				}
				progressDialog.setMax(max);
				progressDialog.show();
			}

			@Override
			public void failure(Tumblr tumblr, Exception ex) {
				progressDialog.dismiss();
				DialogUtils.showErrorDialog(getContext(), ex);
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
			boolean publish = v.getId() == R.id.publish_button;
			String selectedBlogName = (String) blogList
					.getSelectedItem();
			appSupport.setSelectedBlogName(selectedBlogName);

			String[] urls = getImageUrls();
			final PostCallback callback = new PostCallback(urls.length, publish);
			if (publish) {
				for (String url : urls) {
					Tumblr.getSharedTumblr(getContext()).publishPhotoPost(selectedBlogName,
							url, getPostTitle(), getPostTags(),
							callback);
				}
			} else {
				for (String url : urls) {
					Tumblr.getSharedTumblr(getContext()).draftPhotoPost(selectedBlogName,
							url, getPostTitle(), getPostTags(),
							callback);
				}
			}
			dismiss();
		}
	}

	private void editPost() {
		final HashMap<String, String> newValues = new HashMap<String, String>();
		newValues.put("id", String.valueOf(postId));
		newValues.put("caption", getPostTitle());
		newValues.put("tags", getPostTags());

		Tumblr.getSharedTumblr(getContext()).editPost(appSupport.getSelectedBlogName(), newValues, new Callback<JSONObject>() {

			@Override
			public void complete(Tumblr tumblr, JSONObject result) {
				dismiss();
				if (dialogClickListener != null) {
					dialogClickListener.onClick(TumblrPostDialog.this, BUTTON_POSITIVE);
				}
			}

			@Override
			public void failure(Tumblr tumblr, Exception ex) {
				dismiss();
				DialogUtils.showErrorDialog(getContext(), ex);
			}
		});
	}

	public void setEditButton(OnClickListener dialogClickListener) {
		this.dialogClickListener = dialogClickListener;
	}

	private void parseTitle() {
		TitleData titleData = TitleParser.instance().parseTitle(postTitle.getText().toString());
		setPostTitle(titleData.toString());
		setPostTags(titleData.getTags());
	}
}
