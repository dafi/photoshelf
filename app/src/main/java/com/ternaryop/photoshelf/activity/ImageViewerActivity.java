package com.ternaryop.photoshelf.activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.Callable;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.DownloadManager;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ternaryop.photoshelf.AppSupport;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.service.PublishIntentService;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.utils.DialogUtils;
import com.ternaryop.utils.IOUtils;
import com.ternaryop.utils.ShareUtils;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

@SuppressLint("SetJavaScriptEnabled")
public class ImageViewerActivity extends AbsPhotoShelfActivity {
    private static final String IMAGE_URL = "imageUrl";
    private static final String IMAGE_TITLE = "imageTitle";
    private static final String IMAGE_TAG = "imageTag";
    private boolean webViewLoaded;
    private Menu optionsMenu;
    private String imageHostUrl;
    private TextView detailsText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("");

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.webview_progressbar);
        detailsText = (TextView) findViewById(R.id.details_text);

        String imageUrl = getImageUrl();
        if (imageUrl == null) {
            return;
        }
        String data = "<body><img src=\"" + imageUrl + "\"/></body>";
        prepareWebView(progressBar).loadDataWithBaseURL(null, data, "text/html", "UTF-8", null);
        try {
            imageHostUrl = new URI(imageUrl).getHost();
        } catch (URISyntaxException ignored) {
        }
    }

    @Override
    public int getContentViewLayoutId() {
        return R.layout.activity_webview;
    }

    @Override
    public Fragment createFragment() {
        return null;
    }

    private WebView prepareWebView(final ProgressBar progressBar) {
        WebView webView = (WebView) findViewById(R.id.webview_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(this, "dimRetriever");
        webViewLoaded = false;

        webView.setInitialScale(1);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setDisplayZoomControls(false);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setProgress(0);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                webViewLoaded = true;
                progressBar.setVisibility(View.GONE);
                view.loadUrl("javascript:var img = document.querySelector('img');dimRetriever.setDimensions(img.width, img.height)");
                // onPrepareOptionsMenu should be called after onPageFinished
                if (optionsMenu != null) {
                    showMenus(optionsMenu, true);
                }
            }
        });
        return webView;
    }

    public static void startImageViewer(Context context, String url, TumblrPhotoPost post) {
        Intent intent = new Intent(context, ImageViewerActivity.class);
        Bundle bundle = new Bundle();

        bundle.putString(IMAGE_URL, url);
        if (post != null) {
            bundle.putString(IMAGE_TITLE, post.getCaption());
            if (!post.getTags().isEmpty()) {
                bundle.putString(IMAGE_TAG, post.getTags().get(0));
            }
        }
        intent.putExtras(bundle);

        Bundle animBundle = ActivityOptions.makeCustomAnimation(context,
                R.anim.slide_in_left, R.anim.slide_out_left).toBundle();
        context.startActivity(intent, animBundle);
    }

    @JavascriptInterface
    public void setDimensions(final int w, final int h) {
        runOnUiThread(new Runnable() {
            public void run() {
                detailsText.setVisibility(View.VISIBLE);
                detailsText.setText(String.format(Locale.US, "%s (%1dx%2d)", imageHostUrl , w, h));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        detailsText.setVisibility(View.GONE);
                    }
                }, 3 * 1000);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_viewer, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        optionsMenu = menu;
        if (!webViewLoaded) {
            showMenus(menu, false);
        }
        return true;
    }

    private void showMenus(Menu menu, boolean isVisible) {
        menu.findItem(R.id.action_image_viewer_wallpaper).setVisible(isVisible);
        menu.findItem(R.id.action_image_viewer_share).setVisible(isVisible);
        menu.findItem(R.id.action_image_viewer_download).setVisible(isVisible);
        menu.findItem(R.id.action_image_viewer_copy_url).setVisible(isVisible);
        menu.findItem(R.id.action_image_viewer_details).setVisible(isVisible);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_image_viewer_wallpaper:
                changeWallpaper();
                return true;
            case R.id.action_image_viewer_share:
                shareImage();
                return true;
            case R.id.action_image_viewer_download:
                download();
                return true;
            case R.id.action_image_viewer_copy_url:
                copyURL();
                return true;
            case R.id.action_image_viewer_details:
                toogleDetails();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void toogleDetails() {
        detailsText.setVisibility(detailsText.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
    }

    private void download() {
        try {
            final String imageUrl = getImageUrl();

            if (imageUrl == null) {
                return;
            }

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                String imageTag = getIntent().getExtras() == null ? null : getIntent().getExtras().getString(IMAGE_TAG);
                String fileName = buildFileName(imageUrl, imageTag);
                DownloadManager.Request request = new DownloadManager
                        .Request(Uri.parse(imageUrl))
                        .setDestinationUri(Uri.fromFile(new File(AppSupport.getPicturesDirectory(), fileName)));
                downloadManager.enqueue(request);
            }
        } catch (Exception e) {
            DialogUtils.showErrorDialog(this, e);
        }
    }

    private void copyURL() {
        String imageUrl = getImageUrl();

        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            return;
        }
        clipboardManager.setPrimaryClip(ClipData.newPlainText(getString(R.string.image_url_description), imageUrl));
        Toast.makeText(this,
                R.string.url_copied_to_clipboard_title,
                Toast.LENGTH_SHORT)
                .show();
    }

    private void shareImage() {
        try {
            final String imageUrl = getImageUrl();
            if (imageUrl == null) {
                return;
            }
            String fileName = buildFileName(imageUrl, null);
            // write to a public location otherwise the called app can't access to file
            final File destFile = new File(AppSupport.getPicturesDirectory(), fileName);
            downloadImageUrl(new URL(imageUrl), destFile)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action() {
                                   @Override
                                   public void run() throws Exception {
                                        startShareImage(destFile);
                                   }
                               },
                            new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    DialogUtils.showErrorDialog(ImageViewerActivity.this, throwable);
                                }
                            });
        } catch (Exception e) {
            DialogUtils.showErrorDialog(this, e);
        }
    }

    private void startShareImage(File destFile) {
        String title = getIntent().getExtras() == null ? null : getIntent().getExtras().getString(IMAGE_TITLE);
        if (title == null) {
            title = "";
        } else {
            title = Html.fromHtml(title).toString();
        }

        ShareUtils.shareImage(this,
                destFile.getAbsolutePath(),
                "image/jpeg",
                title,
                getString(R.string.share_image_title));
    }

    private void changeWallpaper() {
        PublishIntentService.startChangeWallpaperIntent(this, Uri.parse(getImageUrl()));
    }

    public Completable downloadImageUrl(final URL imageUrl, final File destFile) {
        return Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
                try (InputStream is = connection.getInputStream(); FileOutputStream os = new FileOutputStream(destFile)) {
                    IOUtils.copy(is, os);
                }
                return null;
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
    }

    private @Nullable String getImageUrl() {
        final Bundle extras = getIntent().getExtras();
        if (extras == null) {
            return null;
        }
        return extras.getString(IMAGE_URL);
    }

    @NonNull
    private String buildFileName(final String imageUrl, final String fileName) throws URISyntaxException {
        if (fileName == null) {
            String nameFromUrl = new URI(imageUrl).getPath();
            int index = nameFromUrl.lastIndexOf('/');
            if (index != -1) {
                nameFromUrl = nameFromUrl.substring(index + 1);
            }
            return nameFromUrl;
        }
        int index = imageUrl.lastIndexOf(".");
        // append extension with "."
        if (index != -1) {
            return fileName + imageUrl.substring(index);
        }
        return fileName;
    }
}