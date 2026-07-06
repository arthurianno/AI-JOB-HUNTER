package com.multi.aijobhunter.feature.profile

import androidx.lifecycle.viewModelScope
import com.multi.aijobhunter.core.common.BaseViewModel
import com.multi.aijobhunter.core.model.UserProfile
import com.multi.aijobhunter.domain.usecase.UserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.multi.aijobhunter.domain.VacancyRepository

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileUseCase: UserProfileUseCase,
    private val vacancyRepository: VacancyRepository
) : BaseViewModel<ProfileUiState, ProfileIntent, ProfileSideEffect>(ProfileUiState()) {

    init {
        handleIntent(ProfileIntent.LoadProfile)
        observeLogs()
    }

    private fun observeLogs() {
        viewModelScope.launch {
            vacancyRepository.getScoutLogsFlow().collect { logs ->
                updateState { copy(scoutLogs = logs) }
            }
        }
    }

    override fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.LoadProfile -> loadProfile()
            is ProfileIntent.ChangeFullName -> updateState { copy(fullName = intent.name) }
            is ProfileIntent.ChangeTargetPosition -> updateState { copy(targetPosition = intent.position) }
            is ProfileIntent.ChangeSkillsText -> updateState { copy(skillsText = intent.skills) }
            is ProfileIntent.ChangeCustomAiPrompt -> updateState { copy(customAiPrompt = intent.prompt) }
            is ProfileIntent.ChangeHhAccessToken -> updateState { copy(hhAccessToken = intent.token) }
            is ProfileIntent.ChangeHhContactEmail -> updateState { copy(hhContactEmail = intent.email) }
            is ProfileIntent.ChangeHhClientId -> updateState { copy(hhClientId = intent.clientId) }
            is ProfileIntent.ChangeHhClientSecret -> updateState { copy(hhClientSecret = intent.clientSecret) }
            is ProfileIntent.ChangeHhBackendUrl -> updateState { copy(hhBackendUrl = intent.url) }
            is ProfileIntent.SaveProfile -> saveProfile()
            is ProfileIntent.ClearLogs -> clearLogs()
        }
    }

    private fun loadProfile() {
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            val profile = userProfileUseCase.getProfile().firstOrNull()
            val hhToken = vacancyRepository.getHhAccessToken()
            val hhEmail = vacancyRepository.getHhContactEmail()
            val hhClientIdVal = vacancyRepository.getHhClientId()
            val hhClientSecretVal = vacancyRepository.getHhClientSecret()
            val hhBackendUrlVal = vacancyRepository.getHhBackendUrl()
            if (profile != null) {
                updateState {
                    copy(
                        isLoading = false,
                        profile = profile,
                        fullName = profile.fullName,
                        targetPosition = profile.targetPosition,
                        skillsText = profile.skills.joinToString(", "),
                        customAiPrompt = profile.customAiPrompt,
                        hhAccessToken = hhToken,
                        hhContactEmail = hhEmail,
                        hhClientId = hhClientIdVal,
                        hhClientSecret = hhClientSecretVal,
                        hhBackendUrl = hhBackendUrlVal
                    )
                }
            } else {
                updateState {
                    copy(
                        isLoading = false,
                        hhAccessToken = hhToken,
                        hhContactEmail = hhEmail,
                        hhClientId = hhClientIdVal,
                        hhClientSecret = hhClientSecretVal,
                        hhBackendUrl = hhBackendUrlVal
                    )
                }
            }
        }
    }

    private fun saveProfile() {
        updateState { copy(isSaving = true) }
        viewModelScope.launch {
            val listSkills = currentState.skillsText
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            
            val updated = UserProfile(
                fullName = currentState.fullName,
                rawResumeText = currentState.profile?.rawResumeText ?: "",
                skills = listSkills,
                targetPosition = currentState.targetPosition,
                customAiPrompt = currentState.customAiPrompt
            )
            
            userProfileUseCase.saveProfile(updated)
            vacancyRepository.saveHhAccessToken(currentState.hhAccessToken)
            vacancyRepository.saveHhContactEmail(currentState.hhContactEmail)
            vacancyRepository.saveHhClientId(currentState.hhClientId)
            vacancyRepository.saveHhClientSecret(currentState.hhClientSecret)
            vacancyRepository.saveHhBackendUrl(currentState.hhBackendUrl)
            updateState { copy(isSaving = false, profile = updated) }
            sendSideEffect(ProfileSideEffect.ShowMessage("Profile updated successfully //"))
            sendSideEffect(ProfileSideEffect.NavigateBack)
        }
    }

    private fun clearLogs() {
        viewModelScope.launch {
            vacancyRepository.clearScoutLogs()
            sendSideEffect(ProfileSideEffect.ShowMessage("Scouting logs cleared //"))
        }
    }
}
