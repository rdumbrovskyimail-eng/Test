package com.test.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://api.example.com/v1/"
    private const val TIMEOUT_SECONDS = 30L

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    inline fun <reified T> create(): T = retrofit.create(T::class.java)
}

data class ApiResponse<T>(
    val data: T? = null,
    val error: String? = null,
    val code: Int = 0
)

sealed class NetworkResult<out T> {
    data class Success<T>(val value: T) : NetworkResult<T>()
    data class Error(val code: Int, val message: String) : NetworkResult<Nothing>()
    object Loading : NetworkResult<Nothing>()
}

object NetworkUtils {

    fun isSuccessCode(code: Int): Boolean = code in 200..299

    fun parseErrorBody(body: String?): String {
        return body?.let {
            try {
                it.substringAfter("\"message\":\"").substringBefore("\"")
            } catch (e: Exception) {
                "Unknown error"
            }
        } ?: "No error body"
    }

    fun buildQueryParams(map: Map<String, Any>): String {
        return map.entries.joinToString("&") { (k, v) -> "$k=$v" }
    }
}

interface UserApi {
    @retrofit2.http.GET("users/{id}")
    suspend fun getUser(@retrofit2.http.Path("id") id: String): UserDto

    @retrofit2.http.GET("users")
    suspend fun getUsers(
        @retrofit2.http.Query("page") page: Int,
        @retrofit2.http.Query("limit") limit: Int
    ): List<UserDto>

    @retrofit2.http.POST("users")
    suspend fun createUser(@retrofit2.http.Body body: CreateUserRequest): UserDto

    @retrofit2.http.PUT("users/{id}")
    suspend fun updateUser(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Body body: UpdateUserRequest
    ): UserDto

    @retrofit2.http.DELETE("users/{id}")
    suspend fun deleteUser(@retrofit2.http.Path("id") id: String): retrofit2.Response<Unit>
}

data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val createdAt: String,
    val updatedAt: String,
    val role: String,
    val isActive: Boolean
)

data class CreateUserRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String = "user"
)

data class UpdateUserRequest(
    val name: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val isActive: Boolean? = null
)

class ApiException(val code: Int, message: String) : Exception(message)

suspend fun <T> safeApiCall(call: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(call())
    } catch (e: retrofit2.HttpException) {
        val body = e.response()?.errorBody()?.string()
        NetworkResult.Error(e.code(), NetworkUtils.parseErrorBody(body))
    } catch (e: java.io.IOException) {
        NetworkResult.Error(-1, "Network error: ${e.message}")
    } catch (e: Exception) {
        NetworkResult.Error(-2, "Unexpected error: ${e.message}")
    }
}

object PaginationHelper {
    const val DEFAULT_PAGE = 1
    const val DEFAULT_LIMIT = 20

    data class Page(val current: Int, val limit: Int, val total: Int) {
        val hasNext: Boolean get() = current * limit < total
        val hasPrev: Boolean get() = current > 1
        val nextPage: Int get() = if (hasNext) current + 1 else current
        val prevPage: Int get() = if (hasPrev) current - 1 else current
    }

    fun fromHeaders(headers: okhttp3.Headers): Page {
        val total = headers["X-Total-Count"]?.toIntOrNull() ?: 0
        val page = headers["X-Page"]?.toIntOrNull() ?: DEFAULT_PAGE
        val limit = headers["X-Limit"]?.toIntOrNull() ?: DEFAULT_LIMIT
        return Page(page, limit, total)
    }
}

object RetryPolicy {
    private const val MAX_RETRIES = 3
    private const val INITIAL_DELAY_MS = 500L

    suspend fun <T> withRetry(
        maxRetries: Int = MAX_RETRIES,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                val delay = INITIAL_DELAY_MS * (attempt + 1)
                kotlinx.coroutines.delay(delay)
            }
        }
        throw lastException ?: Exception("Retry failed")
    }
}

class AuthInterceptor(private val tokenProvider: () -> String?) : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val token = tokenProvider()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}

class TokenRefreshAuthenticator(
    private val refreshToken: suspend () -> String?
) : okhttp3.Authenticator {
    override fun authenticate(route: okhttp3.Route?, response: okhttp3.Response): okhttp3.Request? {
        if (response.code == 401) {
            return null
        }
        return null
    }
}

fun OkHttpClient.Builder.addAuthInterceptor(tokenProvider: () -> String?) =
    this.addInterceptor(AuthInterceptor(tokenProvider))