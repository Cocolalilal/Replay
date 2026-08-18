package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.maxrave.domain.data.model.pinned.PinnedItem
import com.maxrave.domain.data.model.pinned.PinnedType
import com.maxrave.domain.utils.LocalResource
import com.maxrave.simpmusic.extension.NonLazyGrid
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.GradientHeartIcon
import com.maxrave.simpmusic.ui.component.LocalPlaylistBottomSheet
import com.maxrave.simpmusic.ui.component.PlaylistBottomSheet
import com.maxrave.simpmusic.ui.component.ReplayTopBar
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.DownloadForOffline
import com.maxrave.simpmusic.ui.icon.Edit
import com.maxrave.simpmusic.ui.icon.Favorite
import com.maxrave.simpmusic.ui.icon.KeyboardArrowDown
import com.maxrave.simpmusic.ui.icon.LibraryMusic
import com.maxrave.simpmusic.ui.icon.PeopleAlt
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.TrendingUp
import androidx.compose.ui.graphics.Brush
import com.maxrave.simpmusic.ui.navigation.destination.home.SettingsDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.LocalPlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.LoginDestination
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistType
import com.maxrave.simpmusic.ui.theme.itemSubtitleFontFamily
import com.maxrave.simpmusic.ui.theme.itemTitleFontFamily
import com.maxrave.simpmusic.ui.theme.sectionTitleFontFamily
import com.maxrave.simpmusic.viewModel.LibraryCardItem
import com.maxrave.simpmusic.viewModel.LibraryCardType
import com.maxrave.simpmusic.viewModel.LibraryFilter
import com.maxrave.simpmusic.viewModel.LibrarySortOrder
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import kotlinx.coroutines.launch
import com.maxrave.domain.manager.DataStoreManager
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import com.maxrave.simpmusic.expect.shareUrl
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import com.maxrave.simpmusic.ui.icon.AddPhotoAlternate
import com.maxrave.simpmusic.util.CustomCoverHelper
import com.maxrave.simpmusic.util.resolvePlaylistCover
import com.mohamedrejeb.calf.io.readByteArray
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import org.jetbrains.compose.resources.painterResource
import simpmusic.composeapp.generated.resources.monochrome
import com.maxrave.simpmusic.ui.component.ActionButton
import com.maxrave.simpmusic.ui.component.EndOfModalBottomSheet
import com.maxrave.simpmusic.ui.component.ReplayConfirmationDialog
import com.maxrave.simpmusic.ui.component.rememberSurfaceDarkColors
import com.maxrave.simpmusic.ui.icon.Delete
import com.maxrave.simpmusic.ui.icon.PushPin
import com.maxrave.simpmusic.ui.icon.Share
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.create
import simpmusic.composeapp.generated.resources.delete
import simpmusic.composeapp.generated.resources.delete_playlist
import simpmusic.composeapp.generated.resources.edit_title
import simpmusic.composeapp.generated.resources.playlist_name
import simpmusic.composeapp.generated.resources.save
import simpmusic.composeapp.generated.resources.share
import simpmusic.composeapp.generated.resources.title

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    innerPadding: PaddingValues = PaddingValues(),
    viewModel: LibraryViewModel = koinViewModel(),
    navController: NavController,
    onScrolling: (onTop: Boolean, direction: Int) -> Unit = { _, _ -> },
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val pinnedItems by viewModel.pinnedItems.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedSortOrder by viewModel.selectedSortOrder.collectAsStateWithLifecycle()
    val libraryItemsResource by viewModel.libraryItems.collectAsStateWithLifecycle()
    val accountThumbnail by viewModel.accountThumbnail.collectAsStateWithLifecycle()

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
    var isRefreshing by remember { mutableStateOf(false) }

    var showCreatePlaylistSheet by remember { mutableStateOf(false) }
    var filterDropdownExpanded by remember { mutableStateOf(false) }
    var sortDropdownExpanded by remember { mutableStateOf(false) }

    // Selected item for long-click bottom sheet
    var selectedItemForMenu by remember { mutableStateOf<LibraryCardItem?>(null) }
    var showItemMenu by remember { mutableStateOf(false) }

    // Confirmation dialog states
    var playlistToDelete by remember { mutableStateOf<LibraryCardItem?>(null) }
    var pinToRemove by remember { mutableStateOf<PinnedItem?>(null) }

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
        viewModel.loadCurrentFilterData()
    }

    LaunchedEffect(libraryItemsResource) {
        if (libraryItemsResource !is LocalResource.Loading) {
            isRefreshing = false
            coroutineScope.launch {
                pullToRefreshState.animateToHidden()
            }
        }
    }

    // Create playlist bottom sheet
    if (showCreatePlaylistSheet) {
        var newTitle by remember { mutableStateOf("") }
        var newDescription by remember { mutableStateOf("") }
        val createSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val hideCreateSheet: () -> Unit = {
            coroutineScope.launch {
                createSheetState.hide()
                showCreatePlaylistSheet = false
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showCreatePlaylistSheet = false },
            sheetState = createSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors().copy(containerColor = Color(0xFF1C1C20)),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Gray),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text(text = stringResource(Res.string.playlist_name), color = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = { Text(text = "Description (Optional)", color = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                viewModel.createPlaylist(
                                    title = newTitle,
                                    description = newDescription.takeIf { it.isNotBlank() },
                                )
                                hideCreateSheet()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(Color(0xFF2B3E52), RoundedCornerShape(12.dp)),
                    ) {
                        Text(
                            text = stringResource(Res.string.create),
                            fontFamily = itemTitleFontFamily(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }

    if (showItemMenu && selectedItemForMenu != null) {
        val item = selectedItemForMenu!!
        val isPinned = viewModel.isPinned(item.targetId)
        LibraryItemBottomSheet(
            item = item,
            isPinned = isPinned,
            onDismiss = {
                showItemMenu = false
                selectedItemForMenu = null
            },
            onTogglePin = {
                viewModel.togglePin(item)
            },
            onEditTitle = if (item.type == LibraryCardType.YOUTUBE_PLAYLIST) {
                { newTitle -> viewModel.updatePlaylistTitle(newTitle, item.targetId) }
            } else null,
            onDelete = if (item.type == LibraryCardType.YOUTUBE_PLAYLIST) {
                {
                    playlistToDelete = item
                    showItemMenu = false
                    selectedItemForMenu = null
                }
            } else null,
            onShare = {
                val url = when (item.type) {
                    LibraryCardType.YOUTUBE_PLAYLIST, LibraryCardType.FAVORITE_SONGS ->
                        "https://music.youtube.com/playlist?list=${item.targetId.replaceFirst("VL", "")}"
                    LibraryCardType.ALBUM ->
                        "https://music.youtube.com/playlist?list=${item.targetId.replaceFirst("VL", "")}"
                    LibraryCardType.ARTIST ->
                        "https://music.youtube.com/channel/${item.targetId}"
                    else -> "https://music.youtube.com"
                }
                shareUrl("Share ${item.title}", url)
            },
        )
    }

    if (playlistToDelete != null) {
        val target = playlistToDelete!!
        ReplayConfirmationDialog(
            title = stringResource(Res.string.delete_playlist),
            message = "Are you sure you want to delete \"${target.title}\"? This action cannot be undone.",
            confirmText = stringResource(Res.string.delete),
            onConfirm = {
                viewModel.deletePlaylist(target.targetId)
                playlistToDelete = null
            },
            onDismiss = { playlistToDelete = null },
        )
    }

    if (pinToRemove != null) {
        val target = pinToRemove!!
        ReplayConfirmationDialog(
            title = "Remove Pin",
            message = "Are you sure you want to remove \"${target.title}\" from pinned items?",
            confirmText = "Remove",
            onConfirm = {
                viewModel.removePin(target.id)
                pinToRemove = null
            },
            onDismiss = { pinToRemove = null },
        )
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
            LazyColumn(
                state = scrollState,
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Top Bar
                item(key = "library_top_bar") {
                    ReplayTopBar(
                        avatarUrl = accountThumbnail,
                        onAvatarClick = {
                            if (!accountThumbnail.isNullOrEmpty()) {
                                navController.navigate(SettingsDestination)
                            } else {
                                navController.navigate(LoginDestination)
                            }
                        },
                    )
                }

                // 1. Pinned Section (WITH 2.5dp OUTLINE - Only when not empty)
                if (pinnedItems.isNotEmpty()) {
                    item(key = "pinned_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Pinned",
                                fontFamily = sectionTitleFontFamily(),
                                fontSize = 22.sp,
                                color = Color.White,
                            )

                            // Pencil / Done Edit Button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isEditMode) Color(0xFF2B3E52) else Color(0xFF1E1E22))
                                    .clickable { viewModel.toggleEditMode() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (isEditMode) SimpIcons.Check else SimpIcons.Edit,
                                    contentDescription = "Edit Pinned",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    item(key = "pinned_grid") {
                        ReorderablePinnedGrid(
                            items = pinnedItems,
                            isEditMode = isEditMode,
                            customCoversMap = customCoversMap,
                            onItemClick = { pin ->
                                navigateToPinnedItem(pin, navController)
                            },
                            onDeleteItem = { pin ->
                                pinToRemove = pin
                            },
                            onLongClickItem = { pin ->
                                val cardItem = LibraryCardItem(
                                    id = pin.id,
                                    title = pin.title,
                                    subtitle = pin.subtitle ?: "",
                                    thumbnailUrl = pin.thumbnailUrl,
                                    type = when (pin.type) {
                                        PinnedType.FAVORITE_SONGS -> LibraryCardType.FAVORITE_SONGS
                                        PinnedType.PLAYLIST -> LibraryCardType.YOUTUBE_PLAYLIST
                                        PinnedType.ALBUM -> LibraryCardType.ALBUM
                                        PinnedType.ARTIST -> LibraryCardType.ARTIST
                                        PinnedType.DOWNLOADED_SONGS -> LibraryCardType.DOWNLOADED_SONGS
                                        PinnedType.MOST_PLAYED -> LibraryCardType.MOST_PLAYED
                                        PinnedType.SONG -> LibraryCardType.YOUTUBE_PLAYLIST
                                    },
                                    targetId = pin.targetId,
                                )
                                selectedItemForMenu = cardItem
                                showItemMenu = true
                            },
                            onReorder = { reorderedList ->
                                viewModel.updatePins(reorderedList)
                            },
                        )
                    }
                }

                // 2. "Your [Filter] ⌄" Section Header
                item(key = "your_library_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Dropdown Header ("Your Library ⌄")
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { filterDropdownExpanded = true }
                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Your ",
                                    fontFamily = sectionTitleFontFamily(),
                                    fontSize = 22.sp,
                                    color = Color.White,
                                )
                                Text(
                                    text = when (selectedFilter) {
                                        LibraryFilter.YOUR_LIBRARY -> "Library"
                                        LibraryFilter.PLAYLISTS -> "Playlists"
                                        LibraryFilter.ALBUMS -> "Albums"
                                        LibraryFilter.ARTISTS -> "Artists"
                                    },
                                    fontFamily = sectionTitleFontFamily(),
                                    fontSize = 22.sp,
                                    color = Color(0xFFAAAAAA),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = SimpIcons.KeyboardArrowDown,
                                    contentDescription = "Filter Dropdown",
                                    tint = Color(0xFFAAAAAA),
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            // Material3 DropdownMenu
                            DropdownMenu(
                                expanded = filterDropdownExpanded,
                                onDismissRequest = { filterDropdownExpanded = false },
                                modifier = Modifier.background(Color(0xFF1E1E22)),
                            ) {
                                LibraryFilter.entries.forEach { filter ->
                                    val isSelected = filter == selectedFilter
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = when (filter) {
                                                    LibraryFilter.YOUR_LIBRARY -> "All"
                                                    LibraryFilter.PLAYLISTS -> "Playlists"
                                                    LibraryFilter.ALBUMS -> "Albums"
                                                    LibraryFilter.ARTISTS -> "Artists"
                                                },
                                                fontFamily = itemTitleFontFamily(),
                                                color = if (isSelected) Color(0xFF8BA7C4) else Color.White,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            )
                                        },
                                        onClick = {
                                            viewModel.setSelectedFilter(filter)
                                            filterDropdownExpanded = false
                                        },
                                    )
                                }
                            }
                        }

                        // Right Buttons: Sort Order Dropdown Pill & Add Button
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Sort Order Dropdown Pill (No sort icon)
                            Box {
                                Row(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color(0xFF1E1E22))
                                        .clickable { sortDropdownExpanded = true }
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = selectedSortOrder.displayName,
                                        fontFamily = itemTitleFontFamily(),
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Icon(
                                        imageVector = SimpIcons.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color(0xFFAAAAAA),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }

                                DropdownMenu(
                                    expanded = sortDropdownExpanded,
                                    onDismissRequest = { sortDropdownExpanded = false },
                                    modifier = Modifier.background(Color(0xFF1E1E22)),
                                ) {
                                    LibrarySortOrder.entries.forEach { sortOrder ->
                                        val isSelected = sortOrder == selectedSortOrder
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = sortOrder.displayName,
                                                    fontFamily = itemTitleFontFamily(),
                                                    color = if (isSelected) Color(0xFF8BA7C4) else Color.White,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                )
                                            },
                                            onClick = {
                                                viewModel.setSelectedSortOrder(sortOrder)
                                                sortDropdownExpanded = false
                                            },
                                        )
                                    }
                                }
                            }

                            // Plus Button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E1E22))
                                    .clickable { showCreatePlaylistSheet = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = SimpIcons.Add,
                                    contentDescription = "Create Playlist",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }

                // 3. Library Content (List View with Optical Grouping)
                when (val res = libraryItemsResource) {
                    is LocalResource.Loading -> {
                        item(key = "library_loading") {
                            CenterLoadingBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                            )
                        }
                    }

                    is LocalResource.Success -> {
                        val itemsList = res.data ?: emptyList()
                        if (itemsList.isEmpty()) {
                            item(key = "library_empty") {
                                EmptyLibraryState(
                                    filter = selectedFilter,
                                    onCreatePlaylist = { showCreatePlaylistSheet = true },
                                )
                            }
                        } else {
                            // List View: Full Width Rounded Cards (WITHOUT OUTLINES, with optical grouping)
                            item(key = "library_list_group") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    itemsList.forEachIndexed { index, item ->
                                        val isFirst = index == 0
                                        val isLast = index == itemsList.lastIndex
                                        val customCover = resolvePlaylistCover(item.targetId, item.thumbnailUrl, customCoversMap)
                                            ?: resolvePlaylistCover(item.id, item.thumbnailUrl, customCoversMap)

                                        LibraryListCard(
                                            item = item,
                                            isFirstInGroup = isFirst,
                                            isLastInGroup = isLast,
                                            customCover = customCover,
                                            onCardClick = { navigateToLibraryItem(item, navController) },
                                            onLongClick = {
                                                selectedItemForMenu = item
                                                showItemMenu = true
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    else -> {}
                }

                item(key = "library_end") {
                    EndOfPage()
                }
            }
        }
    }
}

/**
 * 3-Column Reorderable Pinned Items Grid supporting fast immediate drag (in edit mode)
 * and long-press drag (in normal or edit mode), with real-time cell swapping and persistent state.
 */
@Composable
fun ReorderablePinnedGrid(
    items: List<PinnedItem>,
    isEditMode: Boolean,
    customCoversMap: Map<String, String>,
    onItemClick: (PinnedItem) -> Unit,
    onDeleteItem: (PinnedItem) -> Unit,
    onLongClickItem: (PinnedItem) -> Unit,
    onReorder: (List<PinnedItem>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var localItems by remember(items) { mutableStateOf(items) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        val totalWidth = maxWidth
        val spacing = 16.dp
        val columns = 3
        val columnWidth = (totalWidth - spacing * (columns - 1)) / columns
        val cellWidthPx = with(LocalDensity.current) { (columnWidth + spacing).toPx() }
        val cellHeightPx = with(LocalDensity.current) { (columnWidth + 30.dp + spacing).toPx() }

        val rows = (localItems.size + columns - 1) / columns
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            for (rowIndex in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    for (colIndex in 0 until columns) {
                        val index = rowIndex * columns + colIndex
                        if (index < localItems.size) {
                            val pin = localItems[index]
                            val isDragging = draggingIndex == index
                            val customCover = resolvePlaylistCover(pin.targetId, pin.thumbnailUrl, customCoversMap)
                                ?: resolvePlaylistCover(pin.id, pin.thumbnailUrl, customCoversMap)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .zIndex(if (isDragging) 10f else 1f)
                                    .graphicsLayer {
                                        if (isDragging) {
                                            translationX = dragOffset.x
                                            translationY = dragOffset.y
                                            scaleX = 1.08f
                                            scaleY = 1.08f
                                            shadowElevation = 12.dp.toPx()
                                        }
                                    }
                                    .pointerInput(pin.id, isEditMode) {
                                        if (isEditMode) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    draggingIndex = index
                                                    dragOffset = Offset.Zero
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffset += dragAmount
                                                    val currentIdx = draggingIndex ?: return@detectDragGestures
                                                    val startCol = currentIdx % columns
                                                    val startRow = currentIdx / columns
                                                    val targetCol = ((startCol * cellWidthPx + dragOffset.x + cellWidthPx / 2) / cellWidthPx).toInt().coerceIn(0, columns - 1)
                                                    val targetRow = ((startRow * cellHeightPx + dragOffset.y + cellHeightPx / 2) / cellHeightPx).toInt().coerceAtLeast(0)
                                                    val targetIdx = (targetRow * columns + targetCol).coerceIn(0, localItems.lastIndex)
                                                    if (targetIdx != currentIdx && targetIdx in localItems.indices) {
                                                        val mutable = localItems.toMutableList()
                                                        val movedItem = mutable.removeAt(currentIdx)
                                                        mutable.add(targetIdx, movedItem)
                                                        localItems = mutable
                                                        draggingIndex = targetIdx
                                                        dragOffset = Offset.Zero
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggingIndex = null
                                                    dragOffset = Offset.Zero
                                                    onReorder(localItems)
                                                },
                                                onDragCancel = {
                                                    draggingIndex = null
                                                    dragOffset = Offset.Zero
                                                    onReorder(localItems)
                                                },
                                            )
                                        } else {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggingIndex = index
                                                    dragOffset = Offset.Zero
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffset += dragAmount
                                                    val currentIdx = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                                    val startCol = currentIdx % columns
                                                    val startRow = currentIdx / columns
                                                    val targetCol = ((startCol * cellWidthPx + dragOffset.x + cellWidthPx / 2) / cellWidthPx).toInt().coerceIn(0, columns - 1)
                                                    val targetRow = ((startRow * cellHeightPx + dragOffset.y + cellHeightPx / 2) / cellHeightPx).toInt().coerceAtLeast(0)
                                                    val targetIdx = (targetRow * columns + targetCol).coerceIn(0, localItems.lastIndex)
                                                    if (targetIdx != currentIdx && targetIdx in localItems.indices) {
                                                        val mutable = localItems.toMutableList()
                                                        val movedItem = mutable.removeAt(currentIdx)
                                                        mutable.add(targetIdx, movedItem)
                                                        localItems = mutable
                                                        draggingIndex = targetIdx
                                                        dragOffset = Offset.Zero
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggingIndex = null
                                                    dragOffset = Offset.Zero
                                                    onReorder(localItems)
                                                },
                                                onDragCancel = {
                                                    draggingIndex = null
                                                    dragOffset = Offset.Zero
                                                    onReorder(localItems)
                                                },
                                            )
                                        }
                                    },
                            ) {
                                PinnedItemCard(
                                    item = pin,
                                    isEditMode = isEditMode,
                                    customCover = customCover,
                                    onCardClick = {
                                        if (isEditMode) {
                                            onDeleteItem(pin)
                                        } else {
                                            onItemClick(pin)
                                        }
                                    },
                                    onDeleteClick = { onDeleteItem(pin) },
                                    onLongClick = { onLongClickItem(pin) },
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pinned item card in 3-column grid (WITH OUTLINE)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PinnedItemCard(
    item: PinnedItem,
    isEditMode: Boolean,
    customCover: String? = null,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isArtist = item.type == PinnedType.ARTIST
    val pinShape = if (isArtist) CircleShape else RoundedCornerShape(18.dp)
    val innerShape = if (isArtist) CircleShape else RoundedCornerShape(15.5.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onCardClick() },
                onLongClick = { onLongClick?.invoke() },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(pinShape)
                .background(Color(0xFF15181C))
                .padding(2.5.dp)
                .clip(innerShape)
                .background(Color(0xFF141416)),
            contentAlignment = Alignment.Center,
        ) {
            val activeThumbnail = customCover ?: item.thumbnailUrl
            // Artwork or Gradient Special Icon
            when (item.type) {
                PinnedType.FAVORITE_SONGS -> {
                    if (customCover != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(customCover)
                                .crossfade(true)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(innerShape),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF141416)),
                            contentAlignment = Alignment.Center,
                        ) {
                            GradientHeartIcon(size = 44.dp)
                        }
                    }
                }

                PinnedType.DOWNLOADED_SONGS -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF141416)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = SimpIcons.DownloadForOffline,
                            contentDescription = "Downloads",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }

                PinnedType.MOST_PLAYED -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF141416)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = SimpIcons.TrendingUp,
                            contentDescription = "Most Played",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }

                else -> {
                    if (!activeThumbnail.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(activeThumbnail)
                                .crossfade(true)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(innerShape),
                        )
                    } else {
                        Icon(
                            imageVector = if (isArtist) SimpIcons.PeopleAlt else SimpIcons.LibraryMusic,
                            contentDescription = item.title,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(38.dp),
                        )
                    }
                }
            }

            // Edit Mode Delete Badge
            if (isEditMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                        .clickable { onDeleteClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = SimpIcons.Close,
                        contentDescription = "Remove Pin",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            fontFamily = itemTitleFontFamily(),
            fontSize = 13.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Full-width horizontal rounded card with grouping optical corners (WITHOUT OUTLINE)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryListCard(
    item: LibraryCardItem,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    customCover: String? = null,
    onCardClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isArtist = item.type == LibraryCardType.ARTIST
    val cardShape = RoundedCornerShape(
        topStart = if (isFirstInGroup) 20.dp else 6.dp,
        topEnd = if (isFirstInGroup) 20.dp else 6.dp,
        bottomStart = if (isLastInGroup) 20.dp else 6.dp,
        bottomEnd = if (isLastInGroup) 20.dp else 6.dp,
    )

    val artShape = if (isArtist) {
        CircleShape
    } else {
        RoundedCornerShape(
            topStart = if (isFirstInGroup) 14.dp else 6.dp,
            topEnd = 6.dp,
            bottomStart = if (isLastInGroup) 14.dp else 6.dp,
            bottomEnd = 6.dp,
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(cardShape)
            .background(Color(0xFF15181C))
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val activeThumbnail = customCover ?: item.thumbnailUrl
        // Thumbnail with concentric optical corner rounding
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(artShape)
                .background(Color(0xFF222226)),
            contentAlignment = Alignment.Center,
        ) {
            when (item.type) {
                LibraryCardType.FAVORITE_SONGS -> {
                    if (customCover != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(customCover)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(artShape),
                        )
                    } else {
                        GradientHeartIcon(size = 28.dp)
                    }
                }

                LibraryCardType.DOWNLOADED_SONGS -> {
                    Icon(
                        imageVector = SimpIcons.DownloadForOffline,
                        contentDescription = "Downloads",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(28.dp),
                    )
                }

                LibraryCardType.MOST_PLAYED -> {
                    Icon(
                        imageVector = SimpIcons.TrendingUp,
                        contentDescription = "Most Played",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(28.dp),
                    )
                }

                else -> {
                    if (!activeThumbnail.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(activeThumbnail)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(artShape),
                        )
                    } else {
                        Icon(
                            imageVector = if (isArtist) SimpIcons.PeopleAlt else SimpIcons.LibraryMusic,
                            contentDescription = item.title,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = item.title,
                fontFamily = itemTitleFontFamily(),
                fontSize = 15.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                fontFamily = itemSubtitleFontFamily(),
                fontSize = 12.5.sp,
                color = Color(0xFF9E9EA4),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 3-Column Grid card for Library items (WITHOUT OUTLINE)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryGridCard(
    item: LibraryCardItem,
    customCover: String? = null,
    onCardClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isArtist = item.type == LibraryCardType.ARTIST
    val cardShape = if (isArtist) CircleShape else RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onLongClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val activeThumbnail = customCover ?: item.thumbnailUrl
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(cardShape)
                .background(Color(0xFF15181C)),
            contentAlignment = Alignment.Center,
        ) {
            if (item.type == LibraryCardType.FAVORITE_SONGS) {
                if (customCover != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(customCover)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(cardShape),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF15181C)),
                        contentAlignment = Alignment.Center,
                    ) {
                        GradientHeartIcon(size = 40.dp)
                    }
                }
            } else if (!activeThumbnail.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(activeThumbnail)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(cardShape),
                )
            } else {
                Icon(
                    imageVector = if (isArtist) SimpIcons.PeopleAlt else SimpIcons.LibraryMusic,
                    contentDescription = item.title,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            fontFamily = itemTitleFontFamily(),
            fontSize = 13.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = item.subtitle,
            fontFamily = itemSubtitleFontFamily(),
            fontSize = 11.sp,
            color = Color(0xFF9E9EA4),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun EmptyLibraryState(
    filter: LibraryFilter,
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = SimpIcons.LibraryMusic,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(56.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No items in ${filter.displayName}",
            fontFamily = sectionTitleFontFamily(),
            fontSize = 16.sp,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Add items or create playlists to build your collection.",
            fontFamily = itemSubtitleFontFamily(),
            fontSize = 13.sp,
            color = Color(0xFF9E9EA4),
            textAlign = TextAlign.Center,
        )
        if (filter == LibraryFilter.PLAYLISTS || filter == LibraryFilter.YOUR_LIBRARY) {
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF2B3E52))
                    .clickable { onCreatePlaylist() }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "+ Create Playlist",
                    fontFamily = itemTitleFontFamily(),
                    fontSize = 14.sp,
                    color = Color.White,
                )
            }
        }
    }
}

private fun navigateToPinnedItem(pin: PinnedItem, navController: NavController) {
    when (pin.type) {
        PinnedType.FAVORITE_SONGS -> {
            navController.navigate(PlaylistDestination("LM", isYourYouTubePlaylist = true))
        }

        PinnedType.PLAYLIST -> {
            navController.navigate(PlaylistDestination(pin.targetId, isYourYouTubePlaylist = true))
        }

        PinnedType.ALBUM -> {
            navController.navigate(AlbumDestination(pin.targetId))
        }

        PinnedType.ARTIST -> {
            navController.navigate(ArtistDestination(pin.targetId))
        }

        else -> {}
    }
}

private fun navigateToLibraryItem(item: LibraryCardItem, navController: NavController) {
    when (item.type) {
        LibraryCardType.FAVORITE_SONGS -> {
            navController.navigate(PlaylistDestination("LM", isYourYouTubePlaylist = true))
        }

        LibraryCardType.YOUTUBE_PLAYLIST -> {
            navController.navigate(PlaylistDestination(item.targetId, isYourYouTubePlaylist = true))
        }

        LibraryCardType.ALBUM -> {
            navController.navigate(AlbumDestination(item.targetId))
        }

        LibraryCardType.ARTIST -> {
            navController.navigate(ArtistDestination(item.targetId))
        }

        else -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryItemBottomSheet(
    item: LibraryCardItem,
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onEditTitle: ((String) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hideModalBottomSheet: () -> Unit = {
        coroutineScope.launch {
            modelBottomSheetState.hide()
            onDismiss()
        }
    }
    var showEditTitle by remember { mutableStateOf(false) }

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

    val isCustomizable = item.type == LibraryCardType.YOUTUBE_PLAYLIST ||
        item.type == LibraryCardType.ALBUM ||
        item.type == LibraryCardType.FAVORITE_SONGS

    val playlistTargetKey = if (item.type == LibraryCardType.FAVORITE_SONGS) {
        item.targetId.ifEmpty { "LM" }
    } else {
        item.targetId.ifEmpty { item.id }
    }

    val hasCustomCover = remember(customCoversMap, playlistTargetKey, item.id) {
        CustomCoverHelper.hasCustomCover(playlistTargetKey, customCoversMap) ||
            CustomCoverHelper.hasCustomCover(item.id, customCoversMap)
    }

    val calfPlatformContext = com.mohamedrejeb.calf.core.LocalPlatformContext.current
    val picker = rememberFilePickerLauncher(
        type = FilePickerFileType.Image,
        selectionMode = FilePickerSelectionMode.Single,
        onResult = { files ->
            val file = files.firstOrNull()
            if (file != null) {
                coroutineScope.launch {
                    val bytes = runCatching { file.readByteArray(calfPlatformContext) }.getOrNull()
                    if (bytes != null) {
                        CustomCoverHelper.saveCustomCover(playlistTargetKey, bytes, dataStoreManager)
                    }
                    hideModalBottomSheet()
                }
            }
        },
    )

    if (showEditTitle) {
        var newTitle by remember { mutableStateOf(item.title) }
        val showEditTitleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val hideEditTitleBottomSheet: () -> Unit = {
            coroutineScope.launch {
                showEditTitleSheetState.hide()
                onDismiss()
            }
        }
        ModalBottomSheet(
            onDismissRequest = { showEditTitle = false },
            sheetState = showEditTitleSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().container),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier = Modifier.width(60.dp).height(4.dp),
                        colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().handle),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { s -> newTitle = s },
                        label = { Text(text = stringResource(Res.string.title)) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                onEditTitle?.invoke(newTitle)
                                hideEditTitleBottomSheet()
                                hideModalBottomSheet()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    ) {
                        Text(text = stringResource(Res.string.save), color = Color.White)
                    }
                    EndOfModalBottomSheet()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modelBottomSheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0) },
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().container),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Card(
                    modifier = Modifier.width(48.dp).height(4.dp),
                    colors = CardDefaults.cardColors().copy(containerColor = rememberSurfaceDarkColors().handle),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(16.dp))

                // Item Header Preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val isArtist = item.type == LibraryCardType.ARTIST
                    val shape = if (isArtist) CircleShape else RoundedCornerShape(10.dp)
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(shape)
                            .background(Color(0xFF141416)),
                        contentAlignment = Alignment.Center,
                    ) {
                        val activeCover = resolvePlaylistCover(playlistTargetKey, item.thumbnailUrl, customCoversMap)
                            ?: resolvePlaylistCover(item.id, item.thumbnailUrl, customCoversMap)
                            ?: item.thumbnailUrl
                        when (item.type) {
                            LibraryCardType.FAVORITE_SONGS -> {
                                if (hasCustomCover && !activeCover.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalPlatformContext.current)
                                            .data(activeCover)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    GradientHeartIcon(size = 32.dp)
                                }
                            }
                            LibraryCardType.DOWNLOADED_SONGS -> Icon(
                                imageVector = SimpIcons.DownloadForOffline,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(32.dp),
                            )
                            LibraryCardType.MOST_PLAYED -> Icon(
                                imageVector = SimpIcons.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(32.dp),
                            )
                            else -> AsyncImage(
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(activeCover)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontFamily = itemTitleFontFamily(),
                            fontSize = 15.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.subtitle.ifEmpty {
                                when (item.type) {
                                    LibraryCardType.YOUTUBE_PLAYLIST -> "Playlist"
                                    LibraryCardType.ALBUM -> "Album"
                                    LibraryCardType.ARTIST -> "Artist"
                                    LibraryCardType.FAVORITE_SONGS -> "Auto Playlist"
                                    LibraryCardType.DOWNLOADED_SONGS -> "Auto Playlist"
                                    LibraryCardType.MOST_PLAYED -> "Auto Playlist"
                                }
                            },
                            fontFamily = itemSubtitleFontFamily(),
                            fontSize = 13.sp,
                            color = Color(0xFF8E8E93),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pin / Unpin Action
                ActionButton(
                    icon = SimpIcons.PushPin,
                    text = null,
                    textString = if (isPinned) "Unpin from library" else "Pin to library",
                ) {
                    onTogglePin()
                    hideModalBottomSheet()
                }

                // If playlist, allow editing title and deleting
                if (item.type == LibraryCardType.YOUTUBE_PLAYLIST) {
                    ActionButton(
                        icon = SimpIcons.Edit,
                        text = Res.string.edit_title,
                    ) {
                        showEditTitle = true
                    }
                    if (onDelete != null) {
                        ActionButton(
                            icon = SimpIcons.Delete,
                            text = Res.string.delete,
                        ) {
                            onDelete()
                            hideModalBottomSheet()
                        }
                    }
                }

                if (isCustomizable) {
                    ActionButton(
                        icon = SimpIcons.AddPhotoAlternate,
                        text = null,
                        textString = "Change playlist cover",
                        trailingContent = {
                            Image(
                                painter = painterResource(Res.drawable.monochrome),
                                contentDescription = "Replay Customization",
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)),
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    ) {
                        picker.launch()
                    }
                    if (hasCustomCover) {
                        ActionButton(
                            icon = SimpIcons.Delete,
                            text = null,
                            textString = "Remove custom cover",
                        ) {
                            coroutineScope.launch {
                                CustomCoverHelper.removeCustomCover(playlistTargetKey, dataStoreManager)
                                hideModalBottomSheet()
                            }
                        }
                    }
                }

                // Share Action
                if (onShare != null) {
                    ActionButton(
                        icon = SimpIcons.Share,
                        text = Res.string.share,
                    ) {
                        onShare()
                        hideModalBottomSheet()
                    }
                }

                EndOfModalBottomSheet()
            }
        }
    }
}