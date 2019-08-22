package com.ternaryop.photoshelf.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import com.ternaryop.photoshelf.EXTRA_BLOG_NAME
import com.ternaryop.photoshelf.EXTRA_TYPE
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.birthday.FindParams
import com.ternaryop.photoshelf.event.CounterEvent
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.draftCount
import com.ternaryop.tumblr.queueCount
import com.ternaryop.utils.date.dayOfMonth
import com.ternaryop.utils.date.month
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import java.util.Calendar

/**
 * Created by dave on 28/10/17.
 * Handle counters retrieval
 */
class CounterIntentService : IntentService("counterIntent") {

    override fun onHandleIntent(intent: Intent?) {
        intent ?: return

        val selectedBlogName = intent.getStringExtra(EXTRA_BLOG_NAME) ?: return
        val type = intent.getIntExtra(EXTRA_TYPE, CounterEvent.NONE)

        if (ACTION_FETCH_COUNTER == intent.action) {
            fetchCounterByType(type, selectedBlogName)
        }
    }

    private fun fetchCounterByType(type: Int, blogName: String) {
        Thread(Runnable {
            if (EventBus.getDefault().hasSubscriberForEvent(CounterEvent::class.java)) {
                EventBus.getDefault().post(CounterEvent(type, getCount(type, blogName)))
            }
        }).start()
    }

    private fun getCount(type: Int, blogName: String): Int {
        try {
            return when (type) {
                CounterEvent.BIRTHDAY -> birthdayCount()
                CounterEvent.DRAFT -> TumblrManager.getInstance(applicationContext).draftCount(blogName)
                CounterEvent.SCHEDULE -> TumblrManager.getInstance(applicationContext).queueCount(blogName)
                CounterEvent.NONE -> 0
                else -> -1
            }
        } catch (ignored: Exception) {
        }

        return 0
    }

    private fun birthdayCount(): Int = runBlocking(Dispatchers.IO) {
        val now = Calendar.getInstance()
        ApiManager.birthdayService().findByDate(
            FindParams(onlyTotal = true, month = now.month + 1, dayOfMonth = now.dayOfMonth).toQueryMap())
            .response.total.toInt()
    }

    companion object {
        private const val ACTION_FETCH_COUNTER = "fetchCounter"

        fun fetchCounter(context: Context,
                         blogName: String,
                         type: Int) {
            val intent = Intent(context, CounterIntentService::class.java)
                .setAction(ACTION_FETCH_COUNTER)
                .putExtra(EXTRA_TYPE, type)
                .putExtra(EXTRA_BLOG_NAME, blogName)

            context.startService(intent)
        }
    }
}
