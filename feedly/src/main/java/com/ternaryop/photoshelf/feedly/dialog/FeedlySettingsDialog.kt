package com.ternaryop.photoshelf.feedly.dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.ternaryop.photoshelf.feedly.R
import java.io.Serializable

class FeedlySettingsDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // for dialogs passing null for root is valid, ignore the warning
        @SuppressLint("InflateParams")
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_feedly_settings, null)

        setupUI(view)
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ -> update(view) }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
            .show()
    }

    private fun setupUI(view: View) {
        val fetchView = view.findViewById<EditText>(R.id.max_fetch_items_count)
        val newerThanHoursView = view.findViewById<EditText>(R.id.newer_than_hours)
        val deleteOnRefreshView = view.findViewById<CheckBox>(R.id.delete_on_refresh)

        val settings = checkNotNull(arguments?.getSerializable(ARG_SETTINGS) as? FeedlySettingsData)

        fetchView.setText(settings.maxFetchItemCount.toString())
        newerThanHoursView.setText(settings.newerThanHours.toString())
        deleteOnRefreshView.isChecked = settings.deleteOnRefresh
    }

    private fun update(view: View) {
        (targetFragment as? OnFeedlySettingsListener)?.also { listener ->
            val settingsData = FeedlySettingsData(
                Integer.parseInt(view.findViewById<EditText>(R.id.max_fetch_items_count).text.toString()),
                Integer.parseInt(view.findViewById<EditText>(R.id.newer_than_hours).text.toString()),
                view.findViewById<CheckBox>(R.id.delete_on_refresh).isChecked)
            listener.onSettings(this, settingsData)
        }
    }

    companion object {
        private const val ARG_SETTINGS = "settings"

        fun newInstance(
            settingsData: FeedlySettingsData,
            target: Fragment
        ) = FeedlySettingsDialog().apply {
            arguments = bundleOf(
                ARG_SETTINGS to settingsData)
            setTargetFragment(target, 0)
        }
    }
}

data class FeedlySettingsData(
    val maxFetchItemCount: Int,
    val newerThanHours: Int,
    val deleteOnRefresh: Boolean
) : Serializable

interface OnFeedlySettingsListener {
    fun onSettings(dialog: DialogFragment, settingsData: FeedlySettingsData)
}
