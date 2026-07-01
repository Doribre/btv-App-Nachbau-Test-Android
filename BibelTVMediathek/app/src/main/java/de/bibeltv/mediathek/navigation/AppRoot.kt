package de.bibeltv.mediathek.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.bibeltv.mediathek.feature.bible.BibleBooksScreen
import de.bibeltv.mediathek.feature.bible.BibleReaderScreen
import de.bibeltv.mediathek.feature.browse.BrowseScreen
import de.bibeltv.mediathek.feature.common.BrandWordmark
import de.bibeltv.mediathek.feature.detail.SeriesDetailScreen
import de.bibeltv.mediathek.feature.detail.VideoDetailScreen
import de.bibeltv.mediathek.feature.home.HomeScreen
import de.bibeltv.mediathek.feature.info.InfoScreen
import de.bibeltv.mediathek.feature.live.LiveScreen
import de.bibeltv.mediathek.feature.player.PlayerScreen
import de.bibeltv.mediathek.feature.search.SearchScreen
import de.bibeltv.mediathek.feature.settings.SettingsScreen
import kotlinx.coroutines.launch
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
    TabItem(Route.Bible, Route.Bible::class, "Bibelthek", Icons.Filled.Book),
    TabItem(Route.Search, Route.Search::class, "Suche", Icons.Filled.Search),
    TabItem(Route.Live, Route.Live::class, "Live", Icons.Filled.LiveTv),
)

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val currentTab = tabs.firstOrNull { tab ->
        currentDestination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
    }
    val isMainTab = currentTab != null

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun openTab(route: Route) {
        nav.navigate(route) {
            popUpTo(Route.Start) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isMainTab || drawerState.isOpen,
        drawerContent = {
            AppDrawer(
                currentDestination = currentDestination,
                onTab = { route -> scope.launch { drawerState.close() }; openTab(route) },
                onInfo = { scope.launch { drawerState.close() }; nav.navigate(Route.Info) { launchSingleTop = true } },
                onSettings = { scope.launch { drawerState.close() }; nav.navigate(Route.Settings) { launchSingleTop = true } },
            )
        },
    ) {
        Scaffold(
            // Insets übernehmen Top-/Bottom-Bar bzw. die jeweiligen Screens selbst → kein doppelter Abstand.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (isMainTab) {
                    CenterAlignedTopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menü")
                            }
                        },
                        title = {
                            if (currentTab?.route == Route.Start) {
                                BrandWordmark()
                            } else {
                                Text(currentTab?.label.orEmpty())
                            }
                        },
                    )
                }
            },
            bottomBar = {
                if (isMainTab) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { openTab(tab.route) },
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
                    SearchScreen(
                        onVideoClick = { v -> nav.navigate(Route.VideoDetail(crn = v.crn)) { launchSingleTop = true } },
                        onOpenBibleVerse = { hit ->
                            nav.navigate(Route.BibleReader(bookSlug = hit.bookSlug, bookName = hit.bookName, chapter = hit.chapter)) { launchSingleTop = true }
                        },
                    )
                }
                composable<Route.Live> {
                    LiveScreen(onLiveClick = { c -> nav.navigate(Route.Player(title = c.title, isLive = true, liveId = c.id)) { launchSingleTop = true } })
                }
                composable<Route.Info> { InfoScreen(onBack = { nav.popBackStack() }) }
                composable<Route.Settings> { SettingsScreen(onBack = { nav.popBackStack() }) }
                composable<Route.Bible> {
                    BibleBooksScreen(
                        onOpenBook = { book ->
                            nav.navigate(Route.BibleReader(bookSlug = book.slug, bookName = book.name, chapter = 1)) { launchSingleTop = true }
                        },
                    )
                }
                composable<Route.BibleReader> {
                    BibleReaderScreen(
                        onBack = { nav.popBackStack() },
                        onPlayVideo = { crn, startSeconds ->
                            nav.navigate(Route.Player(title = "", isLive = false, crn = crn, startSeconds = startSeconds)) { launchSingleTop = true }
                        },
                    )
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
}

@Composable
private fun AppDrawer(
    currentDestination: NavDestination?,
    onTab: (Route) -> Unit,
    onInfo: () -> Unit,
    onSettings: () -> Unit,
) {
    ModalDrawerSheet {
        BrandWordmark(modifier = Modifier.padding(start = 28.dp, top = 24.dp, bottom = 16.dp))
        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
        tabs.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
            NavigationDrawerItem(
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
                selected = selected,
                onClick = { onTab(tab.route) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Info, contentDescription = null) },
            label = { Text("Info") },
            selected = false,
            onClick = onInfo,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text("Einstellungen") },
            selected = false,
            onClick = onSettings,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        )
    }
}
