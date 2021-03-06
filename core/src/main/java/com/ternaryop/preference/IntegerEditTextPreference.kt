package com.ternaryop.preference

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.preference.EditTextPreference

class IntegerEditTextPreference : EditTextPreference {
    constructor(context: Context) : super(context) {
        setupEditListener()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setupEditListener()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        setupEditListener()
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        text = if (restoreValue) getPersistedString(text) else defaultValue.toString()
    }

    override fun getPersistedString(defaultReturnValue: String?): String {
        return getPersistedInt(-1).toString()
    }

    override fun persistString(value: String): Boolean {
        return persistInt(Integer.valueOf(value))
    }

    private fun setupEditListener() {
        setOnBindEditTextListener { it.inputType = InputType.TYPE_CLASS_NUMBER }
    }
}
