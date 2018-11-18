package com.ternaryop.photoshelf.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.MultiAutoCompleteTextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.TagAdapter
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.parser.TitleComponentsResult
import com.ternaryop.photoshelf.dialogs.MisspelledName.Companion.NAME_ALREADY_EXISTS
import com.ternaryop.photoshelf.dialogs.MisspelledName.Companion.NAME_MISSPELLED
import com.ternaryop.photoshelf.dialogs.MisspelledName.Companion.NAME_NOT_FOUND
import com.ternaryop.photoshelf.dialogs.mru.MRUDialog
import com.ternaryop.photoshelf.dialogs.mru.OnMRUListener
import com.ternaryop.photoshelf.service.PublishIntentService
import com.ternaryop.photoshelf.util.mru.MRU
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.utils.text.anyMatches
import com.ternaryop.utils.text.fromHtml
import com.ternaryop.utils.text.toHtml
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.Serializable

class TumblrPostDialog : DialogFragment(), Toolbar.OnMenuItemClickListener {

    private lateinit var blogList: BlogList
    private lateinit var appSupport: AppSupport

    private lateinit var compositeDisposable: CompositeDisposable

    lateinit var data: PostDialogData

    lateinit var tagsHolder: TagsHolder
    lateinit var titleHolder: TitleHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appSupport = AppSupport(context!!)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_PhotoShelf_Dialog)

        compositeDisposable = CompositeDisposable()
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    @SuppressLint("InflateParams") // for dialogs passing null for root is valid, ignore the warning
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        data = (arguments?.getSerializable(ARG_DATA) as PostDialogData?)
            ?: throw IllegalArgumentException("$ARG_DATA is mandatory")

        val view = activity!!.layoutInflater.inflate(R.layout.dialog_publish_post, null)

        tagsHolder = TagsHolder(this, view, appSupport.selectedBlogName!!)
        titleHolder = TitleHolder(this, view, data.sourceTitle, data.htmlSourceTitle)
        fillTags(data.tags)

        setupUI(view)

        val builder = AlertDialog.Builder(context!!)
            .setView(view)
            .setNegativeButton(R.string.cancel_title) { _, _ -> compositeDisposable.clear() }
        if (data.photoPost == null) {
            val onClickPublishListener = OnClickPublishListener()
            builder.setNeutralButton(R.string.publish_post, onClickPublishListener)
            builder.setPositiveButton(R.string.draft_title, onClickPublishListener)
            view.findViewById<View>(R.id.refreshBlogList)
                .setOnClickListener { blogList.fetchBlogNames(dialog as AlertDialog, compositeDisposable) }
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

        blogList = BlogList(appSupport, view.findViewById(R.id.blog), object : BlogList.OnBlogItemSelectedListener() {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                tagsHolder.updateBlogName(blogList.selectedBlogName)
            }
        })

        if (data.photoPost != null) {
            toolbar.setTitle(R.string.edit_post_title)
        } else {
            val size = data.imageUrls!!.size
            toolbar.title = context!!.resources.getQuantityString(
                R.plurals.post_image,
                size,
                size)
        }
    }

    private fun fillTags(tags: List<String>) {
        val firstTag = if (tags.isEmpty()) "" else tags[0]
        tagsHolder.tags = tags.joinToString(", ")

        if (firstTag.isEmpty()) {
            return
        }
        val handler = Handler()
        Thread(Runnable { handler.post { searchMisspelledName(firstTag) } }).start()
    }

    private fun searchMisspelledName(name: String) {
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

        MisspelledName(context!!).getMisspelledInfo(name)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                val dialog = dialog as AlertDialog?
                // protect against NPE because inside onDestroy the dialog is already null
                dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
            }
            .subscribe(object : SingleObserver<Pair<Int, String>> {
                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onSuccess(misspelledInfo: Pair<Int, String>) {
                    tagsHolder.highlightTagName(misspelledInfo.first, misspelledInfo.second)
                }

                override fun onError(e: Throwable) {}
            })
    }

    override fun onStart() {
        super.onStart()
        if (data.photoPost == null) {
            val dialog = dialog as AlertDialog
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = false
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

            val blogSetNames = appSupport.blogList
            if (blogSetNames == null) {
                blogList.fetchBlogNames(dialog, compositeDisposable)
            } else {
                blogList.fillList(blogSetNames)
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = true
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
            }
        }
    }

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.parse_title -> {
                fillTagFromTitleHolder(false)
                true
            }
            R.id.parse_title_swap -> {
                fillTagFromTitleHolder(true)
                true
            }
            R.id.source_title -> {
                titleHolder.restoreSourceTitle()
                true
            }
            else -> false
        }
    }

    private fun fillTagFromTitleHolder(swapDayMonth: Boolean) {
        val d = titleHolder
            .parseAgain(swapDayMonth)
            .subscribe({ fillTags(it.tags) }) {
                android.app.AlertDialog.Builder(context!!)
                    .setTitle(R.string.parsing_error)
                    .setMessage(it.localizedMessage)
                    .show()
            }
        compositeDisposable.add(d)
    }

    private inner class OnClickPublishListener : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            appSupport.selectedBlogName = blogList.selectedBlogName

            tagsHolder.updateMruList()
            createPosts(which == DialogInterface.BUTTON_NEUTRAL,
                blogList.selectedBlogName, data.imageUrls!!.map { Uri.parse(it) }, titleHolder.htmlTitle, tagsHolder.tags)
        }

        private fun createPosts(publish: Boolean,
            selectedBlogName: String, urls: List<Uri>, postTitle: String, postTags: String) {
            for (url in urls) {
                PublishIntentService.startActionIntent(context!!,
                    url,
                    selectedBlogName,
                    postTitle,
                    postTags,
                    publish)
            }
        }
    }

    private fun editPost() {
        tagsHolder.updateMruList()
        (targetFragment as? PostListener)?.onEdit(this, data.photoPost!!, appSupport.selectedBlogName!!)
    }

    interface PostListener {
        fun onEdit(dialog: TumblrPostDialog, post: TumblrPhotoPost, selectedBlogName: String)
    }

    companion object {

        const val ARG_DATA = "data"

        fun newInstance(dialogData: PostDialogData, target: Fragment? = null): TumblrPostDialog {
            val args = Bundle()
            args.putSerializable(ARG_DATA, dialogData)

            val fragment = TumblrPostDialog()
            fragment.arguments = args
            fragment.setTargetFragment(target, 0)

            return fragment
        }
    }
}

class PostDialogData : Serializable {
    val sourceTitle: String
    val htmlSourceTitle: String
    val tags: List<String>
    val photoPost: TumblrPhotoPost?
    val imageUrls: List<String>?

    constructor(photoPost: TumblrPhotoPost) {
        // use the HTML text for source title, too
        this.sourceTitle = photoPost.caption
        this.htmlSourceTitle = photoPost.caption
        this.tags = photoPost.tags
        this.photoPost = photoPost
        this.imageUrls = null
    }

    constructor(sourceTitle: String, htmlSourceTitle: String, tags: List<String>, imageUrls: List<Uri>) {
        this.sourceTitle = sourceTitle
        this.htmlSourceTitle = htmlSourceTitle
        this.tags = tags
        // URIs can't be serialized so we convert them to string
        this.imageUrls = imageUrls.map { it.toString() }
        this.photoPost = null
    }
}

class TagsHolder(private val fragment: Fragment, rootView: View, blogName: String) : OnMRUListener {
    private val textView: MultiAutoCompleteTextView = rootView.findViewById(R.id.post_tags)
    private val mruButton: ImageButton = rootView.findViewById(R.id.mruTags)

    private var defaultColor = textView.textColors
    private var defaultBackground = textView.background

    private val mru = MRU(fragment.context!!, MRU_TAGS_KEY, MRU_TAGS_MAX_SIZE)

    // the ContextThemeWrapper is necessary otherwise the autocomplete drop down items
    // and the toolbar overflow menu items are styled incorrectly
    // since the switch to the AlertDialog the toolbar isn't styled from code
    // so to fix it the theme is declared directly into xml
    private val tagAdapter = TagAdapter(
        ContextThemeWrapper(fragment.activity!!, R.style.Theme_PhotoShelf_Dialog),
        android.R.layout.simple_dropdown_item_1line,
        blogName)

    init {
        textView.setAdapter<TagAdapter>(tagAdapter)
        textView.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())

        mruButton.setOnClickListener { openMRUDialog() }
        mruButton.isEnabled = mru.list.isNotEmpty()
    }

    fun updateBlogName(blogName: String) {
        tagAdapter.blogName = blogName
        tagAdapter.notifyDataSetChanged()
    }

    var tags: String
        get() {
            // remove the empty string at the end, if present
            return textView.text.toString().replace(",\\s*$".toRegex(), "")
        }
        set(value) = textView.setText(value)

    fun highlightTagName(nameType: Int, correctedName: String) {
        when (nameType) {
            NAME_ALREADY_EXISTS -> {
                textView.setTextColor(defaultColor)
                textView.background = defaultBackground
                val enabledState = intArrayOf(android.R.attr.state_enabled)
                DrawableCompat.setTint(mruButton.drawable,
                    defaultColor!!.getColorForState(enabledState, Color.GREEN))
                mruButton.setBackgroundColor(Color.TRANSPARENT)
            }
            NAME_MISSPELLED -> {
                textView.setTextColor(Color.RED)
                textView.setBackgroundColor(Color.YELLOW)
                textView.setText(correctedName)
                DrawableCompat.setTint(mruButton.drawable, Color.RED)
                mruButton.setBackgroundColor(Color.YELLOW)
            }
            NAME_NOT_FOUND -> {
                textView.setTextColor(Color.WHITE)
                textView.setBackgroundColor(Color.RED)
                DrawableCompat.setTint(mruButton.drawable, Color.WHITE)
                mruButton.setBackgroundColor(Color.RED)
            }
        }
    }

    fun updateMruList() {
        val tags = TumblrPost.tagsFromString(tags)
        tags.removeAt(0)
        mru.add(tags)
        mru.save()
    }

    override fun onItemsSelected(dialog: MRUDialog, positions: IntArray) {
        val tags = TumblrPost.tagsFromString(tags)
        for (position in positions) {
            val selectedTag = dialog.getItem(position)
            if (!tags.anyMatches(selectedTag)) {
                tags.add(selectedTag)
            }
            mru.add(selectedTag)
        }
        textView.setText(tags.joinToString(", "))
        dialog.dismiss()
    }

    override fun onItemDelete(dialog: MRUDialog, position: Int) {
        val selectedTag = dialog.getItem(position)
        mru.remove(selectedTag)
        mru.save()
    }

    private fun openMRUDialog() {
        MRUDialog.newInstance(mru.list)
            .setOnMRUListener(this)
            .show(fragment.activity!!.supportFragmentManager, MRU_FRAGMENT_DIALOG_TAG)
    }

    companion object {
        private const val MRU_TAGS_KEY = "mruTags"
        private const val MRU_TAGS_MAX_SIZE = 20
        private const val MRU_FRAGMENT_DIALOG_TAG = "mruFragmentDialogTag"
    }
}

class TitleHolder(private val fragment: Fragment,
    rootView: View,
    private var sourceTitle: String,
    htmlSourceTitle: String) {
    private val editText: EditText = rootView.findViewById(R.id.post_title)

    var htmlTitle: String
        get() {
            editText.clearComposingText()
            return editText.text.toHtml()
        }
        set(value) {
            editText.setText(value.fromHtml())
            // move caret to end
            editText.setSelection(editText.length())
        }

    init {
        htmlTitle = htmlSourceTitle
    }

    fun restoreSourceTitle() {
        htmlTitle = sourceTitle
    }

    fun parseAgain(swapDayMonth: Boolean): Single<TitleComponentsResult> {
        return ApiManager.parserService(fragment.context!!).components(editText.text.toString(), swapDayMonth)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                htmlTitle = it.response.html
                it.response
            }
    }
}
