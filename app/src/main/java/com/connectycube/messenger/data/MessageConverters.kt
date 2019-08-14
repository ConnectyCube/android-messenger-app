package com.connectycube.messenger.data

import androidx.room.TypeConverter
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Type converters to allow Room to reference complex data types.
 */
class MessageConverters {
    @TypeConverter
    fun fromString(value: String): ConnectycubeChatMessage {
        val chat = object : TypeToken<ConnectycubeChatMessage>() {

        }.type
        return Gson().fromJson(value, chat)
    }

    @TypeConverter
    fun fromConnectycubeChat(chat: ConnectycubeChatMessage): String {
        val gson = Gson()
        return gson.toJson(chat)
    }
}