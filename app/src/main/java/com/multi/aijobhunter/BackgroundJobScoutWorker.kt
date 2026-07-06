package com.multi.aijobhunter

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.multi.aijobhunter.domain.VacancyRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class BackgroundJobScoutWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: VacancyRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val matchedList = repository.fetchAndAnalyzeNewVacancies()
            if (matchedList.isNotEmpty()) {
                showSuccessNotification(matchedList)
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun showSuccessNotification(matchedList: List<com.multi.aijobhunter.core.model.Vacancy>) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "career_scout_notifications"
        val channelName = "Career AI Scout Status"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val bestMatch = matchedList.maxByOrNull { it.aiAnalysis?.matchScore ?: 0 }
        val title = if (matchedList.size == 1) {
            "🔥 Hot Match Found: ${bestMatch?.aiAnalysis?.matchScore}%!"
        } else {
            "🔥 ${matchedList.size} Hot Matches Found!"
        }
        
        val contentText = if (matchedList.size == 1) {
            "${bestMatch?.title} at ${bestMatch?.companyName}"
        } else {
            "Best: ${bestMatch?.title} (${bestMatch?.aiAnalysis?.matchScore}% match)"
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(101, notification)
    }
}
