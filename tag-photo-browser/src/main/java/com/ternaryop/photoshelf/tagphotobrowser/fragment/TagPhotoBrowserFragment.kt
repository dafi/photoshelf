package com.ternaryop.photoshelf.tagphotobrowser.fragment

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import com.ternaryop.photoshelf.activity.ImageViewerActivityStarter
import com.ternaryop.photoshelf.lifecycle.EventObserver
import com.ternaryop.photoshelf.lifecycle.Status
import com.ternaryop.photoshelf.tagnavigator.adapter.TagCursorAdapter
import com.ternaryop.photoshelf.tagphotobrowser.R
import com.ternaryop.photoshelf.tagphotobrowser.activity.TagPhotoBrowserActivity
import com.ternaryop.photoshelf.tumblr.dialog.TumblrPostDialog
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.switcher.AdapterSwitcherConfig
import com.ternaryop.photoshelf.tumblr.ui.core.fragment.AbsPagingPostsListFragment
import com.ternaryop.photoshelf.util.post.PageFetcher
import dagger.hilt.android.AndroidEntryPoint

private const val KEY_STATE_POST_TAG = "postTag"
private const val KEY_STATE_ALLOW_SEARCH = "allowSearch"

@AndroidEntryPoint
class TagPhotoBrowserFragment(
    iav: ImageViewerActivityStarter,
    pd: TumblrPostDialog
) : AbsPagingPostsListFragment(iav, pd), SearchView.OnSuggestionListener {
    private var postTag: String? = null
    private var allowSearch: Boolean = false
    private val viewModel: TagPhotoBrowserViewModel by viewModels()

    override val actionModeMenuId: Int
        get() = R.menu.tag_browser_context

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

        viewModel.result.observe(viewLifecycleOwner, EventObserver { result ->
            when (result) {
                is TagPhotoBrowserResult.FindTags -> onFindTagsModelResult(result)
                is TagPhotoBrowserResult.Photos -> onPhotosModelResult(result)
            }
        })

        savedInstanceState?.also {
            postTag = it.getString(KEY_STATE_POST_TAG)
            allowSearch = it.getBoolean(KEY_STATE_ALLOW_SEARCH)
        }
        if (blogName != null) {
            postTag?.trim()?.let { tag -> if (tag.isNotEmpty()) fetchPosts(true) }
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.tag_browser, menu)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val isMenuVisible = allowSearch && !fragmentActivityStatus.isDrawerMenuOpen
        menu.findItem(R.id.action_search).isVisible = isMenuVisible
        super.onPrepareOptionsMenu(menu)
    }

    override fun setupSearchView(menu: Menu): SearchView? {
        super.setupSearchView(menu)

        searchView?.setOnSuggestionListener(this)
        val adapter = TagCursorAdapter(
            checkNotNull(supportActionBar).themedContext,
            R.layout.ab_simple_dropdown_item_1line,
            requireBlogName)
        searchView?.suggestionsAdapter = adapter
        return searchView
    }

    override val pageFetcher: PageFetcher<PhotoShelfPost>
        get() = viewModel.pageFetcher

    override fun fetchPosts(fetchCache: Boolean) {
        refreshUI()

        val params = HashMap<String, String>()
        params["tag"] = checkNotNull(postTag)
        params["notes_info"] = "true"
        params["offset"] = pageFetcher.pagingInfo.offset.toString()

        photoShelfSwipe.setRefreshingAndWaitingResult(true)
        viewModel.photos(requireBlogName, params, fetchCache)
    }

    override fun onQueryTextChange(newText: String): Boolean {
        val pattern = newText.trim()

        if (pattern.isEmpty()) {
            return true
        }

        viewModel.findTags(requireBlogName, pattern)
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        postTag = query
        pageFetcher.clear()
        photoAdapter.clear()
        photoAdapter.notifyDataSetChanged()
        fetchPosts(false)
        return false
    }

    override fun onSuggestionClick(position: Int): Boolean {
        searchView?.apply { setQuery((suggestionsAdapter.getItem(position) as Cursor).getString(1), true) }
        return true
    }

    override fun onSuggestionSelect(position: Int): Boolean {
        return true
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
            Status.ERROR -> { }
            Status.PROGRESS -> { }
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
            Status.PROGRESS -> { }
        }
    }
}
