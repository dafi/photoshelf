package com.ternaryop.photoshelf.dialogs

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ternaryop.feedly.Category
import com.ternaryop.feedly.FeedlyClient
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.CheckBoxItem
import com.ternaryop.photoshelf.adapter.feedly.FeedlyCategoryAdapter
import com.ternaryop.photoshelf.fragment.feedly.FeedlyPrefs
import com.ternaryop.utils.dialog.showErrorDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_feedly_categories.cancelButton
import kotlinx.android.synthetic.main.dialog_feedly_categories.category_list
import kotlinx.android.synthetic.main.dialog_feedly_categories.ok_button

class FeedlyCategoriesDialog(
    private val feedlyClient: FeedlyClient,
    private val onCloseDialogListener: OnCloseDialogListener<FeedlyCategoriesDialog>) : DialogFragment() {
    private var categoryAdapter: FeedlyCategoryAdapter? = null
    private lateinit var feedlyPrefs: FeedlyPrefs
    private val compositeDisposable = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater
            .inflate(R.layout.dialog_feedly_categories, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()

        context?.also { context ->
            feedlyPrefs = FeedlyPrefs(context)

            compositeDisposable.add(feedlyClient.getCategories()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ categories ->
                    categoryAdapter = fillAdapter(context, categories)
                    setupList()
                },
                    { it.showErrorDialog(context)}))
        }
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    private fun setupUI() {
        ok_button.isEnabled = true
        ok_button.setOnClickListener {
            categoryAdapter?.also { adapter ->
                feedlyPrefs.selectedCategoriesId = adapter
                    .checkedItems()
                    .map { it.item.id }
                    .toSet()
            }
            if (onCloseDialogListener.onClose(this, DialogInterface.BUTTON_POSITIVE)) {
                dismiss()
            }
        }
        cancelButton.setOnClickListener {
            if (onCloseDialogListener.onClose(this, DialogInterface.BUTTON_NEGATIVE)) {
                dismiss()
            }
        }
    }

    private fun fillAdapter(context: Context, categories: List<Category>): FeedlyCategoryAdapter {
        val selected = feedlyPrefs.selectedCategoriesId
        val checkboxList = categories
            .map { CheckBoxItem(selected.contains(it.id), it) }
            .sortedWith(Comparator { lhs, rhs ->
                if (lhs.checked != rhs.checked) {
                    if (lhs.checked) -1 else 1
                } else {
                    lhs.item.label.compareTo(rhs.item.label, true)
                }
            })
        return FeedlyCategoryAdapter(context, checkboxList)
    }

    private fun setupList() {
        category_list?.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = categoryAdapter
        }
    }

    companion object {
        fun newInstance(
            feedlyClient: FeedlyClient,
            onCloseDialogListener: OnCloseDialogListener<FeedlyCategoriesDialog>): FeedlyCategoriesDialog {
            return FeedlyCategoriesDialog(feedlyClient, onCloseDialogListener)
        }
    }
}
