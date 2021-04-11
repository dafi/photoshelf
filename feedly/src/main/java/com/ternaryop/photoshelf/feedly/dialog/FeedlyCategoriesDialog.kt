package com.ternaryop.photoshelf.feedly.dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
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
                else -> throw AssertionError("No valid $result")
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
        val selectedCategoriesId = categoryAdapter?.checkedItems()?.map { it.item.id }?.toSet()
            ?: emptySet()
        parentFragmentManager.setFragmentResult(
            checkNotNull(arguments?.getString(EXTRA_REQUEST_KEY)),
            bundleOf(
                EXTRA_SELECTED_CATEGORIES_ID to selectedCategoriesId
            ))
    }

    @Suppress("UNCHECKED_CAST")
    private fun fillAdapter(categories: List<Category>): FeedlyCategoryAdapter {
        val selected = checkNotNull(arguments?.getSerializable(EXTRA_SELECTED_CATEGORIES_ID)) as Set<String>
        val checkboxList = categories
            .map { CheckBoxItem(selected.contains(it.id), it) }
            .sortedWith { lhs, rhs ->
                if (lhs.checked != rhs.checked) {
                    if (lhs.checked) -1 else 1
                } else {
                    lhs.item.label.compareTo(rhs.item.label, true)
                }
            }
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
        const val EXTRA_SELECTED_CATEGORIES_ID = "selectedCategoriesId"
        private const val EXTRA_REQUEST_KEY = "requestKey"

        fun newInstance(
            selectedCategoriesId: Set<String>,
            requestKey: String,
        ) = FeedlyCategoriesDialog().apply {
            arguments = bundleOf(
                EXTRA_REQUEST_KEY to requestKey,
                EXTRA_SELECTED_CATEGORIES_ID to selectedCategoriesId)
        }
    }
}
