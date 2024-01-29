package com.connectycube.messenger.utilities

import android.content.Context
import com.connectycube.messenger.R
import java.lang.Long.parseLong
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

const val CREATED_AT_SIMPLE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"

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

fun getPrettyLastActivityDate(context: Context, date: Date): String {
    val lastRequestAt = Calendar.getInstance().apply { time = date }
    val currentDate = Calendar.getInstance().apply { time = Date() }

    val lastRequestAtDay = lastRequestAt.get(Calendar.DAY_OF_YEAR)
    val currentDay = currentDate.get(Calendar.DAY_OF_YEAR)

    return when (currentDay - lastRequestAtDay) {
        0 -> context.getString(R.string.today) + " at " + DateFormat.getTimeInstance(DateFormat.SHORT).format(
            date
        )
        1 -> context.getString(R.string.yesterday) + " at " + DateFormat.getTimeInstance(DateFormat.SHORT).format(
            date
        )
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(date)
    }
}

fun getDateAsHeaderId(milliseconds: Long): Long {
    val dateFormat = SimpleDateFormat("ddMMyyyy", Locale.getDefault())
    return parseLong(dateFormat.format(Date(milliseconds)))
}

fun getPrettyMessageDate(context: Context, milliseconds: Long): String {
    val messageDate = Calendar.getInstance().apply { time = Date(milliseconds) }
    val currentDate = Calendar.getInstance().apply { time = Date() }

    val messageDay = messageDate.get(Calendar.DAY_OF_YEAR)
    val currentDay = currentDate.get(Calendar.DAY_OF_YEAR)

    return when (currentDay - messageDay) {
        0 -> context.getString(R.string.today)
        1 -> context.getString(R.string.yesterday)
        else -> DateFormat.getDateInstance(DateFormat.SHORT).format(milliseconds)
    }
}

