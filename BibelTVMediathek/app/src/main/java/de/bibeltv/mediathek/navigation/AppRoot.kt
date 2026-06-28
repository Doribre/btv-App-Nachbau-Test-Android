package de.bibeltv.mediathek.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.bibeltv.mediathek.feature.common.PlaceholderScreen
import de.bibeltv.mediathek.feature.home.HomeScreen
import de.bibeltv.mediathek.feature.player.PlayerScreen
import kotlin.reflect.KClass

private data class TabItem(
    val route: Route,
    val routeClass: KClass<*>,
    val label: String,
    val icon: ImageVector,
)

private val tabs = listOf(
    TabItem(Route.Start, Route.Start::class, "Start", Icons.Filled.Home),
    TabItem(Route.Discover, Route.Discover::class, "Entdecken", Icons.Filled.Explore),
    TabItem(Route.Search, Route.Search::class, "Suche", Icons.Filled.Search),
    TabItem(Route.Live, Route.Live::class, "Live", Icons.Filled.LiveTv),
)

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val onPlayer = currentDestination?.hasRoute(Route.Player::class) == true

    Scaffold(
        bottomBar = {
            if (!onPlayer) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(Route.Start) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Route.Start,
            modifier = Modifier.padding(padding),
        ) {
            composable<Route.Start> {
                HomeScreen(
                    onVideoClick = { v -> nav.navigate(Route.Player(title = v.title, isLive = false, crn = v.crn)) },
                    onLiveClick = { c -> nav.navigate(Route.Player(title = c.title, isLive = true, liveId = c.id)) },
                )
            }
            composable<Route.Discover> { PlaceholderScreen("Entdecken") }
            composable<Route.Search> { PlaceholderScreen("Suche") }
            composable<Route.Live> { PlaceholderScreen("Live") }
            composable<Route.Player> { PlayerScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
