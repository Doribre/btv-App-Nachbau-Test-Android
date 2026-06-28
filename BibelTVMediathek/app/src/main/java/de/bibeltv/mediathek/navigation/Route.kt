package de.bibeltv.mediathek.navigation

import kotlinx.serialization.Serializable

/** Type-safe Navigationsziele (kotlinx.serialization). */
sealed interface Route {
    @Serializable data object Start : Route
    @Serializable data object Discover : Route
    @Serializable data object Search : Route
    @Serializable data object Live : Route

    @Serializable
    data class Player(
        val title: String,
        val isLive: Boolean,
        val crn: String = "",
        val liveId: Int = -1,
    ) : Route
}
