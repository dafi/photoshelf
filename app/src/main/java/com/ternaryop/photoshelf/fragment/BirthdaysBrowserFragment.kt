package com.ternaryop.photoshelf.fragment

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.SearchView
import android.widget.Spinner
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.db.Birthday
import com.ternaryop.photoshelf.db.BirthdayShowFlags
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.view.BirthdaysMonthList
import com.ternaryop.utils.date.dayOfMonth
import com.ternaryop.utils.date.month
import com.ternaryop.utils.date.year
import java.text.DateFormatSymbols
import java.util.Calendar

class BirthdaysBrowserFragment : AbsPhotoShelfFragment(), AbsListView.MultiChoiceModeListener {
    private lateinit var toolbarSpinner: Spinner
    private var currentSelectedItemId = R.id.action_show_all
    private val singleSelectionMenuIds = intArrayOf(R.id.item_edit)
    private lateinit var monthSelector: BirthdaysMonthList
    private val actionModeMenuId: Int
        get() = R.menu.birthdays_context

    enum class ItemAction {
        MARK_AS_IGNORED,
        DELETE
    }

    override fun onCreateView(inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_birthdays_by_name, container, false)

        monthSelector = BirthdaysMonthList(activity!!,
            rootView.findViewById<View>(R.id.list) as ListView,
            fragmentActivityStatus.appSupport.selectedBlogName!!)
        monthSelector.listView.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE_MODAL
        monthSelector.listView.setMultiChoiceModeListener(this)

        (rootView.findViewById<View>(R.id.searchView1) as SearchView)
            .setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    monthSelector.adapter.filter.filter(newText)
                    return true
                }
            })

        setupActionBar()
        setHasOptionsMenu(true)

        return rootView
    }

    override fun setRetainInstance(retain: Boolean) {
        // retain can't be set to true when calling nested fragment like tabs
        // so we ignore any call
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.setTitle(R.string.select_items)
        mode.subtitle = resources.getQuantityString(R.plurals.selected_items, 1, 1)
        val inflater = mode.menuInflater
        inflater.inflate(actionModeMenuId, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_delete -> {
                showConfirmDialog(ItemAction.DELETE, mode)
                true
            }
            R.id.item_mark_as_ignored -> {
                showConfirmDialog(ItemAction.MARK_AS_IGNORED, mode)
                true
            }
            R.id.item_edit -> {
                showEditBirthdateDialog(mode)
                true
            }
            else -> false
        }
    }

    private fun showEditBirthdateDialog(mode: ActionMode) {
        val birthdays = monthSelector.selectedPosts
        if (birthdays.size != 1) {
            return
        }
        val birthday = birthdays[0]
        val c = birthday.birthDate ?: Calendar.getInstance()

        DatePickerDialog(context!!, DatePickerDialog.OnDateSetListener { _, pickedYear, pickedMonth, pickedDay ->
            c.year = pickedYear
            c.month = pickedMonth
            c.dayOfMonth = pickedDay

            birthday.birthDate = c
            if (birthday.id < 0) {
                DBHelper.getInstance(context!!).birthdayDAO.insert(birthday)
            } else {
                DBHelper.getInstance(context!!).birthdayDAO.update(birthday)
            }
            monthSelector.adapter.refresh()
            mode.finish()
        }, c.year, c.month, c.dayOfMonth).show()
    }

    override fun onDestroyActionMode(mode: ActionMode) {}

    override fun onItemCheckedStateChanged(mode: ActionMode, position: Int,
        id: Long, checked: Boolean) {
        val selectCount = monthSelector.listView.checkedItemCount
        val singleSelection = selectCount == 1
        val menu = mode.menu
        if (monthSelector.adapter.showFlags.isShowMissing) {
            for (i in 0 until menu.size()) {
                val itemId = menu.getItem(i).itemId
                menu.getItem(i).isVisible = MISSING_BIRTHDAYS_ITEMS.contains(itemId)
            }
        }

        for (itemId in singleSelectionMenuIds) {
            val item = menu.findItem(itemId)
            if (item != null) {
                item.isVisible = singleSelection
            }
        }

        mode.subtitle = resources.getQuantityString(
            R.plurals.selected_items,
            selectCount,
            selectCount)
    }

    private fun showConfirmDialog(postAction: ItemAction, mode: ActionMode) {
        val birthdays = monthSelector.selectedPosts
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> when (postAction) {
                    BirthdaysBrowserFragment.ItemAction.DELETE -> deleteBirthdays(birthdays, mode)
                    BirthdaysBrowserFragment.ItemAction.MARK_AS_IGNORED -> markAsIgnored(birthdays, mode)
                }
            }
        }
        val message = when (postAction) {
            ItemAction.DELETE -> resources.getQuantityString(R.plurals.delete_items_confirm,
                birthdays.size,
                birthdays.size,
                birthdays[0].name)
            ItemAction.MARK_AS_IGNORED -> resources.getQuantityString(R.plurals.update_items_confirm,
                birthdays.size,
                birthdays.size,
                birthdays[0].name)
        }

        AlertDialog.Builder(context!!)
            .setMessage(message)
            .setPositiveButton(android.R.string.yes, dialogClickListener)
            .setNegativeButton(android.R.string.no, dialogClickListener)
            .show()
    }

    private fun markAsIgnored(birthdays: List<Birthday>, mode: ActionMode) {
        for (b in birthdays) {
            DBHelper.getInstance(context!!).birthdayDAO.markAsIgnored(b.id)
        }
        monthSelector.adapter.refresh()
        mode.finish()
    }

    private fun deleteBirthdays(list: List<Birthday>, mode: ActionMode) {
        for (b in list) {
            DBHelper.getInstance(context!!).birthdayDAO.remove(b.id)
        }
        monthSelector.adapter.refresh()
        mode.finish()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.birthdays_browser, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // if selected item is already selected don't change anything
        if (currentSelectedItemId == item.itemId) {
            return true
        }
        val isChecked = !item.isChecked
        val showFlag: Int

        currentSelectedItemId = item.itemId
        val subTitle: CharSequence?
        when (item.itemId) {
            R.id.action_show_all -> {
                setSpinnerVisibility(true)
                showFlag = BirthdayShowFlags.SHOW_ALL
                subTitle = null
            }
            R.id.action_show_ignored -> {
                setSpinnerVisibility(false)
                subTitle = item.title
                showFlag = BirthdayShowFlags.SHOW_IGNORED
            }
            R.id.action_show_birthdays_in_same_day -> {
                setSpinnerVisibility(false)
                subTitle = item.title
                showFlag = BirthdayShowFlags.SHOW_IN_SAME_DAY
            }
            R.id.action_show_birthdays_missing -> {
                setSpinnerVisibility(false)
                subTitle = item.title
                showFlag = BirthdayShowFlags.SHOW_MISSING
            }
            R.id.action_show_birthdays_without_posts -> {
                setSpinnerVisibility(false)
                subTitle = item.title
                showFlag = BirthdayShowFlags.SHOW_WITHOUT_POSTS
            }
            else -> return super.onOptionsItemSelected(item)
        }

        supportActionBar?.subtitle = subTitle
        item.isChecked = isChecked
        monthSelector.adapter.showFlags.setFlag(showFlag, isChecked)
        monthSelector.adapter.refresh()
        return true
    }

    private fun setSpinnerVisibility(visible: Boolean) {
        if (visible) {
            toolbarSpinner.visibility = View.VISIBLE
            supportActionBar?.setDisplayShowTitleEnabled(false)
        } else {
            toolbarSpinner.visibility = View.GONE
            supportActionBar?.setDisplayShowTitleEnabled(true)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        if (fragmentActivityStatus.isDrawerMenuOpen) {
            fragmentActivityStatus.drawerToolbar.removeView(toolbarSpinner)
            supportActionBar?.setDisplayShowTitleEnabled(true)
        } else {
            // check if view is already added (eg when the overflow menu is opened)
            if (monthSelector.adapter.showFlags.isOn(BirthdayShowFlags.SHOW_ALL)
                && fragmentActivityStatus.drawerToolbar.indexOfChild(toolbarSpinner) == -1) {
                fragmentActivityStatus.drawerToolbar.addView(toolbarSpinner)
                supportActionBar?.setDisplayShowTitleEnabled(false)
            }
            menu.findItem(currentSelectedItemId).isChecked = true
        }
        super.onPrepareOptionsMenu(menu)
    }

    private fun setupActionBar() {
        supportActionBar?.setDisplayShowTitleEnabled(false)
        val months = arrayOfNulls<String>(MONTH_COUNT + 1)
        months[0] = getString(R.string.all)
        System.arraycopy(DateFormatSymbols().months, 0, months, 1, MONTH_COUNT)
        val monthAdapter = ArrayAdapter<String>(
            supportActionBar?.themedContext,
            android.R.layout.simple_spinner_item,
            months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        toolbarSpinner = LayoutInflater
            .from(supportActionBar?.themedContext)
            .inflate(R.layout.toolbar_spinner,
                fragmentActivityStatus.drawerToolbar,
                false) as Spinner
        toolbarSpinner.adapter = monthAdapter
        toolbarSpinner.setSelection(Calendar.getInstance().month + 1)
        toolbarSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                monthSelector.changeMonth(pos)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    companion object {
        private const val MONTH_COUNT = 12
        private val MISSING_BIRTHDAYS_ITEMS = intArrayOf(R.id.item_edit)
    }
}
