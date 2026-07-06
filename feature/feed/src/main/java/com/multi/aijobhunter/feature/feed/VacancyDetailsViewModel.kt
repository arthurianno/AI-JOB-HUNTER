package com.multi.aijobhunter.feature.feed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.multi.aijobhunter.core.common.BaseViewModel
import com.multi.aijobhunter.core.model.VacancyStatus
import com.multi.aijobhunter.domain.usecase.GenerateCoverLetterUseCase
import com.multi.aijobhunter.domain.usecase.GetVacancyDetailsUseCase
import com.multi.aijobhunter.domain.usecase.UpdateVacancyStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VacancyDetailsViewModel @Inject constructor(
    private val getVacancyDetailsUseCase: GetVacancyDetailsUseCase,
    private val generateCoverLetterUseCase: GenerateCoverLetterUseCase,
    private val updateStatusUseCase: UpdateVacancyStatusUseCase,
    private val savedStateHandle: SavedStateHandle
) : BaseViewModel<VacancyDetailsUiState, VacancyDetailsIntent, VacancyDetailsSideEffect>(VacancyDetailsUiState()) {

    private var currentVacancyId: String? = null

    init {
        savedStateHandle.get<String>("vacancyId")?.let { id ->
            handleIntent(VacancyDetailsIntent.LoadDetails(id))
        }
    }

    override fun handleIntent(intent: VacancyDetailsIntent) {
        when (intent) {
            is VacancyDetailsIntent.LoadDetails -> observeVacancyDetails(intent.vacancyId)
            is VacancyDetailsIntent.GenerateLetterClick -> runLetterGeneration(intent.style)
            is VacancyDetailsIntent.ChangeStatus -> updateVacancyStatus(intent.status)
            is VacancyDetailsIntent.ClearError -> updateState { copy(errorMessage = null) }
        }
    }

    private fun observeVacancyDetails(id: String) {
        currentVacancyId = id
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            getVacancyDetailsUseCase(id).collect { vacancy ->
                if (vacancy != null) {
                    updateState {
                        copy(
                            isLoading = false,
                            vacancy = vacancy,
                            generatedLetter = vacancy.aiAnalysis?.generatedCoverLetter
                        )
                    }
                } else {
                    updateState {
                        copy(
                            isLoading = false,
                            errorMessage = "Vacancy details not found in local db."
                        )
                    }
                }
            }
        }
    }

    private fun runLetterGeneration(style: String) {
        val id = currentVacancyId ?: return
        updateState { copy(isGeneratingLetter = true) }
        viewModelScope.launch {
            generateCoverLetterUseCase(id, style)
                .onSuccess { letter ->
                    updateState { copy(isGeneratingLetter = false, generatedLetter = letter) }
                    sendSideEffect(VacancyDetailsSideEffect.ShowSnackbar("Cover letter ready //"))
                }
                .onFailure { error ->
                    updateState { copy(isGeneratingLetter = false) }
                    sendSideEffect(VacancyDetailsSideEffect.ShowSnackbar("Generation failed: ${error.localizedMessage}"))
                }
        }
    }

    private fun updateVacancyStatus(status: VacancyStatus) {
        val id = currentVacancyId ?: return
        viewModelScope.launch {
            updateStatusUseCase(id, status)
            sendSideEffect(VacancyDetailsSideEffect.ShowSnackbar("Status updated to ${status.name}"))
        }
    }
}
