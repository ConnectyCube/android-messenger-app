package com.connectycube.messenger.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.connectycube.messenger.api.*
import com.connectycube.messenger.vo.AppExecutors
import com.connectycube.messenger.vo.NetworkBoundResource
import com.connectycube.messenger.vo.Resource
import com.connectycube.users.model.ConnectycubeUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository that handles Users objects.
 */

class UserRepository private constructor(
    private val userDao: UserDao,
    private val appExecutors: AppExecutors
) {
    val service: ConnectycubeService = ConnectycubeService()

    suspend fun saveUser(user: User) {
        withContext(Dispatchers.IO) {
            userDao.insert(user)
        }
    }

    fun updateUserName(userId: Int, newName: String): LiveData<Resource<User>> {
        return object : NetworkBoundResource<User, User>(appExecutors) {
            override fun saveCallResult(item: User) {
                userDao.insert(item)
            }

            override fun shouldFetch(data: User?, newData: User?) =
                data?.conUser?.fullName != newName

            override fun loadFromDb() = userDao.getUser(userId)

            override fun createCall() = service.updateUserName(userId, newName)
        }.asLiveData()
    }

    fun updateUserAvatar(userId: Int, newAvatar: String): LiveData<Resource<ConnectycubeUser>> {
        val result = MediatorLiveData<Resource<ConnectycubeUser>>()
        result.value = Resource.loading(null)

        val apiResponse = service.updateUserAvatar(userId, newAvatar)
        result.addSource(apiResponse) { response ->
            when (response) {
                is ApiSuccessResponse -> {
                    appExecutors.diskIO().execute { userDao.insert(response.body) }
                    result.value = Resource.success(response.body.conUser)
                }
                is ApiEmptyResponse -> {
                    result.value = Resource.success(null)
                }
                is ApiProgressResponse -> {
                    result.value = Resource.loadingProgress(null, response.progress)
                }
                is ApiErrorResponse -> {
                    result.value = Resource.error(response.errorMessage, null)
                }
            }
        }
        return result
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

    fun getUsersByIds(vararg usersIds: Int): LiveData<List<User>> {
        return userDao.getUsersByIds(*usersIds)
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