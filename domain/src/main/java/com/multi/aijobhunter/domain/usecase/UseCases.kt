package com.multi.aijobhunter.domain.usecase

import androidx.paging.PagingData
import com.multi.aijobhunter.domain.VacancyRepository
import com.multi.aijobhunter.core.model.Vacancy
import com.multi.aijobhunter.core.model.VacancyStatus
import com.multi.aijobhunter.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetVacancyDetailsUseCase @Inject constructor(
    private val repository: VacancyRepository
) {
    operator fun invoke(id: String): Flow<Vacancy?> = repository.getVacancyById(id)
}

class GenerateCoverLetterUseCase @Inject constructor(
    private val repository: VacancyRepository
) {
    suspend operator fun invoke(id: String, style: String): Result<String> = runCatching {
        repository.generateCoverLetter(id, style)
    }
}

class UpdateVacancyStatusUseCase @Inject constructor(
    private val repository: VacancyRepository
) {
    suspend operator fun invoke(id: String, status: VacancyStatus) {
        repository.updateVacancyStatus(id, status)
    }
}

class GetMatchedVacanciesUseCase @Inject constructor(
    private val repository: VacancyRepository
) {
    operator fun invoke(): Flow<PagingData<Vacancy>> = repository.getMatchedVacanciesPaged()
}

class FetchNewVacanciesUseCase @Inject constructor(
    private val repository: VacancyRepository
) {
    suspend operator fun invoke() {
        repository.fetchAndAnalyzeNewVacancies()
    }
}

class UserProfileUseCase @Inject constructor(
    private val repository: VacancyRepository
) {
    fun getProfile(): Flow<UserProfile?> = repository.getUserProfileFlow()
    suspend fun saveProfile(profile: UserProfile) = repository.saveUserProfile(profile)
}

class GetAllVacanciesUseCase @Inject constructor(
    private val repository: VacancyRepository
) {
    operator fun invoke(): Flow<List<Vacancy>> = repository.getAllVacanciesFlow()
}
