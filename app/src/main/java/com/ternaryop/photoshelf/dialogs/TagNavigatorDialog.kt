package com.ternaryop.photoshelf.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.PhotoShelfPost

/**
 * Created by dave on 17/05/15.
 * Allow to select tag
 */
private const val SORT_TAG_NAME = 0
private const val SORT_TAG_COUNT = 1

private const val SELECTED_TAG = "selectedTag"
private const val ARG_TAG_LIST = "list"
private const val PREF_NAME_TAG_SORT = "tagNavigatorSort"

class TagNavigatorDialog : DialogFragment() {
    private lateinit var adapter: ArrayAdapter<TagCounter>
    private lateinit var sortButton: Button

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
                .setView(setupUI())
                .setTitle(resources.getString(R.string.tag_navigator_title, adapter.count))
                .setNegativeButton(resources.getString(R.string.close), null)
                .create()
    }

    @SuppressLint("InflateParams")
    private fun setupUI(): View {
        val view = activity!!.layoutInflater.inflate(R.layout.dialog_tag_navigator, null)
        adapter = createAdapter(arguments!!.getStringArrayList(ARG_TAG_LIST)!!)
        sortButton = view.findViewById<View>(R.id.sort_tag) as Button
        val tagList = view.findViewById<View>(R.id.tag_list) as ListView

        tagList.adapter = adapter
        tagList.onItemClickListener = AdapterView.OnItemClickListener(function = { _, _, position, _ ->
            val item = adapter.getItem(position)
            if (item != null) {
                val intent = Intent()
                intent.putExtra(SELECTED_TAG, item.tag)
                targetFragment!!.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
            }
            dismiss()
        })

        val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
        changeSortType(preferences.getInt(PREF_NAME_TAG_SORT, SORT_TAG_NAME))
        val sortClick = View.OnClickListener { v ->
            when (v.id) {
                R.id.sort_tag -> {
                    var sortType = preferences.getInt(PREF_NAME_TAG_SORT, SORT_TAG_NAME)
                    sortType = if (sortType == SORT_TAG_NAME) SORT_TAG_COUNT else SORT_TAG_NAME
                    preferences.edit().putInt(PREF_NAME_TAG_SORT, sortType).apply()
                    changeSortType(sortType)
                }
            }
        }
        sortButton.setOnClickListener(sortClick)
        return view
    }

    private fun changeSortType(sortType: Int) {
        when (sortType) {
            SORT_TAG_NAME -> {
                sortButton.setText(R.string.sort_by_count)
                sortByTagName()
            }
            SORT_TAG_COUNT -> {
                sortButton.setText(R.string.sort_by_name)
                sortByTagCount()
            }
        }
    }

    private fun createAdapter(tagList: List<String>): ArrayAdapter<TagCounter> {
        val map = HashMap<String, TagCounter>(tagList.size)
        for (s in tagList) {
            val lower = s.toLowerCase()
            var tagCounter: TagCounter? = map[lower]
            if (tagCounter == null) {
                tagCounter = TagCounter(s)
                map[lower] = tagCounter
            } else {
                ++tagCounter.count
            }
        }

        return ArrayAdapter(activity!!, android.R.layout.simple_list_item_1, map.values.toList())
    }

    private fun sortByTagCount() {
        adapter.sort { lhs, rhs ->
            // sort descending
            val sign = rhs.count - lhs.count
            if (sign == 0) lhs.compareTagTo(rhs) else sign
        }
        adapter.notifyDataSetChanged()
    }

    private fun sortByTagName() {
        adapter.sort { lhs, rhs -> lhs.compareTagTo(rhs) }
        adapter.notifyDataSetChanged()
    }

    private class TagCounter(val tag: String) {
        var count: Int = 1

        override fun toString(): String {
            return if (count == 1) {
                tag
            } else "$tag ($count)"
        }

        fun compareTagTo(other: TagCounter): Int = tag.compareTo(other.tag, ignoreCase = true)
    }

    companion object {
        fun newInstance(photoList: List<PhotoShelfPost>, target: Fragment, requestCode: Int): TagNavigatorDialog {
            val args = Bundle()
            val strings = photoList.mapTo(ArrayList()) { it.firstTag }
            args.putStringArrayList(ARG_TAG_LIST, strings)

            val fragment = TagNavigatorDialog()
            fragment.arguments = args
            fragment.setTargetFragment(target, requestCode)
            return fragment
        }

        /**
         * Helper method to use from dialog caller
         * @param photoList the list where to look for the tag
         * @param data the data returned by TagNavigatorDialog
         * @return the tag index if found, -1 otherwise
         */
        fun findTagIndex(photoList: List<PhotoShelfPost>, data: Intent): Int {
            val tag = data.getStringExtra(SELECTED_TAG)
            return photoList.indexOfFirst { it.firstTag.compareTo(tag, ignoreCase = true) == 0 }
        }
    }
}
