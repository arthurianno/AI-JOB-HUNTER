package com.multi.aijobhunter.core.data

import com.multi.aijobhunter.core.model.*
import com.multi.aijobhunter.core.database.VacancyEntity
import com.multi.aijobhunter.core.database.ScoutLogEntity
import com.multi.aijobhunter.core.network.RawVacancy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun VacancyEntity.toDomainModel(): Vacancy {
    val salary = if (salaryFrom != null || salaryTo != null) {
        Salary(from = salaryFrom, to = salaryTo, currency = salaryCurrency)
    } else {
        null
    }
    val aiAnalysis = aiAnalysisJson?.let {
        try {
            json.decodeFromString<AiAnalysis>(it)
        } catch (e: Exception) {
            null
        }
    }
    return Vacancy(
        id = id,
        title = title,
        companyName = companyName,
        salary = salary,
        description = description,
        url = url,
        source = try { JobSource.valueOf(source) } catch (e: Exception) { JobSource.CUSTOM },
        status = try { VacancyStatus.valueOf(status) } catch (e: Exception) { VacancyStatus.PENDING_ANALYSIS },
        aiAnalysis = aiAnalysis,
        createdAt = createdAt
    )
}

fun RawVacancy.toEntity(): VacancyEntity {
    return VacancyEntity(
        id = id,
        title = title,
        companyName = companyName,
        salaryFrom = salaryFrom,
        salaryTo = salaryTo,
        salaryCurrency = salaryCurrency,
        description = description,
        url = url,
        source = source,
        status = VacancyStatus.PENDING_ANALYSIS.name,
        matchScore = 0,
        aiAnalysisJson = null,
        createdAt = createdAt
    )
}

fun Vacancy.toEntity(): VacancyEntity {
    return VacancyEntity(
        id = id,
        title = title,
        companyName = companyName,
        salaryFrom = salary?.from,
        salaryTo = salary?.to,
        salaryCurrency = salary?.currency ?: "USD",
        description = description,
        url = url,
        source = source.name,
        status = status.name,
        matchScore = aiAnalysis?.matchScore ?: 0,
        aiAnalysisJson = aiAnalysis?.let { json.encodeToString(it) },
        createdAt = createdAt
    )
}

fun ScoutLogEntity.toDomainModel(): ScoutLog {
    return ScoutLog(
        id = id,
        timestamp = timestamp,
        scannedCount = scannedCount,
        matchedCount = matchedCount,
        status = status,
        message = message
    )
}

fun ScoutLog.toEntity(): ScoutLogEntity {
    return ScoutLogEntity(
        id = id,
        timestamp = timestamp,
        scannedCount = scannedCount,
        matchedCount = matchedCount,
        status = status,
        message = message
    )
}
