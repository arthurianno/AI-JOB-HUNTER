package com.multi.aijobhunter.feature.tracker

import androidx.lifecycle.viewModelScope
import com.multi.aijobhunter.core.common.BaseViewModel
import com.multi.aijobhunter.core.model.VacancyStatus
import com.multi.aijobhunter.domain.usecase.GetAllVacanciesUseCase
import com.multi.aijobhunter.domain.usecase.UpdateVacancyStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackerViewModel @Inject constructor(
    private val getAllVacanciesUseCase: GetAllVacanciesUseCase,
    private val updateVacancyStatusUseCase: UpdateVacancyStatusUseCase
) : BaseViewModel<TrackerUiState, TrackerIntent, TrackerSideEffect>(TrackerUiState()) {

    init {
        handleIntent(TrackerIntent.LoadTrackerData)
    }

    override fun handleIntent(intent: TrackerIntent) {
        when (intent) {
            is TrackerIntent.LoadTrackerData -> observeVacancies()
            is TrackerIntent.UpdateVacancyStatus -> updateStatus(intent.vacancyId, intent.status)
        }
    }

    private fun observeVacancies() {
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            getAllVacanciesUseCase().collect { list ->
                updateState {
                    copy(
                        isLoading = false,
                        // Отображаем только значимые для воронки вакансии (MATCHED, APPLIED, INTERVIEW, REJECTED)
                        vacancies = list.filter {
                            it.status != VacancyStatus.PENDING_ANALYSIS && it.status != VacancyStatus.ARCHIVED
                        }
                    )
                }
            }
        }
    }

    private fun updateStatus(vacancyId: String, status: VacancyStatus) {
        viewModelScope.launch {
            try {
                updateVacancyStatusUseCase(vacancyId, status)
                sendSideEffect(TrackerSideEffect.ShowMessage("Job moved to ${status.name} //"))
            } catch (e: Exception) {
                sendSideEffect(TrackerSideEffect.ShowMessage("Error updating state: ${e.localizedMessage}"))
            }
        }
    }
}
