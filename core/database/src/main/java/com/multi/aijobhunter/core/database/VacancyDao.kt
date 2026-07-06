package com.multi.aijobhunter.core.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VacancyDao {
    @Query("SELECT * FROM vacancies WHERE status = 'MATCHED' ORDER BY createdAt DESC")
    fun getMatchedVacanciesPagingSource(): PagingSource<Int, VacancyEntity>

    @Query("SELECT * FROM vacancies WHERE id = :id")
    fun getVacancyByIdFlow(id: String): Flow<VacancyEntity?>

    @Query("SELECT * FROM vacancies WHERE id = :id")
    suspend fun getVacancyById(id: String): VacancyEntity?

    @Query("SELECT * FROM vacancies WHERE status = 'PENDING_ANALYSIS'")
    suspend fun getPendingVacancies(): List<VacancyEntity>

    @Query("SELECT * FROM vacancies ORDER BY createdAt DESC")
    fun getAllVacanciesFlow(): Flow<List<VacancyEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVacancies(vacancies: List<VacancyEntity>): List<Long>

    @Query("UPDATE vacancies SET status = 'PENDING_ANALYSIS' WHERE status = 'ARCHIVED' OR status = 'MATCHED'")
    suspend fun resetAnalyzedVacancies()

    @Update
    suspend fun updateVacancy(vacancy: VacancyEntity)

    @Query("UPDATE vacancies SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE vacancies SET aiAnalysisJson = :aiAnalysisJson WHERE id = :id")
    suspend fun updateVacancyAnalysis(id: String, aiAnalysisJson: String?)
}
