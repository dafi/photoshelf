package com.ternaryop.photoshelf.tagphotobrowser.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.MultiAutoCompleteTextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.viewModels
import com.ternaryop.photoshelf.activity.ImageViewerActivityStarter
import com.ternaryop.photoshelf.lifecycle.EventObserver
import com.ternaryop.photoshelf.lifecycle.Status
import com.ternaryop.photoshelf.tagnavigator.adapter.TagCursorAdapter
import com.ternaryop.photoshelf.tagphotobrowser.R
import com.ternaryop.photoshelf.tagphotobrowser.activity.TagPhotoBrowserActivity
import com.ternaryop.photoshelf.tumblr.dialog.TumblrPostDialog
import com.ternaryop.photoshelf.tumblr.dialog.editor.viewholder.TagsHolder
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.switcher.AdapterSwitcherConfig
import com.ternaryop.photoshelf.tumblr.ui.core.fragment.AbsPagingPostsListFragment
import com.ternaryop.photoshelf.util.post.PageFetcher
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder

private const val KEY_STATE_POST_TAG = "postTag"
private const val KEY_STATE_ALLOW_SEARCH = "allowSearch"

@AndroidEntryPoint
class TagPhotoBrowserFragment(
    iav: ImageViewerActivityStarter,
    pd: TumblrPostDialog
) : AbsPagingPostsListFragment(iav, pd) {
    private var postTag: String? = null
    private var allowSearch: Boolean = false
    private val viewModel: TagPhotoBrowserViewModel by viewModels()

    override val actionModeMenuId: Int
        get() = R.menu.tag_photo_browser_context

    override val actionBarGroupMenuId: Int
        get() = -1

    override val adapterSwitcherConfig: AdapterSwitcherConfig
        get() = AdapterSwitcherConfig("TagPhotoBrowser", false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_tag_browse_photo_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoAdapter.setEmptyView(view.findViewById(android.R.id.empty))

        photoShelfSwipe.setOnRefreshListener(null)

        viewModel.result.observe(
            viewLifecycleOwner,
            EventObserver { result ->
                when (result) {
                    is TagPhotoBrowserResult.FindTags -> onFindTagsModelResult(result)
                    is TagPhotoBrowserResult.Photos -> onPhotosModelResult(result)
                }
            }
        )

        savedInstanceState?.also {
            postTag = it.getString(KEY_STATE_POST_TAG)
            allowSearch = it.getBoolean(KEY_STATE_ALLOW_SEARCH)
        }
        blogName?.also { blogName ->
            postTag?.trim()?.let { tag ->
                if (tag.isNotEmpty()) {
                    fetchPosts(true)
                }
            }
            if (allowSearch) {
                setupSearch(view, blogName)
            }
        }
    }

    private fun setupSearch(view: View, blogName: String) {
        val textView = view.findViewById<MultiAutoCompleteTextView>(R.id.search_tags)
        view.findViewById<Group>(R.id.search_group).visibility = View.VISIBLE
        textView.setText(postTag)
        TagsHolder(requireContext(), textView, blogName)
        view.findViewById<AppCompatImageButton>(R.id.search_button).setOnClickListener {
            val tags = TagsHolder.cleanSeparators(textView.text)
            if (tags.isNotEmpty()) {
                onSearchTags(tags)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val bundle = (context as Activity?)?.intent?.extras ?: arguments
        val data = TagPhotoBrowserActivity.tagPhotoBrowserData(bundle)
        returnSelectedPost = TagPhotoBrowserActivity.returnSelectedPost(bundle, false)
        if (data == null) {
            allowSearch = true
        } else {
            postTag = data.tag
            allowSearch = data.allowSearch
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_STATE_POST_TAG, postTag)
        outState.putBoolean(KEY_STATE_ALLOW_SEARCH, allowSearch)
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.tag_photo_browser, menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_open_tag_web_browser -> {
                openTagInWebBrowser()
                return true
            }
            else -> super.onMenuItemSelected(item)
        }
    }

    override val pageFetcher: PageFetcher<PhotoShelfPost>
        get() = viewModel.pageFetcher

    override fun fetchPosts(fetchCache: Boolean) {
        refreshUI()

        val params = HashMap<String, String>()
        checkNotNull(postTag).split(",").forEachIndexed { index, tag ->
            params["tag[$index]"] = tag
        }
        params["notes_info"] = "true"
        params["offset"] = pageFetcher.pagingInfo.offset.toString()

        photoShelfSwipe.setRefreshingAndWaitingResult(true)
        viewModel.photos(requireBlogName, params, fetchCache)
    }

    private fun onSearchTags(tags: String) {
        postTag = tags
        pageFetcher.clear()
        photoAdapter.clear()
        photoAdapter.notifyDataSetChanged()
        fetchPosts(false)
    }

    override fun onTagClick(position: Int, clickedTag: String) {
        // do nothing if tags are equal otherwise a new TagBrowser on same tag is launched
        val tag = postTag ?: return
        if (!tag.equals(clickedTag, ignoreCase = true)) {
            super.onTagClick(position, clickedTag)
        }
    }

    private fun onFindTagsModelResult(result: TagPhotoBrowserResult.FindTags) {
        when (result.command.status) {
            Status.SUCCESS -> {
                result.command.data?.also { tags ->
                    (searchView?.suggestionsAdapter as? TagCursorAdapter)?.also { adapter ->
                        adapter.swapCursor(adapter.createCursor(tags.pattern, tags.tags))
                    }
                }
            }
            Status.ERROR -> {}
            Status.PROGRESS -> {}
        }
    }

    private fun onPhotosModelResult(result: TagPhotoBrowserResult.Photos) {
        when (result.command.status) {
            Status.SUCCESS -> {
                photoShelfSwipe.setRefreshingAndWaitingResult(false)
                result.command.data?.also { fetched ->
                    photoAdapter.setPosts(fetched.list)
                    refreshUI()
                }
            }
            Status.ERROR -> {
                photoShelfSwipe.setRefreshingAndWaitingResult(false)
                snackbarHolder.show(recyclerView, result.command.error)
            }
            Status.PROGRESS -> {}
        }
    }

    private fun openTagInWebBrowser() {
        val tag = postTag ?: return
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse("https://www.google.com/search?q=" + URLEncoder.encode(tag, "UTF-8"))
        startActivity(i)
    }
}
