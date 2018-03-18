package com.ternaryop.photoshelf.dialogs.mru

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.View
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.view.SwipeToDeleteCallback

/**
 * Created by dave on 17/05/15.
 * Allow to select items from MRU
 */
private const val ARG_MRU_LIST = "mruList"

class MRUDialog : DialogFragment() {

    private lateinit var adapter: MRUAdapter
    private lateinit var onMRUListener: OnMRUListener

    fun setOnMRUListener(onMRUListener: OnMRUListener): MRUDialog {
        this.onMRUListener = onMRUListener
        return this
    }

    fun getItem(position: Int): String {
        return adapter.getItem(position)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context!!)
                .setView(setupUI())
                .setTitle(resources.getString(R.string.recently_used_tags))
                .setNegativeButton(resources.getString(R.string.close), null)
                .create()
    }

    @SuppressLint("InflateParams")
    private fun setupUI(): View {
        val view = activity!!.layoutInflater.inflate(R.layout.mru_list, null)
        initRecyclerView(view)

        return view
    }

    private fun initRecyclerView(rootView: View) {
        adapter = MRUAdapter(this, arguments!!.getStringArrayList(ARG_MRU_LIST)!!)
        adapter.onMRUListener = onMRUListener

        val recyclerView = rootView.findViewById<RecyclerView>(R.id.list)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        addSwipeToDelete(recyclerView)
    }

    private fun addSwipeToDelete(recyclerView: RecyclerView) {
        val swipeHandler = object : SwipeToDeleteCallback(context!!,
                ContextCompat.getDrawable(context!!, R.drawable.ic_action_delete)!!,
                ColorDrawable(ContextCompat.getColor(context!!, R.color.animation_delete_bg))) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                adapter.removeAt(viewHolder.adapterPosition)
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    companion object {
        fun newInstance(list: List<String>): MRUDialog {
            val args = Bundle()
            args.putStringArrayList(ARG_MRU_LIST, ArrayList(list))

            val fragment = MRUDialog()
            fragment.arguments = args
            return fragment
        }
    }
}
