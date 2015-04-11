package com.ternaryop.photoshelf;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.ternaryop.utils.URLUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ImageUrlRetriever {
    private final Context context;
    private String title;
    private Exception error = null;
    private final OnImagesRetrieved callback;
    private boolean useFile;
    private ArrayList<String> imageUrls;
    private ArrayList<File> imageFiles;

    public ImageUrlRetriever(Context context, OnImagesRetrieved callback) {
        this.context = context;
        this.callback = callback;
    }

    public String getTitle() {
        return title;
    }


    public void setTitle(String title) {
        this.title = title;
    }

    public void retrieve(List<ImageInfo> list, boolean useFile) {
        this.useFile = useFile;
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
            if (useFile) {
                imageFiles = new ArrayList<File>();
                imageUrls = null;
            } else {
                imageUrls = new ArrayList<String>();
                imageFiles = null;
            }
            try {
                int i = 1;
                for (ImageInfo imageInfo : list) {
                    String selector = imageInfo.getSelector();
                    String url = imageInfo.getDestinationDocumentURL();
                    String link;
                    // if the selector is empty then 'url' is an image
                    // and doesn't need to be parsed
                    if (selector.trim().length() == 0) {
                        link = url;
                    } else {
                        // parse document on if the imageURL is not set
                        if (imageInfo.getImageURL() == null) {
                            Document htmlDocument = Jsoup.connect(url).get();
                            if (title == null) {
                                title = htmlDocument.title();
                            }
                            link = htmlDocument.select(selector).attr("src");
                        } else {
                            link = imageInfo.getImageURL();
                        }
                    }
                    if (!link.isEmpty()) {
                        // if necessary resolve relative urls
                        try {
                            URI uri = new URI(link);
                            if (!uri.isAbsolute()) {
                                uri = new URI(url).resolve(uri);
                                link = uri.toString();
                            }
                            if (imageFiles != null) {
                                File file = new File(context.getCacheDir(), String.valueOf(link.hashCode()));
                                FileOutputStream fos = new FileOutputStream(file);
                                try {
                                    URLUtils.saveURL(link, fos);
                                    imageFiles.add(file);
                                } finally {
                                    fos.close();
                                }
                            } else {
                                imageUrls.add(link);
                            }
                        } catch (URISyntaxException ignored) {
                        }
                    }
                    publishProgress(i++);
                }
            } catch (Exception e) {
                error = e;
                return null;
            }
            return null;
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
    
    public List<File> getImageFiles() {
        return imageFiles;
    }
    
    public List<String> getImageUrls() {
        return imageUrls;
    }
}
