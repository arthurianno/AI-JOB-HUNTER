package com.multi.aijobhunter.core.data

import androidx.paging.PagingSource
import com.multi.aijobhunter.core.common.CoroutineDispatchers
import com.multi.aijobhunter.core.database.VacancyDao
import com.multi.aijobhunter.core.database.VacancyEntity
import com.multi.aijobhunter.core.model.*
import com.multi.aijobhunter.core.ai.AiService
import com.multi.aijobhunter.core.network.JobPluginManager
import com.multi.aijobhunter.core.network.RawVacancy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FakeVacancyDao : VacancyDao {
    val insertedItems = mutableListOf<VacancyEntity>()

    override fun getMatchedVacanciesPagingSource(): PagingSource<Int, VacancyEntity> {
        throw UnsupportedOperationException()
    }
    
    override fun getVacancyByIdFlow(id: String): Flow<VacancyEntity?> {
        return flowOf(insertedItems.find { it.id == id })
    }
    
    override suspend fun getVacancyById(id: String): VacancyEntity? {
        return insertedItems.find { it.id == id }
    }
    
    override suspend fun getPendingVacancies(): List<VacancyEntity> {
        return insertedItems.filter { it.status == VacancyStatus.PENDING_ANALYSIS.name }
    }
    
    override fun getAllVacanciesFlow(): Flow<List<VacancyEntity>> {
        return flowOf(insertedItems)
    }
    
    override suspend fun insertVacancies(vacancies: List<VacancyEntity>): List<Long> {
        insertedItems.addAll(vacancies)
        return vacancies.map { 1L }
    }

    override suspend fun resetAnalyzedVacancies() {
        insertedItems.forEachIndexed { idx, item ->
            if (item.status == VacancyStatus.ARCHIVED.name || item.status == VacancyStatus.MATCHED.name) {
                insertedItems[idx] = item.copy(status = VacancyStatus.PENDING_ANALYSIS.name)
            }
        }
    }
    
    override suspend fun updateVacancy(vacancy: VacancyEntity) {
        val idx = insertedItems.indexOfFirst { it.id == vacancy.id }
        if (idx != -1) {
            insertedItems[idx] = vacancy
        }
    }
    
    override suspend fun updateStatus(id: String, status: String) {
        val idx = insertedItems.indexOfFirst { it.id == id }
        if (idx != -1) {
            insertedItems[idx] = insertedItems[idx].copy(status = status)
        }
    }
    
    override suspend fun updateVacancyAnalysis(id: String, aiAnalysisJson: String?) {
        val idx = insertedItems.indexOfFirst { it.id == id }
        if (idx != -1) {
            insertedItems[idx] = insertedItems[idx].copy(aiAnalysisJson = aiAnalysisJson)
        }
    }
}

class FakeAiService : AiService {
    override suspend fun analyze(vacancy: Vacancy, profile: UserProfile): AiAnalysis {
        return AiAnalysis(
            matchScore = 85,
            summary = "Heuristics match 85%",
            pros = listOf("Stack fit"),
            cons = emptyList(),
            radarMetrics = RadarMetrics(0.8f, 0.8f, 0.8f, 0.8f, 0.8f)
        )
    }

    override suspend fun createCoverLetter(vacancy: Vacancy, profile: UserProfile, style: String): String {
        return "Fake Cover Letter"
    }
}

class FakeUserPreferences : UserPreferences {
    var profile: UserProfile? = UserProfile(
        fullName = "Art Developer",
        rawResumeText = "Senior Android Developer, Kotlin, Compose",
        skills = listOf("Kotlin", "Compose"),
        targetPosition = "Senior Android Dev",
        customAiPrompt = ""
    )

    private val _profileFlow = MutableStateFlow(profile)
    override val profileFlow: StateFlow<UserProfile?> = _profileFlow.asStateFlow()

    override fun getApiKey(): String = "demo"
    override fun saveApiKey(key: String) {}
    override fun getBaseUrl(): String = ""
    override fun saveBaseUrl(url: String) {}
    
    var modelPreset: String = "gpt-4o-mini"
    override fun getModelName(): String = modelPreset
    override fun saveModelName(model: String) {
        modelPreset = model
    }
    
    override fun saveUserProfile(profile: UserProfile) {
        this.profile = profile
        _profileFlow.value = profile
    }

    override fun readProfile(): UserProfile? = profile
}

class VacancyRepositoryTest {

    private lateinit var repository: VacancyRepositoryImpl
    private val fakeDao = FakeVacancyDao()
    private val fakeAiService = FakeAiService()
    private val fakePrefs = FakeUserPreferences()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val fakeScoutLogDao = object : com.multi.aijobhunter.core.database.ScoutLogDao {
        val logs = mutableListOf<com.multi.aijobhunter.core.database.ScoutLogEntity>()
        override suspend fun insertLog(log: com.multi.aijobhunter.core.database.ScoutLogEntity) {
            logs.add(log)
        }
        override fun getLatestLogsFlow(): kotlinx.coroutines.flow.Flow<List<com.multi.aijobhunter.core.database.ScoutLogEntity>> {
            return kotlinx.coroutines.flow.flowOf(logs)
        }
        override suspend fun clearLogs() {
            logs.clear()
        }
    }

    @Before
    fun setUp() {
        val customDispatchers = CoroutineDispatchers(
            io = testDispatcher,
            default = testDispatcher,
            main = testDispatcher,
            unconfined = testDispatcher
        )

        repository = VacancyRepositoryImpl(
            vacancyDao = fakeDao,
            scoutLogDao = fakeScoutLogDao,
            aiService = fakeAiService,
            pluginManager = JobPluginManager(),
            userPreferences = fakePrefs,
            dispatchers = customDispatchers
        )
    }

    @Test
    fun fetch_vacancies_saves_items_to_room_database_on_success() = runTest(testDispatcher) {
        // Given & When
        repository.fetchAndAnalyzeNewVacancies()
        
        // Then
        val items = fakeDao.insertedItems
        assert(items.isNotEmpty())
        
        // Check that the items are processed by fake AI Service and matched/scored
        val matched = items.filter { it.status == VacancyStatus.MATCHED.name }
        assert(matched.isNotEmpty())
        assertEquals(85, matched.first().matchScore)
    }

    @Test
    fun save_user_profile_resets_analyzed_vacancies_to_pending_analysis() = runTest(testDispatcher) {
        // Given
        val initialVacancy = VacancyEntity(
            id = "test-1",
            title = "Android dev",
            companyName = "Elta",
            salaryFrom = null,
            salaryTo = null,
            salaryCurrency = "RUB",
            description = "Some job",
            url = "http://elta.ru",
            source = "HH",
            status = VacancyStatus.ARCHIVED.name,
            matchScore = 0,
            aiAnalysisJson = null,
            createdAt = 1783338270L
        )
        fakeDao.insertedItems.add(initialVacancy)

        // When
        val profile = UserProfile("New Name", "New Resume", emptyList(), "New Position", "New Prompt")
        repository.saveUserProfile(profile)

        // Then
        assertEquals(VacancyStatus.PENDING_ANALYSIS.name, fakeDao.insertedItems.first().status)
    }
}
