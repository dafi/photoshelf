package com.ternaryop.photoshelf.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.SearchView
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.activity.TagPhotoBrowserActivity
import com.ternaryop.photoshelf.adapter.TagAdapter
import com.ternaryop.photoshelf.api.post.TagInfo
import kotlinx.android.synthetic.main.fragment_list_tags.list
import kotlinx.android.synthetic.main.fragment_list_tags.searchView1

class TagListFragment : AbsPhotoShelfFragment(), OnItemClickListener {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_list_tags, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = TagAdapter(
            context!!,
            android.R.layout.simple_list_item_1,
            blogName!!)

        list.adapter = adapter
        list.onItemClickListener = this
        list.isTextFilterEnabled = true
        // start with list filled
        adapter.filter.filter("")
        searchView1
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

    override fun onItemClick(parent: AdapterView<*>, v: View, position: Int, id: Long) {
        val tag = (parent.getItemAtPosition(position) as TagInfo).tag
        TagPhotoBrowserActivity.startPhotoBrowserActivity(context!!, blogName!!, tag, false)
    }
}
