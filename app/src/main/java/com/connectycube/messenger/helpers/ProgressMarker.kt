package com.connectycube.messenger.helpers

import java.util.concurrent.atomic.AtomicBoolean

class ProgressMarker {
    private val isPending = AtomicBoolean(false)

    fun isPending(): Boolean {
        return isPending.get()
    }

    fun setPending(boolean: Boolean) {
        isPending.set(boolean)
    }
}