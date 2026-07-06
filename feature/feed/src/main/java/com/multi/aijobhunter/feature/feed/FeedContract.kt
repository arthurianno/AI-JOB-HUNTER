package com.multi.aijobhunter.feature.feed

import androidx.compose.runtime.Immutable
import androidx.paging.PagingData
import com.multi.aijobhunter.core.model.Vacancy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Immutable
data class FeedUiState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val filterRemote: Boolean = false,
    val filterMatch85: Boolean = false,
    val filterHighSalary: Boolean = false,
    val isOffline: Boolean = false
)

sealed interface FeedIntent {
    data object ToggleRemoteFilter : FeedIntent
    data object ToggleMatch85Filter : FeedIntent
    data object ToggleHighSalaryFilter : FeedIntent
    data object RefreshFeed : FeedIntent
    data object ForceScanNow : FeedIntent
}

sealed interface FeedSideEffect {
    data class ShowMessage(val message: String) : FeedSideEffect
}
