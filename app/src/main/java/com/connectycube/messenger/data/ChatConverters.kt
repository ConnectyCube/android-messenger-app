package com.connectycube.messenger.data

import androidx.room.TypeConverter
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Type converters to allow Room to reference complex data types.
 */
class ChatConverters {
    @TypeConverter
    fun fromString(value: String): ConnectycubeChatDialog {
        val chat = object : TypeToken<ConnectycubeChatDialog>() {

        }.type
        return Gson().fromJson(value, chat)
    }

    @TypeConverter
    fun fromConnectycubeChat(chat: ConnectycubeChatDialog): String {
        val gson = Gson()
        return gson.toJson(chat)
    }
}