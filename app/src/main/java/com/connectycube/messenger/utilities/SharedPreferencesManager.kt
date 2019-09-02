package com.connectycube.messenger.utilities

import android.content.Context
import android.content.SharedPreferences
import com.connectycube.users.model.ConnectycubeUser

const val CUBE_SHARED_PREFERENCES_NAME = "connectycube_messenger"
const val CUBE_USER_LOGIN = "cube_user_login"
const val CUBE_USER_PASSWORD = "cube_user_password"
const val CUBE_USER_ID = "cube_user_id"
const val CUBE_USER_NAME = "cube_user_name"
const val CUBE_USER_AVATAR = "cube_user_avatar"

class SharedPreferencesManager private constructor(val applicationContext: Context) {

    private var sharedPreferences: SharedPreferences =
        applicationContext.getSharedPreferences(CUBE_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)


    companion object {
        private var instance: SharedPreferencesManager? = null

        fun getInstance(applicationContext: Context): SharedPreferencesManager {
            if (instance == null)
                instance = SharedPreferencesManager(applicationContext)

            return instance!!
        }
    }

    fun saveCurrentUser(user: ConnectycubeUser) {
        val editor = sharedPreferences.edit()
        editor.putString(CUBE_USER_LOGIN, user.login)
        editor.putString(CUBE_USER_PASSWORD, user.password)
        editor.putInt(CUBE_USER_ID, user.id)
        editor.putString(CUBE_USER_NAME, user.fullName)
        editor.putString(CUBE_USER_AVATAR, user.avatar)
        editor.apply()
    }

    fun deleteCurrentUser() {
        val editor = sharedPreferences.edit()
        editor.remove(CUBE_USER_LOGIN)
        editor.remove(CUBE_USER_PASSWORD)
        editor.remove(CUBE_USER_ID)
        editor.remove(CUBE_USER_NAME)
        editor.remove(CUBE_USER_AVATAR)
        editor.apply()
    }

    fun updateCurrentUserName(user: ConnectycubeUser) {
        val editor = sharedPreferences.edit()
        editor.putString(CUBE_USER_NAME, user.fullName)
        editor.apply()
    }

    fun updateCurrentUserAvatar(user: ConnectycubeUser) {
        val editor = sharedPreferences.edit()
        editor.putString(CUBE_USER_AVATAR, user.avatar)
        editor.apply()
    }

    fun currentUserExists(): Boolean {
        return sharedPreferences.contains(CUBE_USER_LOGIN)
    }

    fun getCurrentUser(): ConnectycubeUser {
        val currentUser = ConnectycubeUser()
        if (currentUserExists()) {
            currentUser.let {
                it.login = sharedPreferences.getString(CUBE_USER_LOGIN, null)
                it.password = sharedPreferences.getString(CUBE_USER_PASSWORD, null)
                it.id = sharedPreferences.getInt(CUBE_USER_ID, 0)
                it.fullName = sharedPreferences.getString(CUBE_USER_NAME, null)
                it.avatar = sharedPreferences.getString(CUBE_USER_AVATAR, null)
            }
        }
        return currentUser
    }
}