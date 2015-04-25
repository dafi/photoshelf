package com.ternaryop.photoshelf.fragment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import com.ternaryop.photoshelf.Constants;
import com.ternaryop.photoshelf.ImageDOMSelectorFinder;
import com.ternaryop.photoshelf.ImageInfo;
import com.ternaryop.photoshelf.ImageUrlRetriever;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.activity.ImageViewerActivity;
import com.ternaryop.photoshelf.adapter.ImagePickerAdapter;
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClick;
import com.ternaryop.photoshelf.dialogs.TumblrPostDialog;
import com.ternaryop.photoshelf.parsers.AndroidTitleParserConfig;
import com.ternaryop.photoshelf.parsers.TitleData;
import com.ternaryop.photoshelf.parsers.TitleParser;
import com.ternaryop.utils.AbsProgressIndicatorAsyncTask;
import com.ternaryop.utils.TaskWithUI;
import com.ternaryop.utils.URLUtils;
import com.ternaryop.utils.dialog.DialogUtils;
import com.ternaryop.widget.ProgressHighlightViewLayout;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ImagePickerFragment extends AbsPhotoShelfFragment implements GridView.MultiChoiceModeListener, AdapterView.OnItemClickListener, ImageUrlRetriever.OnImagesRetrieved, OnPhotoBrowseClick {
    private GridView gridView;
    private ProgressHighlightViewLayout progressHighlightViewLayout;

    private ImageUrlRetriever imageUrlRetriever;
    private ImagePickerAdapter imagePickerAdapter;
    private ImageDOMSelectorFinder domSelectorFinder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_image_picker, container, false);
        getActivity().setTitle(R.string.image_picker_activity_title);

        imagePickerAdapter = new ImagePickerAdapter(getActivity());
        imagePickerAdapter.setOnPhotoBrowseClick(this);
        domSelectorFinder = new ImageDOMSelectorFinder(getActivity());
        imageUrlRetriever = new ImageUrlRetriever(getActivity(), this);

        progressHighlightViewLayout = (ProgressHighlightViewLayout) rootView.findViewById(android.R.id.empty);
        progressHighlightViewLayout.setAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_loop));

        gridView = (GridView) rootView.findViewById(R.id.gridview);
        gridView.setAdapter(imagePickerAdapter);
        gridView.setOnItemClickListener(this);
        gridView.setMultiChoiceModeListener(this);
        gridView.setEmptyView(progressHighlightViewLayout);

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

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.image_picker_context, menu);
        imagePickerAdapter.setShowButtons(true);
        imagePickerAdapter.notifyDataSetChanged();
        return true;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        mode.setTitle(getActivity().getString(R.string.select_images));
        return true;
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.showDialog:
                imageUrlRetriever.retrieve(getCheckedImageInfoList());
                mode.finish();
                return true;
            case R.id.create_from_file:
                imageUrlRetriever.retrieve(getCheckedImageInfoList(), true);
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    private List<ImageInfo> getCheckedImageInfoList() {
        SparseBooleanArray checkedItemPositions = gridView.getCheckedItemPositions();
        ArrayList<ImageInfo> list = new ArrayList<ImageInfo>();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            int key = checkedItemPositions.keyAt(i);
            if (checkedItemPositions.get(key)) {
                list.add(imagePickerAdapter.getItem(key));
            }
        }
        return list;
    }

    public void onDestroyActionMode(ActionMode mode) {
        imagePickerAdapter.setShowButtons(false);
        imagePickerAdapter.notifyDataSetChanged();
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position,
                                          long id, boolean checked) {
        int selectCount = gridView.getCheckedItemCount();
        mode.setSubtitle(getActivity().getResources().getQuantityString(
                R.plurals.selected_items,
                selectCount,
                selectCount));
    }

    @Override
    public void onImagesRetrieved(ImageUrlRetriever imageUrlRetriever) {
        TitleData titleData = TitleParser.instance(new AndroidTitleParserConfig(getActivity())).parseTitle(imageUrlRetriever.getTitle());
        Bundle args = new Bundle();

        if (imageUrlRetriever.getImageUrls() != null) {
            args.putStringArrayList(TumblrPostDialog.ARG_IMAGE_URLS, new ArrayList<String>(imageUrlRetriever.getImageUrls()));
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
        args.putStringArrayList(TumblrPostDialog.ARG_INITIAL_TAG_LIST, new ArrayList<String>(new ArrayList<String>(firstTag)));

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
            InputStream input;
            HttpURLConnection connection = null;
            List<ImageInfo> imageInfoList = new ArrayList<ImageInfo>();

            try {
                URL url = new URL(urls[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

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
                    Document htmlDocument = Jsoup.parse(baos.toString());
                    imageUrlRetriever.setTitle(htmlDocument.title());
                    Elements thumbnailImages = htmlDocument.select("a img[src*=jpg]");
                    publishProgress(getResources().getQuantityString(R.plurals.image_found, thumbnailImages.size(), thumbnailImages.size()));
                    for (int i = 0; i < thumbnailImages.size(); i++) {
                        Element thumbnailImage = thumbnailImages.get(i);
                        String thumbnailURL = thumbnailImage.attr("src");
                        String destinationDocumentURL = thumbnailImage.parent().attr("href");
                        String selector = domSelectorFinder.getSelectorFromUrl(destinationDocumentURL);
                        if (selector != null) {
                            imageInfoList.add(new ImageInfo(thumbnailURL, destinationDocumentURL, selector));
                        }
                    }
                }
            } catch (Exception e) {
                error = e;
            } finally {
                if (connection != null) try {
                    connection.disconnect();
                } catch (Exception ignored) {
                }
            }
            return imageInfoList;
        }

        @Override
        protected void onPostExecute(List<ImageInfo> result) {
            super.onPostExecute(null);
            if (error == null) {
                progressHighlightViewLayout.stopProgress();
                getSupportActionBar().setSubtitle(imageUrlRetriever.getTitle());
                imagePickerAdapter.addAll(result);
                imagePickerAdapter.notifyDataSetChanged();
                gridView.invalidateViews();
            } else {
                DialogUtils.showErrorDialog(getActivity(), error);
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        onThumbnailImageClick(position);
    }

    @Override
    public void onPhotoBrowseClick(int position) {
    }

    @Override
    public void onThumbnailImageClick(int position) {
        final ImageInfo imageInfo = imagePickerAdapter.getItem(position);
        if (imageInfo.getImageURL() == null) {
            List<ImageInfo> imageInfoList = new ArrayList<ImageInfo>();
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
}
