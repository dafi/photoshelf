package com.ternaryop.photoshelf;

import java.net.URI;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.ternaryop.photoshelf.extractor.ImageExtractorManager;
import com.ternaryop.photoshelf.extractor.ImageInfo;
import com.ternaryop.utils.UriUtils;

public class ImageUrlRetriever {
    private final Context context;
    private String title;
    private Exception error = null;
    private final OnImagesRetrieved callback;
    private final ImageCollector imageCollector;

    public ImageUrlRetriever(Context context, OnImagesRetrieved callback) {
        this.context = context;
        this.callback = callback;
        imageCollector = new ImageCollector(context);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void retrieve(List<ImageInfo> list, boolean useFile) {
        imageCollector.clear();
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

        private void retrieveImageUrl(ImageInfo imageInfo) throws Exception {
            String link = getImageURL(imageInfo);

            if (link.isEmpty()) {
                return;
            }
            imageCollector.addUrl(resolveRelativeURL(imageInfo.getDocumentUrl(), link));
        }

        private String resolveRelativeURL(final String baseURL, final String link) throws Exception {
            URI uri =  UriUtils.encodeIllegalChar(link, "UTF-8", 20);
            if (uri.isAbsolute()) {
                return uri.toString();
            }
            return UriUtils.encodeIllegalChar(baseURL, "UTF-8", 20).resolve(uri).toString();
        }

        private String getImageURL(ImageInfo imageInfo) throws Exception {
            final String link = imageInfo.getImageUrl();
            // parse document only if the imageURL is not set (ie isn't cached)
            if (link != null) {
                return link;
            }
            final String url = imageInfo.getDocumentUrl();
            return new ImageExtractorManager(context.getString(R.string.PHOTOSHELF_EXTRACTOR_ACCESS_TOKEN)).getImageUrl(url);
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

    public ImageCollector getImageCollector() {
        return imageCollector;
    }
}
