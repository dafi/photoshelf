package com.ternaryop.photoshelf.lifecycle

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class ValueHolder<T> {
    var lastValue: T? = null

    suspend fun execute(
        returnsLastValueIfAny: Boolean,
        action: suspend () -> T
    ) = if (returnsLastValueIfAny && lastValue != null) {
        Command.success(lastValue)
    } else {
        Command.execute { action() }.apply {
            if (status == Status.SUCCESS) {
                lastValue = data
            }
        }
    }
}

class CommandMutableLiveData<T> : MutableLiveData<Command<T>>() {
    private val valueHolder = ValueHolder<T>()

    suspend fun post(
        returnsLastValueIfAny: Boolean,
        action: suspend () -> T
    ): CommandMutableLiveData<T> {
        postValue(valueHolder.execute(returnsLastValueIfAny, action))
        return this
    }

    fun setLastValue(value: T?, postValue: Boolean): CommandMutableLiveData<T> {
        valueHolder.lastValue = value
        if (postValue) {
            postValue(Command.success(value))
        }
        return this
    }

    fun asLiveData(): LiveData<Command<T>> = this
}
