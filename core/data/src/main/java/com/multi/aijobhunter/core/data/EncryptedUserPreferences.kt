package com.multi.aijobhunter.core.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.multi.aijobhunter.core.model.UserProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedUserPreferences @Inject constructor(context: Context) : UserPreferences {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        "secure_user_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _profileFlow = MutableStateFlow<UserProfile?>(readProfile())
    override val profileFlow: StateFlow<UserProfile?> = _profileFlow.asStateFlow()

    override fun getApiKey(): String {
        return sharedPreferences.getString("api_key", "") ?: ""
    }

    override fun saveApiKey(key: String) {
        sharedPreferences.edit().putString("api_key", key).apply()
    }

    override fun getBaseUrl(): String {
        val url = sharedPreferences.getString("base_url", "") ?: ""
        return if (url.isBlank()) "https://api.openai.com/" else url
    }

    override fun saveBaseUrl(url: String) {
        sharedPreferences.edit().putString("base_url", url).apply()
    }

    override fun getModelName(): String {
        val model = sharedPreferences.getString("model_name", "") ?: ""
        return if (model.isBlank()) "gpt-4o-mini" else model
    }

    override fun saveModelName(model: String) {
        sharedPreferences.edit().putString("model_name", model).apply()
    }

    override fun saveUserProfile(profile: UserProfile) {
        val json = Json.encodeToString(profile)
        sharedPreferences.edit().putString("user_profile", json).apply()
        _profileFlow.value = profile
    }

    override fun readProfile(): UserProfile? {
        val json = sharedPreferences.getString("user_profile", null) ?: return null
        return try {
            Json.decodeFromString<UserProfile>(json)
        } catch (e: Exception) {
            null
        }
    }

    override fun getHhAccessToken(): String {
        return sharedPreferences.getString("hh_access_token", "") ?: ""
    }

    override fun saveHhAccessToken(token: String) {
        sharedPreferences.edit().putString("hh_access_token", token).apply()
    }

    override fun getHhContactEmail(): String {
        return sharedPreferences.getString("hh_contact_email", "") ?: ""
    }

    override fun saveHhContactEmail(email: String) {
        sharedPreferences.edit().putString("hh_contact_email", email).apply()
    }

    override fun getHhClientId(): String {
        return sharedPreferences.getString("hh_client_id", "") ?: ""
    }

    override fun saveHhClientId(clientId: String) {
        sharedPreferences.edit().putString("hh_client_id", clientId).apply()
    }

    override fun getHhClientSecret(): String {
        return sharedPreferences.getString("hh_client_secret", "") ?: ""
    }

    override fun saveHhClientSecret(clientSecret: String) {
        sharedPreferences.edit().putString("hh_client_secret", clientSecret).apply()
    }

    override fun getHhRefreshToken(): String {
        return sharedPreferences.getString("hh_refresh_token", "") ?: ""
    }

    override fun saveHhRefreshToken(token: String) {
        sharedPreferences.edit().putString("hh_refresh_token", token).apply()
    }

    override fun getHhBackendUrl(): String {
        return sharedPreferences.getString("hh_backend_url", "") ?: ""
    }

    override fun saveHhBackendUrl(url: String) {
        sharedPreferences.edit().putString("hh_backend_url", url).apply()
    }
}
