package com.multi.aijobhunter.feature.tracker

import androidx.compose.runtime.Immutable
import com.multi.aijobhunter.core.model.Vacancy
import com.multi.aijobhunter.core.model.VacancyStatus

@Immutable
data class TrackerUiState(
    val isLoading: Boolean = false,
    val vacancies: List<Vacancy> = emptyList()
)

sealed interface TrackerIntent {
    data object LoadTrackerData : TrackerIntent
    data class UpdateVacancyStatus(val vacancyId: String, val status: VacancyStatus) : TrackerIntent
}

sealed interface TrackerSideEffect {
    data class ShowMessage(val message: String) : TrackerSideEffect
}
