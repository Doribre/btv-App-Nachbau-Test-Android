package de.bibeltv.mediathek.navigation

import androidx.annotation.OptIn
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
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.bibeltv.mediathek.feature.browse.BrowseScreen
import de.bibeltv.mediathek.feature.detail.SeriesDetailScreen
import de.bibeltv.mediathek.feature.detail.VideoDetailScreen
import de.bibeltv.mediathek.feature.home.HomeScreen
import de.bibeltv.mediathek.feature.live.LiveScreen
import de.bibeltv.mediathek.feature.player.PlayerScreen
import de.bibeltv.mediathek.feature.search.SearchScreen
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

@OptIn(UnstableApi::class)
@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val hideBottomBar = currentDestination?.let { d ->
        d.hasRoute(Route.Player::class) || d.hasRoute(Route.VideoDetail::class) || d.hasRoute(Route.SeriesDetail::class)
    } == true

    Scaffold(
        bottomBar = {
            if (!hideBottomBar) {
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
                    onVideoClick = { v -> nav.navigate(Route.VideoDetail(crn = v.crn)) { launchSingleTop = true } },
                    onLiveClick = { c -> nav.navigate(Route.Player(title = c.title, isLive = true, liveId = c.id)) { launchSingleTop = true } },
                )
            }
            composable<Route.Discover> {
                BrowseScreen(onVideoClick = { v -> nav.navigate(Route.VideoDetail(crn = v.crn)) { launchSingleTop = true } })
            }
            composable<Route.Search> {
                SearchScreen(onVideoClick = { v -> nav.navigate(Route.VideoDetail(crn = v.crn)) { launchSingleTop = true } })
            }
            composable<Route.Live> {
                LiveScreen(onLiveClick = { c -> nav.navigate(Route.Player(title = c.title, isLive = true, liveId = c.id)) { launchSingleTop = true } })
            }
            composable<Route.Player> { PlayerScreen(onBack = { nav.popBackStack() }) }
            composable<Route.VideoDetail> {
                VideoDetailScreen(
                    onBack = { nav.popBackStack() },
                    onPlay = { crn -> nav.navigate(Route.Player(title = "", isLive = false, crn = crn)) { launchSingleTop = true } },
                    onOpenSeries = { id -> nav.navigate(Route.SeriesDetail(id = id)) { launchSingleTop = true } },
                )
            }
            composable<Route.SeriesDetail> {
                SeriesDetailScreen(
                    onBack = { nav.popBackStack() },
                    onEpisode = { crn -> nav.navigate(Route.VideoDetail(crn = crn)) { launchSingleTop = true } },
                )
            }
        }
    }
}
