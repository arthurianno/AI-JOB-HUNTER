package com.multi.aijobhunter.core.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.multi.aijobhunter.core.common.CoroutineDispatchers
import com.multi.aijobhunter.core.database.VacancyDao
import com.multi.aijobhunter.core.model.*
import com.multi.aijobhunter.core.ai.AiService
import com.multi.aijobhunter.core.network.JobPluginManager
import com.multi.aijobhunter.domain.VacancyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject

import com.multi.aijobhunter.core.database.ScoutLogDao

@Singleton
class VacancyRepositoryImpl @Inject constructor(
    private val vacancyDao: VacancyDao,
    private val scoutLogDao: ScoutLogDao,
    private val aiService: AiService,
    private val pluginManager: JobPluginManager,
    private val userPreferences: UserPreferences,
    private val dispatchers: CoroutineDispatchers,
    private val okHttpClient: OkHttpClient
) : VacancyRepository {

    override fun getScoutLogsFlow(): Flow<List<ScoutLog>> {
        return scoutLogDao.getLatestLogsFlow().map { list ->
            list.map { it.toDomainModel() }
        }.flowOn(dispatchers.io)
    }

    override suspend fun addScoutLog(log: ScoutLog) = withContext(dispatchers.io) {
        scoutLogDao.insertLog(log.toEntity())
    }

    override suspend fun clearScoutLogs() = withContext(dispatchers.io) {
        scoutLogDao.clearLogs()
    }

    override fun getMatchedVacanciesPaged(
        filterRemote: Boolean,
        filterMatch85: Boolean,
        filterHighSalary: Boolean
    ): Flow<PagingData<Vacancy>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { 
                vacancyDao.getMatchedVacanciesPagingSource(
                    filterRemote = filterRemote,
                    filterMatch85 = filterMatch85,
                    filterHighSalary = filterHighSalary
                ) 
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }.flowOn(dispatchers.io)
    }

    override fun getVacancyById(id: String): Flow<Vacancy?> {
        return vacancyDao.getVacancyByIdFlow(id).map { it?.toDomainModel() }.flowOn(dispatchers.io)
    }

    override suspend fun fetchAndAnalyzeNewVacancies(): List<Vacancy> = withContext(dispatchers.io) {
        try {
            val profile = userPreferences.readProfile() ?: return@withContext emptyList()
            val raw = pluginManager.executeAllPlugins(profile.targetPosition)
            val entities = raw.map { it.toEntity() }
            vacancyDao.insertVacancies(entities)

            val baseUrl = userPreferences.getBaseUrl()
            val isFreeOrLocal = baseUrl.contains("googleapis.com") || 
                                baseUrl.contains("openrouter.ai") || 
                                baseUrl.contains("10.0.2.2") || 
                                baseUrl.contains("11434")

            val pending = vacancyDao.getPendingVacancies()
            val limit = if (isFreeOrLocal) 5 else pending.size
            val toAnalyze = pending.take(limit)
            val matchedList = mutableListOf<Vacancy>()
            
            toAnalyze.forEachIndexed { index, entity ->
                if (isFreeOrLocal && index > 0) {
                    kotlinx.coroutines.delay(3500)
                }

                val vacancyDomain = entity.toDomainModel()
                val aiResult = try {
                    aiService.analyze(vacancyDomain, profile)
                } catch (e: Exception) {
                    AiAnalysis(
                        matchScore = 0,
                        summary = "Scouting error: ${e.message}",
                        pros = emptyList(),
                        cons = listOf("Network error occurred during analysis: ${e.localizedMessage}"),
                        radarMetrics = RadarMetrics(0f, 0f, 0f, 0f, 0f)
                    )
                }
                
                val finalStatus = if (aiResult.matchScore >= 60) VacancyStatus.MATCHED else VacancyStatus.REJECTED
                
                val updated = entity.copy(
                    status = finalStatus.name,
                    matchScore = aiResult.matchScore,
                    aiAnalysisJson = Json.encodeToString(aiResult)
                )
                vacancyDao.updateVacancy(updated)
                if (finalStatus == VacancyStatus.MATCHED) {
                    matchedList.add(updated.toDomainModel())
                }
            }

            val status = "SUCCESS"
            val message = "Found ${matchedList.size} matches. Scanned: ${raw.size}. Analyzed: ${toAnalyze.size}/${pending.size} (Throttled: $isFreeOrLocal)."
            scoutLogDao.insertLog(ScoutLog(
                timestamp = System.currentTimeMillis(),
                scannedCount = raw.size,
                matchedCount = matchedList.size,
                status = status,
                message = message
            ).toEntity())

            return@withContext matchedList
        } catch (e: Exception) {
            scoutLogDao.insertLog(ScoutLog(
                timestamp = System.currentTimeMillis(),
                scannedCount = 0,
                matchedCount = 0,
                status = "ERROR",
                message = "Scout run failed: ${e.localizedMessage}"
            ).toEntity())
            throw e
        }
    }

    override suspend fun generateCoverLetter(vacancyId: String, style: String): String = withContext(dispatchers.io) {
        val entity = vacancyDao.getVacancyById(vacancyId) ?: throw IllegalArgumentException("Vacancy not found")
        val vacancy = entity.toDomainModel()
        val profile = userPreferences.readProfile() ?: throw IllegalStateException("Profile not configured")
        
        val letter = aiService.createCoverLetter(vacancy, profile, style)
        val updatedAnalysis = (vacancy.aiAnalysis ?: AiAnalysis(
            matchScore = 70,
            summary = "Unknown",
            pros = emptyList(),
            cons = emptyList(),
            radarMetrics = RadarMetrics(0.7f, 0.7f, 0.7f, 0.7f, 0.7f)
        )).copy(generatedCoverLetter = letter)
        
        vacancyDao.updateVacancyAnalysis(vacancyId, Json.encodeToString(updatedAnalysis))
        return@withContext letter
    }

    override suspend fun updateVacancyStatus(id: String, status: VacancyStatus) = withContext(dispatchers.io) {
        vacancyDao.updateStatus(id, status.name)
    }

    override fun getAllVacanciesFlow(): Flow<List<Vacancy>> {
        return vacancyDao.getAllVacanciesFlow().map { list ->
            list.map { it.toDomainModel() }
        }.flowOn(dispatchers.io)
    }

    override fun getUserProfileFlow(): Flow<UserProfile?> {
        return userPreferences.profileFlow
    }

    override suspend fun saveUserProfile(profile: UserProfile) = withContext(dispatchers.io) {
        userPreferences.saveUserProfile(profile)
        vacancyDao.resetAnalyzedVacancies()
    }

    override fun getApiKey(): String {
        return userPreferences.getApiKey()
    }

    override fun saveApiKey(key: String) {
        userPreferences.saveApiKey(key)
    }

    override fun getBaseUrl(): String {
        return userPreferences.getBaseUrl()
    }

    override fun saveBaseUrl(url: String) {
        userPreferences.saveBaseUrl(url)
    }

    override fun getModelName(): String {
        return userPreferences.getModelName()
    }

    override fun saveModelName(model: String) {
        userPreferences.saveModelName(model)
    }

    override fun getHhAccessToken(): String {
        return userPreferences.getHhAccessToken()
    }

    override fun saveHhAccessToken(token: String) {
        userPreferences.saveHhAccessToken(token)
    }

    override fun getHhContactEmail(): String {
        return userPreferences.getHhContactEmail()
    }

    override fun saveHhContactEmail(email: String) {
        userPreferences.saveHhContactEmail(email)
    }

    override fun getHhClientId(): String {
        return userPreferences.getHhClientId()
    }

    override fun saveHhClientId(clientId: String) {
        userPreferences.saveHhClientId(clientId)
    }

    override fun getHhClientSecret(): String {
        return userPreferences.getHhClientSecret()
    }

    override fun saveHhClientSecret(clientSecret: String) {
        userPreferences.saveHhClientSecret(clientSecret)
    }

    override fun getHhRefreshToken(): String {
        return userPreferences.getHhRefreshToken()
    }

    override fun saveHhRefreshToken(token: String) {
        userPreferences.saveHhRefreshToken(token)
    }

    override fun getHhBackendUrl(): String {
        return userPreferences.getHhBackendUrl()
    }

    override fun saveHhBackendUrl(url: String) {
        userPreferences.saveHhBackendUrl(url)
    }

    override suspend fun exchangeHhCode(code: String): Boolean = withContext(dispatchers.io) {
        try {
            val backendUrl = userPreferences.getHhBackendUrl()
            val request = if (backendUrl.isNotBlank()) {
                val jsonObject = JSONObject().apply {
                    put("code", code)
                }
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = okhttp3.RequestBody.create(mediaType, jsonObject.toString())
                Request.Builder()
                    .url(backendUrl)
                    .post(requestBody)
                    .build()
            } else {
                val clientId = userPreferences.getHhClientId()
                val clientSecret = userPreferences.getHhClientSecret()
                if (clientId.isBlank() || clientSecret.isBlank()) return@withContext false

                val requestBody = FormBody.Builder()
                    .add("grant_type", "authorization_code")
                    .add("client_id", clientId)
                    .add("client_secret", clientSecret)
                    .add("code", code)
                    .add("redirect_uri", "aijobhunter://oauth")
                    .build()

                Request.Builder()
                    .url("https://api.hh.ru/token")
                    .header("User-Agent", "AiJobHunter/1.0 (${userPreferences.getHhContactEmail().ifBlank { "support@aijobhunter.com" }})")
                    .post(requestBody)
                    .build()
            }

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false

            val body = response.body?.string() ?: return@withContext false
            val json = JSONObject(body)
            val accessToken = json.getString("access_token")
            val refreshToken = json.optString("refresh_token", "")

            userPreferences.saveHhAccessToken(accessToken)
            if (refreshToken.isNotBlank()) {
                userPreferences.saveHhRefreshToken(refreshToken)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
