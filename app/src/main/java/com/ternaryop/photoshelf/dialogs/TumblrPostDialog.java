package com.ternaryop.photoshelf.dialogs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;

import com.ternaryop.photoshelf.AppSupport;
import com.ternaryop.photoshelf.ImageInfo;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.db.DBHelper;
import com.ternaryop.photoshelf.db.TagCursorAdapter;
import com.ternaryop.photoshelf.parsers.TitleData;
import com.ternaryop.photoshelf.parsers.TitleParser;
import com.ternaryop.photoshelf.service.PublishIntentService;
import com.ternaryop.tumblr.AbsCallback;
import com.ternaryop.tumblr.Blog;
import com.ternaryop.tumblr.Callback;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.utils.DialogUtils;
import org.json.JSONObject;

public class TumblrPostDialog extends Dialog implements View.OnClickListener {

    private final EditText postTitle;
    private final MultiAutoCompleteTextView postTags;
    private final Spinner blogList;
    private final AppSupport appSupport;
    private final long postId;
    private final TagCursorAdapter tagAdapter;
    private List<String> imageUrls;
    private List<File> imageFiles;
    private OnClickListener dialogClickListener;
    private boolean blockUIWhilePublish;
    private String sourceTitle;

    public TumblrPostDialog(Context context) {
        this(context, 0);
    }
    
    /**
     * @param context the context
     * @param postId, 0 to enable publish and draft otherwise edit the post
     */
    public TumblrPostDialog(Context context, long postId) {
        super(context);
        setContentView(R.layout.dialog_publish_post);

        this.postId = postId;

        postTitle = (EditText)findViewById(R.id.post_title);
        postTags = (MultiAutoCompleteTextView)findViewById(R.id.post_tags);
        blogList = (Spinner) findViewById(R.id.blog);
        
        appSupport = new AppSupport(context);
        findViewById(R.id.cancelButton).setOnClickListener(this);
        findViewById(R.id.parse_title_button).setOnClickListener(this);
        findViewById(R.id.source_title_button).setOnClickListener(this);

        tagAdapter = new TagCursorAdapter(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                "");
        tagAdapter.setBlogName(appSupport.getSelectedBlogName());
        postTags.setAdapter(tagAdapter);
        postTags.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        blogList.setOnItemSelectedListener(new BlogItemSelectedListener());        
        
        if (postId > 0) {
            findViewById(R.id.publish_button).setVisibility(View.GONE);
            findViewById(R.id.draft_button).setVisibility(View.GONE);
            findViewById(R.id.blog_list).setVisibility(View.GONE);
            findViewById(R.id.edit_button).setVisibility(View.VISIBLE);
            findViewById(R.id.edit_button).setOnClickListener(this);
        } else {
            findViewById(R.id.publish_button).setOnClickListener(new OnClickPublishListener());
            findViewById(R.id.draft_button).setOnClickListener(new OnClickPublishListener());
            findViewById(R.id.refreshBlogList).setOnClickListener(this);
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
            case R.id.source_title_button:
                fillWithSourceTitle();
        }
    }

    @Override
    public void show() {
        ((CheckBox)findViewById(R.id.block_ui)).setChecked(blockUIWhilePublish);

        if (postId > 0) {
            setTitle(R.string.edit_post_title);
        } else {
            int size = imageUrls != null ? imageUrls.size() : imageFiles.size();
            setTitle(getContext().getResources().getQuantityString(
                    R.plurals.post_image,
                    size,
                    size));
        }
        // move caret to end
        postTitle.setSelection(postTitle.length());
        super.show();
    }
    
    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = new ArrayList<String>(imageUrls);
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

    /**
     * Set the formatted and the source unformatted titles
     * @param htmlTitle the formatted title
     * @param sourceTitle the source title, can be in HTML format
     */
    public void setPostTitle(String htmlTitle, String sourceTitle) {
        this.sourceTitle = sourceTitle;
        this.postTitle.setText(Html.fromHtml(htmlTitle));
    }

    public String getPostTags() {
        return postTags.getText().toString();
    }

    public void setPostTags(List<String> tags) {
        final String firstTag = tags.isEmpty() ? "" : tags.get(0);
        this.postTags.setText(TextUtils.join(", ", tags));

        if (!firstTag.isEmpty()) {
            // if tag doesn't exist then show the textfield in red
            final Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    long count = DBHelper.getInstance(getContext()).getPostTagDAO()
                            .getPostCountByTag(firstTag, appSupport.getSelectedBlogName());
                    if (count == 0) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                postTags.setTextColor(Color.WHITE);
                                postTags.setBackgroundColor(Color.RED);
                            }
                        });
                    }
                }
            };
            new Thread(runnable).start();
        }
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
                for (Blog blog : result) {
                    blogNames.add(blog.getName());
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
            blockUIWhilePublish = ((CheckBox)findViewById(R.id.block_ui)).isChecked();

            List<?> urlsOrFiles = getImageUrls() != null ? getImageUrls() : getImageFiles();
            if (blockUIWhilePublish) {
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
            } else {
                if (publish) {
                    for (Object url : urlsOrFiles) {
                        PublishIntentService.startPublishIntent(getContext(),
                                url,
                                selectedBlogName,
                                getPostTitle(),
                                getPostTags());
                    }
                } else {
                    for (Object url : urlsOrFiles) {
                        PublishIntentService.startSaveAsDraftIntent(getContext(),
                                url,
                                selectedBlogName,
                                getPostTitle(),
                                getPostTags());
                    }
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
                newValues.put("tumblrName", appSupport.getSelectedBlogName());
                DBHelper.getInstance(getContext()).getPostTagDAO().update(newValues);
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
        TitleData titleData = TitleParser.instance(getContext()).parseTitle(postTitle.getText().toString());
        // only the edited title is updated, the sourceTitle remains unchanged
        setPostTitle(titleData.toHtml(), sourceTitle);
        // get only first tag
        List<String> firstTag = titleData.getTags().isEmpty() ? titleData.getTags() : titleData.getTags().subList(0, 1);
        setPostTags(firstTag);
    }

    public List<File> getImageFiles() {
        return imageFiles;
    }

    public void setImageFiles(List<File> imageFiles) {
        this.imageFiles = new ArrayList<File>(imageFiles);
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

    public boolean isBlockUIWhilePublish() {
        return blockUIWhilePublish;
    }

    public void setBlockUIWhilePublish(boolean blockUIWhilePublish) {
        this.blockUIWhilePublish = blockUIWhilePublish;
    }

    private void fillWithSourceTitle() {
        // treat the sourceTitle always as HTML
        this.postTitle.setText(Html.fromHtml(sourceTitle));
    }

}
