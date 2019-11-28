package com.connectycube.messenger.utilities

import android.os.SystemClock
import android.view.View


/**
 * The class preventing multiple times clicking in a row with predefined interval.
 */
class SingleClickListener(
    private var defaultInterval: Int = 1000,
    private val onSingleClick: (View) -> Unit
) : View.OnClickListener {
    private var lastTimeClicked: Long = 0
    override fun onClick(v: View) {
        if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
            return
        }
        lastTimeClicked = SystemClock.elapsedRealtime()
        onSingleClick(v)
    }
}

fun View.setSingleOnClickListener(onSingleClick: (View) -> Unit): Boolean {
    val singleTimeClickListener = SingleClickListener {
        onSingleClick(it)
    }
    setOnClickListener(singleTimeClickListener)
    return true
}