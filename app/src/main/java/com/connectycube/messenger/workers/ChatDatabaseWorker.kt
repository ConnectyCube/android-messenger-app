package com.connectycube.messenger.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.connectycube.messenger.data.AppDatabase
import com.connectycube.messenger.data.User
import com.connectycube.messenger.utilities.SAMPLE_CONFIG_FILE_NAME
import com.connectycube.users.model.ConnectycubeUser
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

class ChatDatabaseWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val TAG by lazy { ChatDatabaseWorker::class.java.simpleName }

    override suspend fun doWork(): Result = coroutineScope {

        try {
            applicationContext.assets.open(SAMPLE_CONFIG_FILE_NAME).use { inputStream ->
                JsonReader(inputStream.reader()).use { jsonReader ->
                    val userType = object : TypeToken<Map<String, Map<String, Int>>>() {}.type
                    val userMap: Map<String, Map<String, Int>> = Gson().fromJson(jsonReader, userType)
                    val userList = ArrayList<User>()
                    userMap.forEach { (login, mapPassword) ->
                        userList.add(
                            User(
                                mapPassword[login]!!,
                                login,
                                "",
                                ConnectycubeUser(login, mapPassword.keys.elementAt(0)).apply { id = mapPassword[login]}
                            )
                        )
                    }
                    val database = AppDatabase.getInstance(applicationContext)
                    database.userDao().insertAll(userList)
                    Result.success()
                }
            }
        } catch (ex: Exception) {
            Timber.e("Error seeding database", ex)
            Result.failure()
        }
    }
}