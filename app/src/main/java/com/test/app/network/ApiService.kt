package com.test.app.network

import com.test.app.data.User

interface ApiService {
    
    @GET("users/{id}")
    suspend fun getUserProfile(userId: String): User
    
    @PUT("users/{id}/email")
    @FormUrlEncoded
    suspend fun updateEmail(userId: String, newEmail: String): Boolean
    
    suspend fun deleteAccount(userId: String)
}