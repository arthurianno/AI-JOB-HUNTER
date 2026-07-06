package com.multi.aijobhunter.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vacancies",
    indices = [
        Index(value = ["status"]),
        Index(value = ["matchScore"]),
        Index(value = ["id"], unique = true)
    ]
)
data class VacancyEntity(
    @PrimaryKey val id: String,
    val title: String,
    val companyName: String,
    val salaryFrom: Double?,
    val salaryTo: Double?,
    val salaryCurrency: String,
    val description: String,
    val url: String,
    val source: String,
    val status: String,
    val matchScore: Int,
    val aiAnalysisJson: String?,
    val createdAt: Long
)
