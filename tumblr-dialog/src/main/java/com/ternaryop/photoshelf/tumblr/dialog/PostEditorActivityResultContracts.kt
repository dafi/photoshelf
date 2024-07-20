package com.ternaryop.photoshelf.tumblr.dialog

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.ternaryop.compat.os.getSerializableCompat

object PostEditorActivityResultContracts {
    class New constructor(
        private val tumblrPostDialog: TumblrPostDialog
    ) : ActivityResultContract<NewPostEditorData, NewPostEditorResult?>() {
        override fun createIntent(context: Context, input: NewPostEditorData): Intent {
            return tumblrPostDialog.newPostEditorIntent(context, input)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): NewPostEditorResult? {
            if (resultCode == Activity.RESULT_OK) {
                return intent?.extras?.getSerializableCompat(
                    TumblrPostDialog.ARG_RESULT,
                    NewPostEditorResult::class.java
                )
            }
            return null
        }
    }

    class Edit constructor(
        private val tumblrPostDialog: TumblrPostDialog
    ) : ActivityResultContract<EditPostEditorData, PostEditorResult?>() {
        override fun createIntent(context: Context, input: EditPostEditorData): Intent {
            return tumblrPostDialog.editPostEditorIntent(context, input)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): PostEditorResult? {
            if (resultCode == Activity.RESULT_OK) {
                return intent?.extras?.getSerializableCompat(TumblrPostDialog.ARG_RESULT, PostEditorResult::class.java)
            }
            return null
        }
    }
}
