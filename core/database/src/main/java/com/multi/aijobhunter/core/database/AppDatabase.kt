package com.multi.aijobhunter.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [VacancyEntity::class, ScoutLogEntity::class], version = 2, exportSchema = false)
@TypeConverters(RoomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vacancyDao(): VacancyDao
    abstract fun scoutLogDao(): ScoutLogDao
}
