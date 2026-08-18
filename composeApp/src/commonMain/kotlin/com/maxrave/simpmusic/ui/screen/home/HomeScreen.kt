package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HomeShimmer
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.OfflineErrorState
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
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistType
import com.maxrave.simpmusic.ui.theme.itemSubtitleFontFamily
import com.maxrave.simpmusic.ui.theme.itemTitleFontFamily
import com.maxrave.simpmusic.ui.theme.sectionTitleFontFamily
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.HomeViewModel
import com.maxrave.simpmusic.viewModel.ListState
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.home_offline_cached_notice
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

    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    val currentNowPlayingVideoId = nowPlayingData?.songEntity?.videoId ?: nowPlayingData?.track?.videoId
    val isPlaying = controllerState.isPlaying
    val quickPicksTitle = stringResource(Res.string.quick_picks)

    // Quick Picks Section identification (always matches reliably on first paint)
    val quickPicksData = remember(homeData, quickPicksTitle) {
        homeData.firstOrNull {
            it.title.equals(quickPicksTitle, ignoreCase = true) ||
                it.title.contains("quick", ignoreCase = true) ||
                it.title.contains("pick", ignoreCase = true) ||
                it.title.contains("listen again", ignoreCase = true) ||
                it.title.contains("chọn nhanh", ignoreCase = true)
        } ?: homeData.firstOrNull { it.contents.any { c -> c?.videoId?.isNotEmpty() == true } }
    }

    // Hero carousel items (Mixed recommendations & new releases)
    val featuredItems by viewModel.featuredCarouselItems.collectAsStateWithLifecycle()

    // Lower sections
    val lowerSections = remember(homeData, quickPicksData) {
        homeData.filterNot { it == quickPicksData }
    }

    val prevScrollPosition = rememberSaveable {
        mutableFloatStateOf(scrollState.firstVisibleItemIndex + scrollState.firstVisibleItemScrollOffset / 10000.0f)
    }

    LaunchedEffect(scrollState) {
        snapshotFlow {
            val idx = scrollState.firstVisibleItemIndex
            val off = scrollState.firstVisibleItemScrollOffset
            Triple(idx == 0 && off == 0, idx, off)
        }.collect { (isAtTop, idx, off) ->
            val position = idx + (off / 10000.0f)
            val direction = if (position > prevScrollPosition.floatValue) {
                -1
            } else if (position < prevScrollPosition.floatValue) {
                1
            } else {
                0
            }
            prevScrollPosition.floatValue = position
            onScrolling(isAtTop, direction)
        }
    }

    val onRefresh: () -> Unit = {
        isRefreshing = true
        viewModel.retryHome()
    }

    LaunchedEffect(loading) {
        if (!loading) {
            isRefreshing = false
            coroutineScope.launch {
                pullToRefreshState.animateToHidden()
            }
        }
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

    // Modal bottom sheet state for long-click track options
    var selectedTrackForSheet by remember { mutableStateOf<Track?>(null) }
    if (selectedTrackForSheet != null) {
        NowPlayingBottomSheet(
            onDismiss = { selectedTrackForSheet = null },
            song = selectedTrackForSheet?.toSongEntity(),
            navController = navController,
        )
    }

    val surfaceOutlineColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    val carouselOutlineStroke = BorderStroke(2.5.dp, surfaceOutlineColor)

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
                                    LibraryDynamicPlaylistDestination(
                                        type = LibraryDynamicPlaylistType.Favorite.toStringParams(),
                                    ),
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

                        // 1. Hero Carousel (WITH 2.5dp OUTLINE - Official M3 Multi-Browse Carousel)
                        if (featuredItems.isNotEmpty()) {
                            item(key = "hero_carousel") {
                                Material3MultiBrowseCarousel(
                                    items = featuredItems,
                                    outlineStroke = carouselOutlineStroke,
                                    onItemClick = { item ->
                                        val vid = item.videoId
                                        val browse = item.browseId
                                        if (!vid.isNullOrEmpty()) {
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
                                    onPlayAllClick = {
                                        viewModel.playAllQuickPicks(quickPicksData)
                                    },
                                    onTrackClick = { item ->
                                        val vid = item.videoId.orEmpty()
                                        if (vid.isNotEmpty()) {
                                            viewModel.playQuickPickTrack(quickPicksData, item)
                                        } else if (!item.browseId.isNullOrEmpty()) {
                                            val browse = item.browseId.orEmpty()
                                            if (browse.startsWith("VL") || browse.startsWith("PL")) {
                                                navController.navigate(PlaylistDestination(browse, isYourYouTubePlaylist = false))
                                            } else if (browse.startsWith("UC") || browse.startsWith("FEmusic_library_privately_owned_artist")) {
                                                navController.navigate(ArtistDestination(browse))
                                            } else {
                                                navController.navigate(AlbumDestination(browse))
                                            }
                                        }
                                    },
                                    onTrackLongClick = { item ->
                                        if (!item.videoId.isNullOrEmpty()) {
                                            selectedTrackForSheet = item.toTrack()
                                        }
                                    },
                                )
                            }
                        }

                        // 3. Dynamic Lower Sections ("Albums for you", "Mixed for you", etc. - WITHOUT OUTLINE)
                        items(lowerSections, key = { it.title + it.hashCode() }) { section ->
                            SectionCarousel(
                                section = section,
                                onItemClick = { item ->
                                    val browse = item.browseId.orEmpty()
                                    val vid = item.videoId.orEmpty()
                                    if (browse.isNotEmpty()) {
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

/**
 * Official Material 3 Multi-Browse Carousel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3MultiBrowseCarousel(
    items: List<Content>,
    outlineStroke: BorderStroke,
    onItemClick: (Content) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val carouselState = rememberCarouselState { items.size }
    val cardShape = RoundedCornerShape(24.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clipToBounds(),
    ) {
        val screenWidth = maxWidth
        val preferredItemWidth = (screenWidth * 0.58f).coerceIn(190.dp, 270.dp)
        val cardHeight = (preferredItemWidth * 1.14f).coerceIn(215.dp, 280.dp)

        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = preferredItemWidth,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight),
        ) { index ->
            val item = items[index]
            val artworkUrl = item.thumbnails.lastOrNull()?.url.orEmpty()

            val info = carouselItemDrawInfo
            val sizeFraction = if (info.maxSize > info.minSize) {
                ((info.size - info.minSize) / (info.maxSize - info.minSize)).coerceIn(0f, 1f)
            } else 1f
            val overlayAlpha = if (sizeFraction > 0.5f) (sizeFraction - 0.5f) / 0.5f else 0f
            // Smoothly dims: 100% brightness at full size, 80% at medium, 60% at small
            val darkenAlpha = (1f - sizeFraction) * 0.40f

            val browse = item.browseId
            val badgeText = when {
                browse?.startsWith("VL") == true || browse?.startsWith("PL") == true -> "Playlist"
                browse?.startsWith("UC") == true || browse?.startsWith("FEmusic_library_privately_owned_artist") == true -> "Artist"
                browse?.startsWith("MPRE") == true || browse?.startsWith("FEmusic_library_privately_owned_release") == true -> "Album"
                item.videoId != null -> "New Release!"
                else -> "Featured"
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(cardShape)
                    .maskBorder(outlineStroke, cardShape)
                    .background(Color(0xFF141416))
                    .clickable {
                        if (carouselState.currentItem != index) {
                            coroutineScope.launch {
                                carouselState.animateScrollToItem(index)
                            }
                        }
                        onItemClick(item)
                    },
            ) {
                // Full Artwork with smooth GPU scaling
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

                // Dynamic dimming: 80% brightness for 2nd option, 60% for 3rd (minimized) option
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
                            text = item.title,
                            fontFamily = sectionTitleFontFamily(),
                            fontSize = 22.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val artistText = item.artists?.map { it.name }?.connectArtists() ?: item.description.orEmpty()
                        Text(
                            text = artistText,
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

/**
 * 4-Row Quick Picks Section
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickPicksSection(
    homeItem: HomeItem,
    currentPlayingVideoId: String?,
    isPlaying: Boolean,
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
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artworkUrl = item.thumbnails.lastOrNull()?.url.orEmpty()
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

    // Smooth animations for corner rounding and colors
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF2B3E52) else Color(0xFF15181C),
        animationSpec = tween(durationMillis = 300),
    )

    // Compute optical corner radius for card
    val topStartRadius by animateDpAsState(
        targetValue = when {
            isSelected -> 50.dp
            isFirstInGroup -> 20.dp
            else -> 6.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )
    val topEndRadius by animateDpAsState(
        targetValue = when {
            isSelected -> 50.dp
            isFirstInGroup -> 20.dp
            else -> 6.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )
    val bottomStartRadius by animateDpAsState(
        targetValue = when {
            isSelected -> 50.dp
            isLastInGroup -> 20.dp
            else -> 6.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )
    val bottomEndRadius by animateDpAsState(
        targetValue = when {
            isSelected -> 50.dp
            isLastInGroup -> 20.dp
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
            isFirstInGroup -> 14.dp
            else -> 6.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )
    val artBottomStart by animateDpAsState(
        targetValue = when {
            isSelected -> 50.dp
            isLastInGroup -> 14.dp
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

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = item.title,
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
@Composable
fun SectionCarousel(
    section: HomeItem,
    onItemClick: (Content) -> Unit,
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
            items(nonNullContents, key = { (it.browseId ?: it.videoId ?: "") + it.hashCode() }) { item ->
                val artworkUrl = item.thumbnails.lastOrNull()?.url.orEmpty()
                val subtitleText = item.artists?.map { it.name }?.connectArtists() ?: item.description.orEmpty()

                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .clickable { onItemClick(item) },
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF15181C)),
                    ) {
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

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = item.title,
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