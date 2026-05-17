package com.test.app.network

import com.test.app.data.User

interface ApiService {
    
    fun getUserProfile(userId: String): User
    
    fun updateEmail(userId: String, newEmail: String): Boolean
    
    fun deleteAccount(userId: String)
}