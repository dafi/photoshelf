package com.ternaryop.photoshelf.view

import android.app.Activity
import android.view.View
import android.widget.AdapterView
import android.widget.Filter
import android.widget.ListView
import com.ternaryop.photoshelf.db.Birthday
import com.ternaryop.photoshelf.db.BirthdayCursorAdapter
import com.ternaryop.photoshelf.util.date.dayOfMonth
import java.util.Calendar

/**
 * Created by dave on 15/03/18.
 * Hold the view used to show the birthdays celebrated in a selected month
 */
class BirthdaysMonthList(val activity: Activity, val listView: ListView, blogName: String)
    : AdapterView.OnItemClickListener {

    private var alreadyScrolledToFirst = false
    val adapter = BirthdayCursorAdapter(activity, blogName)

    init {
        listView.adapter = adapter
        listView.isTextFilterEnabled = true
        listView.onItemClickListener = this
    }

    val selectedPosts: List<Birthday>
        get() {
            val checkedItemPositions = listView.checkedItemPositions
            val list = mutableListOf<Birthday>()
            for (i in 0 until checkedItemPositions.size()) {
                val key = checkedItemPositions.keyAt(i)
                if (checkedItemPositions.get(key)) {
                    val birthday = adapter.getBirthdayItem(key)
                    if (birthday != null) {
                        list.add(birthday)
                    }
                }
            }
            return list
        }

    override fun onItemClick(parent: AdapterView<*>, v: View, position: Int, id: Long) {
        adapter.browsePhotos(activity, position)
    }

    fun changeMonth(month: Int) {
        adapter.month = month
        adapter.refresh(Filter.FilterListener {
            // when month changes scroll to first item unless must be scrolled to first birthday item
            if (alreadyScrolledToFirst) {
                listView.setSelection(0)
            } else {
                alreadyScrolledToFirst = true
                val dayPos = adapter.findDayPosition(Calendar.getInstance().dayOfMonth)
                if (dayPos >= 0) {
                    listView.setSelection(dayPos)
                }
            }
        })
    }
}