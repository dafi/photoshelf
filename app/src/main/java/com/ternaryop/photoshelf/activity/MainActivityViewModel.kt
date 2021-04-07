package com.ternaryop.photoshelf.activity

import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ternaryop.photoshelf.birthday.repository.BirthdayRepository
import com.ternaryop.photoshelf.core.prefs.fetchBlogNames
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.Event
import com.ternaryop.photoshelf.repository.tumblr.TumblrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val birthdayRepository: BirthdayRepository,
    private val tumblrRepository: TumblrRepository
) : ViewModel() {

    private val _result = MediatorLiveData<Event<MainActivityModelResult>>()
    val result: LiveData<Event<MainActivityModelResult>> = _result

    init {
        _result.addSource(tumblrRepository.draftCount) {
            _result.value = Event(MainActivityModelResult.DraftCount(it)) }
        _result.addSource(tumblrRepository.scheduledCount) {
            _result.value = Event(MainActivityModelResult.ScheduledCount(it)) }
        _result.addSource(tumblrRepository.authenticate) {
            _result.value = Event(MainActivityModelResult.TumblrAuthenticated(it)) }
        _result.addSource(birthdayRepository.count) {
            _result.value = Event(MainActivityModelResult.BirthdaysCount(it)) }
    }

    fun draftCount(blogName: String) {
        viewModelScope.launch(Dispatchers.IO) { tumblrRepository.draftCount(blogName) }
    }

    fun scheduledCount(blogName: String) {
        viewModelScope.launch(Dispatchers.IO) { tumblrRepository.scheduledCount(blogName) }
    }

    fun birthdaysCount() {
        viewModelScope.launch(Dispatchers.IO) { birthdayRepository.count() }
    }

    fun fetchBlogNames() {
        viewModelScope.launch {
            val command = Command.execute {
                sharedPreferences.fetchBlogNames(tumblrRepository.tumblr)
            }
            _result.postValue(Event(MainActivityModelResult.BlogNames(command)))
        }
    }

    fun handleCallbackTumblrUri(uri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) { tumblrRepository.handleCallbackUri(uri) }
    }

    fun clearCounters() {
        birthdayRepository.clearCount()
        tumblrRepository.clearCount()
    }
}

sealed class MainActivityModelResult {
    class DraftCount(val command: Command<Int>) : MainActivityModelResult()
    class ScheduledCount(val command: Command<Int>) : MainActivityModelResult()
    class BirthdaysCount(val command: Command<Int>) : MainActivityModelResult()
    class BlogNames(val command: Command<List<String>>) : MainActivityModelResult()
    class TumblrAuthenticated(val command: Command<Boolean>) : MainActivityModelResult()
}
