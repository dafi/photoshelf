package com.ternaryop.photoshelf.dialogs

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.adapter.tagnavigator.TagNavigatorAdapter
import com.ternaryop.photoshelf.adapter.tagnavigator.TagNavigatorListener
import com.ternaryop.photoshelf.api.post.TagInfo
import kotlinx.android.synthetic.main.dialog_tag_navigator.distinct_tag_count
import kotlinx.android.synthetic.main.dialog_tag_navigator.distinct_tag_title
import kotlinx.android.synthetic.main.dialog_tag_navigator.sort_tag
import kotlinx.android.synthetic.main.dialog_tag_navigator.tag_list

/**
 * Created by dave on 17/05/15.
 * Allow to select tag
 */
private const val SORT_TAG_NAME = 0
private const val SORT_TAG_COUNT = 1

private const val SELECTED_TAG = "selectedTag"
private const val ARG_TAG_LIST = "list"
private const val PREF_NAME_TAG_SORT = "tagNavigatorSort"

class TagNavigatorDialog : BottomSheetDialogFragment(), TagNavigatorListener {
    private lateinit var adapter: TagNavigatorAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // https://github.com/material-components/material-components-android/issues/99
        return inflater
            .cloneInContext(ContextThemeWrapper(activity, R.style.Theme_PhotoShelf))
            .inflate(R.layout.dialog_tag_navigator, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TagNavigatorAdapter(activity!!,
            TagInfo.fromStrings(arguments!!.getStringArrayList(ARG_TAG_LIST)!!),
            "",
            this)
        tag_list.setHasFixedSize(true)
        tag_list.layoutManager = LinearLayoutManager(activity)
        tag_list.adapter = adapter

        distinct_tag_count.text = String.format("%d", adapter.itemCount)
        distinct_tag_title.text = resources.getString(R.string.tag_navigator_distinct_title)

        val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
        changeSortType(preferences.getInt(PREF_NAME_TAG_SORT, SORT_TAG_NAME))
        sort_tag.setOnClickListener { v ->
            when (v.id) {
                R.id.sort_tag -> {
                    var sortType = preferences.getInt(PREF_NAME_TAG_SORT, SORT_TAG_NAME)
                    sortType = if (sortType == SORT_TAG_NAME) SORT_TAG_COUNT else SORT_TAG_NAME
                    preferences.edit().putInt(PREF_NAME_TAG_SORT, sortType).apply()
                    changeSortType(sortType)
                }
            }
        }
    }

    override fun onClick(item: TagInfo) {
        val intent = Intent()
        intent.putExtra(SELECTED_TAG, item.tag)
        targetFragment!!.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
        dismiss()
    }

    private fun changeSortType(sortType: Int) {
        when (sortType) {
            SORT_TAG_NAME -> {
                sort_tag.setText(R.string.sort_by_count)
                adapter.sortByTagName()
            }
            SORT_TAG_COUNT -> {
                sort_tag.setText(R.string.sort_by_name)
                adapter.sortByTagCount()
            }
        }
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
            val tag = data.getStringExtra(SELECTED_TAG) ?: return -1
            return photoList.indexOfFirst { it.firstTag.compareTo(tag, ignoreCase = true) == 0 }
        }
    }
}
