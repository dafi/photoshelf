package com.ternaryop.phototumblrshare.activity;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.utils.DialogUtils;
import com.ternaryop.utils.ImageUtils;
import com.ternaryop.utils.ShareUtils;

@SuppressLint("SetJavaScriptEnabled")
public class ImageViewerActivity extends PhotoTumblrActivity {
	private static final String IMAGE_URL = "imageUrl";
	private static final String IMAGE_TITLE = "imageTitle";
	private boolean webViewLoaded;
	private Menu optionsMenu;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_webview);
	    setActionBarIcon();

	    ProgressBar progressBar = (ProgressBar) findViewById(R.id.webview_progressbar);
	    
	    Bundle bundle = getIntent().getExtras();
		String imageUrl = bundle.getString(IMAGE_URL);
    	String data = "<body><img src=\"" + imageUrl +"\"/></body>";
    	prepareWebView(progressBar).loadData(data, "text/html", "UTF-8");
	    try {
			getActionBar().setSubtitle(new URI(imageUrl).getHost());
		} catch (URISyntaxException e) {
		}
	}

	private WebView prepareWebView(final ProgressBar progressBar) {
		WebView webView = (WebView) findViewById(R.id.webview_view);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.addJavascriptInterface(this, "dimRetriever");
		webViewLoaded = false;

//		webView.setInitialScale(1);
//		webView.getSettings().setLoadWithOverviewMode(true);
		webView.getSettings().setUseWideViewPort(true);		
		
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
				// onPrepareOptionsMenu should be called after onPageFinished
				if (optionsMenu != null) {
					optionsMenu.findItem(R.id.action_image_viewer_wallpaper).setVisible(true);
					optionsMenu.findItem(R.id.action_image_viewer_share).setVisible(true);
				}
			}
		});
		return webView;
	}

	public static void startImageViewer(Context context, String url, String title) {
		Intent intent = new Intent(context, ImageViewerActivity.class);
		Bundle bundle = new Bundle();

		bundle.putString(IMAGE_URL, url);
		if (title != null) {
			bundle.putString(IMAGE_TITLE, title);
		}
		intent.putExtras(bundle);

		context.startActivity(intent);
	}

	@JavascriptInterface
	public void setDimensions(final int w, final int h) {
		runOnUiThread(new Runnable() {
		    public void run() {
				setTitle(getString(R.string.image_size, w, h));
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
			menu.findItem(R.id.action_image_viewer_wallpaper).setVisible(false);
			menu.findItem(R.id.action_image_viewer_share).setVisible(false);
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_image_viewer_wallpaper:
			setWallpaper();
			return true;
		case R.id.action_image_viewer_share:
			shareImage();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void shareImage() {
		try {
			final String imageUrl = getIntent().getExtras().getString(IMAGE_URL);
			String path = new URI(imageUrl).getPath();
			int index = path.lastIndexOf('/');
			if (index != -1) {
				path = path.substring(index + 1);
			}
			// write to a public location otherwise the called app can't access to file
			final File destFile = new File(Environment.getExternalStorageDirectory(), path);
			new AsyncTask<Void, Void, Exception>() {

				@Override
				protected Exception doInBackground(Void... voidParams) {
					BufferedOutputStream bos = null;
					InputStream is = null;
			    	try {
			    		URL url = new URL(imageUrl);
			    		HttpURLConnection connection  = (HttpURLConnection) url.openConnection();
			    	
			    	    bos = new BufferedOutputStream(new FileOutputStream(destFile));
			    		is = connection.getInputStream();
			    		byte[] buff = new byte[100 * 1024];
			    		int count;

			    		while ((count = is.read(buff)) != -1) {
			    			bos.write(buff, 0, count);
			    		}

			    	    bos.flush();
			    	} catch (Exception e) {
			    		return e;
			    	} finally {
						if (is != null) try { is.close(); } catch (Exception e) {}
						if (bos != null) try { bos.close(); } catch (Exception e) {}
			    	}
					return null;
				}
				
				@Override
				protected void onPostExecute(Exception error) {
					if (error != null) {
						DialogUtils.showErrorDialog(ImageViewerActivity.this, error);
						return;
					}
				    String title = getIntent().getExtras().getString(IMAGE_TITLE);
				    if (title == null) {
				    	title = "";
				    } else {
				    	title = Html.fromHtml(title).toString();
				    }

					ShareUtils.shareFile(ImageViewerActivity.this,
							destFile.getAbsolutePath(),
							"image/jpeg",
							title,
							getString(R.string.share_title));
				}
			}.execute();
		} catch (Exception e) {
			DialogUtils.showErrorDialog(this, e);
		}
		
	}

	private void setWallpaper() {
		new AsyncTask<Void, Void, Bitmap>() {

			@Override
			protected Bitmap doInBackground(Void... params) {
				try {
					final String imageUrl = getIntent().getExtras().getString(IMAGE_URL);
					return ImageUtils.readImage(imageUrl);
				} catch (Exception e) {
				}
				
				return null;
			}
			
			@Override
			protected void onPostExecute(Bitmap bitmap) {
				if (bitmap == null) {
					return;
				}
				try {
					WallpaperManager wpm = WallpaperManager.getInstance(ImageViewerActivity.this);
		            float width =  wpm.getDesiredMinimumWidth();
					float height =  wpm.getDesiredMinimumHeight();
					bitmap = ImageUtils.getResizedBitmap(bitmap, width, height);
					wpm.setBitmap(bitmap);
		            Toast.makeText(ImageViewerActivity.this,
		            		getString(R.string.wallpaper_changed_title),
							Toast.LENGTH_LONG)
							.show();
				} catch (IOException e) {
					DialogUtils.showErrorDialog(ImageViewerActivity.this, e);
				}
			}
		}.execute();
	}
}
