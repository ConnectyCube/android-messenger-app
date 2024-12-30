package com.connectycube.messenger.utilities

import android.content.Context
import com.connectycube.messenger.R
import com.connectycube.ConnectyCubeAndroid
import com.connectycube.core.models.ConnectycubeSettings
import com.connectycube.users.models.ConnectycubeUser

object SettingsProvider {

    // ConnectyCube Application credentials
    //
    private val applicationId = "REPLACE_APP_ID"
    private val authKey = "REPLACE_APP_AUTH_KEY"

    fun initConnectycubeCredentials(applicationContext: Context) {
        checkConfigJson(applicationContext)
        checkUserJson(applicationContext)
        initCredentials(applicationContext)
    }

    private fun initCredentials(applicationContext: Context) {
        ConnectyCubeAndroid.init(applicationId, authKey, context = applicationContext)
        ConnectycubeSettings.isDebugEnabled = true

        // Uncomment and put your Api and Chat servers endpoints if you want to point the sample
        // against your own server.
//        val connectycubeConfig: ConnectycubeConfig = ConnectycubeConfig("https://your_api_endpoint.com", "your_chat_endpoint")
//        ConnectyCubeAndroid.init(applicationId, authKey, connectycubeConfig, context = applicationContext)
    }

    private fun checkConfigJson(applicationContext: Context) {
        if (applicationId.isEmpty() || authKey.isEmpty()) {
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