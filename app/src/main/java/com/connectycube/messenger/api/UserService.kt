package com.connectycube.messenger.api

import android.content.Context
import com.connectycube.messenger.ChatConnectionManager
import com.connectycube.messenger.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import com.connectycube.ConnectyCube

class UserService private constructor() {

    private object Holder {
        val INSTANCE = UserService()
    }

    companion object {
        val instance: UserService by lazy { Holder.INSTANCE }
    }

    suspend fun ultimateLogout(applicationContext: Context) {
        ChatConnectionManager.getInstance().terminate()
        clearDatabaseAsync(applicationContext).await()
        signOut()
    }

    private fun clearDatabaseAsync(applicationContext: Context) = GlobalScope.async(Dispatchers.IO) {
        AppDatabase.getInstance(applicationContext).clearTablesForLogout()
    }

    suspend fun signOut() {
        try {
            signOutAsync().await()
        } catch (ex: Exception) {
//            suppress exception
        }
    }

    private fun signOutAsync() = GlobalScope.async(Dispatchers.IO) {
        ConnectyCube.destroySession()
    }
}