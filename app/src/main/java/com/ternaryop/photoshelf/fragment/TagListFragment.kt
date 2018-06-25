package com.ternaryop.photoshelf.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.SearchView
import android.widget.SearchView.OnQueryTextListener
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.activity.TagPhotoBrowserActivity
import com.ternaryop.photoshelf.adapter.TagAdapter
import com.ternaryop.photoshelf.api.post.TagInfo

class TagListFragment : AbsPhotoShelfFragment(), OnItemClickListener {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_list_tags, container, false)

        val adapter = TagAdapter(
                context!!,
                android.R.layout.simple_list_item_1,
                blogName!!)

        val listView = rootView.findViewById<View>(R.id.list) as ListView
        listView.adapter = adapter
        listView.onItemClickListener = this
        listView.isTextFilterEnabled = true
        // start with list filled
        adapter.filter.filter("")
        (rootView.findViewById<View>(R.id.searchView1) as SearchView)
            .setOnQueryTextListener(object : OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    adapter.filter.filter(newText)
                    return true
                }
            })
        return rootView
    }

    override fun onItemClick(parent: AdapterView<*>, v: View, position: Int, id: Long) {
        val tag = (parent.getItemAtPosition(position) as TagInfo).tag
        TagPhotoBrowserActivity.startPhotoBrowserActivity(context!!, blogName!!, tag, false)
    }
}
