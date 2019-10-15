package com.connectycube.messenger.data

import androidx.room.TypeConverter
import com.connectycube.chat.model.ConnectycubeAttachment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


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
        return Gson().toJson(attachment)
    }

    @TypeConverter
    fun fromStringAttachmentList(value: String?): Collection<ConnectycubeAttachment>? {
        val listType = object : TypeToken<Collection<ConnectycubeAttachment>>() {
        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toStringAttachmentList(chat: Collection<ConnectycubeAttachment>?): String? {
        return Gson().toJson(chat)
    }

    @TypeConverter
    fun fromIdsCollectionString(value: String?): Collection<Int>? {
        return value?.split(", ")?.map { it.toInt() }
    }

    @TypeConverter
    fun toIdsCollectionString(list: Collection<Int>?): String? {
        return list?.joinToString(", ")
    }
}