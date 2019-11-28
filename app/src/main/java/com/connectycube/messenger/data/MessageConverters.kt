package com.connectycube.messenger.data

import androidx.room.TypeConverter


/**
 * Type converters to allow Room to reference complex data types.
 */

class MessageConverters {

    @TypeConverter
    fun fromIdsCollectionString(value: String?): Collection<Int>? {
        return value?.split(", ")?.map { it.toInt() }
    }

    @TypeConverter
    fun toIdsCollectionString(list: Collection<Int>?): String? {
        return list?.joinToString(", ")
    }
}