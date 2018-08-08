package com.ternaryop.photoshelf.fragment

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.support.v7.widget.SearchView
import android.text.format.DateUtils.SECOND_IN_MILLIS
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import com.ternaryop.photoshelf.EXTRA_ALLOW_SEARCH
import com.ternaryop.photoshelf.EXTRA_BROWSE_TAG
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.adapter.TagCursorAdapter
import com.ternaryop.photoshelf.util.network.ApiManager
import com.ternaryop.photoshelf.util.post.OnScrollPostFetcher
import com.ternaryop.photoshelf.view.PhotoShelfSwipe
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.getPhotoPosts
import io.reactivex.Observable
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class TagPhotoBrowserFragment : AbsPostsListFragment(), SearchView.OnSuggestionListener {
    private var postTag: String? = null
    private var allowSearch: Boolean = false
    private lateinit var photoShelfSwipe: PhotoShelfSwipe

    override val postListViewResource: Int
        get() = R.layout.fragment_tag_browse_photo_list

    override val actionModeMenuId: Int
        get() = R.menu.tag_browser_context

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = super.onCreateView(inflater, container, savedInstanceState) ?: return null

        photoAdapter.setOnPhotoBrowseClick(this)
        photoAdapter.setEmptyView(rootView.findViewById(android.R.id.empty))

        photoShelfSwipe = rootView.findViewById(R.id.swipe_container)

        if (blogName != null) {
            postTag?.trim()?.let { tag -> if (tag.isNotEmpty()) onQueryTextSubmit(tag) }
        }

        return rootView
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        val bundle = (context as Activity?)?.intent?.extras
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

    override fun fetchPosts(listener: OnScrollPostFetcher) {
        refreshUI()

        val params = HashMap<String, String>()
        params["tag"] = postTag!!
        params["notes_info"] = "true"
        params["offset"] = postFetcher.offset.toString()

        Observable
            .just(params)
            .doFinally { postFetcher.isScrolling = false }
            .flatMap<TumblrPhotoPost> { params1 ->
                Observable.fromIterable<TumblrPhotoPost>(TumblrManager.getInstance(context!!)
                    .getPhotoPosts(blogName!!, params1))
            }
            .map { tumblrPost -> PhotoShelfPost(tumblrPost, tumblrPost.timestamp * SECOND_IN_MILLIS) }
            .toList()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .compose(photoShelfSwipe.applySwipe())
            .subscribe(object : SingleObserver<List<PhotoShelfPost>> {
                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onSuccess(photoList: List<PhotoShelfPost>) {
                    postFetcher.incrementReadPostCount(photoList.size)
                    photoAdapter.addAll(photoList)
                    refreshUI()
                }

                override fun onError(t: Throwable) {
                    showSnackbar(makeSnake(recyclerView, t))
                }
            })
    }

    override fun onQueryTextChange(newText: String): Boolean {
        val pattern = newText.trim()

        if (pattern.isEmpty()) {
            return true
        }
        ApiManager.postService(context!!)
            .findTags(blogName!!, pattern)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { compositeDisposable.add(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response ->
                val adapter = searchView!!.suggestionsAdapter as TagCursorAdapter
                adapter.swapCursor(adapter.createCursor(pattern, response.response.tags))
        }
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        postTag = query
        postFetcher.reset()
        photoAdapter.clear()
        photoAdapter.notifyDataSetChanged()
        fetchPosts(postFetcher)
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
}
