package com.ternaryop.photoshelf.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import com.ternaryop.photoshelf.EXTRA_ACTION
import com.ternaryop.photoshelf.EXTRA_BLOG_NAME
import com.ternaryop.photoshelf.EXTRA_TYPE
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.event.CounterEvent
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.draftCount
import com.ternaryop.tumblr.queueCount
import org.greenrobot.eventbus.EventBus
import java.util.Calendar

/**
 * Created by dave on 28/10/17.
 * Handle counters retrieval
 */
class CounterIntentService : IntentService("counterIntent") {

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        val selectedBlogName = intent.getStringExtra(EXTRA_BLOG_NAME)
        val type = intent.getIntExtra(EXTRA_TYPE, CounterEvent.NONE)
        val action = intent.getStringExtra(EXTRA_ACTION)

        if (ACTION_FETCH_COUNTER == action) {
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
                CounterEvent.BIRTHDAY -> DBHelper
                        .getInstance(applicationContext)
                        .birthdayDAO
                        .getBirthdaysCountInDate(Calendar.getInstance().time, blogName).toInt()
                CounterEvent.DRAFT -> Tumblr.getSharedTumblr(applicationContext).draftCount(blogName)
                CounterEvent.SCHEDULE -> Tumblr.getSharedTumblr(applicationContext).queueCount(blogName)
                CounterEvent.NONE -> 0
                else -> -1
            }
        } catch (ignored: Exception) {
        }

        return 0
    }

    companion object {
        private const val ACTION_FETCH_COUNTER = "fetchCounter"

        fun fetchCounter(context: Context,
                         blogName: String,
                         type: Int) {
            val intent = Intent(context, CounterIntentService::class.java)
            intent.putExtra(EXTRA_TYPE, type)
            intent.putExtra(EXTRA_BLOG_NAME, blogName)
            intent.putExtra(EXTRA_ACTION, ACTION_FETCH_COUNTER)

            context.startService(intent)
        }
    }
}
