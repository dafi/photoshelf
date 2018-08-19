package com.ternaryop.photoshelf.fragment

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Environment
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.activity.TagPhotoBrowserActivity
import com.ternaryop.photoshelf.adapter.birthday.BirthdayAdapter
import com.ternaryop.photoshelf.adapter.birthday.BirthdayShowFlags
import com.ternaryop.photoshelf.adapter.birthday.nullDate
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.photoshelf.api.birthday.BirthdayService.Companion.MAX_BIRTHDAY_COUNT
import com.ternaryop.photoshelf.util.log.Log
import com.ternaryop.photoshelf.util.network.ApiManager
import com.ternaryop.photoshelf.util.post.OnScrollPostFetcher
import com.ternaryop.utils.date.dayOfMonth
import com.ternaryop.utils.date.month
import com.ternaryop.utils.date.toIsoFormat
import com.ternaryop.utils.date.year
import com.ternaryop.utils.dialog.showErrorDialog
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.concurrent.TimeUnit

const val DEBOUNCE_TIMEOUT_MILLIS = 600L

class BirthdaysBrowserFragment : AbsPhotoShelfFragment(), ActionMode.Callback,
    View.OnClickListener, View.OnLongClickListener {
    private lateinit var toolbarSpinner: Spinner
    private var currentSelectedItemId = R.id.action_show_all
    private val singleSelectionMenuIds = intArrayOf(R.id.item_edit)
    private val actionModeMenuId: Int
        get() = R.menu.birthdays_context

    enum class ItemAction {
        MARK_AS_IGNORED,
        DELETE
    }

    private lateinit var onScrollPostFetcher: OnScrollPostFetcher
    private lateinit var adapter: BirthdayAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_birthdays_by_name, container, false)

        adapter = BirthdayAdapter(activity!!, fragmentActivityStatus.appSupport.selectedBlogName!!)
        adapter.onClickListener = this
        adapter.onLongClickListener = this
        recyclerView = rootView.findViewById<View>(R.id.list) as RecyclerView
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter

        onScrollPostFetcher = OnScrollPostFetcher(object : OnScrollPostFetcher.PostFetcher {
            override fun fetchPosts(listener: OnScrollPostFetcher) = resubmitQuery()
        }, MAX_BIRTHDAY_COUNT)

        recyclerView.addOnScrollListener(onScrollPostFetcher)

        val searchView = rootView.findViewById<View>(R.id.searchView1) as SearchView

        // Set up the query listener that executes the search
        val d = Observable.create(ObservableOnSubscribe<String> { subscriber ->
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(query: String): Boolean {
                    subscriber.onNext(query)
                    return false
                }

                override fun onQueryTextSubmit(query: String): Boolean = false
            })
        })
            .debounce(DEBOUNCE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .flatMapSingle { pattern ->
                adapter.pattern = pattern
                adapter.find(0, MAX_BIRTHDAY_COUNT)
            }
            .doFinally { onScrollPostFetcher.isScrolling = false }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                val birthdays = response.response.birthdays!!
                resetSearch()
                onScrollPostFetcher.incrementReadPostCount(birthdays.size)
                adapter.addAll(birthdays)
            }) { t ->
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "birthday_browser_errors.txt")
                Log.error(t, file)
                t.showErrorDialog(context!!)
            }
        compositeDisposable.add(d)

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
        val c = if (birthday.birthdate == nullDate) {
            Calendar.getInstance()
        } else {
            birthday.birthdate
        }

        DatePickerDialog(context!!, DatePickerDialog.OnDateSetListener { _, pickedYear, pickedMonth, pickedDay ->
            c.year = pickedYear
            c.month = pickedMonth
            c.dayOfMonth = pickedDay

            birthday.birthdate = c

            ApiManager.birthdayService((context!!))
                .updateByName(birthday.name, birthday.birthdate.toIsoFormat())
                .toSingle { birthday }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { d -> compositeDisposable.add(d) }
                .subscribe({
                    val pos = adapter.findPosition(it)
                    if (pos >= 0) {
                        adapter.notifyItemChanged(pos)
                    }
                }, { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() })
            mode.finish()
        }, c.year, c.month, c.dayOfMonth).show()
    }

    private fun showConfirmDialog(postAction: ItemAction, mode: ActionMode) {
        val birthdays = adapter.selectedPosts
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

    private fun markAsIgnored(list: List<Birthday>, mode: ActionMode) {
        val d = Observable
            .fromIterable(list)
            .flatMapSingle { bday ->
                ApiManager.birthdayService(context!!).markAsIgnored(bday.name).toSingle {
                    bday.birthdate = nullDate
                    bday
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                val pos = adapter.findPosition(it)
                if (pos >= 0) {
                    adapter.notifyItemChanged(pos)
                }
            }, { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() })
        compositeDisposable.add(d)

        mode.finish()
    }

    private fun deleteBirthdays(list: List<Birthday>, mode: ActionMode) {
        val d = Observable
            .fromIterable(list)
            .flatMapSingle { bday -> ApiManager.birthdayService(context!!).deleteByName(bday.name).toSingle { bday } }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                val pos = adapter.findPosition(it)
                if (pos >= 0) {
                    adapter.removeAt(pos)
                    adapter.notifyItemRemoved(pos)
                }
            }, { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() })
        compositeDisposable.add(d)

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

        adapter.showFlags.setFlag(showFlag, isChecked)
        resetSearch()
        resubmitQuery()
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
            if (adapter.showFlags.isOn(BirthdayShowFlags.SHOW_ALL)
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
            supportActionBar?.themedContext!!,
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
                changeMonth(pos)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun resetSearch() {
        adapter.clear()
        onScrollPostFetcher.reset()
    }

    private fun resubmitQuery() {
        val d = adapter.find(onScrollPostFetcher.offset, MAX_BIRTHDAY_COUNT)
            .doFinally { onScrollPostFetcher.isScrolling = false }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response ->
                val birthdays = response.response.birthdays!!
                adapter.addAll(birthdays)
                onScrollPostFetcher.incrementReadPostCount(birthdays.size)
                scrollToFirstTodayBirthday()
            }
        compositeDisposable.add(d)
    }

    private fun scrollToPosition(position: Int) {
        // offset set to 0 put the item to the top
        (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
    }

    fun changeMonth(month: Int) {
        adapter.month = month
        resetSearch()
        resubmitQuery()
    }

    private fun scrollToFirstTodayBirthday() {
        if (!adapter.showFlags.isOn(BirthdayShowFlags.SHOW_ALL)) {
            return
        }
        val dayPos = adapter.findDayPosition(Calendar.getInstance().dayOfMonth)
        if (dayPos >= 0) {
            scrollToPosition(dayPos)
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
            actionMode = activity!!.startActionMode(this)
        }
        updateSelection(v.tag as Int)
        return true
    }

    private fun updateSelection(position: Int) {
        val selection = adapter.selection
        selection.toggle(position)
        if (selection.itemCount == 0) {
            actionMode!!.finish()
        } else {
            updateMenuItems()
            val selectionCount = selection.itemCount
            actionMode!!.subtitle = resources.getQuantityString(
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

        if (adapter.showFlags.isShowMissing) {
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
        TagPhotoBrowserActivity.startPhotoBrowserActivity(activity!!, blogName!!, tag, false)
    }

    companion object {
        private const val MONTH_COUNT = 12
        private val MISSING_BIRTHDAYS_ITEMS = intArrayOf(R.id.item_edit)
    }
}
