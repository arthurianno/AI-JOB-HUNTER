package com.multi.aijobhunter.core.ai

import com.multi.aijobhunter.core.model.AiAnalysis
import com.multi.aijobhunter.core.model.Vacancy
import com.multi.aijobhunter.core.model.UserProfile

interface LlmApiKeyProvider {
    suspend fun getApiKey(): String
    suspend fun getBaseUrl(): String
    suspend fun getModelName(): String
}

interface AiService {
    suspend fun analyze(vacancy: Vacancy, profile: UserProfile): AiAnalysis
    suspend fun createCoverLetter(vacancy: Vacancy, profile: UserProfile, style: String): String
}
