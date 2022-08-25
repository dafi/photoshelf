package com.ternaryop.photoshelf.feedly.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.feedly.Category
import com.ternaryop.photoshelf.adapter.CheckBoxItem
import com.ternaryop.photoshelf.feedly.R

class FeedlyCategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
    private val checkBox: CheckBox = itemView.findViewById(R.id.checkBox1)

    fun bindModel(data: CheckBoxItem<Category>) {
        checkBox.apply {
            isChecked = data.checked
            text = data.item.label
            tag = data
            setOnClickListener(this@FeedlyCategoryViewHolder)
        }
    }

    override fun onClick(view: View) {
        val data = view.tag as CheckBoxItem<*>
        data.checked = (view as CheckBox).isChecked
    }
}

class FeedlyCategoryAdapter(
    private val context: Context,
    val items: List<CheckBoxItem<Category>>
) : RecyclerView.Adapter<FeedlyCategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedlyCategoryViewHolder {
        return FeedlyCategoryViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.list_row_checkbox, parent, false
            )
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: FeedlyCategoryViewHolder, position: Int) {
        holder.bindModel(items[position])
    }

    fun checkedItems(): List<CheckBoxItem<Category>> = items.filter { it.checked }
}
