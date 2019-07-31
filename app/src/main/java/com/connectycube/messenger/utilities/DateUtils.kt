package com.connectycube.messenger.utilities

import android.content.Context
import com.connectycube.messenger.R
import java.text.DateFormat
import java.util.*

fun getPrettyDate(context: Context, date: Long): String {
    val messageDate = Calendar.getInstance().apply { time = Date(date) }
    val currentDate = Calendar.getInstance().apply { time = Date() }

    val messageDay = messageDate.get(Calendar.DAY_OF_YEAR)
    val currentDay = currentDate.get(Calendar.DAY_OF_YEAR)

    return when (currentDay - messageDay) {
        0 -> DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(date))
        1 -> context.getString(R.string.yesterday)
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(date)
    }
}
