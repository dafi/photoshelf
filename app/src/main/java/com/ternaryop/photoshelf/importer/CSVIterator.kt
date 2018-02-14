package com.ternaryop.photoshelf.importer

import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.text.ParseException

class CSVIterator<T> @Throws(IOException::class)
constructor(fis: FileInputStream, private val builder: CSVBuilder<T>) : Iterator<T> {
    private val bufferedReader = BufferedReader(InputStreamReader(fis))
    private var line: String? = null

    @Throws(IOException::class)
    constructor(importPath: String, builder: CSVBuilder<T>) : this(FileInputStream(importPath), builder)

    init {
        line = bufferedReader.readLine()
    }

    override fun hasNext(): Boolean {
        return line != null
    }

    override fun next(): T {
        try {
            val fields = line!!.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val result = builder.parseCSVFields(fields)
            line = bufferedReader.readLine()
            return result
        } catch (e: Exception) {
            throw NoSuchElementException(e.message)
        }
    }

    fun remove() {
        throw UnsupportedOperationException()
    }

    interface CSVBuilder<out T> {
        @Throws(ParseException::class)
        fun parseCSVFields(fields: Array<String>): T
    }
}
