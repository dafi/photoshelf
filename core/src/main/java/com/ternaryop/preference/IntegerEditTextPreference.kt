package com.ternaryop.preference

import android.content.Context
import android.content.res.TypedArray
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

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        setupEditListener()
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        // ensure the value type is Int instead of String
        return a.getInt(index, 0)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        text = getPersistedInt(defaultValue as Int? ?: 0).toString()
    }

    override fun getPersistedString(defaultReturnValue: String?): String {
        return getPersistedInt(0).toString()
    }

    override fun persistString(value: String): Boolean {
        return persistInt(Integer.valueOf(value))
    }

    private fun setupEditListener() {
        setOnBindEditTextListener { it.inputType = InputType.TYPE_CLASS_NUMBER }
    }
}
