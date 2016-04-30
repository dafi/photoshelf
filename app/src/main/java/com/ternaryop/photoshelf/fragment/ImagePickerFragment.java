package com.ternaryop.photoshelf.fragment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
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
import com.ternaryop.photoshelf.HtmlDocumentSupport;
import com.ternaryop.photoshelf.ImageInfo;
import com.ternaryop.photoshelf.ImageUrlRetriever;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.activity.ImageViewerActivity;
import com.ternaryop.photoshelf.adapter.ImagePickerAdapter;
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice;
import com.ternaryop.photoshelf.adapter.Selection;
import com.ternaryop.photoshelf.dialogs.TumblrPostDialog;
import com.ternaryop.photoshelf.parsers.AndroidTitleParserConfig;
import com.ternaryop.photoshelf.parsers.TitleData;
import com.ternaryop.photoshelf.parsers.TitleParser;
import com.ternaryop.photoshelf.selector.DOMSelector;
import com.ternaryop.photoshelf.selector.ImageDOMSelectorFinder;
import com.ternaryop.photoshelf.view.AutofitGridLayoutManager;
import com.ternaryop.utils.AbsProgressIndicatorAsyncTask;
import com.ternaryop.utils.TaskWithUI;
import com.ternaryop.utils.URLUtils;
import com.ternaryop.utils.dialog.DialogUtils;
import com.ternaryop.widget.ProgressHighlightViewLayout;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ImagePickerFragment extends AbsPhotoShelfFragment implements ImageUrlRetriever.OnImagesRetrieved, OnPhotoBrowseClickMultiChoice, ActionMode.Callback {
    private RecyclerView gridView;
    private ProgressHighlightViewLayout progressHighlightViewLayout;

    private ImageUrlRetriever imageUrlRetriever;
    private ImagePickerAdapter imagePickerAdapter;
    private ImageDOMSelectorFinder domSelectorFinder;
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
        domSelectorFinder = new ImageDOMSelectorFinder(getActivity());
        imageUrlRetriever = new ImageUrlRetriever(getActivity(), this);

        RecyclerView.LayoutManager layout = new AutofitGridLayoutManager(getActivity(), (int) getActivity().getResources().getDimension(R.dimen.image_picker_grid_width));
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
        mode.setTitle(getActivity().getString(R.string.select_images));
        mode.setSubtitle(getResources().getQuantityString(R.plurals.selected_items, 1, 1));
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

        if (imageUrlRetriever.getImageUrls() != null) {
            args.putStringArrayList(TumblrPostDialog.ARG_IMAGE_URLS, new ArrayList<>(imageUrlRetriever.getImageUrls()));
        } else {
            ArrayList<String> paths = new ArrayList<>(imageUrlRetriever.getImageFiles().size());
            for (File f : imageUrlRetriever.getImageFiles()) {
                paths.add(f.getAbsolutePath());
            }
            args.putStringArrayList(TumblrPostDialog.ARG_IMAGE_PATHS, paths);
        }
        args.putBoolean(TumblrPostDialog.ARG_BLOCK_UI_WHILE_PUBLISH, false);
        args.putString(TumblrPostDialog.ARG_HTML_TITLE, titleData.toHtml());
        args.putString(TumblrPostDialog.ARG_SOURCE_TITLE, imageUrlRetriever.getTitle());

        // use only first tag, generally other tags are poorly determinated
        List<String> firstTag = titleData.getTags().isEmpty() ? titleData.getTags() : titleData.getTags().subList(0, 1);
        args.putStringArrayList(TumblrPostDialog.ARG_INITIAL_TAG_LIST, new ArrayList<>(new ArrayList<>(firstTag)));

        TumblrPostDialog.newInstance(args, null).show(getFragmentManager(), "dialog");
    }

    /**
     * Find all images present into the passed HTML document
     *
     * @author dave
     */
    class ImageUrlExtractor extends AbsProgressIndicatorAsyncTask<String, String, List<ImageInfo>> {
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
        protected List<ImageInfo> doInBackground(String... urls) {
            List<ImageInfo> imageInfoList = new ArrayList<>();

            try {
                String galleryUrl = urls[0];
                String content = readURLContent(galleryUrl);
                DOMSelector selector = domSelectorFinder.getSelectorFromUrl(galleryUrl);

                Document htmlDocument = Jsoup.parse(content);
                htmlDocument.setBaseUri(galleryUrl);
                imageUrlRetriever.setTitle(findTitle(selector, htmlDocument));
                extractImages(imageInfoList, selector, htmlDocument);

                extractImageFromMultiPage(imageInfoList, selector, htmlDocument);

            } catch (Exception e) {
                error = e;
            }
            return imageInfoList;
        }

        public String findTitle(DOMSelector selector, Document htmlDocument) {
            String title = "";
            if (selector.getTitle() != null) {
                title = htmlDocument.select(selector.getTitle()).text();
            }
            if (title.isEmpty()) {
                return htmlDocument.title();
            }
            return title;
        }

        private void extractImageFromMultiPage(List<ImageInfo> imageInfoList, DOMSelector selector, Document startPageDocument) throws IOException {
            if (selector.getMultiPage() == null) {
                return;
            }
            Element element = startPageDocument.select(selector.getMultiPage()).first();
            while (element != null) {
                String pageUrl = element.absUrl("href");
                String pageContent = readURLContent(pageUrl);
                Document pageDocument = Jsoup.parse(pageContent);
                pageDocument.setBaseUri(pageUrl);
                extractImages(imageInfoList, domSelectorFinder.getSelectorFromUrl(pageUrl), pageDocument);
                element = pageDocument.select(selector.getMultiPage()).first();
            }
        }

        private void extractImages(List<ImageInfo> imageInfoList, DOMSelector selector, Document htmlDocument) {
            Elements thumbnailImages = htmlDocument.select(selector.getContainer());
            int totalSize = imageInfoList.size() + thumbnailImages.size();
            publishProgress(getResources().getQuantityString(R.plurals.image_found, totalSize, totalSize));
            for (Element thumbnailImage : thumbnailImages) {
                String destinationDocumentURL = thumbnailImage.parent().absUrl("href");
                DOMSelector destinationSelector = domSelectorFinder.getSelectorFromUrl(destinationDocumentURL);
                if (destinationSelector.getImage() != null) {
                    String thumbnailURL = thumbnailImage.absUrl("src");
                    imageInfoList.add(new ImageInfo(thumbnailURL, destinationDocumentURL, destinationSelector.getImage()));
                }
            }
        }

        private String readURLContent(String url) throws IOException {
            InputStream input;
            HttpURLConnection connection = null;

            try {
                connection = HtmlDocumentSupport.openConnection(url);

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    // this will be useful to display download percentage
                    // might be -1: server did not report the length

                    int fileLength = connection.getContentLength();
                    input = connection.getInputStream();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    byte data[] = new byte[32768];
                    long total = 0;
                    int count;
                    while ((count = input.read(data)) != -1) {
                        if (isCancelled()) {
                            return null;
                        }
                        total += count;
                        if (fileLength > 0) {
                            publishProgress(null, "" + (total * 100 / fileLength));
                        } else {
                            // quantity must be int so instead of casting total simply pass 2
                            String message = getResources().getQuantityString(R.plurals.download_url_with_count, 2, total);
                            publishProgress(null, message);
                        }
                        baos.write(data, 0, count);
                    }
                    return baos.toString();
                } else {
                    throw new RuntimeException("Unable to read page, HTTP error " + connection.getResponseCode());
                }
            } finally {
                if (connection != null) try {
                    connection.disconnect();
                } catch (Exception ignored) {
                }
            }
        }

        @Override
        protected void onPostExecute(List<ImageInfo> result) {
            super.onPostExecute(null);
            if (error == null) {
                progressHighlightViewLayout.stopProgress();
                detailsText = imageUrlRetriever.getTitle();
                showDetails(Snackbar.LENGTH_LONG);
                getSupportActionBar().setSubtitle(getResources().getQuantityString(R.plurals.image_found, result.size(), result.size()));
                imagePickerAdapter.addAll(result);
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
        if (imageInfo.getImageURL() == null) {
            List<ImageInfo> imageInfoList = new ArrayList<>();
            imageInfoList.add(imageInfo);
            new ImageUrlRetriever(getActivity(), new ImageUrlRetriever.OnImagesRetrieved() {
                @Override
                public void onImagesRetrieved(ImageUrlRetriever imageUrlRetriever) {
                    // cache retrieved value
                    imageInfo.setImageURL(imageUrlRetriever.getImageUrls().get(0));
                    ImageViewerActivity.startImageViewer(getActivity(), imageUrlRetriever.getImageUrls().get(0), null);
                }
            }).retrieve(imageInfoList);
        } else {
            ImageViewerActivity.startImageViewer(getActivity(), imageInfo.getImageURL(), null);
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
            actionMode.setSubtitle(getActivity().getResources().getQuantityString(
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
