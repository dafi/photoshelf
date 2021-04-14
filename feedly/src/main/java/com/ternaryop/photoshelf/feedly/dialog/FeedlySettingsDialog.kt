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
        val settingsData = FeedlySettingsData(
            view.findViewById<EditText>(R.id.max_fetch_items_count).text.toString().toInt(),
            view.findViewById<EditText>(R.id.newer_than_hours).text.toString().toInt(),
            view.findViewById<CheckBox>(R.id.delete_on_refresh).isChecked)
        parentFragmentManager.setFragmentResult(
            checkNotNull(arguments?.getString(EXTRA_REQUEST_KEY)),
            bundleOf(EXTRA_SETTINGS_DATA to settingsData)
        )
    }

    companion object {
        private const val ARG_SETTINGS = "settings"
        private const val EXTRA_REQUEST_KEY = "requestKey"
        const val EXTRA_SETTINGS_DATA = "settingsData"

        fun newInstance(
            settingsData: FeedlySettingsData,
            requestKey: String,
        ) = FeedlySettingsDialog().apply {
            arguments = bundleOf(
                EXTRA_REQUEST_KEY to requestKey,
                ARG_SETTINGS to settingsData)
        }
    }
}

data class FeedlySettingsData(
    val maxFetchItemCount: Int,
    val newerThanHours: Int,
    val deleteOnRefresh: Boolean
) : Serializable
