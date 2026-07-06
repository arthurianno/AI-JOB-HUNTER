package com.multi.aijobhunter.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Vacancy(
    val id: String,
    val title: String,
    val companyName: String,
    val salary: Salary?,
    val description: String,
    val url: String,
    val source: JobSource,
    val status: VacancyStatus,
    val aiAnalysis: AiAnalysis?,
    val createdAt: Long
)

@Serializable
data class Salary(
    val from: Double?,
    val to: Double?,
    val currency: String
)

enum class JobSource {
    HH, LINKEDIN, HABR, TELEGRAM, CUSTOM
}

enum class VacancyStatus {
    PENDING_ANALYSIS, MATCHED, ARCHIVED, APPLIED, REJECTED, INTERVIEW
}

@Serializable
data class AiAnalysis(
    val matchScore: Int,
    val summary: String,
    val pros: List<String>,
    val cons: List<String>,
    val radarMetrics: RadarMetrics,
    val generatedCoverLetter: String? = null
)

@Serializable
data class RadarMetrics(
    val hardSkills: Float,
    val softSkills: Float,
    val experience: Float,
    val salaryMatch: Float,
    val industryMatch: Float
)

@Serializable
data class ScoutLog(
    val id: Long = 0,
    val timestamp: Long,
    val scannedCount: Int,
    val matchedCount: Int,
    val status: String,
    val message: String
)
