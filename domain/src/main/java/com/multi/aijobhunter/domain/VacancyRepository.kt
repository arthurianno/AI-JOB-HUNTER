package com.multi.aijobhunter.domain

import androidx.paging.PagingData
import com.multi.aijobhunter.core.model.Vacancy
import com.multi.aijobhunter.core.model.VacancyStatus
import com.multi.aijobhunter.core.model.UserProfile
import com.multi.aijobhunter.core.model.ScoutLog
import kotlinx.coroutines.flow.Flow

interface VacancyRepository {
    fun getScoutLogsFlow(): Flow<List<ScoutLog>>
    suspend fun addScoutLog(log: ScoutLog)
    suspend fun clearScoutLogs()
    fun getMatchedVacanciesPaged(
        filterRemote: Boolean,
        filterMatch85: Boolean,
        filterHighSalary: Boolean
    ): Flow<PagingData<Vacancy>>
    fun getVacancyById(id: String): Flow<Vacancy?>
    suspend fun fetchAndAnalyzeNewVacancies(): List<Vacancy>
    suspend fun generateCoverLetter(vacancyId: String, style: String): String
    suspend fun updateVacancyStatus(id: String, status: VacancyStatus)
    fun getAllVacanciesFlow(): Flow<List<Vacancy>>
    fun getUserProfileFlow(): Flow<UserProfile?>
    suspend fun saveUserProfile(profile: UserProfile)
    fun getApiKey(): String
    fun saveApiKey(key: String)
    fun getBaseUrl(): String
    fun saveBaseUrl(url: String)
    fun getModelName(): String
    fun saveModelName(model: String)
    fun getHhAccessToken(): String
    fun saveHhAccessToken(token: String)
    fun getHhContactEmail(): String
    fun saveHhContactEmail(email: String)
    fun getHhClientId(): String
    fun saveHhClientId(clientId: String)
    fun getHhClientSecret(): String
    fun saveHhClientSecret(clientSecret: String)
    fun getHhRefreshToken(): String
    fun saveHhRefreshToken(token: String)
    fun getHhBackendUrl(): String
    fun saveHhBackendUrl(url: String)
    suspend fun exchangeHhCode(code: String): Boolean
}
