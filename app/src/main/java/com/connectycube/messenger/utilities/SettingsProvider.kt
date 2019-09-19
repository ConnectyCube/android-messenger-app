package com.connectycube.messenger.utilities

import android.content.Context
import com.connectycube.auth.session.ConnectycubeSettings
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.chat.connections.tcp.TcpChatConnectionFabric
import com.connectycube.chat.connections.tcp.TcpConfigurationBuilder
import com.connectycube.core.LogLevel
import com.connectycube.messenger.R
import com.connectycube.users.model.ConnectycubeUser

object SettingsProvider {

    // ConnectyCube Application credentials
    //
    private val applicationID = ""
    private val authKey = ""
    private val authSecret = ""
    private val accountKey = ""

    fun initConnectycubeCredentials(applicationContext: Context) {
        checkConfigJson(applicationContext)
        checkUserJson(applicationContext)
        initCredentials(applicationContext)
    }

    private fun initCredentials(applicationContext: Context) {
        ConnectycubeSettings.getInstance().init(applicationContext, applicationID, authKey, authSecret)
        ConnectycubeSettings.getInstance().accountKey = accountKey

        // Uncomment and put your Api and Chat servers endpoints if you want to point the sample
        // against your own server.
        //
//        ConnectycubeSettings.getInstance().setEndpoints("https://your_api_endpoint.com", "your_chat_endpoint", ServiceZone.PRODUCTION);
//        ConnectycubeSettings.getInstance().setZone(ServiceZone.PRODUCTION)
    }

    private fun checkConfigJson(applicationContext: Context) {
        if (applicationID.isEmpty() || authKey.isEmpty() || authSecret.isEmpty()) {
            throw AssertionError(applicationContext.getString(R.string.error_credentials_empty))
        }
    }

    private fun checkUserJson(applicationContext: Context) {
        val users = getAllUsersFromFile(SAMPLE_CONFIG_FILE_NAME, applicationContext)
        if (users.size !in 2..5 || isUsersEmpty(users))
            throw AssertionError(applicationContext.getString(R.string.error_users_empty))
    }

    private fun isUsersEmpty(users: ArrayList<ConnectycubeUser>): Boolean {
        users.forEach { user -> if (user.login.isBlank() || user.password.isBlank()) return true }
        return false
    }

    fun initChatConfiguration() {
        ConnectycubeSettings.getInstance().logLevel = LogLevel.DEBUG
        ConnectycubeChatService.setDebugEnabled(true)
        ConnectycubeChatService.setDefaultConnectionTimeout(30000)
        ConnectycubeChatService.getInstance().setUseStreamManagement(true)

        val builder = TcpConfigurationBuilder()
            .setAllowListenNetwork(true)
            .setUseStreamManagement(true)

        ConnectycubeChatService.setConnectionFabric(TcpChatConnectionFabric(builder.apply { socketTimeout = 0 }))

    }
}