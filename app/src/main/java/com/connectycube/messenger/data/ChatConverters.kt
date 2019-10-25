package com.connectycube.messenger.data

import androidx.room.TypeConverter
import com.connectycube.chat.model.ConnectycubeDialogCustomData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

/**
 * Type converters to allow Room to reference complex data types.
 */
class ChatConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringListString(value: String?): List<String>? {
        return value?.split(", ")
    }

    @TypeConverter
    fun toStringListString(list: List<String>?): String? {
        return list?.joinToString(", ")
    }

    @TypeConverter
    fun fromIdsListString(value: String?): List<Int>? {
        return value?.let { if (value.isNotEmpty()) value.split(", ").map { it.toInt() } else emptyList() }
    }

    @TypeConverter
    fun toIdsListString(list: List<Int>?): String? {
        return list?.joinToString(", ")
    }

    @TypeConverter
    fun fromStringData(value: String?): ConnectycubeDialogCustomData? {
        val data = object : TypeToken<ConnectycubeDialogCustomData>() {

        }.type
        return Gson().fromJson(value, data)
    }

    @TypeConverter
    fun toConnectycubeDialogCustomData(data: ConnectycubeDialogCustomData?): String {
        return Gson().toJson(data)
    }
}