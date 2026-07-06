package com.multi.aijobhunter.core.data.di

import android.content.Context
import androidx.room.Room
import com.multi.aijobhunter.core.ai.AiService
import com.multi.aijobhunter.core.ai.AiServiceImpl
import com.multi.aijobhunter.core.ai.LlmApiKeyProvider
import com.multi.aijobhunter.core.data.LlmApiKeyProviderImpl
import com.multi.aijobhunter.core.data.EncryptedUserPreferences
import com.multi.aijobhunter.core.data.UserPreferences

import com.multi.aijobhunter.core.common.CoroutineDispatchers
import com.multi.aijobhunter.core.common.JobSearchCredentialsProvider
import com.multi.aijobhunter.core.data.VacancyRepositoryImpl
import com.multi.aijobhunter.core.database.AppDatabase
import com.multi.aijobhunter.core.database.VacancyDao
import com.multi.aijobhunter.core.database.ScoutLogDao
import com.multi.aijobhunter.core.network.ErrorInterceptor
import com.multi.aijobhunter.core.network.JobPluginManager
import com.multi.aijobhunter.core.network.LlmApiService
import com.multi.aijobhunter.domain.VacancyRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindsModule {

    @Binds
    @Singleton
    abstract fun bindVacancyRepository(
        impl: VacancyRepositoryImpl
    ): VacancyRepository

    @Binds
    @Singleton
    abstract fun bindLlmApiKeyProvider(
        impl: LlmApiKeyProviderImpl
    ): LlmApiKeyProvider

    @Binds
    @Singleton
    abstract fun bindAiService(
        impl: AiServiceImpl
    ): AiService
}

@Module
@InstallIn(SingletonComponent::class)
object DataProvidersModule {

    @Provides
    @Singleton
    fun provideCoroutineDispatchers(): CoroutineDispatchers = CoroutineDispatchers()

    @Provides
    @Singleton
    fun provideUserPreferences(
        @ApplicationContext context: Context
    ): UserPreferences = EncryptedUserPreferences(context)

    @Provides
    @Singleton
    fun provideRoomDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return try {
            net.sqlcipher.database.SQLiteDatabase.loadLibs(context)
            val passphrase = net.sqlcipher.database.SQLiteDatabase.getBytes("secure_passphrase".toCharArray())
            val factory = net.sqlcipher.database.SupportFactory(passphrase)
            Room.databaseBuilder(context, AppDatabase::class.java, "job_hunter_secure.db")
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        } catch (e: Throwable) {
            System.err.println("SQLCipher initialization failed, falling back to standard SQLite Room: ${e.localizedMessage}")
            Room.databaseBuilder(context, AppDatabase::class.java, "job_hunter_secure.db")
                .fallbackToDestructiveMigration()
                .build()
        }
    }

    @Provides
    fun provideVacancyDao(database: AppDatabase): VacancyDao = database.vacancyDao()

    @Provides
    fun provideScoutLogDao(database: AppDatabase): ScoutLogDao = database.scoutLogDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
            redactHeader("Authorization")
        }
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(ErrorInterceptor())
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        userPreferences: UserPreferences
    ): Retrofit {
        val baseUrl = userPreferences.getBaseUrl()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideLlmApiService(retrofit: Retrofit): LlmApiService {
        return retrofit.create(LlmApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideJobPluginManager(
        okHttpClient: OkHttpClient,
        userPreferences: UserPreferences
    ): JobPluginManager = JobPluginManager(okHttpClient, userPreferences)
}
