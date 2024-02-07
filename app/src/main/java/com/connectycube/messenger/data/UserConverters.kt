package com.connectycube.messenger.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.connectycube.users.models.ConnectycubeUser

/**
 * Type converters to allow Room to reference complex data types.
 */
class UserConverters {
    @TypeConverter
    fun fromString(value: String): ConnectycubeUser {
        val user = object : TypeToken<ConnectycubeUser>() {

        }.type
        return Gson().fromJson(value, user)
    }

    @TypeConverter
    fun fromConnectycubeUser(user: ConnectycubeUser): String {
        val gson = Gson()
        return gson.toJson(user)
    }
}