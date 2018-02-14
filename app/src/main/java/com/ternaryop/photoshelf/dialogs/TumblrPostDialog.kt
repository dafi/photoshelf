package com.ternaryop.photoshelf.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.DialogFragment
import android.app.Fragment
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.util.Pair
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.MultiAutoCompleteTextView
import android.widget.Spinner
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.customsearch.GoogleCustomSearchClient
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.db.TagCursorAdapter
import com.ternaryop.photoshelf.dialogs.mru.MRUDialog
import com.ternaryop.photoshelf.dialogs.mru.OnMRUListener
import com.ternaryop.photoshelf.parsers.AndroidTitleParserConfig
import com.ternaryop.photoshelf.parsers.TitleParser
import com.ternaryop.photoshelf.service.PublishIntentService
import com.ternaryop.photoshelf.util.mru.MRU
import com.ternaryop.photoshelf.util.text.fromHtml
import com.ternaryop.photoshelf.util.text.toHtml
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.utils.DialogUtils
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class TumblrPostDialog : DialogFragment(), Toolbar.OnMenuItemClickListener, OnMRUListener {

    private lateinit var postTitleView: EditText
    private lateinit var postTagsView: MultiAutoCompleteTextView
    private lateinit var blogList: Spinner
    private lateinit var appSupport: AppSupport
    private var photoPost: TumblrPhotoPost? = null
    private lateinit var tagAdapter: TagCursorAdapter
    var imageUrls: List<Uri>? = null
        private set
    private lateinit var htmlTitle: String
    private lateinit var sourceTitle: String
    private var initialTagList: List<String>? = null
    private var defaultPostTagsColor: ColorStateList? = null
    private var defaultPostTagsBackground: Drawable? = null

    private lateinit var compositeDisposable: CompositeDisposable
    private lateinit var mruTags: MRU
    private lateinit var mruTagsButton: ImageButton

    val postTitle: String
        get() {
            postTitleView.clearComposingText()
            return postTitleView.text.toHtml()
        }

    val postTags: String
        get() {
            // remove the empty string at the end, if present
            return postTagsView.text.toString().replace(",\\s*$".toRegex(), "")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appSupport = AppSupport(activity)
        decodeArguments()
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_PhotoShelf_Dialog)

        compositeDisposable = CompositeDisposable()

        // if the device rotates set again the listener
        if (savedInstanceState != null) {
            (fragmentManager.findFragmentByTag(MRU_FRAGMENT_DIALOG_TAG) as MRUDialog).setOnMRUListener(this)
        }
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    @SuppressLint("InflateParams") // for dialogs passing null for root is valid, ignore the warning
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = activity.layoutInflater.inflate(R.layout.dialog_publish_post, null)
        setupUI(view)
        val builder = AlertDialog.Builder(activity)
                .setView(view)
                .setNegativeButton(R.string.cancel_title) { _, _ -> compositeDisposable.clear() }
        if (photoPost == null) {
            val onClickPublishListener = OnClickPublishListener()
            builder.setNeutralButton(R.string.publish_post, onClickPublishListener)
            builder.setPositiveButton(R.string.draft_title, onClickPublishListener)
            view.findViewById<View>(R.id.refreshBlogList).setOnClickListener { fetchBlogNames() }
        } else {
            view.findViewById<View>(R.id.blog_list).visibility = View.GONE
            builder.setPositiveButton(R.string.edit_post_title) { _, _ -> editPost() }
        }

        return builder.create()
    }

    override fun onResume() {
        super.onResume()
        // Dimensions defined on xml layout are not used so we set them here (it works only if called inside onResume)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun setupUI(view: View) {
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.publish_post_overflow)
        toolbar.setOnMenuItemClickListener(this)

        postTitleView = view.findViewById(R.id.post_title)
        postTagsView = view.findViewById(R.id.post_tags)
        blogList = view.findViewById(R.id.blog)

        // the ContextThemeWrapper is necessary otherwise the autocomplete drop down items and the toolbar overflow menu items are styled incorrectly
        // since the switch to the AlertDialog the toolbar isn't styled from code so to fix it the theme is declared directly into xml
        tagAdapter = TagCursorAdapter(
                ContextThemeWrapper(activity, R.style.Theme_PhotoShelf_Dialog),
                android.R.layout.simple_dropdown_item_1line,
                "")
        tagAdapter.blogName = appSupport.selectedBlogName!!
        postTagsView.setAdapter<TagCursorAdapter>(tagAdapter)
        postTagsView.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())

        blogList.onItemSelectedListener = BlogItemSelectedListener()

        mruTagsButton = view.findViewById(R.id.mruTags)
        mruTagsButton.setOnClickListener({ this.openMRUDialog(it) })
        mruTags = MRU(activity, MRU_TAGS_KEY, MRU_TAGS_MAX_SIZE)
        mruTagsButton.isEnabled = mruTags.list.isNotEmpty()

        fillTags(initialTagList)
        postTitleView.setText(htmlTitle.fromHtml())
        // move caret to end
        postTitleView.setSelection(postTitleView.length())

        if (photoPost != null) {
            toolbar.setTitle(R.string.edit_post_title)
        } else {
            val size = imageUrls!!.size
            toolbar.title = activity.resources.getQuantityString(
                    R.plurals.post_image,
                    size,
                    size)
        }
    }

    private fun openMRUDialog(view: View) {
        MRUDialog.newInstance(mruTags.list)
                .setOnMRUListener(this)
                .show(fragmentManager, MRU_FRAGMENT_DIALOG_TAG)
    }

    fun contains(arr: List<String>, needle: String): Boolean {
        return arr.any { s -> s.trim { it <= ' ' }.compareTo(needle, ignoreCase = true) == 0 }
    }

    private fun fillTags(tags: List<String>?) {
        val firstTag = if (tags!!.isEmpty()) "" else tags[0]
        this.postTagsView.setText(TextUtils.join(", ", tags))

        if (firstTag.isEmpty()) {
            return
        }
        val handler = Handler()
        Thread(Runnable { handler.post { searchMisspelledName(firstTag) } }).start()
    }

    private fun searchMisspelledName(name: String) {
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

        Single
                .fromCallable { getMisspelledInfo(name) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally {
                    val dialog = dialog as AlertDialog?
                    // protect against NPE because inside onDestroy the dialog is already null
                    dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
                    if (dialog != null) {
                    }
                }
                .subscribe(object : SingleObserver<Pair<Int, String>> {
                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onSuccess(misspelledInfo: Pair<Int, String>) {
                        highlightTagName(misspelledInfo.first, misspelledInfo.second)
                    }

                    override fun onError(e: Throwable) {}
                })
    }

    @Throws(Exception::class)
    private fun getMisspelledInfo(name: String): Pair<Int, String> {
        var pair = getMatchingName(name)
        if (pair != null) {
            return pair
        }
        pair = getMisspelledName(name)
        return if (pair != null) {
            pair
        } else Pair.create(NAME_NOT_FOUND, name)
    }

    private fun getMatchingName(name: String): Pair<Int, String>? {
        val correctedName = DBHelper.getInstance(activity).tagMatcherDAO.getMatchingTag(name)
        if (name.equals(correctedName, ignoreCase = true)) {
            return Pair.create(NAME_ALREADY_EXISTS, correctedName)
        }
        return if (correctedName == null) {
            null
        } else Pair.create(NAME_MISSPELLED, correctedName)
    }

    @Throws(Exception::class)
    private fun getMisspelledName(name: String): Pair<Int, String>? {
        val correctedName = GoogleCustomSearchClient(
                getString(R.string.GOOGLE_CSE_APIKEY),
                getString(R.string.GOOGLE_CSE_CX))
                .getCorrectedQuery(name) ?: return null
        return Pair.create(NAME_MISSPELLED, correctedName)
    }

    private fun highlightTagName(nameType: Int, correctedName: String) {
        if (defaultPostTagsColor == null) {
            defaultPostTagsColor = postTagsView.textColors
            defaultPostTagsBackground = postTagsView.background
        }

        when (nameType) {
            NAME_ALREADY_EXISTS -> {
                postTagsView.setTextColor(defaultPostTagsColor)
                postTagsView.background = defaultPostTagsBackground
                val enabledState = intArrayOf(android.R.attr.state_enabled)
                DrawableCompat.setTint(mruTagsButton.drawable, defaultPostTagsColor!!.getColorForState(enabledState, Color.GREEN))
                mruTagsButton.setBackgroundColor(Color.TRANSPARENT)
            }
            NAME_MISSPELLED -> {
                postTagsView.setTextColor(Color.RED)
                postTagsView.setBackgroundColor(Color.YELLOW)
                postTagsView.setText(correctedName)
                DrawableCompat.setTint(mruTagsButton.drawable, Color.RED)
                mruTagsButton.setBackgroundColor(Color.YELLOW)
            }
            NAME_NOT_FOUND -> {
                postTagsView.setTextColor(Color.WHITE)
                postTagsView.setBackgroundColor(Color.RED)
                DrawableCompat.setTint(mruTagsButton.drawable, Color.WHITE)
                mruTagsButton.setBackgroundColor(Color.RED)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (photoPost == null) {
            val dialog = dialog as AlertDialog
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = false
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

            val blogSetNames = appSupport.blogList
            if (blogSetNames == null) {
                fetchBlogNames()
            } else {
                fillBlogList(blogSetNames)
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = true
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
            }
        }
    }

    private fun fillBlogList(blogNames: List<String>) {
        val adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, blogNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        blogList.adapter = adapter

        val selectedName = appSupport.selectedBlogName
        if (selectedName != null) {
            val position = adapter.getPosition(selectedName)
            if (position >= 0) {
                blogList.setSelection(position)
                tagAdapter.blogName = selectedName
                tagAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun fetchBlogNames() {
        val dialog = dialog as AlertDialog
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = false
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

        appSupport.clearBlogList()
        appSupport.fetchBlogNames(activity)
                .subscribe(object : SingleObserver<List<String>> {
                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onSuccess(blogNames: List<String>) {
                        fillBlogList(blogNames)
                        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = true
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    }

                    override fun onError(e: Throwable) {
                        dismiss()
                        DialogUtils.showErrorDialog(activity, e)
                    }
                })
    }

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.parse_title -> {
                parseTitle(false)
                return true
            }
            R.id.parse_title_swap -> {
                parseTitle(true)
                return true
            }
            R.id.source_title -> {
                fillWithSourceTitle()
                return true
            }
        }
        return false
    }

    private inner class OnClickPublishListener : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            val selectedBlogName = blogList.selectedItem as String
            appSupport.selectedBlogName = selectedBlogName

            val tags = TumblrPost.tagsFromString(postTags)
            tags.removeAt(0)
            mruTags.add(tags)
            mruTags.save()
            createPosts(which == DialogInterface.BUTTON_NEUTRAL, selectedBlogName, imageUrls!!, postTitle, postTags)
        }

        private fun createPosts(publish: Boolean, selectedBlogName: String, urls: List<Uri>, postTitle: String, postTags: String) {
            for (url in urls) {
                PublishIntentService.startActionIntent(activity,
                        url,
                        selectedBlogName,
                        postTitle,
                        postTags,
                        publish)
            }
        }
    }

    private fun editPost() {
        val newValues = mutableMapOf(
                "id" to photoPost!!.postId.toString(),
                "caption" to postTitle,
                "tags" to postTags
        )
        val selectedBlogName = appSupport.selectedBlogName!!

        val completable = Completable
                .fromAction {
                    Tumblr.getSharedTumblr(appSupport).editPost(selectedBlogName, newValues)
                    newValues["tumblrName"] = selectedBlogName
                    DBHelper.getInstance(appSupport).postDAO.update(appSupport, newValues)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        if (targetFragment is PostListener) {
            (targetFragment as PostListener).onEditDone(this, photoPost!!, completable)
        } else {
            completable.subscribe({ }) { }
        }
    }

    private fun parseTitle(swapDayMonth: Boolean) {
        val titleData = TitleParser.instance(AndroidTitleParserConfig(activity)).parseTitle(postTitleView.text.toString(), swapDayMonth)
        // only the edited title is updated, the sourceTitle remains unchanged
        htmlTitle = titleData.toHtml()
        postTitleView.setText(htmlTitle.fromHtml())

        fillTags(titleData.tags)
    }

    private inner class BlogItemSelectedListener : OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
            tagAdapter.blogName = blogList.selectedItem as String
            tagAdapter.notifyDataSetChanged()
        }

        override fun onNothingSelected(parent: AdapterView<*>) {}
    }

    private fun fillWithSourceTitle() {
        // treat the sourceTitle always as HTML
        postTitleView.setText(sourceTitle.fromHtml())
    }

    private fun decodeArguments() {
        val args = arguments
        val photoPost = args.getSerializable(ARG_PHOTO_POST) as TumblrPhotoPost?
        if (photoPost == null) {
            this.imageUrls = args.getParcelableArrayList(ARG_IMAGE_URLS)
            this.htmlTitle = args.getString(ARG_HTML_TITLE) ?: ""
            this.sourceTitle = args.getString(ARG_SOURCE_TITLE) ?: ""
            initialTagList = args.getStringArrayList(ARG_INITIAL_TAG_LIST)
        } else {
            this.photoPost = photoPost
            // pass the same HTML text for source title
            this.htmlTitle = photoPost.caption
            this.sourceTitle = photoPost.caption
            initialTagList = photoPost.tags
        }
    }

    override fun onItemsSelected(dialog: MRUDialog, positions: IntArray) {
        val tags = TumblrPost.tagsFromString(postTags)
        for (position in positions) {
            val selectedTag = dialog.getItem(position)
            if (!contains(tags, selectedTag)) {
                tags.add(selectedTag)
            }
            mruTags.add(selectedTag)
        }
        postTagsView.setText(TextUtils.join(", ", tags))
        dialog.dismiss()
    }

    override fun onItemDelete(dialog: MRUDialog, position: Int) {
        val selectedTag = dialog.getItem(position)
        mruTags.remove(selectedTag)
        mruTags.save()
    }

    interface PostListener {
        fun onEditDone(dialog: TumblrPostDialog, post: TumblrPhotoPost, completable: Completable)
    }

    companion object {

        const val ARG_PHOTO_POST = "photoPost"
        const val ARG_IMAGE_URLS = "imageUrls"
        const val ARG_HTML_TITLE = "htmlTitle"
        const val ARG_SOURCE_TITLE = "sourceTitle"
        const val ARG_INITIAL_TAG_LIST = "initialTagList"

        private const val NAME_ALREADY_EXISTS = 0
        private const val NAME_NOT_FOUND = 1
        private const val NAME_MISSPELLED = 2

        private const val MRU_TAGS_KEY = "mruTags"
        private const val MRU_TAGS_MAX_SIZE = 20
        private const val MRU_FRAGMENT_DIALOG_TAG = "mruFragmentDialogTag"

        fun newInstance(args: Bundle, target: Fragment?): TumblrPostDialog {
            val keys = args.keySet()
            require((keys.contains(ARG_PHOTO_POST) && !keys.contains(ARG_IMAGE_URLS))
            || (!keys.contains(ARG_PHOTO_POST) && keys.contains(ARG_IMAGE_URLS))) {
                "Only one type must be specified between $ARG_PHOTO_POST, $ARG_IMAGE_URLS"
            }

            val fragment = TumblrPostDialog()
            fragment.arguments = args
            fragment.setTargetFragment(target, 0)

            return fragment
        }

        fun newInstance(photoPost: TumblrPhotoPost, target: Fragment): TumblrPostDialog {
            val args = Bundle()
            args.putSerializable(ARG_PHOTO_POST, photoPost)

            val fragment = TumblrPostDialog()
            fragment.arguments = args
            fragment.setTargetFragment(target, 0)

            return fragment
        }
    }
}
