package com.ternaryop.photoshelf.dialogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;

import com.ternaryop.photoshelf.AppSupport;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.db.DBHelper;
import com.ternaryop.photoshelf.db.TagCursorAdapter;
import com.ternaryop.photoshelf.parsers.AndroidTitleParserConfig;
import com.ternaryop.photoshelf.parsers.TitleData;
import com.ternaryop.photoshelf.parsers.TitleParser;
import com.ternaryop.photoshelf.service.PublishIntentService;
import com.ternaryop.tumblr.AbsCallback;
import com.ternaryop.tumblr.Blog;
import com.ternaryop.tumblr.Callback;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.utils.DialogUtils;
import org.json.JSONObject;

public class TumblrPostDialog extends DialogFragment implements Toolbar.OnMenuItemClickListener {

    public static final String ARG_PHOTO_POST = "photoPost";
    public static final String ARG_IMAGE_URLS = "imageUrls";
    public static final String ARG_HTML_TITLE = "htmlTitle";
    public static final String ARG_SOURCE_TITLE = "sourceTitle";
    public static final String ARG_INITIAL_TAG_LIST = "initialTagList";
    public static final String ARG_BLOCK_UI_WHILE_PUBLISH = "blockUIWhilePublish";

    private EditText postTitle;
    private MultiAutoCompleteTextView postTags;
    private Spinner blogList;
    private AppSupport appSupport;
    private TumblrPhotoPost photoPost;
    private TagCursorAdapter tagAdapter;
    private List<Uri> imageUrls;
    private boolean blockUIWhilePublish;
    private String htmlTitle;
    private String sourceTitle;
    private List<String> initialTagList;
    private ColorStateList defaultPostTagsColor;
    private Drawable defaultPostTagsBackground;

    public static TumblrPostDialog newInstance(Bundle args, Fragment target) {
        TumblrPostDialog fragment = new TumblrPostDialog();

        Set<String> keys = args.keySet();
        if ((keys.contains(ARG_PHOTO_POST) && keys.contains(ARG_IMAGE_URLS))) {
            throw new IllegalArgumentException("Only one type must be specified between " + ARG_PHOTO_POST + ", " + ARG_IMAGE_URLS);
        }
        if (!keys.contains(ARG_PHOTO_POST) && !keys.contains(ARG_IMAGE_URLS)) {
            throw new IllegalArgumentException("One type must be specified, allowed values are " + ARG_PHOTO_POST + ", " + ARG_IMAGE_URLS);
        }
        fragment.setArguments(args);
        fragment.setTargetFragment(target, 0);

        return fragment;
    }


    public static TumblrPostDialog newInstance(TumblrPhotoPost photoPost, boolean blockUIWhilePublish, Fragment target) {
        if (photoPost == null) {
            throw new IllegalArgumentException("photoPost is mandatory");
        }
        TumblrPostDialog fragment = new TumblrPostDialog();

        Bundle args = new Bundle();
        args.putSerializable(ARG_PHOTO_POST, photoPost);
        args.putBoolean(ARG_BLOCK_UI_WHILE_PUBLISH, blockUIWhilePublish);
        fragment.setArguments(args);

        fragment.setTargetFragment(target, 0);
        return fragment;
    }

    public TumblrPostDialog() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appSupport = new AppSupport(getActivity());
        decodeArguments();
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_PhotoShelf_Dialog);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_publish_post, null);
        setupUI(view);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setNegativeButton(R.string.cancel_title, null);
        if (photoPost == null) {
            OnClickPublishListener onClickPublishListener = new OnClickPublishListener();
            builder.setNeutralButton(R.string.publish_post, onClickPublishListener);
            builder.setPositiveButton(R.string.draft_title, onClickPublishListener);
            view.findViewById(R.id.refreshBlogList).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fetchBlogNames();
                }
            });
        } else {
            view.findViewById(R.id.blog_list).setVisibility(View.GONE);
            builder.setPositiveButton(R.string.edit_post_title, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    editPost();
                }
            });
        }

        return builder.create();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Dimensions defined on xml layout are not used so we set them here (it works only if called inside onResume)
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void setupUI(View view) {
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.publish_post_overflow);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.getMenu().findItem(R.id.block_ui).setChecked(blockUIWhilePublish);

        postTitle = (EditText)view.findViewById(R.id.post_title);
        postTags = (MultiAutoCompleteTextView)view.findViewById(R.id.post_tags);
        blogList = (Spinner) view.findViewById(R.id.blog);

        // the ContextThemeWrapper is necessary otherwise the autocomplete drop down items and the toolbar overflow menu items are styled incorrectly
        // since the switch to the AlertDialog the toolbar isn't styled from code so to fix it the theme is declared directly into xml
        tagAdapter = new TagCursorAdapter(
                new ContextThemeWrapper(getActivity(), R.style.Theme_PhotoShelf_Dialog),
                android.R.layout.simple_dropdown_item_1line,
                "");
        tagAdapter.setBlogName(appSupport.getSelectedBlogName());
        postTags.setAdapter(tagAdapter);
        postTags.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        blogList.setOnItemSelectedListener(new BlogItemSelectedListener());

        fillTags(initialTagList);
        postTitle.setText(Html.fromHtml(htmlTitle));
        // move caret to end
        postTitle.setSelection(postTitle.length());

        if (photoPost != null) {
            toolbar.setTitle(R.string.edit_post_title);
        } else {
            int size = imageUrls.size();
            toolbar.setTitle(getActivity().getResources().getQuantityString(
                    R.plurals.post_image,
                    size,
                    size));
        }
    }

    public List<Uri> getImageUrls() {
        return imageUrls;
    }

    public String getPostTitle() {
        postTitle.clearComposingText();
        return Html.toHtml(postTitle.getText());
    }

    private void setInitialTagList(List<String> initialTagList) {
        this.initialTagList = initialTagList;
    }

    public String getPostTags() {
        return postTags.getText().toString();
    }

    private void fillTags(List<String> tags) {
        final String firstTag = tags.isEmpty() ? "" : tags.get(0);
        this.postTags.setText(TextUtils.join(", ", tags));

        if (defaultPostTagsColor == null) {
            defaultPostTagsColor = postTags.getTextColors();
            defaultPostTagsBackground = postTags.getBackground();
        }
        if (!firstTag.isEmpty()) {
            // if tag doesn't exist then show the textfield in red
            final Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    final long count = DBHelper.getInstance(getActivity()).getPostTagDAO()
                            .getPostCountByTag(firstTag, appSupport.getSelectedBlogName());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (count == 0) {
                                    postTags.setTextColor(Color.WHITE);
                                    postTags.setBackgroundColor(Color.RED);
                                } else {
                                    postTags.setTextColor(defaultPostTagsColor);
                                    postTags.setBackground(defaultPostTagsBackground);
                                }
                            }
                        });
                }
            };
            new Thread(runnable).start();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (photoPost == null) {
            AlertDialog dialog = (AlertDialog) getDialog();
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

            List<String> blogSetNames = appSupport.getBlogList();
            if (blogSetNames == null) {
                fetchBlogNames();
            } else {
                fillBlogList(blogSetNames);
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(true);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            }
        }
    }

    private void fillBlogList(List<String> blogNames) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, blogNames);
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
        final AlertDialog dialog = (AlertDialog) getDialog();
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        Tumblr.getSharedTumblr(getActivity()).getBlogList(new Callback<Blog[]>() {

            @Override
            public void complete(Blog[] result) {
                List<String> blogNames = new ArrayList<>(result.length);
                for (Blog blog : result) {
                    blogNames.add(blog.getName());
                }
                appSupport.setBlogList(blogNames);
                fillBlogList(blogNames);
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(true);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            }

            @Override
            public void failure(Exception e) {
                dismiss();
                DialogUtils.showErrorDialog(getActivity(), e);
            }
        });
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.parse_title:
                parseTitle(false);
                return true;
            case R.id.parse_title_swap:
                parseTitle(true);
                return true;
            case R.id.source_title:
                fillWithSourceTitle();
                return true;
            case R.id.block_ui:
                menuItem.setChecked(!menuItem.isChecked());
                this.blockUIWhilePublish = menuItem.isChecked();
                return true;
        }
        return false;
    }

    private final class OnClickPublishListener implements DialogInterface.OnClickListener {
        private ProgressDialog progressDialog;

        private final class PostCallback implements Callback<Long> {
            public PostCallback (int max, boolean publish) {
                progressDialog = new ProgressDialog(getActivity());
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                if (publish) {
                    progressDialog.setMessage(getActivity().getString(R.string.publishing_post));
                } else {
                    progressDialog.setMessage(getActivity().getResources().getQuantityString(R.plurals.saving_post_in_draft, max, max));
                }
                progressDialog.setMax(max);
                progressDialog.show();
            }

            @Override
            public void failure(Exception ex) {
                progressDialog.dismiss();
                DialogUtils.showErrorDialog(getActivity(), ex);
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
        public void onClick(DialogInterface dialog, int which) {
            boolean publish = which == DialogInterface.BUTTON_NEUTRAL;
            String selectedBlogName = (String) blogList.getSelectedItem();
            appSupport.setSelectedBlogName(selectedBlogName);

            if (blockUIWhilePublish) {
                createPostsBlockedUI(publish, selectedBlogName, getImageUrls(), getPostTitle(), getPostTags());
            } else {
                createPostsWithService(publish, selectedBlogName, getImageUrls(), getPostTitle(), getPostTags());
            }
        }

        private void createPostsWithService(boolean publish, String selectedBlogName, List<Uri> urls, String postTitle, String postTags) {
            if (publish) {
                for (Uri url : urls) {
                    PublishIntentService.startPublishIntent(getActivity(),
                            url,
                            selectedBlogName,
                            postTitle,
                            postTags);
                }
            } else {
                for (Uri url : urls) {
                    PublishIntentService.startSaveAsDraftIntent(getActivity(),
                            url,
                            selectedBlogName,
                            postTitle,
                            postTags);
                }
            }
        }

        private void createPostsBlockedUI(boolean publish, String selectedBlogName, List<Uri> urls, String postTitle, String postTags) {
            final PostCallback callback = new PostCallback(urls.size(), publish);
            if (publish) {
                for (Uri url : urls) {
                    Tumblr.getSharedTumblr(getActivity()).publishPhotoPost(selectedBlogName,
                            url, postTitle, postTags,
                            callback);
                }
            } else {
                for (Uri url : urls) {
                    Tumblr.getSharedTumblr(getActivity()).draftPhotoPost(selectedBlogName,
                            url, postTitle, postTags,
                            callback);
                }
            }
        }
    }

    private void editPost() {
        final HashMap<String, String> newValues = new HashMap<>();
        newValues.put("id", String.valueOf(photoPost.getPostId()));
        newValues.put("caption", getPostTitle());
        newValues.put("tags", getPostTags());

        Tumblr.getSharedTumblr(getActivity()).editPost(appSupport.getSelectedBlogName(), newValues, new AbsCallback(this, R.string.generic_error) {

            @Override
            public void complete(JSONObject result) {
                newValues.put("tumblrName", appSupport.getSelectedBlogName());
                DBHelper.getInstance(getActivity()).getPostDAO().update(newValues, getActivity());
                if (getTargetFragment() instanceof PostListener) {
                    ((PostListener) getTargetFragment()).onEditDone(TumblrPostDialog.this, photoPost);
                }
            }
        });
    }

    private void parseTitle(boolean swapDayMonth) {
        TitleData titleData = TitleParser.instance(new AndroidTitleParserConfig(getActivity())).parseTitle(postTitle.getText().toString(), swapDayMonth);
        // only the edited title is updated, the sourceTitle remains unchanged
        htmlTitle = titleData.toHtml();
        this.postTitle.setText(Html.fromHtml(htmlTitle));

        fillTags(titleData.getTags());
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

    private void fillWithSourceTitle() {
        // treat the sourceTitle always as HTML
        this.postTitle.setText(Html.fromHtml(sourceTitle));
    }

    private void decodeArguments() {
        Bundle args = getArguments();
        TumblrPhotoPost photoPost = (TumblrPhotoPost) args.getSerializable(ARG_PHOTO_POST);
        if (photoPost != null) {
            this.photoPost = photoPost;
            // pass the same HTML text for source title
            this.htmlTitle = photoPost.getCaption();
            this.sourceTitle = photoPost.getCaption();
            setInitialTagList(photoPost.getTags());
            blockUIWhilePublish = args.getBoolean(ARG_BLOCK_UI_WHILE_PUBLISH);
        } else {
            this.imageUrls = args.getParcelableArrayList(ARG_IMAGE_URLS);
            this.htmlTitle = args.getString(ARG_HTML_TITLE);
            this.sourceTitle = args.getString(ARG_SOURCE_TITLE);
            setInitialTagList(args.getStringArrayList(ARG_INITIAL_TAG_LIST));
            blockUIWhilePublish = args.getBoolean(ARG_BLOCK_UI_WHILE_PUBLISH);
        }
    }

    public interface PostListener {
        void onEditDone(TumblrPostDialog dialog, TumblrPhotoPost post);
    }
}
