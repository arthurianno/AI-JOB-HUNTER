package com.multi.aijobhunter.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val fullName: String,
    val rawResumeText: String,
    val skills: List<String>,
    val targetPosition: String,
    val customAiPrompt: String
)
