package com.connectycube.messenger.data

import androidx.lifecycle.LiveData
import com.connectycube.messenger.api.ConnectycubeService
import com.connectycube.messenger.vo.AppExecutors
import com.connectycube.messenger.vo.NetworkBoundResource
import com.connectycube.messenger.vo.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository that handles Users objects.
 */

class UserRepository private constructor(private val userDao: UserDao, private val appExecutors: AppExecutors) {
    val service: ConnectycubeService = ConnectycubeService()

    suspend fun saveUser(user: User) {
        withContext(Dispatchers.IO) {
            userDao.insert(user)
        }
    }

    fun getUser(userId: Int) = userDao.getUser(userId)

    fun loadUsers(usersLogins: ArrayList<String>): LiveData<Resource<List<User>>> {
        return object : NetworkBoundResource<List<User>, List<User>>(appExecutors) {
            override fun saveCallResult(item: List<User>) {
                val listOldUsers = userDao.getUsersSync()
                item.forEach { user ->
                    listOldUsers.forEach { oldUser ->
                        if (user.login == oldUser.login) {
                            user.conUser.password = oldUser.conUser.password
                        }
                    }
                }
                userDao.insertAll(item)
            }

            override fun shouldFetch(data: List<User>?, newData: List<User>?) = data.isNullOrEmpty()

            override fun loadFromDb() = userDao.getUsers()

            override fun createCall() = service.loadUsers(usersLogins)
        }.asLiveData()
    }

    fun getUsers(): LiveData<List<User>> {
        return userDao.getUsers()
    }

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: UserRepository? = null

        fun getInstance(userDao: UserDao) =
            instance ?: synchronized(this) {
                instance ?: UserRepository(userDao, AppExecutors()).also { instance = it }
            }
    }
}