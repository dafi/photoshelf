package com.ternaryop.preference

import android.content.Context
import android.util.AttributeSet
import com.takisoft.preferencex.EditTextPreference

class IntegerEditTextPreference : EditTextPreference {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        text = if (restoreValue) getPersistedString(text) else defaultValue.toString()
    }

    override fun getPersistedString(defaultReturnValue: String?): String {
        return getPersistedInt(-1).toString()
    }

    override fun persistString(value: String): Boolean {
        return persistInt(Integer.valueOf(value))
    }
}