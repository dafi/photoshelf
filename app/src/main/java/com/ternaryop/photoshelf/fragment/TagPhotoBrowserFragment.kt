package com.ternaryop.photoshelf.fragment

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
import androidx.lifecycle.ViewModelProviders
import com.ternaryop.photoshelf.EXTRA_ALLOW_SEARCH
import com.ternaryop.photoshelf.EXTRA_BROWSE_TAG
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.adapter.TagCursorAdapter
import com.ternaryop.photoshelf.lifecycle.EventObserver
import com.ternaryop.photoshelf.lifecycle.Status
import com.ternaryop.photoshelf.util.post.PageFetcher

class TagPhotoBrowserFragment : AbsPagingPostsListFragment(), SearchView.OnSuggestionListener {
    private var postTag: String? = null
    private var allowSearch: Boolean = false
    private lateinit var viewModel: TagPhotoBrowserViewModel

    override val actionModeMenuId: Int
        get() = R.menu.tag_browser_context

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tag_browse_photo_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoAdapter.setEmptyView(view.findViewById(android.R.id.empty))

        photoShelfSwipe.setOnRefreshListener(null)

        viewModel = ViewModelProviders.of(this).get(TagPhotoBrowserViewModel::class.java)

        viewModel.result.observe(viewLifecycleOwner, EventObserver { result ->
            when (result) {
                is TagPhotoBrowserResult.FindTags -> onFindTagsModelResult(result)
                is TagPhotoBrowserResult.Photos -> onPhotosModelResult(result)
            }
        })

        if (blogName != null) {
            postTag?.trim()?.let { tag -> if (tag.isNotEmpty()) fetchPosts(true) }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val bundle = (context as Activity?)?.intent?.extras ?: arguments
        if (bundle == null) {
            allowSearch = true
        } else {
            postTag = bundle.getString(EXTRA_BROWSE_TAG)
            allowSearch = bundle.getBoolean(EXTRA_ALLOW_SEARCH, true)
        }
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

    override fun setupSearchView(menu: Menu): SearchView {
        super.setupSearchView(menu)

        searchView!!.setOnSuggestionListener(this)
        val adapter = TagCursorAdapter(
            supportActionBar!!.themedContext,
            R.layout.ab_simple_dropdown_item_1line,
            blogName!!)
        searchView!!.suggestionsAdapter = adapter
        return searchView!!
    }

    override val pageFetcher: PageFetcher<PhotoShelfPost>
        get() = viewModel.pageFetcher

    override fun fetchPosts(fetchCache: Boolean) {

        refreshUI()


        val params = HashMap<String, String>()
        params["tag"] = postTag!!
        params["notes_info"] = "true"
        params["offset"] = pageFetcher.pagingInfo.offset.toString()

        photoShelfSwipe.setRefreshingAndWaitingResult(true)
        viewModel.photos(blogName!!, params, fetchCache)
    }

    override fun onQueryTextChange(newText: String): Boolean {
        val pattern = newText.trim()

        if (pattern.isEmpty()) {
            return true
        }

        viewModel.findTags(blogName!!, pattern)
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
        if (!postTag!!.equals(clickedTag, ignoreCase = true)) {
            super.onTagClick(position, clickedTag)
        }
    }

    private fun onFindTagsModelResult(result: TagPhotoBrowserResult.FindTags) {
        when (result.command.status) {
            Status.SUCCESS -> {
                result.command.data?.also { tags ->
                    val adapter = searchView!!.suggestionsAdapter as TagCursorAdapter
                    adapter.swapCursor(adapter.createCursor(tags.pattern, tags.tags))
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
                result.command.error?.also { showSnackbar(makeSnake(recyclerView, it)) }
            }
            Status.PROGRESS -> { }
        }
    }
}
