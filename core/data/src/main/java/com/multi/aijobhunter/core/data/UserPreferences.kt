package com.multi.aijobhunter.core.data

import com.multi.aijobhunter.core.common.JobSearchCredentialsProvider
import com.multi.aijobhunter.core.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface UserPreferences : JobSearchCredentialsProvider {
    val profileFlow: StateFlow<UserProfile?>
    fun getApiKey(): String
    fun saveApiKey(key: String)
    fun getBaseUrl(): String
    fun saveBaseUrl(url: String)
    fun getModelName(): String
    fun saveModelName(model: String)
    fun saveUserProfile(profile: UserProfile)
    fun readProfile(): UserProfile?
    
    override fun getHhAccessToken(): String
    fun saveHhAccessToken(token: String)
    override fun getHhContactEmail(): String
    fun saveHhContactEmail(email: String)
    
    fun getHhClientId(): String
    fun saveHhClientId(clientId: String)
    fun getHhClientSecret(): String
    fun saveHhClientSecret(clientSecret: String)
    fun getHhRefreshToken(): String
    fun saveHhRefreshToken(token: String)
    fun getHhBackendUrl(): String
    fun saveHhBackendUrl(url: String)
}
