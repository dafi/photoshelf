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
import android.widget.Filter
import android.widget.ListView
import android.widget.SearchView
import android.widget.Spinner
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.db.Birthday
import com.ternaryop.photoshelf.db.BirthdayCursorAdapter
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.util.date.dayOfMonth
import com.ternaryop.photoshelf.util.date.month
import com.ternaryop.photoshelf.util.date.year
import java.text.DateFormatSymbols
import java.util.Calendar

class BirthdaysBrowserFragment : AbsPhotoShelfFragment(), AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener, AdapterView.OnItemSelectedListener {

    private lateinit var toolbarSpinner: Spinner
    private lateinit var listView: ListView
    private lateinit var birthdayAdapter: BirthdayCursorAdapter

    private var currentSelectedItemId = R.id.action_show_all
    private val singleSelectionMenuIds = intArrayOf(R.id.item_edit)

    private var alreadyScrolledToFirstBirthday = false

    private val actionModeMenuId: Int
        get() = R.menu.birthdays_context

    private val selectedPosts: List<Birthday>
        get() {
            val checkedItemPositions = listView.checkedItemPositions
            val list = mutableListOf<Birthday>()
            for (i in 0 until checkedItemPositions.size()) {
                val key = checkedItemPositions.keyAt(i)
                if (checkedItemPositions.get(key)) {
                    val birthday = birthdayAdapter.getBirthdayItem(key)
                    if (birthday != null) {
                        list.add(birthday)
                    }
                }
            }
            return list
        }

    enum class ItemAction {
        MARK_AS_IGNORED,
        DELETE
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_birthdays_by_name, container, false)

        birthdayAdapter = BirthdayCursorAdapter(
                activity,
                fragmentActivityStatus.appSupport.selectedBlogName!!)
        // listView is filled from onNavigationItemSelected
        listView = rootView.findViewById<View>(R.id.list) as ListView
        listView.adapter = birthdayAdapter
        listView.isTextFilterEnabled = true
        listView.onItemClickListener = this
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
        listView.setMultiChoiceModeListener(this)

        (rootView.findViewById<View>(R.id.searchView1) as SearchView).setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                birthdayAdapter.filter.filter(newText)
                return true
            }
        })

        setupActionBar()
        setHasOptionsMenu(true)

        return rootView
    }

    override fun onItemClick(parent: AdapterView<*>, v: View, position: Int, id: Long) {
        birthdayAdapter.browsePhotos(activity, position)
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
        val birthdays = selectedPosts
        if (birthdays.size != 1) {
            return
        }
        val birthday = birthdays[0]
        val c = birthday.birthDate ?: Calendar.getInstance()

        DatePickerDialog(activity, DatePickerDialog.OnDateSetListener { _, pickedYear, pickedMonth, pickedDay ->
            c.year = pickedYear
            c.month = pickedMonth
            c.dayOfMonth = pickedDay

            birthday.birthDate = c
            if (birthday.id < 0) {
                DBHelper.getInstance(activity).birthdayDAO.insert(birthday)
            } else {
                DBHelper.getInstance(activity).birthdayDAO.update(birthday)
            }
            birthdayAdapter.refresh()
            mode.finish()
        }, c.year, c.month, c.dayOfMonth).show()
    }

    override fun onDestroyActionMode(mode: ActionMode) {}

    override fun onItemCheckedStateChanged(mode: ActionMode, position: Int,
                                           id: Long, checked: Boolean) {
        val selectCount = listView.checkedItemCount
        val singleSelection = selectCount == 1

        val menu = mode.menu
        if (birthdayAdapter.isShowMissing) {
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
        val birthdays = selectedPosts
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> when (postAction) {
                    BirthdaysBrowserFragment.ItemAction.DELETE -> deleteBirthdays(birthdays, mode)
                    BirthdaysBrowserFragment.ItemAction.MARK_AS_IGNORED -> markAsIgnored(birthdays, mode)
                }
            }
        }

        val message = when (postAction) {
            BirthdaysBrowserFragment.ItemAction.DELETE -> resources.getQuantityString(R.plurals.delete_items_confirm,
                    birthdays.size,
                    birthdays.size,
                    birthdays[0].name)
            BirthdaysBrowserFragment.ItemAction.MARK_AS_IGNORED -> resources.getQuantityString(R.plurals.update_items_confirm,
                    birthdays.size,
                    birthdays.size,
                    birthdays[0].name)
        }

        AlertDialog.Builder(activity)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, dialogClickListener)
                .setNegativeButton(android.R.string.no, dialogClickListener)
                .show()
    }

    private fun markAsIgnored(birthdays: List<Birthday>, mode: ActionMode) {
        for (b in birthdays) {
            DBHelper.getInstance(activity).birthdayDAO.markAsIgnored(b.id)
        }
        birthdayAdapter.refresh()
        mode.finish()
    }

    private fun deleteBirthdays(list: List<Birthday>, mode: ActionMode) {
        for (b in list) {
            DBHelper.getInstance(activity).birthdayDAO.remove(b.id)
        }
        birthdayAdapter.refresh()
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
                showFlag = BirthdayCursorAdapter.SHOW_BIRTHDAYS_ALL
                subTitle = null
            }
            R.id.action_show_ignored -> {
                setSpinnerVisibility(false)
                subTitle = item.title
                showFlag = BirthdayCursorAdapter.SHOW_BIRTHDAYS_IGNORED
            }
            R.id.action_show_birthdays_in_same_day -> {
                setSpinnerVisibility(false)
                subTitle = item.title
                showFlag = BirthdayCursorAdapter.SHOW_BIRTHDAYS_IN_SAME_DAY
            }
            R.id.action_show_birthdays_missing -> {
                setSpinnerVisibility(false)
                subTitle = item.title
                showFlag = BirthdayCursorAdapter.SHOW_BIRTHDAYS_MISSING
            }
            R.id.action_show_birthdays_without_posts -> {
                setSpinnerVisibility(false)
                subTitle = item.title
                showFlag = BirthdayCursorAdapter.SHOW_BIRTHDAYS_WITHOUT_POSTS
            }
            else -> return super.onOptionsItemSelected(item)
        }

        supportActionBar?.subtitle = subTitle
        item.isChecked = isChecked
        birthdayAdapter.setShow(showFlag, isChecked)
        birthdayAdapter.refresh()
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
            if (birthdayAdapter.isShowFlag(BirthdayCursorAdapter.SHOW_BIRTHDAYS_ALL) && fragmentActivityStatus.drawerToolbar.indexOfChild(toolbarSpinner) == -1) {
                fragmentActivityStatus.drawerToolbar.addView(toolbarSpinner)
                supportActionBar?.setDisplayShowTitleEnabled(false)
            }
            menu.findItem(currentSelectedItemId).isChecked = true
        }
        super.onPrepareOptionsMenu(menu)
    }

    private fun setupActionBar() {
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val months = arrayOfNulls<String>(13)
        months[0] = getString(R.string.all)
        System.arraycopy(DateFormatSymbols().months, 0, months, 1, 12)
        val monthAdapter = ArrayAdapter<String>(
                supportActionBar?.themedContext,
                android.R.layout.simple_spinner_item,
                months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        toolbarSpinner = LayoutInflater.from(supportActionBar?.themedContext)
                .inflate(R.layout.toolbar_spinner,
                        fragmentActivityStatus.drawerToolbar,
                        false) as Spinner
        toolbarSpinner.adapter = monthAdapter
        toolbarSpinner.setSelection(Calendar.getInstance().month + 1)
        toolbarSpinner.onItemSelectedListener = this
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        birthdayAdapter.month = pos
        birthdayAdapter.refresh(Filter.FilterListener {
            // when month changes scroll to first item unless must be scroll to first birthday item
            if (!alreadyScrolledToFirstBirthday) {
                alreadyScrolledToFirstBirthday = true
                val dayPos = birthdayAdapter.findDayPosition(Calendar.getInstance().dayOfMonth)
                if (dayPos >= 0) {
                    listView.setSelection(dayPos)
                }
            } else {
                listView.setSelection(0)
            }
        })
    }

    override fun onNothingSelected(parent: AdapterView<*>) {}

    companion object {
        private val MISSING_BIRTHDAYS_ITEMS = intArrayOf(R.id.item_edit)
    }
}
