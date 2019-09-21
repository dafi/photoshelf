package com.ternaryop.photoshelf.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.MultiAutoCompleteTextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.mru.MRUHolder
import com.ternaryop.photoshelf.adapter.mru.OnMRUListener
import com.ternaryop.photoshelf.adapter.tagnavigator.TagNavigatorArrayAdapter
import com.ternaryop.photoshelf.adapter.tagnavigator.TagNavigatorFilter
import com.ternaryop.photoshelf.dialogs.MisspelledName.Companion.NAME_ALREADY_EXISTS
import com.ternaryop.photoshelf.dialogs.MisspelledName.Companion.NAME_MISSPELLED
import com.ternaryop.photoshelf.dialogs.MisspelledName.Companion.NAME_NOT_FOUND
import com.ternaryop.photoshelf.lifecycle.Status
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.utils.text.fromHtml
import com.ternaryop.utils.text.toHtml
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.io.Serializable
import kotlin.coroutines.CoroutineContext

fun EditText.moveCaretToEnd() = setSelection(length())

abstract class TumblrPostDialog : DialogFragment(), Toolbar.OnMenuItemClickListener, CoroutineScope {
    protected lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    protected lateinit var data: PostDialogData

    protected lateinit var tagsHolder: TagsHolder
    protected lateinit var titleHolder: TitleHolder
    private lateinit var mruHolder: MRUHolder

    private lateinit var viewModel: PostViewModel

    protected lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        data = checkNotNull(arguments?.getSerializable(ARG_DATA) as? PostDialogData)

        job = Job()
        viewModel = ViewModelProviders.of(this).get(PostViewModel::class.java)

        viewModel.result.observe(requireActivity(), Observer { result ->
            when (result) {
                is TumblrPostModelResult.TitleParsed ->  onTitleParsed(result)
                is TumblrPostModelResult.MisspelledInfo -> onMisspelledInfo(result)
            }
        })
    }

    private fun onTitleParsed(result: TumblrPostModelResult.TitleParsed) {
        when (result.command.status) {
            Status.SUCCESS -> result.command.data?.also { data ->
                titleHolder.htmlTitle = data.html
                fillTags(data.tags)
            }
            Status.ERROR -> result.command.error?.also { error ->
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.parsing_error)
                    .setMessage(error.localizedMessage)
                    .show()
            }
            Status.PROGRESS -> { }
        }
    }

    private fun onMisspelledInfo(result: TumblrPostModelResult.MisspelledInfo) {
        // always enable button after success or error
        val dialog = dialog as AlertDialog?
        // protect against NPE because inside onDestroy the dialog is already null
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true

        when (result.command.status) {
            Status.SUCCESS -> result.command.data?.also { data ->
                tagsHolder.highlightTagName(data.first, data.second)
            }
            Status.ERROR -> { }
            Status.PROGRESS -> { }
        }
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    @SuppressLint("InflateParams") // for dialogs passing null for root is valid, ignore the warning
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_publish_post, null)

        setupUI(view)

        return onCreatePostDialog(view)
    }

    protected abstract fun onCreatePostDialog(view: View): Dialog

    override fun onResume() {
        super.onResume()
        // Dimensions defined on xml layout are not used so we set them here (it works only if called inside onResume)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
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

    protected fun searchMisspelledName(name: String) {
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        viewModel.searchMisspelledName(name)
    }

    protected open fun setupUI(view: View) {
        setupToolbar(view)

        tagsHolder = TagsHolder(
            requireContext(),
            view.findViewById(R.id.post_tags),
            data.blogName)
        titleHolder = TitleHolder(view.findViewById(R.id.post_title), data.sourceTitle, data.htmlSourceTitle)
        mruHolder = MRUHolder(requireContext(), view.findViewById(R.id.mru_list), tagsHolder)
        fillTags(data.tags)
    }

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.parse_title -> {
                viewModel.parse(titleHolder.plainTitle, false)
                true
            }
            R.id.parse_title_swap -> {
                viewModel.parse(titleHolder.plainTitle, true)
                true
            }
            R.id.source_title -> {
                titleHolder.restoreSourceTitle()
                true
            }
            else -> false
        }
    }

    protected fun updateMruList() {
        mruHolder.updateMruList(TumblrPost.tagsFromString(tagsHolder.tags).drop(1))
    }

    protected fun setupToolbar(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        toolbar.inflateMenu(R.menu.publish_post_overflow)
        toolbar.setOnMenuItemClickListener(this)
    }

    companion object {
        const val ARG_DATA = "data"
    }
}

data class PostDialogData(
    val blogName: String,
    val sourceTitle: String,
    val htmlSourceTitle: String,
    val tags: List<String>) : Serializable

class TagsHolder(
    context: Context,
    private val textView: MultiAutoCompleteTextView,
    blogName: String) : OnMRUListener {

    private var defaultColor = textView.textColors
    private var defaultBackground = textView.background

    private val tagAdapter = TagNavigatorArrayAdapter(
        context,
        R.layout.tag_navigator_row,
        blogName)

    init {
        textView.setAdapter(tagAdapter)
        textView.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
    }

    fun updateBlogName(blogName: String) {
        (tagAdapter.filter as TagNavigatorFilter).blogName = blogName
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
            }
            NAME_MISSPELLED -> {
                textView.setTextColor(Color.RED)
                textView.setBackgroundColor(Color.YELLOW)
                textView.setText(correctedName)
            }
            NAME_NOT_FOUND -> {
                textView.setTextColor(Color.WHITE)
                textView.setBackgroundColor(Color.RED)
            }
        }
    }

    override fun onItemSelect(item: String) {
        textView.setText(toggleTag(item).joinToString(", "))
        textView.moveCaretToEnd()
        textView.requestFocus()
    }

    private fun toggleTag(item: String): MutableList<String> {
        val tags = TumblrPost.tagsFromString(tags)
        val index = tags.indexOfFirst { it.equals(item, true) }

        if (index < 0) {
            tags.add(item)
        } else {
            tags.removeAt(index)
        }
        return tags
    }

    override fun onItemDelete(item: String) {
    }
}

class TitleHolder(private val editText: EditText,
    private var sourceTitle: String,
    htmlSourceTitle: String) {

    var htmlTitle: String
        get() {
            editText.clearComposingText()
            return editText.text.toHtml()
        }
        set(value) {
            editText.setText(value.fromHtml())
            editText.moveCaretToEnd()
        }

    val plainTitle: String
        get() {
            return editText.text.toString()
        }

    init {
        htmlTitle = htmlSourceTitle
    }

    fun restoreSourceTitle() {
        htmlTitle = sourceTitle
    }
}
