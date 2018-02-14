package com.ternaryop.photoshelf.fragment

import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.GridView
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.GridViewPhotoAdapter

class BestOfFragment : AbsPhotoShelfFragment(), AbsListView.MultiChoiceModeListener {

    private lateinit var gridView: GridView
    private var gridViewPhotoAdapter: GridViewPhotoAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_best_of, container, false)

        gridViewPhotoAdapter = GridViewPhotoAdapter(activity, LOADER_PREFIX)

//        gridView = rootView.findViewById<View>(R.id.gridview) as GridView
        //        gridView.setAdapter(gridViewPhotoAdapter);
        //        gridView.setOnItemClickListener(this);
//        gridView.setMultiChoiceModeListener(this)

        rootView.findViewById<View>(R.id.create_post).setOnClickListener { refresh() }

        return rootView
    }

    private fun refresh() {
//        object : AbsProgressIndicatorAsyncTask<Void, Void, List<TumblrPhotoPost>>(activity, getString(R.string.shaking_images_title)) {
//
//            override fun doInBackground(vararg voidParams: Void): List<TumblrPhotoPost>? {
//                BirthdayUtils.publishedInAgeRange(activity, 0, 30, 7, "BestOfWeek", blogName)
//                return null
//            }
//
//            override fun onPostExecute(posts: List<TumblrPhotoPost>?) {
//                super.onPostExecute(null)
//                if (!hasError()) {
//                    gridViewPhotoAdapter!!.clear()
//                    //                    gridViewPhotoAdapter.addAll(posts);
//                    gridViewPhotoAdapter!!.notifyDataSetChanged()
//                }
//            }
//        }.execute()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.birtdays_publisher, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                refresh()
                true
            }
            R.id.action_selectall -> {
                selectAll(true)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun selectAll(select: Boolean) {
//        if (select) {
//            for (i in 0 until gridView.count) {
//                gridView.setItemChecked(i, select)
//            }
//        } else {
//            gridView.clearChoices()
//        }
    }

    private fun publish(mode: ActionMode, saveAsDraft: Boolean) {
//        object : AbsProgressIndicatorAsyncTask<Void, String, List<TumblrPhotoPost>>(activity, "") {
//            override fun onProgressUpdate(vararg values: String) {
//                setProgressMessage(values[0])
//            }
//
//            override fun doInBackground(vararg voidParams: Void): List<TumblrPhotoPost>? {
//                return null
//            }
//
//            override fun onPostExecute(result: List<TumblrPhotoPost>?) {
//                super.onPostExecute(null)
//                if (!hasError()) {
//                    mode.finish()
//                }
//            }
//        }.execute()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.setTitle(R.string.select_images)
        mode.subtitle = resources.getQuantityString(R.plurals.selected_items, 1, 1)
        val inflater = mode.menuInflater
        inflater.inflate(R.menu.birtdays_publisher_context, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_publish -> {
                publish(mode, false)
                true
            }
            R.id.action_draft -> {
                publish(mode, true)
                true
            }
            else -> false
        }
    }

    //    private List<TumblrPhotoPost> getCheckedPosts() {
    //        SparseBooleanArray checkedItemPositions = gridView.getCheckedItemPositions();
    //        ArrayList<TumblrPhotoPost> list = new ArrayList<TumblrPhotoPost>();
    //        for (int i = 0; i < checkedItemPositions.size(); i++) {
    //            int key = checkedItemPositions.keyAt(i);
    //            if (checkedItemPositions.get(key)) {
    //                list.add(gridViewPhotoAdapter.getItem(key).second);
    //            }
    //        }
    //        return list;
    //    }

    override fun onDestroyActionMode(mode: ActionMode) {}

    override fun onItemCheckedStateChanged(mode: ActionMode, position: Int,
                                           id: Long, checked: Boolean) {
        val selectCount = gridView.checkedItemCount
        mode.subtitle = resources.getQuantityString(R.plurals.selected_items, 1, selectCount)
    }

    companion object {
        private const val PICK_IMAGE_REQUEST_CODE = 100
        private const val LOADER_PREFIX = "mediumThumb"
    }
}
