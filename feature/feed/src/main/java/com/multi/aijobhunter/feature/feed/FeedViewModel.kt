package com.multi.aijobhunter.feature.feed

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.multi.aijobhunter.core.common.BaseViewModel
import com.multi.aijobhunter.core.model.Vacancy
import com.multi.aijobhunter.domain.usecase.FetchNewVacanciesUseCase
import com.multi.aijobhunter.domain.usecase.GetMatchedVacanciesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val getMatchedVacanciesUseCase: GetMatchedVacanciesUseCase,
    private val fetchNewVacanciesUseCase: FetchNewVacanciesUseCase
) : BaseViewModel<FeedUiState, FeedIntent, FeedSideEffect>(FeedUiState()) {

    private val _filtersFlow = MutableStateFlow(Triple(false, false, false))

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val vacanciesFlow: Flow<PagingData<Vacancy>> = _filtersFlow
        .flatMapLatest { filters ->
            val (remote, match85, highSalary) = filters
            getMatchedVacanciesUseCase(
                filterRemote = remote,
                filterMatch85 = match85,
                filterHighSalary = highSalary
            )
        }
        .cachedIn(viewModelScope)

    init {
        // Первичное наполнение базы из плагинов
        viewModelScope.launch {
            try {
                fetchNewVacanciesUseCase()
            } catch (e: Exception) {
                updateState { copy(isOffline = true) }
            }
        }
    }

    override fun handleIntent(intent: FeedIntent) {
        when (intent) {
            FeedIntent.ToggleRemoteFilter -> {
                updateState { copy(filterRemote = !filterRemote) }
                updateFilters()
            }
            FeedIntent.ToggleMatch85Filter -> {
                updateState { copy(filterMatch85 = !filterMatch85) }
                updateFilters()
            }
            FeedIntent.ToggleHighSalaryFilter -> {
                updateState { copy(filterHighSalary = !filterHighSalary) }
                updateFilters()
            }
            FeedIntent.RefreshFeed, FeedIntent.ForceScanNow -> {
                forceScan()
            }
        }
    }

    private fun updateFilters() {
        _filtersFlow.value = Triple(currentState.filterRemote, currentState.filterMatch85, currentState.filterHighSalary)
    }

    private fun forceScan() {
        updateState { copy(isSyncing = true, isOffline = false) }
        viewModelScope.launch {
            try {
                fetchNewVacanciesUseCase()
                sendSideEffect(FeedSideEffect.ShowMessage("Agent scanned 4 plugins successfully //"))
            } catch (e: Exception) {
                updateState { copy(isOffline = true) }
                sendSideEffect(FeedSideEffect.ShowMessage("Network failure. Displaying offline cache //"))
            } finally {
                updateState { copy(isSyncing = false) }
            }
        }
    }
}
