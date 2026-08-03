package ir.hamedan.budgetmanagement.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes
 */
sealed interface AppRoute {

    @Serializable
    data object Splash : AppRoute

    @Serializable
    data object Login : AppRoute

    @Serializable
    data object MainStructure : AppRoute

    @Serializable
    data class AddScreen(
        val highlightId: String? = null
    ) : AppRoute

    @Serializable
    data object Categories : AppRoute

    @Serializable
    data object Limits : AppRoute

    @Serializable
    data object Upcoming : AppRoute

    @Serializable
    data object Goals : AppRoute

    @Serializable
    data object Debt : AppRoute
}

/**
 * Routeهای مربوط به Bottom Navigation داخل MainStructure
 */
sealed interface MainTabRoute {

    @Serializable
    data object Home : MainTabRoute

    @Serializable
    data object Transactions : MainTabRoute

    @Serializable
    data object Analytics : MainTabRoute

    @Serializable
    data object Settings : MainTabRoute
}