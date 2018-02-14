package com.ternaryop.preference

import android.content.Context
import android.preference.EditTextPreference
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import android.widget.EditText

class IntegerEditTextPreference : EditTextPreference {

    constructor(context: Context) : super(context) {
        editText.inputType = InputType.TYPE_CLASS_NUMBER
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        editText.inputType = InputType.TYPE_CLASS_NUMBER
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        editText.inputType = InputType.TYPE_CLASS_NUMBER
    }

    override fun onAddEditTextToDialogView(dialogView: View, editText: EditText) {
        super.onAddEditTextToDialogView(dialogView, editText)
        editText.selectAll()
    }

    override fun getPersistedString(defaultReturnValue: String?): String {
        return getPersistedInt(-1).toString()
    }

    override fun persistString(value: String): Boolean {
        return persistInt(Integer.valueOf(value)!!)
    }
}