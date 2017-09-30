package com.ternaryop.photoshelf.fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ternaryop.photoshelf.Constants;
import com.ternaryop.photoshelf.ImageUrlRetriever;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.activity.ImageViewerActivity;
import com.ternaryop.photoshelf.adapter.ImagePickerAdapter;
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice;
import com.ternaryop.photoshelf.adapter.Selection;
import com.ternaryop.photoshelf.dialogs.TumblrPostDialog;
import com.ternaryop.photoshelf.extractor.ImageGallery;
import com.ternaryop.photoshelf.extractor.ImageInfo;
import com.ternaryop.photoshelf.parsers.AndroidTitleParserConfig;
import com.ternaryop.photoshelf.parsers.TitleData;
import com.ternaryop.photoshelf.parsers.TitleParser;
import com.ternaryop.photoshelf.view.AutofitGridLayoutManager;
import com.ternaryop.utils.DialogUtils;
import com.ternaryop.widget.ProgressHighlightViewLayout;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class ImagePickerFragment extends AbsPhotoShelfFragment implements OnPhotoBrowseClickMultiChoice, ActionMode.Callback {
    private RecyclerView gridView;
    private ProgressHighlightViewLayout progressHighlightViewLayout;

    private ImageUrlRetriever imageUrlRetriever;
    private ImagePickerAdapter imagePickerAdapter;
    private String detailsText;
    private String parsableTitle;
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
        imageUrlRetriever = new ImageUrlRetriever(getActivity(), (ProgressBar) rootView.findViewById(R.id.progressbar));

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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        compositeDisposable.clear();
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
            readImageGallery(m.group(1));
        } else {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.url_not_found)
                    .setMessage(getString(R.string.url_not_found_description, textWithUrl))
                    .show();
        }
    }

    private void readImageGallery(String url) {
        imageUrlRetriever.readImageGallery(url)
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        compositeDisposable.add(disposable);
                    }
                })
                .subscribe(new Consumer<ImageGallery>() {
                               @Override
                               public void accept(ImageGallery gallery) throws Exception {
                                   onGalleryRetrieved(gallery);
                               }
                           },
                new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        DialogUtils.showErrorDialog(getActivity(), throwable);
                    }
                });
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
                retrieveImages(false);
                mode.finish();
                return true;
            case R.id.create_from_file:
                retrieveImages(true);
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

    private void retrieveImages(boolean useFile) {
        imageUrlRetriever.retrieve(imagePickerAdapter.getSelectedItems(), useFile)
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        compositeDisposable.add(disposable);
                    }
                })
                .toList()
                .subscribe(new Consumer<List<Uri>>() {
                               @Override
                               public void accept(List<Uri> uris) throws Exception {
                                   onImagesRetrieved(uris);
                               }
                           },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                DialogUtils.showSimpleMessageDialog(getActivity(), R.string.url_not_found, throwable.getLocalizedMessage());
                            }
                        });
    }

    public void onImagesRetrieved(List<Uri> imageUriList) {
        try {
            TitleData titleData = TitleParser.instance(new AndroidTitleParserConfig(getActivity())).parseTitle(parsableTitle);
            Bundle args = new Bundle();

            args.putParcelableArrayList(TumblrPostDialog.ARG_IMAGE_URLS, new ArrayList<>(imageUriList));
            args.putString(TumblrPostDialog.ARG_HTML_TITLE, titleData.toHtml());
            args.putString(TumblrPostDialog.ARG_SOURCE_TITLE, parsableTitle);

            args.putStringArrayList(TumblrPostDialog.ARG_INITIAL_TAG_LIST, new ArrayList<>(titleData.getTags()));

            TumblrPostDialog.newInstance(args, null).show(getFragmentManager(), "dialog");
        } catch (Exception e) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.parsing_error)
                    .setMessage(e.getLocalizedMessage())
                    .show();
        }
    }

    public void onGalleryRetrieved(ImageGallery imageGallery) {
        progressHighlightViewLayout.stopProgress();
        detailsText = imageGallery.getTitle();
        parsableTitle = buildParsableTitle(imageGallery);
        showDetails(Snackbar.LENGTH_LONG);
        final List<ImageInfo> imageInfoList = imageGallery.getImageInfoList();
        getSupportActionBar().setSubtitle(getResources().getQuantityString(R.plurals.image_found, imageInfoList.size(), imageInfoList.size()));
        imagePickerAdapter.addAll(imageInfoList);
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
            imageUrlRetriever.retrieve(imageInfoList, false)
                    .doOnSubscribe(new Consumer<Disposable>() {
                        @Override
                        public void accept(Disposable disposable) throws Exception {
                            compositeDisposable.add(disposable);
                        }
                    })
                    .take(1)
                    .subscribe(new Consumer<Uri>() {
                                   @Override
                                   public void accept(Uri uri) throws Exception {
                                       // cache retrieved value
                                       final String url = uri.toString();
                                       imageInfo.setImageUrl(url);
                                       ImageViewerActivity.startImageViewer(getActivity(), url, null);
                                   }
                               },
                            new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    DialogUtils.showSimpleMessageDialog(getActivity(), R.string.url_not_found, throwable.getLocalizedMessage());
                                }
                            });
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
        updateSelection(position);
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

    /**
     * Return a string that can be used by the title parser
     * @param imageGallery the source
     * @return the title plus domain string
     */
    public String buildParsableTitle(final ImageGallery imageGallery) {
        return imageGallery.getTitle() + " ::::: " + imageGallery.getDomain();
    }

}
