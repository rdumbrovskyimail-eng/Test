package com.test.data.repository

import com.test.network.ApiClient
import com.test.network.NetworkResult
import com.test.network.UserApi
import com.test.network.UserDto
import com.test.network.CreateUserRequest
import com.test.network.UpdateUserRequest
import com.test.network.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val role: UserRole,
    val isActive: Boolean
)

enum class UserRole { Admin, Moderator, User, Guest }

fun UserDto.toDomain() = User(
    id = id,
    name = name,
    email = email,
    avatarUrl = avatarUrl,
    role = when (role) {
        "admin" -> UserRole.Admin
        "moderator" -> UserRole.Moderator
        "user" -> UserRole.User
        else -> UserRole.Guest
    },
    isActive = isActive
)

interface UserRepository {
    suspend fun getUser(id: String): NetworkResult<User>
    suspend fun getUsers(page: Int, limit: Int): NetworkResult<List<User>>
    suspend fun createUser(name: String, email: String, password: String): NetworkResult<User>
    suspend fun updateUser(id: String, name: String?, email: String?): NetworkResult<User>
    suspend fun deleteUser(id: String): NetworkResult<Unit>
    fun observeUsers(page: Int, limit: Int): Flow<NetworkResult<List<User>>>
    suspend fun searchUsers(query: String): NetworkResult<List<User>>
    suspend fun getUsersByRole(role: UserRole): NetworkResult<List<User>>
    fun getUserStream(id: String): Flow<NetworkResult<User>>
    fun getUserCount(): Int
    suspend fun updateUserAvatar(id: String, avatarUrl: String): NetworkResult<User>
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val api: UserApi,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : UserRepository {

    private val cache = mutableMapOf<String, User>()
    private val listCache = mutableMapOf<String, List<User>>()

    override suspend fun getUser(id: String): NetworkResult<User> = withContext(dispatcher) {
        cache[id]?.let { return@withContext NetworkResult.Success(it) }
        val result = safeApiCall { api.getUser(id).toDomain() }
        if (result is NetworkResult.Success) {
            cache[id] = result.value
        }
        result
    }

    override suspend fun getUsers(page: Int, limit: Int): NetworkResult<List<User>> =
        withContext(dispatcher) {
            val key = "page_${page}_limit_$limit"
            listCache[key]?.let { return@withContext NetworkResult.Success(it) }
            val result = safeApiCall { api.getUsers(page, limit).map { it.toDomain() } }
            if (result is NetworkResult.Success) {
                listCache[key] = result.value
                result.value.forEach { cache[it.id] = it }
            }
            result
        }

    override suspend fun createUser(
        name: String,
        email: String,
        password: String
    ): NetworkResult<User> = withContext(dispatcher) {
        val result = safeApiCall {
            api.createUser(CreateUserRequest(name, email, password)).toDomain()
        }
        if (result is NetworkResult.Success) {
            cache[result.value.id] = result.value
            clearListCache()
        }
        result
    }

    override suspend fun updateUser(
        id: String,
        name: String?,
        email: String?
    ): NetworkResult<User> = withContext(dispatcher) {
        val result = safeApiCall {
            api.updateUser(id, UpdateUserRequest(name = name, email = email)).toDomain()
        }
        if (result is NetworkResult.Success) {
            cache[id] = result.value
        }
        result
    }

    override suspend fun deleteUser(id: String): NetworkResult<Unit> = withContext(dispatcher) {
        val result = safeApiCall { api.deleteUser(id) }
        if (result is NetworkResult.Success) {
            cache.remove(id)
            clearListCache()
        }
        result.let { if (it is NetworkResult.Error) it else NetworkResult.Success(Unit) }
    }

    override fun observeUsers(page: Int, limit: Int): Flow<NetworkResult<List<User>>> = flow {
        emit(NetworkResult.Loading)
        emit(getUsers(page, limit))
    }.flowOn(dispatcher)

    override suspend fun searchUsers(query: String): NetworkResult<List<User>> =
        withContext(dispatcher) {
            val cached = cache.values.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.email.contains(query, ignoreCase = true)
            }
            if (cached.isNotEmpty()) {
                return@withContext NetworkResult.Success(cached)
            }
            safeApiCall {
                api.getUsers(1, 100).map { it.toDomain() }.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true)
                }
            }
        }

    override suspend fun getUsersByRole(role: UserRole): NetworkResult<List<User>> =
        withContext(dispatcher) {
            safeApiCall {
                api.getUsers(1, 100).map { it.toDomain() }.filter { it.role == role }
            }
        }

    override fun getUserStream(id: String): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading)
        emit(getUser(id))
    }.flowOn(dispatcher)

    override fun getUserCount(): Int = cache.size

    override suspend fun updateUserAvatar(id: String, avatarUrl: String): NetworkResult<User> =
        withContext(dispatcher) {
            val result = safeApiCall {
                api.updateUser(id, UpdateUserRequest(avatarUrl = avatarUrl)).toDomain()
            }
            if (result is NetworkResult.Success) cache[id] = result.value
            result
        }

    suspend fun preloadUsers(): List<NetworkResult<List<User>>> =
        withContext(dispatcher) {
            val pages = (1..3).map { page ->
                kotlinx.coroutines.async {
                    safeApiCall { api.getUsers(page, DEFAULT_PAGE_SIZE).map { it.toDomain() } }
                        .also { result ->
                            if (result is NetworkResult.Success) {
                                val key = "page_${page}_limit_$DEFAULT_PAGE_SIZE"
                                listCache[key] = result.value
                                result.value.forEach { cache[it.id] = it }
                            }
                        }
                }
            }
            pages.map { it.await() }
        }

    private companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }

    fun clearCache() {
        cache.clear()
        listCache.clear()
    }

    private fun clearListCache() = listCache.clear()

    fun getCachedUser(id: String): User? = cache[id]
    fun getCacheSize(): Int = cache.size
}

class FakeUserRepository : UserRepository {
    private val users = mutableListOf<User>()

    override suspend fun getUser(id: String) =
        users.find { it.id == id }
            ?.let { NetworkResult.Success(it) }
            ?: NetworkResult.Error(404, "Not found")

    override suspend fun getUsers(page: Int, limit: Int) = NetworkResult.Success(users.toList())
    override suspend fun createUser(name: String, email: String, password: String): NetworkResult<User> {
        val u = User(System.currentTimeMillis().toString(), name, email, null, UserRole.User, true)
        users.add(u)
        return NetworkResult.Success(u)
    }
    override suspend fun updateUser(id: String, name: String?, email: String?) =
        NetworkResult.Error(501, "Not implemented")
    override suspend fun deleteUser(id: String): NetworkResult<Unit> {
        users.removeAll { it.id == id }
        return NetworkResult.Success(Unit)
    }
    override fun observeUsers(page: Int, limit: Int) = flow { emit(getUsers(page, limit)) }
    override suspend fun searchUsers(query: String) =
        NetworkResult.Success(users.filter { it.name.contains(query, true) })
    override suspend fun getUsersByRole(role: UserRole) =
        NetworkResult.Success(users.filter { it.role == role })
    override fun getUserStream(id: String) = flow { emit(getUser(id)) }
    override fun getUserCount(): Int = users.size
    override suspend fun updateUserAvatar(id: String, avatarUrl: String): NetworkResult<User> =
        NetworkResult.Error(501, "Not implemented")
}