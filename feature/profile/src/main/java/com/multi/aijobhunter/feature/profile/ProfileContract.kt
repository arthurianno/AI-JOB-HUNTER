package com.multi.aijobhunter.feature.profile

import androidx.compose.runtime.Immutable
import com.multi.aijobhunter.core.model.UserProfile

@Immutable
data class ProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val profile: UserProfile? = null,
    val fullName: String = "",
    val targetPosition: String = "",
    val skillsText: String = "",
    val customAiPrompt: String = "",
    val hhAccessToken: String = "",
    val hhContactEmail: String = "",
    val hhClientId: String = "",
    val hhClientSecret: String = "",
    val hhBackendUrl: String = "",
    val scoutLogs: List<com.multi.aijobhunter.core.model.ScoutLog> = emptyList()
)

sealed interface ProfileIntent {
    data object LoadProfile : ProfileIntent
    data class ChangeFullName(val name: String) : ProfileIntent
    data class ChangeTargetPosition(val position: String) : ProfileIntent
    data class ChangeSkillsText(val skills: String) : ProfileIntent
    data class ChangeCustomAiPrompt(val prompt: String) : ProfileIntent
    data class ChangeHhAccessToken(val token: String) : ProfileIntent
    data class ChangeHhContactEmail(val email: String) : ProfileIntent
    data class ChangeHhClientId(val clientId: String) : ProfileIntent
    data class ChangeHhClientSecret(val clientSecret: String) : ProfileIntent
    data class ChangeHhBackendUrl(val url: String) : ProfileIntent
    data object SaveProfile : ProfileIntent
    data object ClearLogs : ProfileIntent
}

sealed interface ProfileSideEffect {
    data class ShowMessage(val message: String) : ProfileSideEffect
    data object NavigateBack : ProfileSideEffect
}
