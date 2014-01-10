package com.ternaryop.photoshelf.dialogs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

import com.ternaryop.photoshelf.AppSupport;
import com.ternaryop.photoshelf.ImageInfo;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.db.TagCursorAdapter;
import com.ternaryop.photoshelf.parsers.TitleData;
import com.ternaryop.photoshelf.parsers.TitleParser;
import com.ternaryop.tumblr.AbsCallback;
import com.ternaryop.tumblr.Blog;
import com.ternaryop.tumblr.Callback;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.utils.DialogUtils;

public class TumblrPostDialog extends Dialog implements View.OnClickListener {

    private EditText postTitle;
    private AutoCompleteTextView postTags;
    private Spinner blogList;
    private List<String> imageUrls;
    private List<File> imageFiles;
    private AppSupport appSupport;
    private long postId;
    private OnClickListener dialogClickListener;
    private TagCursorAdapter tagAdapter;

    public TumblrPostDialog(Context context) {
        this(context, 0);
    }
    
    /**
     * @param context
     * @param postId, 0 to enable publish and draft otherwise edit the post
     */
    public TumblrPostDialog(Context context, long postId) {
        super(context);
        setContentView(R.layout.dialog_publish_post);

        this.postId = postId;

        postTitle = (EditText)findViewById(R.id.post_title);
        postTags = (AutoCompleteTextView)findViewById(R.id.post_tags);
        blogList = (Spinner) findViewById(R.id.blog);
        
        appSupport = new AppSupport(context);
        ((Button)findViewById(R.id.cancelButton)).setOnClickListener(this);
        ((Button)findViewById(R.id.parse_title_button)).setOnClickListener(this);

        tagAdapter = new TagCursorAdapter(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                "");
        tagAdapter.setBlogName(appSupport.getSelectedBlogName());
        postTags.setAdapter(tagAdapter);
        blogList.setOnItemSelectedListener(new BlogItemSelectedListener());        
        
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
            int size = imageUrls != null ? imageUrls.size() : imageFiles.size();
            if (size == 1) {
                setTitle(R.string.tumblr_post_title);
            } else {
                setTitle(getContext().getString(R.string.tumblr_multiple_post_title, size));
            }
        }
        // move caret to end
        postTitle.setSelection(postTitle.length());
        super.show();
    }
    
    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public void setImageUrlsFromImageInfo(List<ImageInfo> imageList) {
        ArrayList<String> tmpList = new ArrayList<String>(imageList.size());

        for (ImageInfo imageInfo : imageList) {
            tmpList.add(imageInfo.getDestinationDocumentURL());
        }
        this.imageUrls = tmpList;
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
                tagAdapter.setBlogName(selectedName);
                tagAdapter.notifyDataSetChanged();
            }
        }
    }
    
    private void fetchBlogNames() {
        findViewById(R.id.publish_button).setEnabled(false);
        findViewById(R.id.draft_button).setEnabled(false);

        Tumblr.getSharedTumblr(getContext()).getBlogList(new Callback<Blog[]>() {

            @Override
            public void complete(Blog[] result) {
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
            public void failure(Exception e) {
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
                    progressDialog.setMessage(getContext().getString(R.string.publishing_post));
                } else {
                    progressDialog.setMessage(getContext().getResources().getQuantityString(R.plurals.saving_post_in_draft, max, max));
                }
                progressDialog.setMax(max);
                progressDialog.show();
            }

            @Override
            public void failure(Exception ex) {
                progressDialog.dismiss();
                DialogUtils.showErrorDialog(getContext(), ex);
            }

            @Override
            public void complete(Long postId) {
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

            List<?> urlsOrFiles = getImageUrls() != null ? getImageUrls() : getImageFiles();
            final PostCallback callback = new PostCallback(urlsOrFiles.size(), publish);
            if (publish) {
                for (Object url : urlsOrFiles) {
                    Tumblr.getSharedTumblr(getContext()).publishPhotoPost(selectedBlogName,
                            url, getPostTitle(), getPostTags(),
                            callback);
                }
            } else {
                for (Object url : urlsOrFiles) {
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

        Tumblr.getSharedTumblr(getContext()).editPost(appSupport.getSelectedBlogName(), newValues, new AbsCallback(this, R.string.generic_error) {

            @Override
            public void complete(JSONObject result) {
                dismiss();
                if (dialogClickListener != null) {
                    dialogClickListener.onClick(TumblrPostDialog.this, BUTTON_POSITIVE);
                }
            }
        });
    }

    public void setEditButton(OnClickListener dialogClickListener) {
        this.dialogClickListener = dialogClickListener;
    }

    private void parseTitle() {
        TitleData titleData = TitleParser.instance().parseTitle(postTitle.getText().toString());
        setPostTitle(titleData.toHtml());
        setPostTags(titleData.getTags());
    }

    public List<File> getImageFiles() {
        return imageFiles;
    }

    public void setImageFiles(List<File> imageFiles) {
        this.imageFiles = imageFiles;
    }

    private class BlogItemSelectedListener implements OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            tagAdapter.setBlogName((String) blogList.getSelectedItem());
            tagAdapter.notifyDataSetChanged();
        }

        public void onNothingSelected(AdapterView<?> parent) {
        }
    }
}
