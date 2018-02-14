package com.ternaryop.photoshelf.activity

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.DownloadManager
import android.app.Fragment
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
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
import android.widget.Toast
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.service.PublishIntentService
import com.ternaryop.photoshelf.util.text.fromHtml
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.utils.DialogUtils
import com.ternaryop.utils.IOUtils
import com.ternaryop.utils.ShareUtils
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
class ImageViewerActivity : AbsPhotoShelfActivity() {
    private var webViewLoaded: Boolean = false
    private var optionsMenu: Menu? = null
    private var imageHostUrl: String? = null
    private lateinit var detailsText: TextView

    override val contentViewLayoutId: Int
        get() = R.layout.activity_webview

    private val imageUrl: String?
        get() = intent.extras?.getString(IMAGE_URL)

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = ""

        val progressBar = findViewById<View>(R.id.webview_progressbar) as ProgressBar
        detailsText = findViewById<View>(R.id.details_text) as TextView

        val imageUrl = imageUrl ?: return
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
    fun setDimensions(w: Int, h: Int) {
        runOnUiThread {
            detailsText.visibility = View.VISIBLE
            detailsText.text = String.format(Locale.US, "%s (%1dx%2d)", imageHostUrl, w, h)
            Handler().postDelayed({ detailsText.visibility = View.GONE }, 3 * 1000L)
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
                changeWallpaper()
                return true
            }
            R.id.action_image_viewer_share -> {
                shareImage()
                return true
            }
            R.id.action_image_viewer_download -> {
                download()
                return true
            }
            R.id.action_image_viewer_copy_url -> {
                copyURL()
                return true
            }
            R.id.action_image_viewer_details -> {
                toggleDetails()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun toggleDetails() {
        detailsText.visibility = if (detailsText.visibility == View.GONE) View.VISIBLE else View.GONE
    }

    private fun download() {
        try {
            val imageUrl = imageUrl ?: return

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val fileName = buildFileName(imageUrl, intent.extras?.getString(IMAGE_TAG))
            val request = DownloadManager.Request(Uri.parse(imageUrl))
                    .setDestinationUri(Uri.fromFile(File(AppSupport.picturesDirectory, fileName)))
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            DialogUtils.showErrorDialog(this, e)
        }
    }

    private fun copyURL() {
        val imageUrl = imageUrl

        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.primaryClip = ClipData.newPlainText(getString(R.string.image_url_description), imageUrl)
        Toast.makeText(this,
                R.string.url_copied_to_clipboard_title,
                Toast.LENGTH_SHORT)
                .show()
    }

    private fun shareImage() {
        try {
            val imageUrl = imageUrl ?: return
            val fileName = buildFileName(imageUrl, null)
            // write to a public location otherwise the called app can't access to file
            val destFile = File(AppSupport.picturesDirectory, fileName)
            downloadImageUrl(URL(imageUrl), destFile)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ startShareImage(destFile) }
                    ) { throwable -> DialogUtils.showErrorDialog(this@ImageViewerActivity, throwable) }
        } catch (e: Exception) {
            DialogUtils.showErrorDialog(this, e)
        }
    }

    private fun startShareImage(destFile: File) {
        var title: String? = if (intent.extras == null) null else intent.extras!!.getString(IMAGE_TITLE)
        title = if (title == null) {
            ""
        } else {
            title.fromHtml().toString()
        }

        ShareUtils.shareImage(this,
                destFile.absolutePath,
                "image/jpeg",
                title,
                getString(R.string.share_image_title))
    }

    private fun changeWallpaper() {
        PublishIntentService.startChangeWallpaperIntent(this, Uri.parse(imageUrl))
    }

    private fun downloadImageUrl(imageUrl: URL, destFile: File): Completable {
        return Completable.fromCallable {
            val connection = imageUrl.openConnection() as HttpURLConnection
            connection.inputStream.use { stream -> FileOutputStream(destFile).use { os -> IOUtils.copy(stream, os) } }
            null
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
    }

    @Throws(URISyntaxException::class)
    private fun buildFileName(imageUrl: String, fileName: String?): String {
        if (fileName == null) {
            var nameFromUrl = URI(imageUrl).path
            val index = nameFromUrl.lastIndexOf('/')
            if (index != -1) {
                nameFromUrl = nameFromUrl.substring(index + 1)
            }
            return nameFromUrl
        }
        val index = imageUrl.lastIndexOf(".")
        // append extension with "."
        return if (index != -1) {
            fileName + imageUrl.substring(index)
        } else fileName
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