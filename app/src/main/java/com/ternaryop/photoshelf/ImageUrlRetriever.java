package com.ternaryop.photoshelf;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.ternaryop.photoshelf.selector.DOMSelector;
import com.ternaryop.photoshelf.selector.ImageDOMSelectorFinder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ImageUrlRetriever {
    private final Context context;
    private final ImageDOMSelectorFinder domSelectorFinder;
    private String title;
    private Exception error = null;
    private final OnImagesRetrieved callback;
    private final ImageCollector imageCollector;

    public ImageUrlRetriever(Context context, OnImagesRetrieved callback) {
        this.context = context;
        this.callback = callback;
        domSelectorFinder = new ImageDOMSelectorFinder(context);
        imageCollector = new ImageCollector(context);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void retrieve(List<ImageInfo> list, boolean useFile) {
        imageCollector.setCollectFiles(useFile);
        new UrlRetrieverAsyncTask(list).execute();
    }

    public void retrieve(List<ImageInfo> list) {
        retrieve(list, false);
    }

    public interface OnImagesRetrieved {
        public void onImagesRetrieved(ImageUrlRetriever imageUrlRetriever);
    }

    class UrlRetrieverAsyncTask extends AsyncTask<Object, Integer, Void> {
        private ProgressDialog progressDialog;
        private final List<ImageInfo> list;

        public UrlRetrieverAsyncTask(List<ImageInfo> list) {
            this.list = list;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(context.getString(R.string.image_retriever_title));
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMax(list.size());
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Object... params) {
            try {
                int i = 1;
                for (ImageInfo imageInfo : list) {
                    retrieveImageUrl(imageInfo);
                    publishProgress(i++);
                }
            } catch (Exception e) {
                error = e;
            }
            return null;
        }

        private void retrieveImageUrl(ImageInfo imageInfo) throws IOException {
            String link = getImageURL(imageInfo);

            if (link.isEmpty()) {
                return;
            }
            try {
                imageCollector.addUrl(resolveRelativeURL(imageInfo.getDestinationDocumentURL(), link));
            } catch (URISyntaxException ignored) {
            }
        }

        private String resolveRelativeURL(final String baseURL, final String link) throws URISyntaxException {
            URI uri = new URI(link);
            if (uri.isAbsolute()) {
                return link;
            }
            return new URI(baseURL).resolve(uri).toString();
        }

        private String getImageURL(ImageInfo imageInfo) throws IOException {
            final String link = imageInfo.getImageURL();
            // parse document only if the imageURL is not set (ie isn't cached)
            if (link != null) {
                return link;
            }
            final String selector = imageInfo.getSelector();
            final String url = imageInfo.getDestinationDocumentURL();
            if (imageInfo.hasPageSel()) {
                return getDocumentFromUrl(url).select(selector).attr(imageInfo.getSelAttr());
            }
            if (selector.trim().isEmpty()) {
                // if the selector is empty then 'url' is an image
                // and doesn't need to be parsed
                return url;
            }
            return getDocumentFromUrl(url).select(selector).attr("src");
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            progressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(Void voidParam) {
            try {
                progressDialog.dismiss();
                if (error == null) {
                    callback.onImagesRetrieved(ImageUrlRetriever.this);
                } else {
                    new AlertDialog.Builder(context)
                    .setTitle(R.string.url_not_found)
                    .setMessage(error.getLocalizedMessage())
                    .show();
                }
            } catch (Exception e) {
                new AlertDialog.Builder(context)
                .setTitle(R.string.parsing_error)
                .setMessage(title + "\n" + e.getLocalizedMessage())
                .show();
            }
        }
    }

    public Document getDocumentFromUrl(String url) throws IOException {
        DOMSelector domSelector = domSelectorFinder.getSelectorFromUrl(url);
        if (domSelector == null || domSelector.getPostData() == null) {
            return HtmlDocumentSupport.getDocument(url);
        }
        return Jsoup.connect(url)
                .userAgent(HtmlDocumentSupport.DESKTOP_USER_AGENT)
                .data(domSelector.getPostData())
                .post();
    }

    public ImageCollector getImageCollector() {
        return imageCollector;
    }
}
