package com.ternaryop.photoshelf.birthday.browser.adapter

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.photoshelf.birthday.R
import com.ternaryop.utils.date.dayOfMonth
import com.ternaryop.utils.date.month
import com.ternaryop.utils.date.yearsBetweenDates
import com.ternaryop.utils.text.fromHtml
import com.ternaryop.utils.text.htmlHighlightPattern
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val dateFormat = SimpleDateFormat("d MMMM, yyyy", Locale.US)

class BirthdayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val title: TextView = itemView.findViewById(android.R.id.text1)
    val subtitle: TextView = itemView.findViewById(android.R.id.text2)

    var onClickListeners: View.OnClickListener? = null
        set(value) {
            if (value != null) {
                val position = adapterPosition
                itemView.setOnClickListener(value)
                itemView.tag = position
            }
        }

    var onLongClickListener: View.OnLongClickListener? = null
        set(value) {
            if (value != null) {
                itemView.setOnLongClickListener(value)
                itemView.isLongClickable = true
                itemView.tag = adapterPosition
            }
        }

    fun bindModel(pattern: String, birthday: Birthday) {
        try {
            updateBackground(birthday)
            updateName(pattern, birthday)
            updateBirthdate(birthday)
        } catch (ignored: ParseException) {
        }
    }

    private fun updateBackground(birthday: Birthday) {
        val date = birthday.birthdate

        if (date == nullDate) {
            itemView.setBackgroundResource(R.drawable.list_selector_post_group_even)
            return
        }

        val now = Calendar.getInstance()

        if (date.dayOfMonth == now.dayOfMonth && date.month == now.month) {
            itemView.setBackgroundResource(R.drawable.list_selector_post_never)
        } else {
            // group by day, not perfect by better than nothing
            val isEven = date.dayOfMonth and 1 == 0
            itemView.setBackgroundResource(
                if (isEven) R.drawable.list_selector_post_group_even
                else R.drawable.list_selector_post_group_odd)
        }
    }

    private fun updateName(pattern: String, birthday: Birthday) {
        title.text = birthday.name.htmlHighlightPattern(pattern).fromHtml()
    }

    private fun updateBirthdate(birthday: Birthday) {
        val date = birthday.birthdate
        if (date == nullDate) {
            subtitle.visibility = View.GONE
        } else {
            subtitle.visibility = View.VISIBLE
            val age = date.yearsBetweenDates(Calendar.getInstance()).toString()
            val dateStr = dateFormat.format(date.time)

            subtitle.text = itemView.context.getString(R.string.name_with_age, dateStr, age)
        }
    }
}
