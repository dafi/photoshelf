package com.ternaryop.photoshelf.misspelled

interface MisspelledName {
    suspend fun getMisspelledInfo(name: String): Info

    sealed class Info(val name: String) {
        class AlreadyExists(name: String) : Info(name)
        class NotFound(name: String) : Info(name)
        class Corrected(name: String, val misspelledName: String) : Info(name)
    }
}
