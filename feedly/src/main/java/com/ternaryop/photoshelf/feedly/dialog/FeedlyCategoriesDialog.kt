package com.ternaryop.photoshelf.feedly.dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.feedly.Category
import com.ternaryop.photoshelf.adapter.CheckBoxItem
import com.ternaryop.photoshelf.feedly.R
import com.ternaryop.photoshelf.feedly.adapter.FeedlyCategoryAdapter
import com.ternaryop.photoshelf.feedly.fragment.FeedlyModelResult
import com.ternaryop.photoshelf.feedly.fragment.FeedlyViewModel
import com.ternaryop.photoshelf.lifecycle.EventObserver
import com.ternaryop.photoshelf.lifecycle.Status
import com.ternaryop.utils.dialog.showErrorDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FeedlyCategoriesDialog : DialogFragment() {
    private var categoryAdapter: FeedlyCategoryAdapter? = null
    private val viewModel: FeedlyViewModel by viewModels()
    private lateinit var categoryList: RecyclerView

    @SuppressLint("InflateParams") // for dialogs passing null for root is valid, ignore the warning
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_feedly_categories, null)

        setupUI(view)

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ -> update() }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.result.observe(requireActivity(), EventObserver { result ->
            when (result) {
                is FeedlyModelResult.Categories -> onCategories(result)
            }
        })

        viewModel.loadCategories()
    }

    private fun setupUI(view: View) {
        categoryList = view.findViewById(R.id.category_list)
    }

    private fun onCategories(result: FeedlyModelResult.Categories) {
        when (result.command.status) {
            Status.SUCCESS -> {
                result.command.data?.also { data ->
                    categoryAdapter = fillAdapter(data)
                    setupList()
                }
            }
            Status.ERROR -> {
                result.command.error?.also { it.showErrorDialog(requireContext()) }
            }
            Status.PROGRESS -> { }
        }
    }

    private fun update() {
        (targetFragment as? OnFeedlyCategoriesListener)?.also { listener ->
            val selectedCategoriesId = categoryAdapter?.checkedItems()?.map { it.item.id }?.toSet()
                ?: emptySet()

            listener.onSelected(this, selectedCategoriesId)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun fillAdapter(categories: List<Category>): FeedlyCategoryAdapter {
        val selected = checkNotNull(arguments?.getSerializable(ARG_SELECTED_CATEGORIES_ID)) as Set<String>
        val checkboxList = categories
            .map { CheckBoxItem(selected.contains(it.id), it) }
            .sortedWith(Comparator { lhs, rhs ->
                if (lhs.checked != rhs.checked) {
                    if (lhs.checked) -1 else 1
                } else {
                    lhs.item.label.compareTo(rhs.item.label, true)
                }
            })
        return FeedlyCategoryAdapter(requireContext(), checkboxList)
    }

    private fun setupList() {
        categoryList.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = categoryAdapter
        }
    }

    companion object {
        private const val ARG_SELECTED_CATEGORIES_ID = "selectedCategoriesId"

        fun newInstance(
            selectedCategoriesId: Set<String>,
            target: Fragment
        ) = FeedlyCategoriesDialog().apply {
            arguments = bundleOf(
                ARG_SELECTED_CATEGORIES_ID to selectedCategoriesId)
            setTargetFragment(target, 0)
        }
    }
}

interface OnFeedlyCategoriesListener {
    fun onSelected(dialog: DialogFragment, selectedCategoriesId: Set<String>)
}
