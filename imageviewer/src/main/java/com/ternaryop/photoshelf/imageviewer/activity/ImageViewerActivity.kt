package com.ternaryop.photoshelf.imageviewer.activity

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.ternaryop.compat.content.getActivityInfoCompat
import com.ternaryop.compat.os.getSerializableCompat
import com.ternaryop.photoshelf.activity.AbsPhotoShelfActivity
import com.ternaryop.photoshelf.activity.ImageViewerData
import com.ternaryop.photoshelf.imageviewer.R
import com.ternaryop.photoshelf.imageviewer.service.WallpaperService
import com.ternaryop.photoshelf.imageviewer.util.ImageViewerUtil
import com.ternaryop.photoshelf.util.menu.enableAll
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
const val FILE_PROVIDER_SHARE_AUTHORITY = "com.ternaryop.photoshelf.imagepicker.viewer.fileProviderShareAuthority"

private const val SUBDIRECTORY_PICTURES = "TernaryOpPhotoShelf"

@SuppressLint("SetJavaScriptEnabled")
class ImageViewerActivity : AbsPhotoShelfActivity() {
    private var webViewLoaded: Boolean = false
    private var imageHostUrl: String? = null
    private lateinit var detailsText: TextView

    override val contentViewLayoutId: Int = R.layout.activity_webview

    // no frame inside the layout so no need for contentFrameId
    override val contentFrameId: Int = 0

    private lateinit var imageViewerData: ImageViewerData

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = ""

        val progressBar = findViewById<View>(R.id.webview_progressbar) as ProgressBar
        detailsText = findViewById<View>(R.id.details_text) as TextView

        imageViewerData = intent.extras?.getSerializableCompat(EXTRA_IMAGE_VIEWER_DATA, ImageViewerData::class.java)
            ?: return

        val data = "<body><img src=\"${imageViewerData.imageUrl}\"/></body>"
        prepareWebView(progressBar).loadDataWithBaseURL(null, data, "text/html", "UTF-8", null)
        try {
            imageHostUrl = URI(imageViewerData.imageUrl).host
        } catch (ignored: URISyntaxException) {
        }
        drawerToolbar.overflowIcon = ContextCompat.getDrawable(this, R.drawable.ic_imageviewer_overflow)
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
                @Suppress("MaxLineLength")
                view.loadUrl("javascript:var img = document.querySelector('img');dimRetriever.setDimensions(img.width, img.height)")
                invalidateOptionsMenu()
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
            Handler(Looper.getMainLooper())
                .postDelayed({ detailsText.visibility = View.GONE }, DIMENSIONS_POST_DELAY_MILLIS)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.image_viewer, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.enableAll(webViewLoaded)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_image_viewer_wallpaper -> {
                WallpaperService.startChange(this, imageViewerData.imageUrl)
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
                ImageViewerUtil.copyToClipboard(
                    this, imageViewerData.imageUrl,
                    getString(R.string.image_url_description), R.string.url_copied_to_clipboard_title
                )
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
        val fileName = ImageViewerUtil.buildFileName(imageViewerData.imageUrl, imageViewerData.tag)
        launch {
            try {
                val relativePath = File(SUBDIRECTORY_PICTURES, fileName)
                ImageViewerUtil.download(this@ImageViewerActivity, imageViewerData.imageUrl, relativePath)
            } catch (t: Throwable) {
                t.showErrorDialog(this@ImageViewerActivity)
            }
        }
    }

    private fun startShareImage() {
        try {
            val destFile = ImageViewerUtil.buildSharePath(this, imageViewerData.imageUrl, "images")
            val shareChooserParams = ShareChooserParams(
                FileProvider.getUriForFile(this, getFileProvider(), destFile),
                getString(R.string.share_image_title),
                imageViewerData.title?.fromHtml()?.toString() ?: ""
            )
            launch(Dispatchers.IO) {
                ImageViewerUtil.shareImage(this@ImageViewerActivity, URL(imageViewerData.imageUrl), shareChooserParams)
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
        overridePendingTransition(com.ternaryop.photoshelf.core.R.anim.slide_in_right, com.ternaryop.photoshelf.core.R.anim.slide_out_right)
    }

    private fun getFileProvider(): String {
        val componentName = ComponentName(this, javaClass)
        val data = packageManager.getActivityInfoCompat(componentName, PackageManager.GET_META_DATA.toLong()).metaData
        return checkNotNull(data.getString(FILE_PROVIDER_SHARE_AUTHORITY)) {
            "Unable to find $FILE_PROVIDER_SHARE_AUTHORITY"
        }
    }

    companion object {
        const val EXTRA_IMAGE_VIEWER_DATA = "com.ternaryop.photoshelf.extra.IMAGE_VIEWER_DATA"

        fun startImageViewer(context: Context, imageViewerData: ImageViewerData) {
            val intent = Intent(context, ImageViewerActivity::class.java)
            val bundle = Bundle()

            bundle.putSerializable(EXTRA_IMAGE_VIEWER_DATA, imageViewerData)
            intent.putExtras(bundle)

            val animBundle = ActivityOptions.makeCustomAnimation(
                context,
                com.ternaryop.photoshelf.core.R.anim.slide_in_left,
                com.ternaryop.photoshelf.core.R.anim.slide_out_left
            ).toBundle()
            context.startActivity(intent, animBundle)
        }
    }
}
