package com.ternaryop.photoshelf.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.android.editTags
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_best_of.results
import kotlinx.android.synthetic.main.fragment_best_of.tags
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.android.synthetic.main.fragment_best_of.create_post as button

private const val THREAD_POOL_SIZE = 25

class BestOfFragment : AbsPhotoShelfFragment() {
    private lateinit var executorService: ExecutorService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_best_of, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE)

        results.keyListener = null

        button.setOnClickListener {
            button.isEnabled = false
            startAddTags()
        }
    }

    private fun startAddTags() {
        results.text.clear()
        try {
            val postIds = File(context!!.filesDir, "postIdList.txt").readLines()

            require(postIds.isNotEmpty()) { "File empty" }

            val newTags = TumblrPost.tagsFromString(tags.text.toString())

            require(newTags.isNotEmpty()) { "No tags" }

            val blogName = AppSupport(context!!).selectedBlogName!!

            val d = Observable
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
            compositeDisposable.add(d)
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
            ApiManager.postService().editTags(postId, tags)
        }
        return postId
    }
}
