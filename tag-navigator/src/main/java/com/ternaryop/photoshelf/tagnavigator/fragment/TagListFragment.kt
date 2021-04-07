package com.ternaryop.photoshelf.tagnavigator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.activity.ImageViewerActivityStarter
import com.ternaryop.photoshelf.activity.TagPhotoBrowserData
import com.ternaryop.photoshelf.api.post.TagInfo
import com.ternaryop.photoshelf.tagnavigator.R
import com.ternaryop.photoshelf.tagnavigator.adapter.TagNavigatorAdapter
import com.ternaryop.photoshelf.tagnavigator.adapter.TagNavigatorListener

class TagListFragment(
    private val imageViewerActivityStarter: ImageViewerActivityStarter
) : Fragment(), TagNavigatorListener {
    private var blogName = ""
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_tag_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        blogName = checkNotNull(arguments?.getString(ARG_BLOG_NAME)) { "Invalid blog name" }

        val adapter = TagNavigatorAdapter(
            requireContext(),
            emptyList(),
            blogName,
            this)

        view.findViewById<RecyclerView>(R.id.tag_list).let {
            it.adapter = adapter
            it.setHasFixedSize(true)
            it.layoutManager = LinearLayoutManager(activity)
        }

        // start with list filled
        adapter.filter.filter("")
        view.findViewById<SearchView>(R.id.searchView1)
            .setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    adapter.filter.filter(newText)
                    return true
                }
            })
    }

    override fun onClick(item: TagInfo) {
        requireContext().startActivity(
            imageViewerActivityStarter.tagPhotoBrowserIntent(requireContext(),
                TagPhotoBrowserData(blogName, item.tag, false)))
    }

    companion object {
        const val ARG_BLOG_NAME = "blogName"
    }
}
