package com.ternaryop.photoshelf.fragment

import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.util.network.ApiManager
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.android.editTags
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val THREAD_POOL_SIZE = 25

class BestOfFragment : Fragment() {
    lateinit var tags: EditText
    lateinit var results: EditText
    lateinit var button: Button
    private lateinit var executorService: ExecutorService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_best_of, container, false)

        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE)

        tags = view.findViewById(R.id.tags)
        results = view.findViewById(R.id.results)
        results.keyListener = null

        button = view.findViewById(R.id.create_post)
        button.setOnClickListener({
            button.isEnabled = false
            startAddTags()
        })

        return view
    }

    private fun startAddTags() {
        results.text.clear()
        try {
            val postIds = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "postIdList.txt").readLines()

            require(postIds.isNotEmpty()) { "File empty" }

            val newTags = TumblrPost.tagsFromString(tags.text.toString())

            require(newTags.isNotEmpty()) { "No tags" }

            val blogName = AppSupport(context!!).selectedBlogName!!

            Observable
                .fromIterable(postIds)
                .flatMap { postId ->
                    Observable
                        .fromCallable { addTags(postId.toLong(), newTags, blogName) }
                        .subscribeOn(Schedulers.from(executorService))
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally {
                    appendLine("Finished")
                    button.isEnabled = true
                }
                .subscribe({ appendLine("done $it")
                }, { appendLine(it.message!!) })
        } catch (e: Exception) {
            appendLine(e.message!!)
            button.isEnabled = true
        }
    }

    private fun appendLine(str: String) {
        results.append(str + "\n")
    }

    private fun addTags(postId: Long, newTags: List<String>, blogName: String): Long {
        context?.let { context ->
            val tags = TumblrManager.getInstance(context).editTags(postId, blogName, newTags) ?: return postId
            ApiManager.postManager(context).editTags(postId, tags)
        }
        return postId
    }
}
