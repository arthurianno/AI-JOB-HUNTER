package com.multi.aijobhunter.feature.auth

import androidx.lifecycle.viewModelScope
import com.multi.aijobhunter.core.common.BaseViewModel
import com.multi.aijobhunter.core.model.UserProfile
import com.multi.aijobhunter.domain.VacancyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: VacancyRepository
) : BaseViewModel<AuthUiState, AuthIntent, AuthSideEffect>(AuthUiState()) {

    init {
        val key = repository.getApiKey()
        val url = repository.getBaseUrl()
        val model = repository.getModelName()
        updateState {
            copy(apiKey = key, baseUrl = url, modelName = model)
        }
    }

    override fun handleIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.ChangeApiKey -> updateState { copy(apiKey = intent.key) }
            is AuthIntent.ChangeBaseUrl -> {
                val guessedModel = when {
                    intent.url.contains("deepseek") -> "deepseek-chat"
                    intent.url.contains("11434") -> "llama3"
                    else -> "gpt-4o-mini"
                }
                updateState { copy(baseUrl = intent.url, modelName = guessedModel) }
            }
            is AuthIntent.ChangeModelName -> updateState { copy(modelName = intent.model) }
            is AuthIntent.ClearError -> updateState { copy(errorMessage = null) }
            is AuthIntent.SaveCredentials -> {
                repository.saveApiKey(currentState.apiKey)
                repository.saveBaseUrl(currentState.baseUrl)
                repository.saveModelName(currentState.modelName)
                sendSideEffect(AuthSideEffect.ShowMessage("Credentials saved successfully //"))
            }
            is AuthIntent.UploadResume -> parseAndSaveResume(intent.rawText)
        }
    }

    private fun parseAndSaveResume(rawText: String) {
        if (rawText.isBlank()) {
            updateState { copy(errorMessage = "Resume text cannot be empty.") }
            return
        }

        updateState {
            copy(
                isParsingCV = true,
                parsingLogs = emptyList(),
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val logs = mutableListOf<String>()
            
            fun addLog(log: String) {
                logs.add(log)
                updateState { copy(parsingLogs = logs.toList()) }
            }

            // Имитация этапов парсинга терминала
            delay(600)
            addLog("[SYSTEM] > Initializing PDF Resume Parser Engine...")
            delay(800)
            addLog("[PARSING PDF...] > Extracting structural blocks and metadata...")
            delay(1000)
            addLog("[EXTRACTING SKILLS...] > Scanning token semantic vectors...")
            
            // Извлекаем ключевые слова для демонстрации
            val skills = mutableListOf("Kotlin", "Android SDK", "Jetpack Compose", "Coroutines", "Flow", "Clean Architecture", "MVI", "Dagger Hilt", "Room")
            if (rawText.lowercase().contains("webrtc")) skills.add("WebRTC")
            if (rawText.lowercase().contains("ci/cd")) skills.add("CI/CD")
            if (rawText.lowercase().contains("git")) skills.add("Git")
            
            delay(800)
            addLog("[CREATING VECTOR PROMPT...] > Matching target career path...")
            
            val profile = UserProfile(
                fullName = "Артур Девелопер",
                rawResumeText = rawText,
                skills = skills,
                targetPosition = "Senior Android Developer",
                customAiPrompt = "Ищи только продуктовый финтех на Kotlin, аутсорс не предлагать. Приоритет удаленке."
            )
            
            repository.saveUserProfile(profile)
            
            delay(600)
            addLog("[SUCCESS] > Profile created with ${skills.size} matching vectors!")
            addLog("[SUCCESS] > Career scout agent activated.")
            
            delay(500)
            updateState {
                copy(
                    isParsingCV = false,
                    resumeUploaded = true
                )
            }
            sendSideEffect(AuthSideEffect.NavigateToFeed)
        }
    }
}
