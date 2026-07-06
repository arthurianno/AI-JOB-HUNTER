package com.multi.aijobhunter.feature.feed

import androidx.compose.runtime.Immutable
import com.multi.aijobhunter.core.model.Vacancy
import com.multi.aijobhunter.core.model.VacancyStatus

@Immutable
data class VacancyDetailsUiState(
    val isLoading: Boolean = false,
    val vacancy: Vacancy? = null,
    val isGeneratingLetter: Boolean = false,
    val generatedLetter: String? = null,
    val errorMessage: String? = null
)

sealed interface VacancyDetailsIntent {
    data class LoadDetails(val vacancyId: String) : VacancyDetailsIntent
    data class GenerateLetterClick(val style: String) : VacancyDetailsIntent
    data class ChangeStatus(val status: VacancyStatus) : VacancyDetailsIntent
    data object ClearError : VacancyDetailsIntent
}

sealed interface VacancyDetailsSideEffect {
    data class ShowSnackbar(val message: String) : VacancyDetailsSideEffect
    data class CopyToClipboard(val text: String) : VacancyDetailsSideEffect
    data object NavigateBack : VacancyDetailsSideEffect
}
