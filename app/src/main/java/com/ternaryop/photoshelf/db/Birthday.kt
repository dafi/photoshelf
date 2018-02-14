package com.ternaryop.photoshelf.db

import android.provider.BaseColumns
import com.ternaryop.photoshelf.util.date.year
import java.io.Serializable
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Birthday : BaseColumns, Serializable {
    var id: Long = 0
    var name: String
    set(value) {
        field = value.toLowerCase(Locale.US)
    }
    var birthDate: Calendar? = null
    var tumblrName: String

    val age: Int
        get() = Calendar.getInstance().year - birthYear

    val birthYear: Int
        get() = birthDate?.year ?: 0

    constructor(name: String, birthDate: Calendar?, tumblrName: String) {
        this.tumblrName = tumblrName
        this.name = name.toLowerCase(Locale.US)
        this.birthDate = birthDate
    }

    @Throws(ParseException::class)
    constructor(name: String, birthDate: String?, tumblrName: String) :
            this(name, if (birthDate == null || birthDate.isBlank()) null else fromIsoFormat(birthDate), tumblrName)

    override fun toString(): String {
        return (if (birthDate == null) "" else ISO_DATE_FORMAT.format(birthDate!!.time) + " ") + name
    }

    companion object {
        private const val serialVersionUID = 5887665998065237345L

        private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        fun fromIsoFormat(isoDateString: String): Calendar {
            val c = Calendar.getInstance()
            c.time = ISO_DATE_FORMAT.parse(isoDateString)
            return c
        }

        fun toIsoFormat(date: Calendar): String = ISO_DATE_FORMAT.format(date.time)
    }
}