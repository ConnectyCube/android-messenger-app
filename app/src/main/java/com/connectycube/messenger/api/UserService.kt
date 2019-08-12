package com.connectycube.messenger.api

import android.content.Context
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.core.exception.ResponseException
import com.connectycube.messenger.data.AppDatabase
import com.connectycube.users.ConnectycubeUsers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class UserService private constructor() {

    private object Holder {
        val INSTANCE = UserService()
    }

    companion object {
        val instance: UserService by lazy { Holder.INSTANCE }
    }

    suspend fun ultimateLogout(applicationContext: Context) {
        ConnectycubeChatService.getInstance().destroy()
        clearDatabaseAsync(applicationContext).await()
        signOut()
    }

    private fun clearDatabaseAsync(applicationContext: Context) = GlobalScope.async(Dispatchers.IO) {
        AppDatabase.getInstance(applicationContext).clearTablesForLogout()
    }

    suspend fun signOut() {
        try {
            signOutAsync().await()
        } catch (ex: ResponseException) {
//            suppress exception
        }
    }

    private fun signOutAsync() = GlobalScope.async(Dispatchers.IO) {
        ConnectycubeUsers.signOut().perform()
    }
}