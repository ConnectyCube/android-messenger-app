package com.connectycube.messenger.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.connectycube.users.models.ConnectycubeUser

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    @ColumnInfo(name = "id") val userId: Int,
    val login: String,
    val name: String,
    val conUser: ConnectycubeUser
) {
    override fun toString() = login
}