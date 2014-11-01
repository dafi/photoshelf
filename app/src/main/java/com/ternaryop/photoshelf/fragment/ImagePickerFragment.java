package com.ternaryop.photoshelf.fragment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.ternaryop.photoshelf.Constants;
import com.ternaryop.photoshelf.ImageDOMSelectorFinder;
import com.ternaryop.photoshelf.ImagePickerWebViewClient;
import com.ternaryop.photoshelf.ImageUrlRetriever;
import com.ternaryop.photoshelf.ImageUrlRetriever.OnImagesRetrieved;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.dialogs.TumblrPostDialog;
import com.ternaryop.photoshelf.parsers.TitleData;
import com.ternaryop.photoshelf.parsers.TitleParser;
import com.ternaryop.utils.URLUtils;

@SuppressLint("SetJavaScriptEnabled")
public class ImagePickerFragment extends AbsPhotoShelfFragment implements OnLongClickListener, OnImagesRetrieved {
    private WebView webView;
    private ImageUrlRetriever imageUrlRetriever;
    private ProgressBar progressBar;

    private ImageDOMSelectorFinder domSelectorFinder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_webview, container, false);
        getActivity().setTitle(R.string.image_picker_activity_title);

        imageUrlRetriever = new ImageUrlRetriever(getActivity(), this);
        progressBar = (ProgressBar) rootView.findViewById(R.id.webview_progressbar);
        prepareWebView(rootView);
        domSelectorFinder = new ImageDOMSelectorFinder(getActivity());

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
    
    private void prepareWebView(View view) {
        webView = (WebView) view.findViewById(R.id.webview_view);
        webView.setWebViewClient(new ImagePickerWebViewClient(progressBar));
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }
        });
        webView.setOnLongClickListener(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDisplayZoomControls(false);
        // force a desktop browser UA
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:25.0) Gecko/20100101 Firefox/25.0");
        webView.addJavascriptInterface(this, "targetUrlRetriever");
    }

    private void openUrl(String textWithUrl) {
        if (textWithUrl == null) {
            return;
        }
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
                    webView.loadUrl(url);
                }
            }.execute(url);
        } else {
            new AlertDialog.Builder(getActivity())
                .setTitle(R.string.url_not_found)
                .setMessage(getString(R.string.url_not_found_description, textWithUrl))
                .show();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        imageUrlRetriever.setTitle(webView.getTitle());
        Message msg = new Message();
        msg.setTarget(new LongClickHandler(this));
        webView.requestFocusNodeHref(msg);
        return false;
    }
    
    // This class is static to be sure the handler doesn't leak
    private final static class LongClickHandler extends Handler {
        private final ImagePickerFragment activity;

        public LongClickHandler(ImagePickerFragment activity) {
            this.activity = activity;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String url = msg.getData().getString("src");
            String js = String.format("document.querySelector(\"img[src='%1$s']\").parentNode.href", url);
            activity.webView.loadUrl("javascript:var url = " + js + "; targetUrlRetriever.setTargetHrefURL(url);");
        }
    }

    @Override
    public void onImagesRetrieved(ImageUrlRetriever imageUrlRetriever) {
        TitleData titleData = TitleParser.instance(getActivity()).parseTitle(imageUrlRetriever.getTitle());
        TumblrPostDialog dialog = new TumblrPostDialog(getActivity());
        dialog.setBlockUIWhilePublish(false);
        if (imageUrlRetriever.getImageUrls() != null) {
            dialog.setImageUrls(imageUrlRetriever.getImageUrls());
        } else {
            dialog.setImageFiles(imageUrlRetriever.getImageFiles());
        }
        dialog.setPostTitle(titleData.toHtml(), imageUrlRetriever.getTitle());
        dialog.setPostTags(titleData.getTags());
        
        dialog.show();
    }

    /**
     * Workaround for 4.4 bug 
     * http://code.google.com/p/android/issues/detail?id=62928
     * @param url the url to use use to add/remove to selected images
     */
    @JavascriptInterface
    public void setTargetHrefURL(final String url) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                String domSelector = domSelectorFinder.getSelectorFromUrl(url);
                if (domSelector != null) {
                    imageUrlRetriever.addOrRemoveUrl(domSelector, url);
                }
            }
        });
    }
}
