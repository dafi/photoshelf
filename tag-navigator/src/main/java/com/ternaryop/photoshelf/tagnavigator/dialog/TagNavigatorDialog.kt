package com.ternaryop.photoshelf.tagnavigator.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ternaryop.photoshelf.api.post.TagInfo
import com.ternaryop.photoshelf.api.post.toTagInfo
import com.ternaryop.photoshelf.tagnavigator.R
import com.ternaryop.photoshelf.tagnavigator.adapter.TagNavigatorAdapter
import com.ternaryop.photoshelf.tagnavigator.adapter.TagNavigatorListener
import java.util.Locale

/**
 * Created by dave on 17/05/15.
 * Allow to select tag
 */
private const val SORT_TAG_NAME = 0
private const val SORT_TAG_COUNT = 1

private const val ARG_TAG_LIST = "list"
private const val PREF_NAME_TAG_SORT = "tagNavigatorSort"

class TagNavigatorDialog : BottomSheetDialogFragment(), TagNavigatorListener {
    private lateinit var adapter: TagNavigatorAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater
            .inflate(R.layout.dialog_tag_navigator, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TagNavigatorAdapter(
            requireContext(),
            arguments?.getStringArrayList(ARG_TAG_LIST)?.toTagInfo() ?: emptyList(),
            "",
            this
        )
        val tagList = view.findViewById<RecyclerView>(R.id.tag_list)
        tagList.setHasFixedSize(true)
        tagList.layoutManager = LinearLayoutManager(activity)
        tagList.adapter = adapter

        view.findViewById<TextView>(R.id.distinct_tag_count).text = String.format(
            Locale.US, "%d", adapter.itemCount
        )
        view.findViewById<TextView>(R.id.distinct_tag_title).text = resources.getString(R.string.tag_navigator_distinct_title)

        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        changeSortType(preferences.getInt(PREF_NAME_TAG_SORT, SORT_TAG_NAME))
        view.findViewById<Button>(R.id.sort_tag).setOnClickListener { v ->
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
        parentFragmentManager.setFragmentResult(
            checkNotNull(arguments?.getString(EXTRA_REQUEST_KEY)),
            bundleOf(EXTRA_SELECTED_TAG to item.tag)
        )
        dismiss()
    }

    private fun changeSortType(sortType: Int) {
        when (sortType) {
            SORT_TAG_NAME -> {
                view?.findViewById<Button>(R.id.sort_tag)?.setText(R.string.sort_by_count)
                adapter.sortByTagName()
            }
            SORT_TAG_COUNT -> {
                view?.findViewById<Button>(R.id.sort_tag)?.setText(R.string.sort_by_name)
                adapter.sortByTagCount()
            }
        }
    }

    companion object {
        private const val EXTRA_REQUEST_KEY = "requestKey"
        const val EXTRA_SELECTED_TAG = "selectedTag"

        fun newInstance(
            tagList: ArrayList<String>,
            requestKey: String
        ) = TagNavigatorDialog().apply {
            arguments = bundleOf(
                EXTRA_REQUEST_KEY to requestKey,
                ARG_TAG_LIST to tagList
            )
        }
    }
}
