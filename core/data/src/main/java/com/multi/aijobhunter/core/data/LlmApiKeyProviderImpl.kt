package com.multi.aijobhunter.core.data

import com.multi.aijobhunter.core.ai.LlmApiKeyProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmApiKeyProviderImpl @Inject constructor(
    private val userPreferences: UserPreferences
) : LlmApiKeyProvider {
    override suspend fun getApiKey(): String = userPreferences.getApiKey()
    override suspend fun getBaseUrl(): String = userPreferences.getBaseUrl()
    override suspend fun getModelName(): String = userPreferences.getModelName()
}
