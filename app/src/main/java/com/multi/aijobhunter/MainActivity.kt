package com.multi.aijobhunter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.multi.aijobhunter.feature.auth.AuthScreen
import com.multi.aijobhunter.feature.auth.AuthViewModel
import com.multi.aijobhunter.feature.feed.FeedViewModel
import com.multi.aijobhunter.feature.feed.VacancyDetailsScreen
import com.multi.aijobhunter.feature.feed.VacancyDetailsViewModel
import com.multi.aijobhunter.feature.feed.VacancyFeedScreen
import com.multi.aijobhunter.feature.profile.ProfileScreen
import com.multi.aijobhunter.feature.profile.ProfileViewModel
import com.multi.aijobhunter.feature.shared_ui.AiJobHunterTheme
import com.multi.aijobhunter.feature.tracker.TrackerScreen
import com.multi.aijobhunter.feature.tracker.TrackerViewModel
import com.multi.aijobhunter.navigation.NavigationRoute
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

import com.multi.aijobhunter.core.data.UserPreferences
import com.multi.aijobhunter.domain.VacancyRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var vacancyRepository: VacancyRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Инициализация WorkManager Periodic Sync
        scheduleBackgroundJobScout()

        // Handle OAuth deep link if opened via cold start
        intent?.let { handleDeepLink(it) }

        // Определяем стартовый экран: если профиль уже есть — сразу в ленту
        val hasProfile = userPreferences.readProfile()?.let {
            it.rawResumeText.isNotBlank()
        } ?: false

        setContent {
            AiJobHunterTheme {
                val navController = rememberNavController()
                val startRoute: Any = if (hasProfile) {
                    NavigationRoute.VacancyFeed
                } else {
                    NavigationRoute.Onboarding
                }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPaddingg ->
                    NavHost(
                        navController = navController,
                        startDestination = startRoute,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable<NavigationRoute.Onboarding> {
                            val authViewModel: AuthViewModel = hiltViewModel()
                            AuthScreen(
                                viewModel = authViewModel,
                                onNavigateToFeed = {
                                    navController.navigate(NavigationRoute.VacancyFeed) {
                                        popUpTo(NavigationRoute.Onboarding) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable<NavigationRoute.VacancyFeed> {
                            val feedViewModel: FeedViewModel = hiltViewModel()
                            VacancyFeedScreen(
                                viewModel = feedViewModel,
                                onNavigateToDetails = { vacancyId ->
                                    navController.navigate(NavigationRoute.VacancyDetails(vacancyId))
                                },
                                onNavigateToProfile = {
                                    navController.navigate(NavigationRoute.UserProfile)
                                },
                                onNavigateToTracker = {
                                    navController.navigate(NavigationRoute.TrackerKanban)
                                }
                            )
                        }

                        composable<NavigationRoute.VacancyDetails>(
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "aijobhunter://vacancy/{vacancyId}" }
                            )
                        ) { backStackEntry ->
                            val vacancyId = backStackEntry.arguments?.getString("vacancyId") ?: ""
                            val detailsViewModel: VacancyDetailsViewModel = hiltViewModel()
                            VacancyDetailsScreen(
                                viewModel = detailsViewModel,
                                vacancyId = vacancyId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable<NavigationRoute.UserProfile> {
                            val profileViewModel: ProfileViewModel = hiltViewModel()
                            ProfileScreen(
                                viewModel = profileViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable<NavigationRoute.TrackerKanban> {
                            val trackerViewModel: TrackerViewModel = hiltViewModel()
                            TrackerScreen(
                                viewModel = trackerViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun scheduleBackgroundJobScout() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Только Wi-Fi
            .setRequiresCharging(true) // Только на зарядке
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<BackgroundJobScoutWorker>(4, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "AiJobScoutUniqueWorkName",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: android.content.Intent) {
        val data = intent.data ?: return
        if (data.scheme == "aijobhunter" && data.host == "oauth") {
            val code = data.getQueryParameter("code")
            if (code != null) {
                lifecycleScope.launch {
                    val success = vacancyRepository.exchangeHhCode(code)
                    if (success) {
                        android.widget.Toast.makeText(this@MainActivity, "HeadHunter account connected successfully!", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        android.widget.Toast.makeText(this@MainActivity, "Failed to exchange HeadHunter authorization code.", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}