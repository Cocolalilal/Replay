package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.maxrave.simpmusic.extension.TrackScrolling
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.common.Config
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.util.isListenAgainSection
import com.maxrave.simpmusic.util.isQuickPicksSection
import com.maxrave.simpmusic.util.resolvePlaylistCover
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HomeShimmer
import com.maxrave.simpmusic.ui.component.LikedSongsCover
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.OfflineErrorState
import com.maxrave.simpmusic.ui.component.PlaylistBottomSheet
import com.maxrave.simpmusic.ui.component.ReplayTopBar
import com.maxrave.simpmusic.ui.icon.CloudOff
import com.maxrave.simpmusic.ui.icon.Favorite
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.SettingsDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.LoginDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PodcastDestination
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistType
import com.maxrave.simpmusic.ui.theme.itemSubtitleFontFamily
import com.maxrave.simpmusic.ui.theme.itemTitleFontFamily
import com.maxrave.simpmusic.ui.theme.sectionTitleFontFamily
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.HomeViewModel
import com.maxrave.simpmusic.viewModel.ListState
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.home_offline_cached_notice
import simpmusic.composeapp.generated.resources.listen_again
import simpmusic.composeapp.generated.resources.quick_picks
import simpmusic.composeapp.generated.resources.retry

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onScrolling: (onTop: Boolean, direction: Int) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val accountInfo by viewModel.accountInfo.collectAsStateWithLifecycle()
    val homeData by viewModel.homeItemList.collectAsStateWithLifecycle()
    val newRelease by viewModel.newRelease.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val homeListState by viewModel.homeListState.collectAsStateWithLifecycle()
    val continuation by viewModel.continuation.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val isRetrying by viewModel.isRetrying.collectAsStateWithLifecycle()
    val nowPlayingData by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()

    val dataStoreManager: DataStoreManager = koinInject()
    val customCoversRaw by dataStoreManager.customPlaylistCovers.collectAsStateWithLifecycle(null)
    val customCoversMap = remember(customCoversRaw) {
        val raw = customCoversRaw
        try {
            if (!raw.isNullOrEmpty()) {
                kotlinx.serialization.json.Json.decodeFromString<Map<String, String>>(raw)
            } else emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = isRetrying

    val currentNowPlayingVideoId = nowPlayingData?.songEntity?.videoId ?: nowPlayingData?.track?.videoId
    val isPlaying = controllerState.isPlaying
    val listenAgainTitle = stringResource(Res.string.listen_again)
    val quickPicksTitle = stringResource(Res.string.quick_picks)

    // Listen Again section from YouTube Music
    val listenAgainData = remember(homeData, listenAgainTitle) {
        homeData.firstOrNull { isListenAgainSection(it.title, listenAgainTitle) }
    }

    // Quick Picks Section identification (matches Quick Picks title or track-based section)
    val quickPicksData = remember(homeData, quickPicksTitle, listenAgainData) {
        homeData.firstOrNull { isQuickPicksSection(it.title, quickPicksTitle) }
            ?: homeData.firstOrNull { section ->
                section != listenAgainData &&
                    !isListenAgainSection(section.title) &&
                    section.contents.filterNotNull().isNotEmpty() &&
                    section.contents.filterNotNull().all { it.videoId?.isNotEmpty() == true }
            }
            ?: homeData.firstOrNull { section ->
                section != listenAgainData &&
                    !isListenAgainSection(section.title) &&
                    section.contents.filterNotNull().count { it.videoId?.isNotEmpty() == true } >= 3
            }
            ?: homeData.firstOrNull { section ->
                section != listenAgainData &&
                    !isListenAgainSection(section.title) &&
                    section.contents.filterNotNull().any { it.videoId?.isNotEmpty() == true }
            }
    }

    // Hero carousel items: Listen Again section contents (with fallback to featuredCarouselItems)
    val featuredItems by viewModel.featuredCarouselItems.collectAsStateWithLifecycle()
    val carouselItems = remember(listenAgainData, featuredItems) {
        listenAgainData?.contents?.filterNotNull()?.filter { it.thumbnails.isNotEmpty() }?.takeIf { it.isNotEmpty() }
            ?: featuredItems
    }

    // Lower sections: all other sections from YouTube Music in the order YouTube Music gives them
    val lowerSections = remember(homeData, listenAgainData, quickPicksData) {
        homeData.filterNot { it == listenAgainData || it == quickPicksData }
    }

    scrollState.TrackScrolling(onScrolling = onScrolling)

    val onRefresh: () -> Unit = {
        viewModel.retryHome()
    }

    val shouldStartPaginate = remember {
        derivedStateOf {
            homeListState != ListState.PAGINATION_EXHAUST &&
                (scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -9) >= (scrollState.layoutInfo.totalItemsCount - 1)
        }
    }

    LaunchedEffect(shouldStartPaginate.value) {
        if (shouldStartPaginate.value && homeListState == ListState.IDLE) {
            viewModel.getContinueHomeItem(continuation)
        }
    }

    // Modal bottom sheet state for long-click track & item options
    var selectedTrackForSheet by remember { mutableStateOf<Track?>(null) }
    var selectedItemForSheet by remember { mutableStateOf<Content?>(null) }

    if (selectedTrackForSheet != null) {
        NowPlayingBottomSheet(
            onDismiss = { selectedTrackForSheet = null },
            song = selectedTrackForSheet?.toSongEntity(),
            navController = navController,
        )
    }

    if (selectedItemForSheet != null) {
        val sheetItem = selectedItemForSheet!!
        val browseId = sheetItem.browseId.orEmpty()
        if (browseId.isNotEmpty()) {
            PlaylistBottomSheet(
                onDismiss = { selectedItemForSheet = null },
                playlistId = browseId,
                playlistName = sheetItem.title,
                thumbnailUrl = sheetItem.thumbnails.lastOrNull()?.url,
                isYourYouTubePlaylist = false,
            )
        }
    }

    val handleItemLongClick: (Content) -> Unit = { item ->
        val vid = item.videoId.orEmpty()
        val browse = item.browseId.orEmpty()
        if (vid.isNotEmpty()) {
            selectedTrackForSheet = item.toTrack()
        } else if (browse.isNotEmpty()) {
            selectedItemForSheet = item
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            onRefresh = onRefresh,
            isRefreshing = isRefreshing,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = PullToRefreshDefaults.indicatorContainerColor,
                    color = PullToRefreshDefaults.indicatorColor,
                )
            },
        ) {
            Crossfade(targetState = loading && homeData.isEmpty(), label = "HomeShimmerCrossfade") { isLoadingInitial ->
                if (isLoadingInitial) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ReplayTopBar(
                            avatarUrl = accountInfo?.second,
                            onAvatarClick = {
                                if (accountInfo != null) {
                                    navController.navigate(SettingsDestination)
                                } else {
                                    navController.navigate(LoginDestination)
                                }
                            },
                        )
                        HomeShimmer()
                    }
                } else if (homeData.isEmpty()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ReplayTopBar(
                            avatarUrl = accountInfo?.second,
                            onAvatarClick = {
                                if (accountInfo != null) {
                                    navController.navigate(SettingsDestination)
                                } else {
                                    navController.navigate(LoginDestination)
                                }
                            },
                        )
                        OfflineErrorState(
                            isRetrying = isRetrying || isRefreshing,
                            onRetry = onRefresh,
                            onOpenDownloaded = {
                                navController.navigate(
                                    LibraryDynamicPlaylistDestination(
                                        type = LibraryDynamicPlaylistType.Downloaded.toStringParams(),
                                    ),
                                )
                            },
                            onOpenLibrary = {
                                navController.navigate(
                                    PlaylistDestination("LM", isYourYouTubePlaylist = true),
                                )
                            },
                        )
                    }
                } else {
                    LazyColumn(
                        state = scrollState,
                        contentPadding = PaddingValues(bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        // Top Bar
                        item(key = "home_top_bar") {
                            ReplayTopBar(
                                avatarUrl = accountInfo?.second,
                                onAvatarClick = {
                                    if (accountInfo != null) {
                                        navController.navigate(SettingsDestination)
                                    } else {
                                        navController.navigate(LoginDestination)
                                    }
                                },
                            )
                        }

                        // Offline notice banner (shown when offline but displaying cached home feed)
                        if (isOffline) {
                            item(key = "home_offline_banner") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .border(
                                                BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                ),
                                                RoundedCornerShape(16.dp),
                                            )
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = SimpIcons.CloudOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = stringResource(Res.string.home_offline_cached_notice),
                                            style = typo().bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                        )
                                        TextButton(
                                            onClick = onRefresh,
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        ) {
                                            Text(
                                                text = stringResource(Res.string.retry),
                                                style = typo().labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 1. Hero Carousel (WITH Solid Outline & Long Click - Official M3 Multi-Browse Carousel)
                        if (carouselItems.isNotEmpty()) {
                            item(key = "hero_carousel") {
                                Material3MultiBrowseCarousel(
                                    items = carouselItems,
                                    customCoversMap = customCoversMap,
                                    onItemClick = { item ->
                                        val vid = item.videoId
                                        val browse = item.browseId
                                        val isLiked = browse == "LM" || browse == "VLLM" || browse == "FEmusic_liked_videos" || (item.title.contains("Liked", ignoreCase = true) && vid == null)
                                        if (isLiked) {
                                            navController.navigate(PlaylistDestination("LM", isYourYouTubePlaylist = true))
                                        } else if (!vid.isNullOrEmpty()) {
                                            val firstTrack = item.toTrack()
                                            viewModel.setQueueData(
                                                QueueData.Data(
                                                    listTracks = arrayListOf(firstTrack),
                                                    firstPlayedTrack = firstTrack,
                                                    playlistId = "RDAMVM$vid",
                                                    playlistName = "\"${item.title}\" Radio",
                                                    playlistType = PlaylistType.RADIO,
                                                    continuation = null,
                                                ),
                                            )
                                            viewModel.loadMediaItem(firstTrack, Config.SONG_CLICK)
                                        } else if (!browse.isNullOrEmpty()) {
                                            if (browse.startsWith("VL") || browse.startsWith("PL")) {
                                                navController.navigate(PlaylistDestination(browse, isYourYouTubePlaylist = false))
                                            } else if (browse.startsWith("UC") || browse.startsWith("FEmusic_library_privately_owned_artist")) {
                                                navController.navigate(ArtistDestination(browse))
                                            } else {
                                                navController.navigate(AlbumDestination(browse))
                                            }
                                        }
                                    },
                                    onItemLongClick = handleItemLongClick,
                                )
                            }
                        }

                        // 2. Quick Picks Section (WITHOUT OUTLINE - with grouping and optical corner radiuses)
                        if (quickPicksData != null && quickPicksData.contents.isNotEmpty()) {
                            item(key = "quick_picks_section") {
                                QuickPicksSection(
                                    homeItem = quickPicksData,
                                    currentPlayingVideoId = currentNowPlayingVideoId,
                                    isPlaying = isPlaying,
                                    customCoversMap = customCoversMap,
                                    onPlayAllClick = {
                                        if (isPlaying) {
                                            sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                        } else {
                                            val currentVid = currentNowPlayingVideoId
                                            val isCurrentInQuickPicks = quickPicksData.contents.any { it?.videoId == currentVid }
                                            if (isCurrentInQuickPicks && !currentVid.isNullOrEmpty()) {
                                                sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                            } else {
                                                viewModel.playAllQuickPicks(quickPicksData)
                                            }
                                        }
                                    },
                                    onTrackClick = { item ->
                                        val vid = item.videoId.orEmpty()
                                        val browse = item.browseId.orEmpty()
                                        val isLiked = browse == "LM" || browse == "VLLM" || browse == "FEmusic_liked_videos" || (item.title.contains("Liked", ignoreCase = true) && vid.isEmpty())
                                        if (isLiked) {
                                            navController.navigate(PlaylistDestination("LM", isYourYouTubePlaylist = true))
                                        } else if (vid.isNotEmpty()) {
                                            viewModel.playQuickPickTrack(quickPicksData, item)
                                        } else if (browse.isNotEmpty()) {
                                            if (browse.startsWith("VL") || browse.startsWith("PL")) {
                                                navController.navigate(PlaylistDestination(browse, isYourYouTubePlaylist = false))
                                            } else if (browse.startsWith("UC") || browse.startsWith("FEmusic_library_privately_owned_artist")) {
                                                navController.navigate(ArtistDestination(browse))
                                            } else {
                                                navController.navigate(AlbumDestination(browse))
                                            }
                                        }
                                    },
                                    onTrackLongClick = handleItemLongClick,
                                )
                            }
                        }

                        // 3. Dynamic Lower Sections ("Albums for you", "Mixed for you", etc. - WITHOUT OUTLINE)
                        itemsIndexed(lowerSections, key = { index, section -> "section_${section.title}_${section.hashCode()}_$index" }) { _, section ->
                            SectionCarousel(
                                section = section,
                                customCoversMap = customCoversMap,
                                onItemClick = { item ->
                                    val browse = item.browseId.orEmpty()
                                    val vid = item.videoId.orEmpty()
                                    val isLiked = browse == "LM" || browse == "VLLM" || browse == "FEmusic_liked_videos" || (item.title.contains("Liked", ignoreCase = true) && vid.isEmpty())
                                    if (isLiked) {
                                        navController.navigate(PlaylistDestination("LM", isYourYouTubePlaylist = true))
                                    } else if (browse.isNotEmpty()) {
                                        if (browse.startsWith("VL") || browse.startsWith("PL")) {
                                            navController.navigate(
                                                PlaylistDestination(
                                                    playlistId = browse,
                                                    isYourYouTubePlaylist = false,
                                                ),
                                            )
                                        } else if (browse.startsWith("UC") || browse.startsWith("FEmusic_library_privately_owned_artist")) {
                                            navController.navigate(ArtistDestination(browse))
                                        } else {
                                            navController.navigate(AlbumDestination(browse))
                                        }
                                    } else if (vid.isNotEmpty()) {
                                        val track = item.toTrack()
                                        viewModel.setQueueData(
                                            QueueData.Data(
                                                listTracks = arrayListOf(track),
                                                firstPlayedTrack = track,
                                                playlistId = "RDAMVM$vid",
                                                playlistName = "\"${item.title}\" Radio",
                                                playlistType = PlaylistType.RADIO,
                                                continuation = null,
                                            ),
                                        )
                                        viewModel.loadMediaItem(track, Config.SONG_CLICK)
                                    }
                                },
                                onItemLongClick = handleItemLongClick,
                            )
                        }

                        item(key = "end_of_page") {
                            EndOfPage()
                        }
                    }
                }
            }
        }
    }
}

private data class CarouselItemVisuals(
    val x: Float,
    val width: Float,
    val alpha: Float,
    val darkenAlpha: Float,
    val overlayAlpha: Float,
    val zIndex: Float,
    val heightScale: Float,
)

/**
 * Physics-Driven 1:1 Square Multi-Browse Carousel:
 * - Mathematical 1:1 Perfect Square focal entry.
 * - Bounded momentum fling physics with critically-damped spring settling.
 * - Dynamic continuous dimming: 100% brightness (focal) -> 80% (2nd) -> 60% (3rd & beyond).
 * - Smooth tuck-under scale and alpha fade on left exit with edge boundary fade on right.
 * - Seamless symmetric transition at end of list.
 * - Solid #15181C outline container with 0 peeking.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Material3MultiBrowseCarousel(
    items: List<Content>,
    customCoversMap: Map<String, String> = emptyMap(),
    onItemClick: (Content) -> Unit,
    onItemLongClick: (Content) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val cardShape = RoundedCornerShape(24.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        val screenWidth = maxWidth
        val screenWidthPx = with(density) { screenWidth.toPx() }

        // Focal item width = height for a mathematically perfect square (1:1 aspect ratio)
        // Scaled proportionally so 3 entries (Focal, Medium, Small peek) are always visible simultaneously
        val focalWidth = (screenWidth * 0.58f).coerceIn(195.dp, 235.dp)
        val cardHeight = focalWidth
        val spacing = 8.dp

        val focalWidthPx = with(density) { focalWidth.toPx() }
        val spacingPx = with(density) { spacing.toPx() }
        val stepPx = focalWidthPx + spacingPx

        val minimizedWidthPx = focalWidthPx * 0.36f
        val trailingWidthPx = focalWidthPx * 0.18f

        val maxOffset = (items.size - 1) * stepPx
        val scrollOffset = remember { Animatable(0f) }
        val velocityTracker = remember { VelocityTracker() }

        val progress = (scrollOffset.value / stepPx).coerceIn(0f, (items.size - 1).toFloat())

        // Continuous helper function for keyline layout & interpolation
        fun getItemVisuals(index: Int): CarouselItemVisuals {
            val count = items.size
            if (count <= 1) {
                return CarouselItemVisuals(
                    x = 0f,
                    width = focalWidthPx,
                    alpha = 1f,
                    darkenAlpha = 0f,
                    overlayAlpha = 1f,
                    zIndex = 100f,
                    heightScale = 1f,
                )
            }
            if (count == 2) {
                val u = progress.coerceIn(0f, 1f)
                val w0 = focalWidthPx + (minimizedWidthPx - focalWidthPx) * u
                val w1 = minimizedWidthPx + (focalWidthPx - minimizedWidthPx) * u
                return if (index == 0) {
                    CarouselItemVisuals(
                        x = 0f,
                        width = w0,
                        alpha = 1f,
                        darkenAlpha = 0.20f * u,
                        overlayAlpha = if (u <= 0.5f) (1f - u * 2f).coerceIn(0f, 1f) else 0f,
                        zIndex = 85f + 15f * (1f - u),
                        heightScale = 1f,
                    )
                } else {
                    CarouselItemVisuals(
                        x = w0 + spacingPx,
                        width = w1,
                        alpha = 1f,
                        darkenAlpha = 0.20f * (1f - u),
                        overlayAlpha = if (u >= 0.5f) (u * 2f - 1f).coerceIn(0f, 1f) else 0f,
                        zIndex = 85f + 15f * u,
                        heightScale = 1f,
                    )
                }
            }

            // count >= 3
            val shiftStartProgress = (count - 3).toFloat()
            if (progress <= shiftStartProgress) {
                val d = index - progress
                return when {
                    d < -1f -> CarouselItemVisuals(
                        x = 0f,
                        width = focalWidthPx * 0.4f,
                        alpha = 0f,
                        darkenAlpha = 0.40f,
                        overlayAlpha = 0f,
                        zIndex = 55f,
                        heightScale = 0.65f,
                    )
                    d in -1f..0f -> CarouselItemVisuals(
                        x = 0f,
                        width = focalWidthPx * (1f + d * 0.6f),
                        alpha = (1f + d * 1.5f).coerceIn(0f, 1f),
                        darkenAlpha = (-d * 0.40f).coerceIn(0f, 0.40f),
                        overlayAlpha = if (d >= -0.5f) (1f + d * 2f).coerceIn(0f, 1f) else 0f,
                        zIndex = 55f + (1f + d).coerceIn(0f, 1f) * 15f,
                        heightScale = (1f + d * 0.35f).coerceIn(0.65f, 1f),
                    )
                    d in 0f..1f -> CarouselItemVisuals(
                        x = (focalWidthPx + spacingPx) * d,
                        width = focalWidthPx + (minimizedWidthPx - focalWidthPx) * d,
                        alpha = 1f,
                        darkenAlpha = 0.20f * d,
                        overlayAlpha = if (d <= 0.5f) (1f - d * 2f).coerceIn(0f, 1f) else 0f,
                        zIndex = 85f + 15f * (1f - d),
                        heightScale = 1f,
                    )
                    d in 1f..2f -> CarouselItemVisuals(
                        x = (focalWidthPx + spacingPx) + (minimizedWidthPx + spacingPx) * (d - 1f),
                        width = minimizedWidthPx + (trailingWidthPx - minimizedWidthPx) * (d - 1f),
                        alpha = 1f,
                        darkenAlpha = 0.20f + 0.20f * (d - 1f),
                        overlayAlpha = 0f,
                        zIndex = 70f + 15f * (2f - d),
                        heightScale = 1f,
                    )
                    d in 2f..3f -> CarouselItemVisuals(
                        x = (focalWidthPx + spacingPx) + (minimizedWidthPx + spacingPx),
                        width = (trailingWidthPx * (3f - d)).coerceIn(0f, trailingWidthPx),
                        alpha = (3f - d).coerceIn(0f, 1f),
                        darkenAlpha = 0.40f,
                        overlayAlpha = 0f,
                        zIndex = 70f,
                        heightScale = (0.65f + 0.35f * (3f - d)).coerceIn(0.65f, 1f),
                    )
                    else -> CarouselItemVisuals(
                        x = (focalWidthPx + spacingPx) + (minimizedWidthPx + spacingPx),
                        width = 0f,
                        alpha = 0f,
                        darkenAlpha = 0.40f,
                        overlayAlpha = 0f,
                        zIndex = 55f,
                        heightScale = 0.65f,
                    )
                }
            } else {
                // End shift transition
                val shift = (progress - shiftStartProgress).coerceIn(0f, 2f)
                if (index < count - 3) {
                    return CarouselItemVisuals(
                        x = 0f,
                        width = focalWidthPx * 0.4f,
                        alpha = 0f,
                        darkenAlpha = 0.40f,
                        overlayAlpha = 0f,
                        zIndex = 55f,
                        heightScale = 0.65f,
                    )
                }
                if (shift <= 1f) {
                    val u = shift
                    val wN3 = focalWidthPx + (minimizedWidthPx - focalWidthPx) * u
                    val wN2 = minimizedWidthPx + (focalWidthPx - minimizedWidthPx) * u
                    return when (index) {
                        count - 3 -> CarouselItemVisuals(
                            x = 0f,
                            width = wN3,
                            alpha = 1f,
                            darkenAlpha = 0.20f * u,
                            overlayAlpha = if (u <= 0.5f) (1f - u * 2f).coerceIn(0f, 1f) else 0f,
                            zIndex = 85f + 15f * (1f - u),
                            heightScale = 1f,
                        )
                        count - 2 -> CarouselItemVisuals(
                            x = wN3 + spacingPx,
                            width = wN2,
                            alpha = 1f,
                            darkenAlpha = 0.20f * (1f - u),
                            overlayAlpha = if (u >= 0.5f) (u * 2f - 1f).coerceIn(0f, 1f) else 0f,
                            zIndex = 85f + 15f * u,
                            heightScale = 1f,
                        )
                        count - 1 -> CarouselItemVisuals(
                            x = wN3 + spacingPx + wN2 + spacingPx,
                            width = trailingWidthPx,
                            alpha = 1f,
                            darkenAlpha = 0.40f,
                            overlayAlpha = 0f,
                            zIndex = 70f,
                            heightScale = 1f,
                        )
                        else -> CarouselItemVisuals(
                            x = wN3 + spacingPx + wN2 + spacingPx,
                            width = 0f,
                            alpha = 0f,
                            darkenAlpha = 0.40f,
                            overlayAlpha = 0f,
                            zIndex = 55f,
                            heightScale = 0.65f,
                        )
                    }
                } else {
                    val v = shift - 1f
                    val wN3 = minimizedWidthPx + (trailingWidthPx - minimizedWidthPx) * v
                    val wN2 = focalWidthPx + (minimizedWidthPx - focalWidthPx) * v
                    val wN1 = trailingWidthPx + (focalWidthPx - trailingWidthPx) * v
                    return when (index) {
                        count - 3 -> CarouselItemVisuals(
                            x = 0f,
                            width = wN3,
                            alpha = 1f,
                            darkenAlpha = 0.20f + 0.20f * v,
                            overlayAlpha = 0f,
                            zIndex = 70f + 15f * (1f - v),
                            heightScale = 1f,
                        )
                        count - 2 -> CarouselItemVisuals(
                            x = wN3 + spacingPx,
                            width = wN2,
                            alpha = 1f,
                            darkenAlpha = 0.20f * v,
                            overlayAlpha = if (v <= 0.5f) (1f - v * 2f).coerceIn(0f, 1f) else 0f,
                            zIndex = 85f + 15f * (1f - v),
                            heightScale = 1f,
                        )
                        count - 1 -> CarouselItemVisuals(
                            x = wN3 + spacingPx + wN2 + spacingPx,
                            width = wN1,
                            alpha = 1f,
                            darkenAlpha = 0.40f * (1f - v),
                            overlayAlpha = if (v >= 0.5f) (v * 2f - 1f).coerceIn(0f, 1f) else 0f,
                            zIndex = 70f + 30f * v,
                            heightScale = 1f,
                        )
                        else -> CarouselItemVisuals(
                            x = wN3 + spacingPx + wN2 + spacingPx,
                            width = 0f,
                            alpha = 0f,
                            darkenAlpha = 0.40f,
                            overlayAlpha = 0f,
                            zIndex = 55f,
                            heightScale = 0.65f,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .pointerInput(items.size, stepPx) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            velocityTracker.resetTracking()
                            coroutineScope.launch {
                                scrollOffset.stop()
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPointerInputChange(change)
                            coroutineScope.launch {
                                val newOffset = (scrollOffset.value - dragAmount).coerceIn(0f, maxOffset)
                                scrollOffset.snapTo(newOffset)
                            }
                        },
                        onDragEnd = {
                            val velocity = velocityTracker.calculateVelocity().x
                            val dragVelocity = -velocity
                            val absVel = abs(dragVelocity)
                            val currentOffset = scrollOffset.value
                            val currentPos = currentOffset / stepPx

                            // Velocity thresholds in dp/s for natural feel across all screen densities
                            val minFlingVelocity = with(density) { 350.dp.toPx() }
                            val mediumFlingVelocity = with(density) { 1400.dp.toPx() }
                            val maxFlingVelocity = with(density) { 2800.dp.toPx() }

                            val targetIndex = when {
                                absVel < minFlingVelocity -> {
                                    currentPos.roundToInt()
                                }
                                absVel < mediumFlingVelocity -> {
                                    if (dragVelocity > 0f) {
                                        (floor(currentPos) + 1).toInt()
                                    } else {
                                        (ceil(currentPos) - 1).toInt()
                                    }
                                }
                                absVel < maxFlingVelocity -> {
                                    if (dragVelocity > 0f) {
                                        (floor(currentPos) + 2).toInt()
                                    } else {
                                        (ceil(currentPos) - 2).toInt()
                                    }
                                }
                                else -> {
                                    if (dragVelocity > 0f) {
                                        (floor(currentPos) + 3).toInt()
                                    } else {
                                        (ceil(currentPos) - 3).toInt()
                                    }
                                }
                            }.coerceIn(0, items.lastIndex)

                            // Clamp initial spring velocity to prevent overshoot while preserving natural momentum
                            val maxSpringVelocity = with(density) { 1600.dp.toPx() }
                            val clampedInitialVelocity = dragVelocity.coerceIn(-maxSpringVelocity, maxSpringVelocity)

                            coroutineScope.launch {
                                scrollOffset.animateTo(
                                    targetValue = targetIndex * stepPx,
                                    initialVelocity = clampedInitialVelocity,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                )
                            }
                        },
                        onDragCancel = {
                            val currentOffset = scrollOffset.value
                            val targetIndex = (currentOffset / stepPx).roundToInt().coerceIn(0, items.lastIndex)
                            coroutineScope.launch {
                                scrollOffset.animateTo(
                                    targetValue = targetIndex * stepPx,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                )
                            }
                        },
                    )
                },
        ) {
            // Render visible items
            val currentFocalIndex = (scrollOffset.value / stepPx).roundToInt().coerceIn(0, items.lastIndex)
            val minVisibleIndex = (currentFocalIndex - 3).coerceAtLeast(0)
            val maxVisibleIndex = (currentFocalIndex + 3).coerceAtMost(items.lastIndex)

            for (index in minVisibleIndex..maxVisibleIndex) {
                val item = items[index]
                val visuals = getItemVisuals(index)

                val itemX = visuals.x
                val itemW = visuals.width
                val itemAlpha = visuals.alpha
                val darkenAlpha = visuals.darkenAlpha
                val zIndex = visuals.zIndex
                val heightScale = visuals.heightScale
                val overlayAlpha = visuals.overlayAlpha

                if (itemAlpha > 0.01f && itemW > 0.5f) {
                    val browse = item.browseId
                    val vid = item.videoId
                    val customCover = resolvePlaylistCover(browse, null, customCoversMap)
                    val artworkUrl = customCover ?: item.thumbnails.lastOrNull()?.url.orEmpty()
                    val isLikedMusic = browse == "LM" || browse == "VLLM" || browse == "FEmusic_liked_videos" || (item.title.contains("Liked", ignoreCase = true) && vid == null)
                    val displayTitle = if (isLikedMusic) "Liked Songs" else item.title

                    val badgeText = when {
                        isLikedMusic -> "Playlist"
                        browse?.startsWith("VL") == true || browse?.startsWith("PL") == true || browse?.startsWith("RD") == true -> "Playlist"
                        browse?.startsWith("UC") == true || browse?.startsWith("FEmusic_library_privately_owned_artist") == true -> "Artist"
                        browse?.startsWith("MPRE") == true || browse?.startsWith("FEmusic_library_privately_owned_release") == true || (item.album != null && vid.isNullOrEmpty()) -> "Album"
                        !vid.isNullOrEmpty() -> "Song"
                        else -> "Featured"
                    }

                    val itemWidthDp = with(density) { itemW.toDp() }
                    val itemXDp = with(density) { itemX.toDp() }

                    val isCurrentFocal = abs(progress - index) < 0.35f

                    Box(
                        modifier = Modifier
                            .offset(x = itemXDp)
                            .width(itemWidthDp)
                            .height(cardHeight)
                            .zIndex(zIndex)
                            .graphicsLayer {
                                alpha = itemAlpha
                                scaleY = heightScale
                            }
                            .clip(cardShape)
                            .background(Color(0xFF15181C))
                            .padding(2.5.dp)
                            .clip(RoundedCornerShape(21.5.dp))
                            .background(Color(0xFF141416))
                            .combinedClickable(
                                onClick = {
                                    if (isCurrentFocal) {
                                        onItemClick(item)
                                    } else {
                                        coroutineScope.launch {
                                            scrollOffset.animateTo(
                                                targetValue = index * stepPx,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMediumLow,
                                                ),
                                            )
                                        }
                                    }
                                },
                                onLongClick = {
                                    onItemLongClick(item)
                                },
                            ),
                    ) {
                        // Full Artwork with smooth GPU scaling
                        if (isLikedMusic) {
                            LikedSongsCover(
                                modifier = Modifier.fillMaxSize(),
                                iconSize = 72.dp,
                            )
                        } else if (customCover != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(customCover)
                                    .crossfade(true)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(artworkUrl)
                                    .crossfade(true)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        // Dynamic dimming: 100% (focal) -> 80% (2nd) -> 60% (3rd)
                        if (darkenAlpha > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = darkenAlpha)),
                            )
                        }

                        if (overlayAlpha > 0f) {
                            // Dark Bottom Gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(cardHeight * 0.5f)
                                    .align(Alignment.BottomCenter)
                                    .graphicsLayer { alpha = overlayAlpha }
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.85f),
                                            ),
                                        ),
                                    ),
                            )

                            // Top-Right Pill Badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 12.dp, end = 12.dp)
                                    .graphicsLayer { alpha = overlayAlpha }
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = badgeText,
                                    fontFamily = itemSubtitleFontFamily(),
                                    fontSize = 11.sp,
                                    color = Color.White,
                                )
                            }

                            // Bottom-Left Title and Subtitle Overlay
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                                    .graphicsLayer { alpha = overlayAlpha },
                            ) {
                                Text(
                                    text = displayTitle,
                                    fontFamily = sectionTitleFontFamily(),
                                    fontSize = 22.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val artistText = item.artists?.map { it.name }?.connectArtists() ?: item.description.orEmpty()
                                Text(
                                    text = if (isLikedMusic) "Auto playlist" else artistText,
                                    fontFamily = itemSubtitleFontFamily(),
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4-Row Quick Picks Section
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickPicksSection(
    homeItem: HomeItem,
    currentPlayingVideoId: String?,
    isPlaying: Boolean,
    customCoversMap: Map<String, String> = emptyMap(),
    onPlayAllClick: () -> Unit,
    onTrackClick: (Content) -> Unit,
    onTrackLongClick: (Content) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val snapFling = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyListState = listState))

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val screenWidth = maxWidth
        val columnWidth = (screenWidth * 0.82f).coerceIn(280.dp, 360.dp)

        Column(modifier = Modifier.fillMaxWidth()) {
            // Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.quick_picks),
                    fontFamily = sectionTitleFontFamily(),
                    fontSize = 22.sp,
                    color = Color.White,
                )

                // Play / Pause Circle Button on right
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E22))
                        .clickable { onPlayAllClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                        contentDescription = "Play All Quick Picks",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Grouped 4-item columns in horizontal snapping row
            val nonNullItems = homeItem.contents.filterNotNull()
            val columns = remember(nonNullItems) { nonNullItems.chunked(4) }

            LazyRow(
                state = listState,
                flingBehavior = snapFling,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(columns) { columnItems ->
                    Column(
                        modifier = Modifier.width(columnWidth),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        columnItems.forEachIndexed { rowIndex, item ->
                            val isSelected = currentPlayingVideoId != null && currentPlayingVideoId == item.videoId
                            val isFirstInGroup = rowIndex == 0
                            val isLastInGroup = rowIndex == columnItems.lastIndex

                            QuickPickCard(
                                item = item,
                                isSelected = isSelected,
                                isFirstInGroup = isFirstInGroup,
                                isLastInGroup = isLastInGroup,
                                customCoversMap = customCoversMap,
                                onCardClick = { onTrackClick(item) },
                                onCardLongClick = { onTrackLongClick(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickPickCard(
    item: Content,
    isSelected: Boolean,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    customCoversMap: Map<String, String> = emptyMap(),
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val customCover = resolvePlaylistCover(item.browseId, null, customCoversMap)
    val artworkUrl = customCover ?: item.thumbnails.lastOrNull()?.url.orEmpty()
    val rawSubtitle = item.artists?.map { it.name }?.connectArtists() ?: item.description.orEmpty()
    val isSong = item.videoId != null
    val artistText = when {
        isSong -> rawSubtitle
        item.browseId?.startsWith("VL") == true || item.browseId?.startsWith("PL") == true -> {
            if (rawSubtitle.isNotEmpty()) "Playlist • $rawSubtitle" else "Playlist"
        }
        item.browseId?.startsWith("UC") == true || item.browseId?.startsWith("FEmusic_library_privately_owned_artist") == true -> {
            if (rawSubtitle.isNotEmpty()) "Artist • $rawSubtitle" else "Artist"
        }
        item.browseId?.startsWith("MPRE") == true || item.browseId?.startsWith("FEmusic_library_privately_owned_release") == true -> {
            if (rawSubtitle.isNotEmpty()) "Album • $rawSubtitle" else "Album"
        }
        else -> if (rawSubtitle.isNotEmpty()) "Playlist • $rawSubtitle" else "Playlist"
    }

    val isLikedMusic = item.browseId == "LM" || item.browseId == "VLLM" || item.browseId == "FEmusic_liked_videos" || (item.title.contains("Liked", ignoreCase = true) && !isSong)
    val displayTitle = if (isLikedMusic) "Liked Songs" else item.title

    // Smooth animations for corner rounding and colors
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF2B3E52) else Color(0xFF15181C),
        animationSpec = tween(durationMillis = 300),
    )

    // Compute optical corner radius for card (26dp for prominent pill groups)
    val topStartRadius by animateDpAsState(
        targetValue = when {
            isSelected -> 50.dp
            isFirstInGroup -> 26.dp
            else -> 6.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )
    val topEndRadius by animateDpAsState(
        targetValue = when {
            isSelected -> 50.dp
            isFirstInGroup -> 26.dp
            else -> 6.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )
    val bottomStartRadius by animateDpAsState(
        targetValue = when {
            isSelected -> 50.dp
            isLastInGroup -> 26.dp
            else -> 6.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )
    val bottomEndRadius by animateDpAsState(
        targetValue = when {
            isSelected -> 50.dp
            isLastInGroup -> 26.dp
            else -> 6.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )

    val cardShape = RoundedCornerShape(
        topStart = topStartRadius,
        topEnd = topEndRadius,
        bottomStart = bottomStartRadius,
        bottomEnd = bottomEndRadius,
    )

    // Compute concentric optical corner radius for inner artwork
    val artTopStart by animateDpAsState(
        targetValue = when {
            isSelected -> 50.dp
            isFirstInGroup -> 18.dp
            else -> 6.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )
    val artBottomStart by animateDpAsState(
        targetValue = when {
            isSelected -> 50.dp
            isLastInGroup -> 18.dp
            else -> 6.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )
    val artOtherCorners by animateDpAsState(
        targetValue = if (isSelected) 50.dp else 6.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )

    val artShape = RoundedCornerShape(
        topStart = artTopStart,
        topEnd = artOtherCorners,
        bottomStart = artBottomStart,
        bottomEnd = artOtherCorners,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(cardShape)
            .background(animatedBg)
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onCardLongClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Artwork
        if (isLikedMusic) {
            LikedSongsCover(
                modifier = Modifier
                    .size(44.dp)
                    .clip(artShape),
                iconSize = 22.dp,
            )
        } else if (customCover != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(customCover)
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(artShape),
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(artworkUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(artShape),
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = displayTitle,
                fontFamily = itemTitleFontFamily(),
                fontSize = 14.5.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelected) {
                    Icon(
                        imageVector = SimpIcons.Favorite,
                        contentDescription = "Now Playing Favorite",
                        tint = Color(0xFF8BA7C4),
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = artistText,
                    fontFamily = itemSubtitleFontFamily(),
                    fontSize = 12.5.sp,
                    color = if (isSelected) Color(0xFFB0CCE6) else Color(0xFF9E9EA4),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Clean Horizontal Section Carousel for "Albums for you", "Mixed for you", etc.
 * NO outlines!
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SectionCarousel(
    section: HomeItem,
    customCoversMap: Map<String, String> = emptyMap(),
    onItemClick: (Content) -> Unit,
    onItemLongClick: (Content) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val snapFling = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyListState = listState))

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = section.title,
            fontFamily = sectionTitleFontFamily(),
            fontSize = 22.sp,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            state = listState,
            flingBehavior = snapFling,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            val nonNullContents = section.contents.filterNotNull()
            itemsIndexed(nonNullContents, key = { index, item -> "item_${item.browseId ?: item.videoId ?: item.title}_${item.hashCode()}_$index" }) { _, item ->
                val customCover = resolvePlaylistCover(item.browseId, null, customCoversMap)
                val artworkUrl = customCover ?: item.thumbnails.lastOrNull()?.url.orEmpty()
                val isLiked = item.browseId == "LM" || item.browseId == "VLLM" || item.browseId == "FEmusic_liked_videos" || (item.title.contains("Liked", ignoreCase = true) && item.videoId == null)
                val displayTitle = if (isLiked) "Liked Songs" else item.title
                val subtitleText = if (isLiked) "Auto playlist" else (item.artists?.map { it.name }?.connectArtists() ?: item.description.orEmpty())

                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .combinedClickable(
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongClick(item) },
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF15181C)),
                    ) {
                        if (isLiked) {
                            LikedSongsCover(
                                modifier = Modifier.fillMaxSize(),
                                iconSize = 54.dp,
                            )
                        } else if (customCover != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(customCover)
                                    .crossfade(true)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(artworkUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = displayTitle,
                        fontFamily = itemTitleFontFamily(),
                        fontSize = 14.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitleText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitleText,
                            fontFamily = itemSubtitleFontFamily(),
                            fontSize = 12.sp,
                            color = Color(0xFF9E9EA4),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}