package com.connectycube.messenger.api

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import com.connectycube.ConnectyCube
import com.connectycube.messenger.R
import com.connectycube.messenger.utilities.SharedPreferencesManager
import com.connectycube.pushnotifications.queries.CreatePushSubscriptionParameters
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import timber.log.Timber
import java.util.UUID

class PushService {
    private object Holder {
        val INSTANCE = PushService()
    }

    companion object {
        val instance: PushService by lazy { Holder.INSTANCE }
    }

    fun subscribeToPushesIfNeed(context: Context) {
        if (!isSubscribedToPushes(context)) {
            subscribeToPushes(context)
        }
    }

    private fun isSubscribedToPushes(context: Context): Boolean {
        return getFcmRegId(context) != 0
    }

    private fun getFcmRegId(context: Context): Int {
        return SharedPreferencesManager.getInstance(context).getSubscriptionId()
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun subscribeToPushes(context: Context, token: String? = null) {
        fun getCloudMessageToken(): String? {
            val instanceID = FirebaseInstanceId.getInstance()
            val creatorToken = instanceID.getToken(
                context.getString(R.string.sender_id),
                FirebaseMessaging.INSTANCE_ID_SCOPE
            );
            return creatorToken
        }

        fun generateDeviceId(context: Context): String {
            var deviceIndustrialName = ""
            var deviceSerialNumber = ""
            var androidDeviceId = ""
            try {
                deviceSerialNumber = "" + Build.SERIAL
                deviceIndustrialName = "" + Build.DEVICE
                androidDeviceId =
                    "" + Settings.Secure.getString(context.contentResolver, "android_id")
            } catch (e: Exception) {
                Timber.d(if (!TextUtils.isEmpty(e.message)) e.message else "Error generating device id")
            }
            val deviceUuid = UUID(
                androidDeviceId.hashCode().toLong(),
                deviceIndustrialName.hashCode().toLong() shl 32 or deviceSerialNumber.hashCode()
                    .toLong()
            )
            Timber.d("subscribeToPushes DEVICE_ID = $deviceUuid")
            return deviceUuid.toString()
        }

        suspend fun subscribeToConnectyCube(pushToken: String) {
            val deviceId: String = generateDeviceId(context)
            val subscriptionParameters = CreatePushSubscriptionParameters(
                environment = "development",
                channel = "gcm", udid = deviceId, platform = "android", pushToken = pushToken
            )

            val subscriptions =
                ConnectyCube.createPushSubscription(subscriptionParameters.getRequestParameters())
            var subscriptionId = 0
            for (subscription in subscriptions) {
                if (subscription.device?.udid.equals(deviceId)) {
                    subscriptionId = subscription.id
                }
            }
            Timber.d("subscribeToConnectyCube successfully")
            SharedPreferencesManager.getInstance(context).saveSubscriptionId(subscriptionId)
        }

        GlobalScope.async(Dispatchers.IO) {
            val pushToken: String? = token ?: getCloudMessageToken()

            if (TextUtils.isEmpty(pushToken)) {
                Timber.d("Device wasn't registered")
            } else {
                Timber.d("Device registered pushToken=%s", pushToken)
                subscribeToConnectyCube(pushToken!!)
            }
        }
    }

    fun unsubscribe(context: Context) = GlobalScope.async(Dispatchers.IO) {
        if (!isSubscribedToPushes(context)) {
            Timber.d("Device isn't registered")
            return@async
        }
        val subscriptionId: Int =
            SharedPreferencesManager.getInstance(context).getSubscriptionId()
        ConnectyCube.deletePushSubscription(subscriptionId)
        FirebaseInstanceId.getInstance().deleteToken(
            context.getString(R.string.sender_id),
            FirebaseMessaging.INSTANCE_ID_SCOPE
        )
        SharedPreferencesManager.getInstance(context).deleteSubscriptionId()
    }
}