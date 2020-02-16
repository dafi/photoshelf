package com.ternaryop.photoshelf.birthday.browser.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.adapter.SelectionArrayViewHolder
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.photoshelf.birthday.R
import com.ternaryop.utils.date.dayOfMonth
import java.util.Calendar

val nullDate: Calendar by lazy {
    val c = Calendar.getInstance()
    c.timeInMillis = 0
    c
}

/**
 * Used by birthday browser
 * @author dave
 */
class BirthdayAdapter(
    private val context: Context,
    var blogName: String
) : RecyclerView.Adapter<BirthdayViewHolder>() {

    var pattern = ""
    set(value) {
        field = value.trim()
    }

    private val items = mutableListOf<Birthday>()
    val selection = SelectionArrayViewHolder(this)

    var onClickListener: View.OnClickListener? = null
    var onLongClickListener: View.OnLongClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BirthdayViewHolder {
        return BirthdayViewHolder(LayoutInflater.from(context).inflate(R.layout.list_row_2, parent, false))
    }

    override fun onBindViewHolder(holder: BirthdayViewHolder, position: Int) {
        holder.bindModel(pattern, items[position])
        holder.itemView.isSelected = selection.isSelected(position)
        holder.onClickListeners = onClickListener
        holder.onLongClickListener = onLongClickListener
    }

    val selectedPosts: List<Birthday>
        get() = selection.selectedPositions.map { getItem(it) }

    override fun getItemCount(): Int = items.size

    fun addAll(collection: Collection<Birthday>) {
        items.addAll(collection)
        notifyDataSetChanged()
    }

    fun setBirthdays(collection: Collection<Birthday>) {
        items.clear()
        items.addAll(collection)
        notifyDataSetChanged()
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    fun findPosition(birthday: Birthday): Int = items.indexOfFirst { it == birthday }

    fun removeAt(pos: Int): Boolean {
        if (0 <= pos && pos < items.size) {
            items.removeAt(pos)
            return true
        }
        return false
    }

    fun getItem(position: Int): Birthday = items[position]

    fun findDayPosition(day: Int): Int {
        if (day !in dayRange) {
            return -1
        }

        return items.indexOfFirst { birthday ->
            val date = birthday.birthdate

            if (date == nullDate) {
                false
            } else {
                // move to bday or closest one
                date.dayOfMonth >= day
            }
        }
    }

    fun updateItems(list: List<Birthday>) {
        list.forEach { birthday ->
            val pos = findPosition(birthday)
            if (pos >= 0) {
                notifyItemChanged(pos)
            }
        }
    }

    fun removeItems(list: List<Birthday>) {
        list.forEach { birthday ->
            val pos = findPosition(birthday)
            if (pos >= 0) {
                removeAt(pos)
                notifyItemRemoved(pos)
            }
        }
    }

    companion object {
        private val dayRange = 0..30
    }
}
