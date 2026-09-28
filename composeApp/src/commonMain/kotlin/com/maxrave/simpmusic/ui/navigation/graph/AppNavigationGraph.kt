package com.maxrave.simpmusic.ui.navigation.graph

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.theme.ForceDarkContent
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDestination
import com.maxrave.simpmusic.ui.navigation.destination.player.FullscreenDestination
import com.maxrave.simpmusic.ui.navigation.destination.search.SearchDestination
import com.maxrave.simpmusic.ui.screen.home.HomeScreen
import com.maxrave.simpmusic.ui.screen.library.LibraryScreen
import com.maxrave.simpmusic.ui.screen.other.SearchScreen
import com.maxrave.simpmusic.ui.screen.player.FullscreenPlayer

private fun NavDestination?.getTabOrder(): Int? {
    if (this == null) return null
    return when {
        hasRoute(HomeDestination::class) -> 0
        hasRoute(SearchDestination::class) -> 1
        hasRoute(LibraryDestination::class) -> 2
        else -> null
    }
}

private const val TAB_SLIDE_FRACTION = 0.30f
private const val DETAIL_SLIDE_FRACTION = 0.85f
private const val PARALLAX_SLIDE_FRACTION = 0.25f
private const val FULLSCREEN_SLIDE_FRACTION = 0.40f

@Composable
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
fun AppNavigationGraph(
    innerPadding: PaddingValues,
    navController: NavHostController,
    startDestination: Any = HomeDestination,
    hideNavBar: () -> Unit = { },
    showNavBar: (shouldShowNowPlayingSheet: Boolean) -> Unit = { },
    showNowPlayingSheet: () -> Unit = {},
    onScrolling: (onTop: Boolean, direction: Int) -> Unit = { _, _ -> },
) {
    NavHost(
        navController,
        startDestination = startDestination,
        enterTransition = {
            val initialTab = initialState.destination.getTabOrder()
            val targetTab = targetState.destination.getTabOrder()
            val isTargetFullscreen = targetState.destination.hasRoute(FullscreenDestination::class)

            when {
                isTargetFullscreen -> {
                    slideInVertically(
                        animationSpec = tween(350, easing = FastOutSlowInEasing),
                    ) { (it * FULLSCREEN_SLIDE_FRACTION).toInt() } +
                        fadeIn(tween(300, easing = FastOutSlowInEasing))
                }
                initialTab != null && targetTab != null -> {
                    if (targetTab > initialTab) {
                        // Moving Left to Right (e.g. Home -> Search -> Library): Target enters from right
                        slideInHorizontally(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                        ) { (it * TAB_SLIDE_FRACTION).toInt() } +
                            fadeIn(tween(300, delayMillis = 50, easing = FastOutSlowInEasing))
                    } else if (targetTab < initialTab) {
                        // Moving Right to Left (e.g. Library -> Search -> Home): Target enters from left
                        slideInHorizontally(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                        ) { (-it * TAB_SLIDE_FRACTION).toInt() } +
                            fadeIn(tween(300, delayMillis = 50, easing = FastOutSlowInEasing))
                    } else {
                        fadeIn(tween(250, easing = FastOutSlowInEasing))
                    }
                }
                else -> {
                    // Forward hierarchical navigation (Push): Child enters from right
                    slideInHorizontally(
                        animationSpec = tween(350, easing = FastOutSlowInEasing),
                    ) { (it * DETAIL_SLIDE_FRACTION).toInt() } +
                        fadeIn(tween(300, easing = FastOutSlowInEasing))
                }
            }
        },
        exitTransition = {
            val initialTab = initialState.destination.getTabOrder()
            val targetTab = targetState.destination.getTabOrder()
            val isInitialFullscreen = initialState.destination.hasRoute(FullscreenDestination::class)

            when {
                isInitialFullscreen -> {
                    slideOutVertically(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                    ) { (it * FULLSCREEN_SLIDE_FRACTION).toInt() } +
                        fadeOut(tween(250, easing = FastOutSlowInEasing))
                }
                initialTab != null && targetTab != null -> {
                    if (targetTab > initialTab) {
                        // Moving Left to Right: Initial exits to left
                        slideOutHorizontally(
                            animationSpec = tween(250, easing = FastOutSlowInEasing),
                        ) { (-it * TAB_SLIDE_FRACTION).toInt() } +
                            fadeOut(tween(200, easing = FastOutSlowInEasing))
                    } else if (targetTab < initialTab) {
                        // Moving Right to Left: Initial exits to right
                        slideOutHorizontally(
                            animationSpec = tween(250, easing = FastOutSlowInEasing),
                        ) { (it * TAB_SLIDE_FRACTION).toInt() } +
                            fadeOut(tween(200, easing = FastOutSlowInEasing))
                    } else {
                        fadeOut(tween(200, easing = FastOutSlowInEasing))
                    }
                }
                else -> {
                    // Forward hierarchical navigation (Push): Parent exits to left with subtle parallax
                    slideOutHorizontally(
                        animationSpec = tween(350, easing = FastOutSlowInEasing),
                    ) { (-it * PARALLAX_SLIDE_FRACTION).toInt() } +
                        fadeOut(tween(250, easing = FastOutSlowInEasing))
                }
            }
        },
        popEnterTransition = {
            val initialTab = initialState.destination.getTabOrder()
            val targetTab = targetState.destination.getTabOrder()
            val isTargetFullscreen = targetState.destination.hasRoute(FullscreenDestination::class)

            when {
                isTargetFullscreen -> {
                    slideInVertically(
                        animationSpec = tween(350, easing = FastOutSlowInEasing),
                    ) { (it * FULLSCREEN_SLIDE_FRACTION).toInt() } +
                        fadeIn(tween(300, easing = FastOutSlowInEasing))
                }
                initialTab != null && targetTab != null -> {
                    if (targetTab > initialTab) {
                        slideInHorizontally(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                        ) { (it * TAB_SLIDE_FRACTION).toInt() } +
                            fadeIn(tween(300, delayMillis = 50, easing = FastOutSlowInEasing))
                    } else if (targetTab < initialTab) {
                        slideInHorizontally(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                        ) { (-it * TAB_SLIDE_FRACTION).toInt() } +
                            fadeIn(tween(300, delayMillis = 50, easing = FastOutSlowInEasing))
                    } else {
                        fadeIn(tween(250, easing = FastOutSlowInEasing))
                    }
                }
                else -> {
                    // Backward hierarchical navigation (Pop): Parent returns from left parallax
                    slideInHorizontally(
                        animationSpec = tween(350, easing = FastOutSlowInEasing),
                    ) { (-it * PARALLAX_SLIDE_FRACTION).toInt() } +
                        fadeIn(tween(300, easing = FastOutSlowInEasing))
                }
            }
        },
        popExitTransition = {
            val initialTab = initialState.destination.getTabOrder()
            val targetTab = targetState.destination.getTabOrder()
            val isInitialFullscreen = initialState.destination.hasRoute(FullscreenDestination::class)

            when {
                isInitialFullscreen -> {
                    slideOutVertically(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                    ) { (it * FULLSCREEN_SLIDE_FRACTION).toInt() } +
                        fadeOut(tween(250, easing = FastOutSlowInEasing))
                }
                initialTab != null && targetTab != null -> {
                    if (targetTab > initialTab) {
                        slideOutHorizontally(
                            animationSpec = tween(250, easing = FastOutSlowInEasing),
                        ) { (-it * TAB_SLIDE_FRACTION).toInt() } +
                            fadeOut(tween(200, easing = FastOutSlowInEasing))
                    } else if (targetTab < initialTab) {
                        slideOutHorizontally(
                            animationSpec = tween(250, easing = FastOutSlowInEasing),
                        ) { (it * TAB_SLIDE_FRACTION).toInt() } +
                            fadeOut(tween(200, easing = FastOutSlowInEasing))
                    } else {
                        fadeOut(tween(200, easing = FastOutSlowInEasing))
                    }
                }
                else -> {
                    // Backward hierarchical navigation (Pop): Child exits to right
                    slideOutHorizontally(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                    ) { (it * DETAIL_SLIDE_FRACTION).toInt() } +
                        fadeOut(tween(220, easing = FastOutSlowInEasing))
                }
            }
        },
    ) {
        // Bottom bar destinations
        composable<HomeDestination> {
            HomeScreen(
                onScrolling = onScrolling,
                navController = navController,
            )
        }
        composable<SearchDestination> {
            SearchScreen(
                navController = navController,
            )
        }
        composable<LibraryDestination> {
            LibraryScreen(
                innerPadding = innerPadding,
                navController = navController,
                onScrolling = onScrolling,
            )
        }
        composable<FullscreenDestination> {
            ForceDarkContent {
                FullscreenPlayer(
                    navController,
                    hideNavBar = hideNavBar,
                    showNavBar = {
                        showNavBar.invoke(true)
                        showNowPlayingSheet.invoke()
                    },
                )
            }
        }
        // Home screen graph
        homeScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
            onScrolling = onScrolling,
        )
        // Library screen graph
        libraryScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
            onScrolling = onScrolling,
        )
        // List screen graph
        listScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
            onScrolling = onScrolling,
        )
        // Login screen graph
        loginScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
            hideBottomBar = hideNavBar,
            showBottomBar = {
                showNavBar(false)
            },
        )
    }
}