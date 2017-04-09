package com.ternaryop.photoshelf.fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.ternaryop.photoshelf.Constants;
import com.ternaryop.photoshelf.ImageUrlRetriever;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.activity.ImageViewerActivity;
import com.ternaryop.photoshelf.adapter.ImagePickerAdapter;
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice;
import com.ternaryop.photoshelf.adapter.Selection;
import com.ternaryop.photoshelf.dialogs.TumblrPostDialog;
import com.ternaryop.photoshelf.extractor.ImageExtractorManager;
import com.ternaryop.photoshelf.extractor.ImageGallery;
import com.ternaryop.photoshelf.extractor.ImageInfo;
import com.ternaryop.photoshelf.parsers.AndroidTitleParserConfig;
import com.ternaryop.photoshelf.parsers.TitleData;
import com.ternaryop.photoshelf.parsers.TitleParser;
import com.ternaryop.photoshelf.view.AutofitGridLayoutManager;
import com.ternaryop.utils.AbsProgressIndicatorAsyncTask;
import com.ternaryop.utils.TaskWithUI;
import com.ternaryop.utils.URLUtils;
import com.ternaryop.utils.dialog.DialogUtils;
import com.ternaryop.widget.ProgressHighlightViewLayout;

public class ImagePickerFragment extends AbsPhotoShelfFragment implements ImageUrlRetriever.OnImagesRetrieved, OnPhotoBrowseClickMultiChoice, ActionMode.Callback {
    private RecyclerView gridView;
    private ProgressHighlightViewLayout progressHighlightViewLayout;

    private ImageUrlRetriever imageUrlRetriever;
    private ImagePickerAdapter imagePickerAdapter;
    private String detailsText;
    ActionMode actionMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_image_picker, container, false);
        getActivity().setTitle(R.string.image_picker_activity_title);

        progressHighlightViewLayout = (ProgressHighlightViewLayout) rootView.findViewById(android.R.id.empty);
        progressHighlightViewLayout.setAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_loop));

        imagePickerAdapter = new ImagePickerAdapter(getActivity());
        imagePickerAdapter.setOnPhotoBrowseClick(this);
        imagePickerAdapter.setEmptyView(progressHighlightViewLayout);
        imageUrlRetriever = new ImageUrlRetriever(getActivity(), this);

        RecyclerView.LayoutManager layout = new AutofitGridLayoutManager(getActivity(), (int) getResources().getDimension(R.dimen.image_picker_grid_width));
        gridView = (RecyclerView) rootView.findViewById(R.id.gridview);
        gridView.setAdapter(imagePickerAdapter);
        gridView.setHasFixedSize(true);
        gridView.setLayoutManager(layout);

        setHasOptionsMenu(true);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        openUrl(getTextWithUrl());
    }

    private String getTextWithUrl() {
        // Search on fragment arguments
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(Constants.EXTRA_URL)) {
            return arguments.getString(Constants.EXTRA_URL);
        }

        // Search on activity intent
        // Get intent, action and MIME type
        Intent intent = getActivity().getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Uri uri = intent.getData();

        String textWithUrl = null;

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                textWithUrl = intent.getStringExtra(Intent.EXTRA_TEXT);
            }
        } else if (Intent.ACTION_VIEW.equals(action) && uri != null) {
            textWithUrl = uri.toString();
        }
        return textWithUrl;
    }

    private void openUrl(String textWithUrl) {
        if (textWithUrl == null) {
            return;
        }
        progressHighlightViewLayout.startProgress();
        final String message = getResources().getQuantityString(R.plurals.download_url_with_count, 1, 0);
        getCurrentTextView().setText(message);
        final Matcher m = Pattern.compile("(http:.*)").matcher(textWithUrl);

        if (m.find()) {
            String url = m.group(1);
            // resolveShortenURL can't be called on main thread so we
            // resolve into a separated thread
            new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    return URLUtils.resolveShortenURL(params[0]);
                }

                @Override
                protected void onPostExecute(String url) {
                    task = (TaskWithUI) new ImageUrlExtractor(getActivity(), message, getCurrentTextView()).execute(url);
                }
            }.execute(url);
        } else {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.url_not_found)
                    .setMessage(getString(R.string.url_not_found_description, textWithUrl))
                    .show();
        }
    }

    public TextView getCurrentTextView() {
        return (TextView) progressHighlightViewLayout.getCurrentView();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle(getString(R.string.select_images));
        mode.setSubtitle(getResources().getQuantityString(
                R.plurals.selected_items_total,
                1,
                1,
                imagePickerAdapter.getItemCount()));
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.image_picker_context, menu);
        imagePickerAdapter.setShowButtons(true);
        imagePickerAdapter.notifyDataSetChanged();
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.showDialog:
                imageUrlRetriever.retrieve(imagePickerAdapter.getSelectedItems());
                mode.finish();
                return true;
            case R.id.create_from_file:
                imageUrlRetriever.retrieve(imagePickerAdapter.getSelectedItems(), true);
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        this.actionMode = null;
        imagePickerAdapter.setShowButtons(false);
        imagePickerAdapter.getSelection().clear();
    }

    @Override
    public void onImagesRetrieved(ImageUrlRetriever imageUrlRetriever) {
        TitleData titleData = TitleParser.instance(new AndroidTitleParserConfig(getActivity())).parseTitle(imageUrlRetriever.getTitle());
        Bundle args = new Bundle();

        args.putParcelableArrayList(TumblrPostDialog.ARG_IMAGE_URLS, imageUrlRetriever.getImageCollector().getImageUrls());
        args.putBoolean(TumblrPostDialog.ARG_BLOCK_UI_WHILE_PUBLISH, false);
        args.putString(TumblrPostDialog.ARG_HTML_TITLE, titleData.toHtml());
        args.putString(TumblrPostDialog.ARG_SOURCE_TITLE, imageUrlRetriever.getTitle());

        args.putStringArrayList(TumblrPostDialog.ARG_INITIAL_TAG_LIST, new ArrayList<>(titleData.getTags()));

        TumblrPostDialog.newInstance(args, null).show(getFragmentManager(), "dialog");
    }

    /**
     * Find all images present into the passed HTML document
     *
     * @author dave
     */
    class ImageUrlExtractor extends AbsProgressIndicatorAsyncTask<String, String, ImageGallery> {
        Exception error;

        public ImageUrlExtractor(Context context, String message, TextView textView) {
            super(context, message, textView);
        }

        @Override
        protected void onProgressUpdate(String... message) {
            // if the first element is null do not increment the progress and get the message from index 1
            if (message[0] == null) {
                getCurrentTextView().setText(message[1]);
            } else {
                progressHighlightViewLayout.incrementProgress();
                getCurrentTextView().setText(message[0]);
            }
        }

        @Override
        protected ImageGallery doInBackground(String... urls) {
            try {
                String galleryUrl = urls[0];

                return new ImageExtractorManager(getString(R.string.PHOTOSHELF_EXTRACTOR_ACCESS_TOKEN)).getGallery(galleryUrl);
            } catch (Exception e) {
                error = e;
            }
            return null;
        }

        public String findTitle(ImageGallery gallery) {
            return gallery.getTitle() + " ::::: " + gallery.getDomain();
        }

        @Override
        protected void onPostExecute(ImageGallery gallery) {
            super.onPostExecute(null);
            if (error == null) {
                progressHighlightViewLayout.stopProgress();
                imageUrlRetriever.setTitle(findTitle(gallery));
                detailsText = gallery.getTitle();
                showDetails(Snackbar.LENGTH_LONG);
                getSupportActionBar().setSubtitle(getResources().getQuantityString(R.plurals.image_found, gallery.getImageInfoList().size(), gallery.getImageInfoList().size()));
                imagePickerAdapter.addAll(gallery.getImageInfoList());
            } else {
                DialogUtils.showErrorDialog(getActivity(), error);
            }
        }
    }

    @Override
    public void onTagClick(int position) {
    }

    @Override
    public void onThumbnailImageClick(int position) {
        final ImageInfo imageInfo = imagePickerAdapter.getItem(position);
        if (imageInfo.getImageUrl() == null) {
            List<ImageInfo> imageInfoList = new ArrayList<>();
            imageInfoList.add(imageInfo);
            new ImageUrlRetriever(getActivity(), new ImageUrlRetriever.OnImagesRetrieved() {
                @Override
                public void onImagesRetrieved(ImageUrlRetriever imageUrlRetriever) {
                    // cache retrieved value
                    final String url = imageUrlRetriever.getImageCollector().getImageUrls().get(0).toString();
                    imageInfo.setImageUrl(url);
                    ImageViewerActivity.startImageViewer(getActivity(), url, null);
                }
            }).retrieve(imageInfoList);
        } else {
            ImageViewerActivity.startImageViewer(getActivity(), imageInfo.getImageUrl(), null);
        }
    }

    @Override
    public void onOverflowClick(View view, int position) {
    }

    @Override
    public void onItemClick(int position) {
        if (actionMode == null) {
            onThumbnailImageClick(position);
        } else {
            updateSelection(position);
        }
    }

    @Override
    public void onItemLongClick(int position) {
        if (actionMode == null) {
            actionMode = getActivity().startActionMode(this);
        }
        imagePickerAdapter.getSelection().toggle(position);
    }

    private void updateSelection(int position) {
        Selection selection = imagePickerAdapter.getSelection();
        selection.toggle(position);
        if (selection.getItemCount() == 0) {
            actionMode.finish();
        } else {
            int selectionCount = selection.getItemCount();
            actionMode.setSubtitle(getResources().getQuantityString(
                    R.plurals.selected_items_total,
                    selectionCount,
                    selectionCount,
                    imagePickerAdapter.getItemCount()));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.image_picker, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_image_viewer_details:
                showDetails(Snackbar.LENGTH_INDEFINITE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showDetails(int duration) {
        Snackbar snackbar = Snackbar.make(gridView, detailsText, duration);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.image_picker_detail_text_bg));
        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(getActivity(), R.color.image_picker_detail_text_text));
        textView.setMaxLines(3);
        snackbar.show();
    }
}
