package com.ternaryop.photoshelf.lifecycle

enum class Status {
    SUCCESS,
    ERROR,
    PROGRESS
}

data class ProgressData(val step: Int, val itemCount: Int)

data class Command<out T>(
    val status: Status,
    val data: T? = null,
    val error: Throwable? = null,
    val progressData: ProgressData? = null
) {
    companion object {
        fun <T> success(data: T?) = Command(Status.SUCCESS, data)
        fun <T> error(error: Throwable, data: T? = null) = Command(Status.ERROR, data, error = error)
        fun <T> progress(progressData: ProgressData) = Command<T>(Status.PROGRESS, progressData = progressData)

        suspend fun <T> execute(action: suspend () -> T) =
            try {
                success(action())
            } catch (expected: Throwable) {
                error<T>(expected)
            }
    }
}
