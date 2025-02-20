package com.connectycube.messenger.utilities

import android.content.Context
import com.connectycube.messenger.R
import com.connectycube.ConnectyCubeAndroid
import com.connectycube.core.models.ConnectycubeSettings
import com.connectycube.users.models.ConnectycubeUser

object SettingsProvider {

    // ConnectyCube Application credentials
    //
    private val appID = "8160"
    private val authKey = "89096191-4555-42B6-BE6D-021F22DD86B5"

    fun initConnectycubeCredentials(applicationContext: Context) {
        checkConfigJson(applicationContext)
        checkUserJson(applicationContext)
        initCredentials(applicationContext)
    }

    private fun initCredentials(applicationContext: Context) {
        ConnectyCubeAndroid.init(appID, authKey, context = applicationContext)
        ConnectycubeSettings.isDebugEnabled = true

        // Uncomment and put your Api and Chat servers endpoints if you want to point the sample
        // against your own server.
//        val connectycubeConfig: ConnectycubeConfig = ConnectycubeConfig("https://your_api_endpoint.com", "your_chat_endpoint")
//        ConnectyCubeAndroid.init(appID, authKey, connectycubeConfig, context = applicationContext)
    }

    private fun checkConfigJson(applicationContext: Context) {
        if (appID.isEmpty() || authKey.isEmpty()) {
            throw AssertionError(applicationContext.getString(R.string.error_credentials_empty))
        }
    }

    private fun checkUserJson(applicationContext: Context) {
        val users = getAllUsersFromFile(SAMPLE_CONFIG_FILE_NAME, applicationContext)
        if (users.size !in 2..5 || isUsersEmpty(users))
            throw AssertionError(applicationContext.getString(R.string.error_users_empty))
    }

    private fun isUsersEmpty(users: ArrayList<ConnectycubeUser>): Boolean {
        users.forEach { user -> if (user.login!!.isBlank() || user.password!!.isBlank()) return true }
        return false
    }

    fun initChatConfiguration() {
//        ConnectyCube.chat.enableLogging()
    }
}