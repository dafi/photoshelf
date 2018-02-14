package com.ternaryop.photoshelf.db

import android.provider.BaseColumns
import java.io.Serializable
import java.util.Locale

class Blog(var id: Long, name: String) : BaseColumns, Serializable {
    var name: String = name.toLowerCase(Locale.US)
    set(value) {
        field = name.toLowerCase(Locale.US)
    }

    override fun toString(): String = name

    companion object {
        private const val serialVersionUID = 5887665998065237345L
    }
}