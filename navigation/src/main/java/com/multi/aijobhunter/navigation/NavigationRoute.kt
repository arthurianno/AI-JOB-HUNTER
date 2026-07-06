package com.multi.aijobhunter.navigation

import kotlinx.serialization.Serializable

sealed interface NavigationRoute {

    @Serializable
    data object AuthGraph : NavigationRoute

    @Serializable
    data object Onboarding : NavigationRoute

    @Serializable
    data object MainGraph : NavigationRoute

    @Serializable
    data object VacancyFeed : NavigationRoute

    @Serializable
    data class VacancyDetails(val vacancyId: String) : NavigationRoute

    @Serializable
    data object TrackerKanban : NavigationRoute

    @Serializable
    data object UserProfile : NavigationRoute
}
