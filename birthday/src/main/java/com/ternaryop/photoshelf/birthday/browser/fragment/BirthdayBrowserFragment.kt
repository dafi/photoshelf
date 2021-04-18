package com.ternaryop.photoshelf.birthday.browser.fragment

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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SearchView
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.activity.ImageViewerActivityStarter
import com.ternaryop.photoshelf.activity.TagPhotoBrowserData
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.photoshelf.birthday.R
import com.ternaryop.photoshelf.birthday.browser.adapter.BirthdayAdapter
import com.ternaryop.photoshelf.birthday.browser.adapter.BirthdayShowFlags
import com.ternaryop.photoshelf.fragment.AbsPhotoShelfFragment
import com.ternaryop.photoshelf.lifecycle.EventObserver
import com.ternaryop.photoshelf.lifecycle.Status
import com.ternaryop.photoshelf.util.post.OnPagingScrollListener
import com.ternaryop.util.coroutine.DebouncingQueryTextListener
import com.ternaryop.utils.date.dayOfMonth
import com.ternaryop.utils.date.month
import com.ternaryop.utils.date.year
import com.ternaryop.utils.dialog.showErrorDialog
import com.ternaryop.utils.recyclerview.scrollItemOnTopByPosition
import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormatSymbols
import java.util.Calendar

private const val PARAM_LAST_PATTERN = "lastPattern"
private const val PARAM_SELECTED_OPTIONS_ITEM_ID = "selectedOptionItemId"
private const val DEBOUNCE_TIMEOUT_MILLIS = 600L

@AndroidEntryPoint
class BirthdayBrowserFragment(
    private val imageViewerActivityStarter: ImageViewerActivityStarter
) : AbsPhotoShelfFragment(), ActionMode.Callback,
    OnPagingScrollListener.OnScrollListener,
    View.OnClickListener, View.OnLongClickListener {
    private lateinit var toolbarSpinner: Spinner
    private var currentSelectedItemId = R.id.action_show_all
    private val singleSelectionMenuIds = intArrayOf(R.id.item_edit)
    private val actionModeMenuId: Int
        get() = R.menu.birthday_browser_context
    private val viewModel: BirthdayBrowserViewModel by viewModels()

    enum class ItemAction {
        MARK_AS_IGNORED,
        DELETE
    }

    private lateinit var adapter: BirthdayAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_birthday_browser, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = BirthdayAdapter(requireContext(), requireBlogName)
        adapter.onClickListener = this
        adapter.onLongClickListener = this
        recyclerView = view.findViewById(R.id.list)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        recyclerView.addOnScrollListener(OnPagingScrollListener(this))

        val searchView = view.findViewById<SearchView>(R.id.searchView1)

        // Set up the query listener that executes the search
        searchView.setOnQueryTextListener(DebouncingQueryTextListener(DEBOUNCE_TIMEOUT_MILLIS) { pattern ->
            // this is called after rotation so we ensure the find runs only when the pattern changes
            if (adapter.pattern != pattern) {
                adapter.pattern = pattern
                viewModel.pageFetcher.clear()
                viewModel.find(BirthdayBrowserModelResult.ActionId.QUERY_BY_TYPING, adapter.pattern, false)
            }
        })

        savedInstanceState?.apply {
            getString(PARAM_LAST_PATTERN)?.also { adapter.pattern = it }
            currentSelectedItemId = getInt(PARAM_SELECTED_OPTIONS_ITEM_ID)
            viewModel.find(BirthdayBrowserModelResult.ActionId.RESUBMIT_QUERY, adapter.pattern, true)
        }

        viewModel.result.observe(viewLifecycleOwner, EventObserver { result ->
            when (result) {
                is BirthdayBrowserModelResult.Find -> when (result.actionId) {
                    BirthdayBrowserModelResult.ActionId.QUERY_BY_TYPING -> onQueryByTyping(result)
                    BirthdayBrowserModelResult.ActionId.RESUBMIT_QUERY -> onResubmitQuery(result)
                }
                is BirthdayBrowserModelResult.MarkAsIgnored -> onMarkAsIgnored(result)
                is BirthdayBrowserModelResult.UpdateByName -> onUpdateByName(result)
                is BirthdayBrowserModelResult.DeleteBirthday -> onDeleteBirthdays(result)
            }
        })

        setupActionBar()
        setHasOptionsMenu(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(PARAM_LAST_PATTERN, adapter.pattern)
        outState.putInt(PARAM_SELECTED_OPTIONS_ITEM_ID, currentSelectedItemId)
    }

    override fun onScrolled(
        onPagingScrollListener: OnPagingScrollListener,
        firstVisibleItem: Int,
        visibleItemCount: Int,
        totalItemCount: Int
    ) {
        if (viewModel.pageFetcher.changedScrollPosition(firstVisibleItem, visibleItemCount, totalItemCount)) {
            viewModel.find(BirthdayBrowserModelResult.ActionId.RESUBMIT_QUERY, adapter.pattern, false)
        }
    }

    private fun onQueryByTyping(result: BirthdayBrowserModelResult.Find) {
        when (result.command.status) {
            Status.SUCCESS -> {
                result.command.data?.also { fetched ->
                    adapter.setBirthdays(fetched.list)
                }
            }
            Status.ERROR -> result.command.error?.also { it.showErrorDialog(requireContext()) }
            Status.PROGRESS -> { }
        }
    }

    private fun onResubmitQuery(result: BirthdayBrowserModelResult.Find) {
        when (result.command.status) {
            Status.SUCCESS -> {
                result.command.data?.also { fetched ->
                    adapter.setBirthdays(fetched.list)
                    scrollToFirstTodayBirthday()
                }
            }
            Status.ERROR -> { }
            Status.PROGRESS -> { }
        }
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

    override fun onDestroyActionMode(mode: ActionMode) {
        this.actionMode = null
        adapter.selection.clear()
    }

    private fun showEditBirthdateDialog(mode: ActionMode) {
        val birthdays = adapter.selectedPosts
        if (birthdays.size != 1) {
            return
        }
        val birthday = birthdays[0]
        val c = birthday.birthdate ?: Calendar.getInstance()

        DatePickerDialog(requireContext(), { _, pickedYear, pickedMonth, pickedDay ->
            c.year = pickedYear
            c.month = pickedMonth
            c.dayOfMonth = pickedDay

            birthday.birthdate = c

            viewModel.updateByName(birthday)
            mode.finish()
        }, c.year, c.month, c.dayOfMonth).show()
    }

    private fun showConfirmDialog(postAction: ItemAction, mode: ActionMode) {
        val birthdays = adapter.selectedPosts
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> when (postAction) {
                    ItemAction.DELETE -> deleteBirthdays(birthdays, mode)
                    ItemAction.MARK_AS_IGNORED -> markAsIgnored(birthdays, mode)
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

        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, dialogClickListener)
            .setNegativeButton(android.R.string.cancel, dialogClickListener)
            .show()
    }

    private fun markAsIgnored(list: List<Birthday>, mode: ActionMode) {
        viewModel.markAsIgnored(list)
        mode.finish()
    }

    private fun onMarkAsIgnored(result: BirthdayBrowserModelResult.MarkAsIgnored) {
        when (result.command.status) {
            Status.SUCCESS -> result.command.data?.also { adapter.updateItems(it) }
            Status.ERROR -> result.command.error?.also { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
            Status.PROGRESS -> { }
        }
    }

    private fun onUpdateByName(result: BirthdayBrowserModelResult.UpdateByName) {
        when (result.command.status) {
            Status.SUCCESS -> result.command.data?.also { adapter.updateItems(listOf(it)) }
            Status.ERROR -> result.command.error?.also { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
            Status.PROGRESS -> { }
        }
    }

    private fun deleteBirthdays(list: List<Birthday>, mode: ActionMode) {
        viewModel.deleteBirthdays(list)
        mode.finish()
    }

    private fun onDeleteBirthdays(result: BirthdayBrowserModelResult.DeleteBirthday) {
        when (result.command.status) {
            Status.SUCCESS -> result.command.data?.also { adapter.removeItems(it) }
            Status.ERROR -> {
                result.command.data?.also { adapter.removeItems(it) }
                result.command.error?.also { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
            }
            Status.PROGRESS -> { }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.birthday_browser, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // if selected item is already selected don't change anything
        if (currentSelectedItemId == item.itemId) {
            return true
        }
        currentSelectedItemId = item.itemId
        val isChecked = !item.isChecked

        updateSubTitle(item)
        item.isChecked = isChecked

        val showFlag = when (item.itemId) {
            R.id.action_show_all -> BirthdayShowFlags.SHOW_ALL
            R.id.action_show_ignored -> BirthdayShowFlags.SHOW_IGNORED
            R.id.action_show_birthdays_in_same_day -> BirthdayShowFlags.SHOW_IN_SAME_DAY
            R.id.action_show_birthdays_missing -> BirthdayShowFlags.SHOW_MISSING
            R.id.action_show_birthdays_without_posts -> BirthdayShowFlags.SHOW_WITHOUT_POSTS
            else -> return super.onOptionsItemSelected(item)
        }

        viewModel.showFlags.setFlag(showFlag, isChecked)
        viewModel.pageFetcher.clear()
        viewModel.find(BirthdayBrowserModelResult.ActionId.RESUBMIT_QUERY, adapter.pattern, false)

        return true
    }

    private fun updateSubTitle(item: MenuItem) {
        supportActionBar?.subtitle = when (item.itemId) {
            R.id.action_show_all -> {
                setSpinnerVisibility(true)
                null
            }
            R.id.action_show_ignored -> {
                setSpinnerVisibility(false)
                item.title
            }
            R.id.action_show_birthdays_in_same_day -> {
                setSpinnerVisibility(false)
                item.title
            }
            R.id.action_show_birthdays_missing -> {
                setSpinnerVisibility(false)
                item.title
            }
            R.id.action_show_birthdays_without_posts -> {
                setSpinnerVisibility(false)
                item.title
            }
            else -> null
        }
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
            if (viewModel.showFlags.isOn(BirthdayShowFlags.SHOW_ALL) &&
                fragmentActivityStatus.drawerToolbar.indexOfChild(toolbarSpinner) == -1) {
                fragmentActivityStatus.drawerToolbar.addView(toolbarSpinner)
                supportActionBar?.setDisplayShowTitleEnabled(false)
            }
            menu.findItem(currentSelectedItemId).apply {
                isChecked = true
                updateSubTitle(this)
            }
        }
        super.onPrepareOptionsMenu(menu)
    }

    private fun setupActionBar() {
        val supportActionBar = supportActionBar ?: return
        supportActionBar.setDisplayShowTitleEnabled(false)
        val months = arrayOfNulls<String>(MONTH_COUNT + 1)
        months[0] = getString(R.string.all)
        System.arraycopy(DateFormatSymbols().months, 0, months, 1, MONTH_COUNT)
        val monthAdapter = ArrayAdapter<String>(
            supportActionBar.themedContext,
            android.R.layout.simple_spinner_item,
            months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        toolbarSpinner = LayoutInflater
            .from(supportActionBar.themedContext)
            .inflate(R.layout.toolbar_spinner,
                fragmentActivityStatus.drawerToolbar,
                false) as Spinner
        toolbarSpinner.adapter = monthAdapter
        toolbarSpinner.setSelection(Calendar.getInstance().month + 1)
        toolbarSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                changeMonth(pos)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    fun changeMonth(month: Int) {
        if (viewModel.month == month) {
            return
        }
        viewModel.month = month
        viewModel.pageFetcher.clear()
        viewModel.find(BirthdayBrowserModelResult.ActionId.RESUBMIT_QUERY, adapter.pattern, false)
    }

    private fun scrollToFirstTodayBirthday() {
        if (!viewModel.showFlags.isOn(BirthdayShowFlags.SHOW_ALL)) {
            return
        }
        val dayPos = adapter.findDayPosition(Calendar.getInstance().dayOfMonth)
        if (dayPos >= 0) {
            recyclerView.scrollItemOnTopByPosition(dayPos)
        }
    }

    override fun onClick(view: View?) {
        view?.let {
            val position = it.tag as Int
            if (actionMode == null) {
                browsePhotos(position)
            } else {
                updateSelection(position)
            }
        }
    }

    override fun onLongClick(v: View): Boolean {
        if (actionMode == null) {
            actionMode = requireActivity().startActionMode(this)
        }
        updateSelection(v.tag as Int)
        return true
    }

    private fun updateSelection(position: Int) {
        val selection = adapter.selection
        selection.toggle(position)
        if (selection.itemCount == 0) {
            actionMode?.finish()
        } else {
            updateMenuItems()
            val selectionCount = selection.itemCount
            actionMode?.subtitle = resources.getQuantityString(
                R.plurals.selected_items,
                selectionCount,
                selectionCount)
        }
    }

    private fun updateMenuItems() {
        val selectCount = adapter.selection.itemCount
        val singleSelection = selectCount == 1

        for (itemId in singleSelectionMenuIds) {
            actionMode?.menu?.findItem(itemId)?.isVisible = singleSelection
        }

        if (viewModel.showFlags.isShowMissing) {
            actionMode?.menu?.let { menu ->
                for (i in 0 until menu.size()) {
                    val itemId = menu.getItem(i).itemId
                    menu.getItem(i).isVisible = MISSING_BIRTHDAYS_ITEMS.contains(itemId)
                }
            }
        }
    }

    private fun browsePhotos(position: Int) {
        val tag = adapter.getItem(position).name
        requireContext().startActivity(
            imageViewerActivityStarter.tagPhotoBrowserIntent(requireContext(),
            TagPhotoBrowserData(requireBlogName, tag, false)))
    }

    companion object {
        private const val MONTH_COUNT = 12
        private val MISSING_BIRTHDAYS_ITEMS = intArrayOf(R.id.item_edit)
    }
}
