package com.connectycube.messenger.utilities

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.connectycube.users.models.ConnectycubeUser
import java.io.BufferedReader

fun getAllUsersFromFile(filename: String, context: Context): ArrayList<ConnectycubeUser> {
    val jsonInputStream = context.assets.open(filename)
    val jsonUsers = jsonInputStream.bufferedReader().use(BufferedReader::readText)
    jsonInputStream.close()

    val userType = object : TypeToken<Map<String, Map<String, Int>>>() {}.type
    val userMap: Map<String, Map<String, Int>> = Gson().fromJson(jsonUsers, userType)

    val users = ArrayList<ConnectycubeUser>()
    userMap.forEach { (login, mapPassword) -> users.add(ConnectycubeUser(login = login, password = mapPassword.keys.elementAt(0))) }
    return users
}