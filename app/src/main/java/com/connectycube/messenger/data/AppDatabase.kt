package com.connectycube.messenger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.connectycube.messenger.utilities.DATABASE_NAME
import com.connectycube.messenger.workers.ChatDatabaseWorker
import timber.log.Timber

/**
 * The Room database for this app
 */
@Database(entities = [User::class, Chat::class, Message::class, Attachment::class], version = 2, exportSchema = false)
@TypeConverters(UserConverters::class, ChatConverters::class, MessageConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun messageWithAttachmentDao(): MessageWithAttachmentsDao

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            Timber.d("buildDatabase start")
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        Timber.d("buildDatabase created")
                        val request = OneTimeWorkRequestBuilder<ChatDatabaseWorker>().build()
                        WorkManager.getInstance(context).enqueue(request)
                    }
                })
                .build()
        }
    }

    fun clearTablesForLogout() {
        chatDao().nukeTable()
        messageDao().nukeTable()
        attachmentDao().nukeTable()
    }
}