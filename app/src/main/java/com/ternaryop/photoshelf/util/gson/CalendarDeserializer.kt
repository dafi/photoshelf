package com.ternaryop.photoshelf.util.gson

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.ternaryop.utils.date.fromIsoFormat
import java.lang.reflect.Type
import java.util.Calendar

class CalendarDeserializer : JsonDeserializer<Calendar> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Calendar {
        return Calendar.getInstance().fromIsoFormat(json!!.asString)
    }
}