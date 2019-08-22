package com.ternaryop.photoshelf.activity

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.ternaryop.photoshelf.BuildConfig
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.service.WallpaperIntentService
import com.ternaryop.photoshelf.util.viewer.ImageViewerUtil
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.utils.dialog.showErrorDialog
import com.ternaryop.utils.intent.ShareChooserParams
import com.ternaryop.utils.text.fromHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.util.Locale

const val DIMENSIONS_POST_DELAY_MILLIS = 3000L
const val FILE_PROVIDER_SHARE_AUTHORITY = "${BuildConfig.APPLICATION_ID}.fileprovider"

private const val SUBDIRECTORY_PICTURES = "TernaryOpPhotoShelf"

private fun getPicturesDirectory(): File {
    val fullDirPath = File(Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_PICTURES), SUBDIRECTORY_PICTURES)
    if (!fullDirPath.exists()) {
        fullDirPath.mkdirs()
    }
    return fullDirPath
}

@SuppressLint("SetJavaScriptEnabled")
class ImageViewerActivity : AbsPhotoShelfActivity() {
    private var webViewLoaded: Boolean = false
    private var optionsMenu: Menu? = null
    private var imageHostUrl: String? = null
    private lateinit var detailsText: TextView

    override val contentViewLayoutId: Int
        get() = R.layout.activity_webview

    private lateinit var imageUrl: String

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = ""

        val progressBar = findViewById<View>(R.id.webview_progressbar) as ProgressBar
        detailsText = findViewById<View>(R.id.details_text) as TextView

        imageUrl = intent.extras?.getString(IMAGE_URL) ?: return

        val data = "<body><img src=\"$imageUrl\"/></body>"
        prepareWebView(progressBar).loadDataWithBaseURL(null, data, "text/html", "UTF-8", null)
        try {
            imageHostUrl = URI(imageUrl).host
        } catch (ignored: URISyntaxException) {
        }
    }

    override fun createFragment(): Fragment? = null

    private fun prepareWebView(progressBar: ProgressBar): WebView {
        val webView = findViewById<View>(R.id.webview_view) as WebView
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(this, "dimRetriever")
        webViewLoaded = false

        webView.setInitialScale(1)
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.settings.builtInZoomControls = true
        webView.settings.setSupportZoom(true)
        webView.settings.displayZoomControls = false

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                progressBar.progress = 0
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                webViewLoaded = true
                progressBar.visibility = View.GONE
                view.loadUrl("javascript:var img = document.querySelector('img');dimRetriever.setDimensions(img.width, img.height)")
                // onPrepareOptionsMenu should be called after onPageFinished
                if (optionsMenu != null) {
                    showMenus(optionsMenu!!, true)
                }
            }
        }
        return webView
    }

    @JavascriptInterface
    @Suppress("unused")
    fun setDimensions(w: Int, h: Int) {
        runOnUiThread {
            detailsText.visibility = View.VISIBLE
            detailsText.text = String.format(Locale.US, "%s (%1dx%2d)", imageHostUrl, w, h)
            Handler().postDelayed({ detailsText.visibility = View.GONE }, DIMENSIONS_POST_DELAY_MILLIS)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.image_viewer, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        optionsMenu = menu
        if (!webViewLoaded) {
            showMenus(menu, false)
        }
        return true
    }

    private fun showMenus(menu: Menu, isVisible: Boolean) {
        menu.findItem(R.id.action_image_viewer_wallpaper).isVisible = isVisible
        menu.findItem(R.id.action_image_viewer_share).isVisible = isVisible
        menu.findItem(R.id.action_image_viewer_download).isVisible = isVisible
        menu.findItem(R.id.action_image_viewer_copy_url).isVisible = isVisible
        menu.findItem(R.id.action_image_viewer_details).isVisible = isVisible
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_image_viewer_wallpaper -> {
                WallpaperIntentService.startChangeWallpaperIntent(this, Uri.parse(imageUrl))
                return true
            }
            R.id.action_image_viewer_share -> {
                startShareImage()
                return true
            }
            R.id.action_image_viewer_download -> {
                startDownload()
                return true
            }
            R.id.action_image_viewer_copy_url -> {
                ImageViewerUtil.copyToClipboard(this, imageUrl,
                    getString(R.string.image_url_description), R.string.url_copied_to_clipboard_title)
                return true
            }
            R.id.action_image_viewer_details -> {
                toggleDetails()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun startDownload() {
        val fileName = ImageViewerUtil.buildFileName(imageUrl, intent.extras?.getString(IMAGE_TAG))
        ImageViewerUtil.download(this, imageUrl, Uri.fromFile(File(getPicturesDirectory(), fileName)))
    }

    private fun startShareImage() {
        try {
            val destFile = ImageViewerUtil.buildSharePath(this, imageUrl, "images")
            val shareChooserParams = ShareChooserParams(
                FileProvider.getUriForFile(this, FILE_PROVIDER_SHARE_AUTHORITY, destFile),
                getString(R.string.share_image_title),
                intent.extras?.getString(IMAGE_TITLE)?.fromHtml()?.toString() ?: "")
            launch(Dispatchers.IO) {
                ImageViewerUtil.shareImage(this@ImageViewerActivity, URL(imageUrl), shareChooserParams)
            }
        } catch (t: Throwable) {
            t.showErrorDialog(this)
        }
    }

    private fun toggleDetails() {
        detailsText.visibility = if (detailsText.visibility == View.GONE) View.VISIBLE else View.GONE
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
    }

    companion object {
        private const val IMAGE_URL = "imageUrl"
        private const val IMAGE_TITLE = "imageTitle"
        private const val IMAGE_TAG = "imageTag"

        fun startImageViewer(context: Context, url: String, post: TumblrPhotoPost? = null) {
            val intent = Intent(context, ImageViewerActivity::class.java)
            val bundle = Bundle()

            bundle.putString(IMAGE_URL, url)
            if (post != null) {
                bundle.putString(IMAGE_TITLE, post.caption)
                if (post.tags.isNotEmpty()) {
                    bundle.putString(IMAGE_TAG, post.tags[0])
                }
            }
            intent.putExtras(bundle)

            val animBundle = ActivityOptions.makeCustomAnimation(context,
                    R.anim.slide_in_left, R.anim.slide_out_left).toBundle()
            context.startActivity(intent, animBundle)
        }
    }
}