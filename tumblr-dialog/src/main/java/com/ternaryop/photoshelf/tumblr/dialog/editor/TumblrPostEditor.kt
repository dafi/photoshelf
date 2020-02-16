package com.ternaryop.photoshelf.tumblr.dialog.editor

import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBar
import com.ternaryop.photoshelf.mru.adapter.MRUHolder
import com.ternaryop.photoshelf.tumblr.dialog.PostEditorResult
import com.ternaryop.photoshelf.tumblr.dialog.editor.viewholder.TagsHolder
import com.ternaryop.photoshelf.tumblr.dialog.editor.viewholder.TitleHolder
import com.ternaryop.tumblr.TumblrPost

interface TumblrPostEditor {
    fun setupUI(actionBar: ActionBar?, view: View)
    fun onPrepareMenu(menu: Menu)
    fun canExecute(item: MenuItem): Boolean
    fun execute(item: MenuItem): PostEditorResult?
}

abstract class AbsTumblrPostEditor(
    val titleHolder: TitleHolder,
    val tagsHolder: TagsHolder,
    val mruHolder: MRUHolder
) : TumblrPostEditor {

    fun updateMruList() {
        mruHolder.updateMruList(TumblrPost.tagsFromString(tagsHolder.tags).drop(1))
    }
}
