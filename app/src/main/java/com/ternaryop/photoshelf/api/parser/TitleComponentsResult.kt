package com.ternaryop.photoshelf.api.parser

data class EventDate(val day: Int?, val month: Int?, val year: Int?)

data class TitleComponentsResult(val who: List<String>, val tags: List<String>, val html: String,
    val location: String?, val city: String?, val eventDate: EventDate?)

