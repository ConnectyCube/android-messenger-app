package com.connectycube.messenger.data

import androidx.room.TypeConverter
import com.connectycube.chat.model.ConnectycubeAttachment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*


/**
 * Type converters to allow Room to reference complex data types.
 */

class MessageConverters {
    @TypeConverter
    fun toStringAttachment(value: String): ConnectycubeAttachment {
        return Gson().fromJson(value, ConnectycubeAttachment::class.java)
    }

    @TypeConverter
    fun fromStringAttachment(attachment: ConnectycubeAttachment): String? {
        val gson = Gson()
        return gson.toJson(attachment)
    }

    @TypeConverter
    fun fromStringAttachmentList(value: String?): Collection<ConnectycubeAttachment>? {
        val listType = object : TypeToken<Collection<ConnectycubeAttachment>>() {
        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toStringAttachmentList(chat: Collection<ConnectycubeAttachment>?): String? {
        val gson = Gson()
        return gson.toJson(chat)
    }

    @TypeConverter
    fun fromIdsCollectionString(value: String): Collection<Int>? {
        val listType = object : TypeToken<Collection<Int>>() {
        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toIdsCollectionString(list: Collection<Int>?): String? {
        val gson = Gson()
        return gson.toJson(list)
    }
}