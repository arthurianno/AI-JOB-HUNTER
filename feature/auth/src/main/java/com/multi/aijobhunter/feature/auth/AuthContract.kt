package com.multi.aijobhunter.feature.auth

import androidx.compose.runtime.Immutable

@Immutable
data class AuthUiState(
    val isLoading: Boolean = false,
    val apiKey: String = "",
    val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta/openai/",
    val isParsingCV: Boolean = false,
    val parsingLogs: List<String> = emptyList(),
    val errorMessage: String? = null,
    val resumeUploaded: Boolean = false,
    val modelName: String = "gpt-4o-mini"
)

sealed interface AuthIntent {
    data class ChangeApiKey(val key: String) : AuthIntent
    data class ChangeBaseUrl(val url: String) : AuthIntent
    data class ChangeModelName(val model: String) : AuthIntent
    data object SaveCredentials : AuthIntent
    data class UploadResume(val rawText: String) : AuthIntent
    data object ClearError : AuthIntent
}

sealed interface AuthSideEffect {
    data class ShowMessage(val message: String) : AuthSideEffect
    data object NavigateToFeed : AuthSideEffect
}
