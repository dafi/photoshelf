package com.ternaryop.photoshelf.db

import android.provider.BaseColumns
import java.io.Serializable
import java.util.Locale

class Tag(name: String) : BaseColumns, Serializable {
    var id: Long = 0
    var name: String = name.toLowerCase(Locale.US)
    set(value) {
        field = value.toLowerCase(Locale.US)
    }

    companion object {
        private const val serialVersionUID = 5887665998065237345L
    }
}