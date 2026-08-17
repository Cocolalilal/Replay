package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.domain.utils.connectArtists
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.ui.component.glass.BottomNavigationOrchestrator
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDestination
import com.maxrave.simpmusic.ui.navigation.destination.search.SearchDestination
import com.maxrave.simpmusic.viewModel.SearchViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import org.koin.compose.koinInject
import kotlin.reflect.KClass

private const val TAG = "LiquidGlassAppBottomNavigationBar"

@Composable
actual fun LiquidGlassAppBottomNavigationBar(
    startDestination: Any,
    navController: NavController,
    backdrop: PlatformBackdrop,
    viewModel: SharedViewModel,
    isScrolledToTop: Boolean,
    scrollDirection: Int,
    scrollEpoch: Int,
    onOpenNowPlaying: () -> Unit,
    reloadDestinationIfNeeded: (KClass<*>) -> Unit,
) {
    val searchViewModel: SearchViewModel = koinInject()
    val nowPlayingData by viewModel.nowPlayingState.collectAsStateWithLifecycle()
    val controllerState by viewModel.controllerState.collectAsStateWithLifecycle()
    val searchScreenState by searchViewModel.searchScreenState.collectAsStateWithLifecycle()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    var isShowMiniPlayer by rememberSaveable { mutableStateOf(false) }
    var selectedIndex by rememberSaveable {
        mutableIntStateOf(
            when (startDestination) {
                is HomeDestination -> 0
                is LibraryDestination -> 1
                else -> 0
            }
        )
    }

    var isSearchActive by remember { mutableStateOf(false) }
    var isManuallyExpanded by remember { mutableStateOf(false) }

    var currentRouteKey by remember { mutableStateOf("home") }
    val routeAtTop = remember { mutableStateMapOf<String, Boolean>() }
    var lastLiveAtTop by remember { mutableStateOf(isScrolledToTop) }

    LaunchedEffect(isScrolledToTop, scrollDirection, scrollEpoch) {
        if (scrollDirection < 0) {
            isManuallyExpanded = false
        }
        lastLiveAtTop = isScrolledToTop
        routeAtTop[currentRouteKey] = isScrolledToTop
        Logger.d(TAG, "scroll: atTop=$isScrolledToTop dir=$scrollDirection epoch=$scrollEpoch route=$currentRouteKey manual=$isManuallyExpanded lastLive=$lastLiveAtTop")
    }

    LaunchedEffect(nowPlayingData) {
        isShowMiniPlayer = !(nowPlayingData?.mediaItem == null || nowPlayingData?.mediaItem == GenericMediaItem.EMPTY)
    }

    LaunchedEffect(currentBackStackEntry) {
        currentBackStackEntry?.destination?.let { current ->
            Logger.d(TAG, "LiquidGlassAppBottomNavigationBar: current route: ${current.route}")
            val inSearch = current.hasRoute(SearchDestination::class)
            val inLibrary = current.hasRoute(LibraryDestination::class)
            val inHome = current.hasRoute(HomeDestination::class)

            val key = when {
                inSearch -> "search"
                inLibrary -> "library"
                inHome -> "home"
                else -> "detail"
            }
            if (key != currentRouteKey) {
                routeAtTop[currentRouteKey] = lastLiveAtTop
                currentRouteKey = key
                if (!isManuallyExpanded) {
                    lastLiveAtTop = routeAtTop[key] ?: true
                }
                Logger.d(TAG, "route change -> $key, restored lastLive=$lastLiveAtTop, manual=$isManuallyExpanded, map=$routeAtTop")
            }

            isSearchActive = inSearch
            searchViewModel.setSearchBarActive(inSearch)
            if (inLibrary) selectedIndex = 1
            else if (inHome) selectedIndex = 0
        }
    }

    val targetCollapse = if (isManuallyExpanded || lastLiveAtTop) 0f else 1f
    val scrollCollapseProgress by animateFloatAsState(
        targetValue = targetCollapse,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 500f),
        label = "scrollCollapseProgress"
    )

    fun selectTab(index: Int) {
        val dest = if (index == 0) HomeDestination else LibraryDestination
        val destClass = if (index == 0) HomeDestination::class else LibraryDestination::class

        if (selectedIndex == index) {
            if (currentBackStackEntry?.destination?.hierarchy?.any { it.hasRoute(destClass) } == true) {
                reloadDestinationIfNeeded(destClass)
            } else {
                navController.navigate(dest)
            }
        } else {
            selectedIndex = index
            navController.navigate(dest) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val songEntity: SongEntity? = nowPlayingData?.songEntity
    val trackTitle = songEntity?.title ?: nowPlayingData?.track?.title ?: ""
    val trackArtist = songEntity?.artistName?.connectArtists() ?: nowPlayingData?.track?.artists?.joinToString(", ") { it.name } ?: ""
    val artworkUrl = songEntity?.thumbnails ?: nowPlayingData?.track?.thumbnails?.lastOrNull()?.url ?: ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
            .imePadding()
    ) {
        BottomNavigationOrchestrator(
            selectedTabIndex = selectedIndex,
            onTabSelected = { index -> selectTab(index) },
            isSearchActive = isSearchActive,
            onSearchActiveChange = { active ->
                isSearchActive = active
                searchViewModel.setSearchBarActive(active)
                if (active) {
                    if (currentBackStackEntry?.destination?.hasRoute(SearchDestination::class) != true) {
                        navController.navigate(SearchDestination) {
                            launchSingleTop = true
                        }
                    }
                } else {
                    isManuallyExpanded = true
                    if (currentBackStackEntry?.destination?.hasRoute(SearchDestination::class) == true) {
                        navController.popBackStack()
                    }
                }
            },
            backdrop = backdrop as LayerBackdrop,
            scrollCollapseProgress = scrollCollapseProgress,
            isShowMiniPlayer = isShowMiniPlayer,
            trackTitle = trackTitle,
            trackArtist = trackArtist,
            artworkUrl = artworkUrl,
            isPlaying = controllerState.isPlaying,
            onPlayPauseToggle = {
                viewModel.onUIEvent(UIEvent.PlayPause)
            },
            onPreviousTrack = { viewModel.onUIEvent(UIEvent.Previous) },
            onNextTrack = { viewModel.onUIEvent(UIEvent.Next) },
            onExpandFullPlayer = onOpenNowPlaying,
            onDismissMiniPlayer = {
                viewModel.onUIEvent(UIEvent.Stop)
                viewModel.isServiceRunning = false
            },
            searchText = searchScreenState.barQuery,
            onSearchTextChange = { text ->
                searchViewModel.setSearchBarQuery(text)
                if (text.isNotEmpty()) {
                    searchViewModel.suggestQuery(text)
                }
            },
            onSearchSubmit = { query ->
                if (query.isNotEmpty()) {
                    searchViewModel.insertSearchHistory(query)
                    searchViewModel.searchAll(query)
                    if (currentBackStackEntry?.destination?.hasRoute(SearchDestination::class) != true) {
                        navController.navigate(SearchDestination) {
                            launchSingleTop = true
                        }
                    }
                }
            },
            onSearchFieldTapped = {
                searchViewModel.setSearchFieldTapped(true)
            },
            onExpandRequested = {
                isManuallyExpanded = true
                Logger.d(TAG, "manual expand requested -> $isManuallyExpanded")
            }
        )
    }
}