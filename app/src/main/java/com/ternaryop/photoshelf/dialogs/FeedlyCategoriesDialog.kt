package com.ternaryop.photoshelf.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
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
    context: Context,
    feedlyClient: FeedlyClient,
    private val onCloseDialogListener: OnCloseDialogListener<FeedlyCategoriesDialog>) : Dialog(context) {
    private lateinit var categoryAdapter: FeedlyCategoryAdapter
    private val feedlyPrefs = FeedlyPrefs(context)
    private val compositeDisposable = CompositeDisposable()

    init {
        setContentView(R.layout.dialog_feedly_categories)

        setupUI()
        compositeDisposable.add(feedlyClient.getCategories()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                categoryAdapter = fillAdapter(it)
                setupList()
            },
                { it.showErrorDialog(context)}))
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    private fun setupUI() {
        ok_button.isEnabled = true
        ok_button.setOnClickListener {
            feedlyPrefs.selectedCategoriesId = categoryAdapter
                .checkedItems()
                .map { it.item.id }
                .toSet()
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

    private fun fillAdapter(categories: List<Category>): FeedlyCategoryAdapter {
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
}
