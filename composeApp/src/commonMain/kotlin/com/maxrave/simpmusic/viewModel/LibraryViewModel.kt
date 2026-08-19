package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.model.pinned.PinnedItem
import com.maxrave.domain.data.model.pinned.PinnedType
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.LocalResource
import com.maxrave.domain.utils.Resource
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.added_local_playlist

enum class LibraryFilter(val displayName: String) {
    YOUR_LIBRARY("Library"),
    PLAYLISTS("Playlists"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
}

enum class LibrarySortOrder(val displayName: String) {
    RECENTLY_ADDED("Recently added"),
    RECENTLY_PLAYED("Recently played"),
    A_TO_Z("A to Z"),
    Z_TO_A("Z to A"),
}

data class LibraryCardItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String? = null,
    val type: LibraryCardType,
    val targetId: String,
)

enum class LibraryCardType {
    YOUTUBE_PLAYLIST,
    ALBUM,
    ARTIST,
    FAVORITE_SONGS,
    DOWNLOADED_SONGS,
    MOST_PLAYED,
}

class LibraryViewModel(
    private val dataStoreManager: DataStoreManager,
    private val songRepository: SongRepository,
    private val commonRepository: CommonRepository,
    private val playlistRepository: PlaylistRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
) : BaseViewModel() {

    val pinnedItems: StateFlow<List<PinnedItem>> =
        dataStoreManager.pinnedItems.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList(),
        )

    val isGridView: StateFlow<Boolean> =
        dataStoreManager.isLibraryGridView.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            false,
        )

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> get() = _isEditMode.asStateFlow()

    private val _selectedFilter = MutableStateFlow(LibraryFilter.YOUR_LIBRARY)
    val selectedFilter: StateFlow<LibraryFilter> get() = _selectedFilter.asStateFlow()

    private val _selectedSortOrder = MutableStateFlow(LibrarySortOrder.RECENTLY_ADDED)
    val selectedSortOrder: StateFlow<LibrarySortOrder> get() = _selectedSortOrder.asStateFlow()

    private val _libraryItems = MutableStateFlow<LocalResource<List<LibraryCardItem>>>(LocalResource.Loading())
    val libraryItems: StateFlow<LocalResource<List<LibraryCardItem>>> get() = _libraryItems.asStateFlow()

    private var rawLibraryItems: List<LibraryCardItem> = emptyList()

    fun setSelectedSortOrder(sortOrder: LibrarySortOrder) {
        _selectedSortOrder.value = sortOrder
        applySort()
    }

    private fun applySort() {
        val current = rawLibraryItems
        if (current.isEmpty()) {
            _libraryItems.value = LocalResource.Success(emptyList())
            return
        }

        // Liked Songs item (if present) always stays pinned at the very top (index 0)
        val favoriteItem = current.firstOrNull { it.id == "favorite_songs" }
        val rest = current.filterNot { it.id == "favorite_songs" }

        val sortedRest = when (_selectedSortOrder.value) {
            LibrarySortOrder.RECENTLY_ADDED -> rest
            LibrarySortOrder.RECENTLY_PLAYED -> rest.reversed()
            LibrarySortOrder.A_TO_Z -> rest.sortedBy { it.title.lowercase() }
            LibrarySortOrder.Z_TO_A -> rest.sortedByDescending { it.title.lowercase() }
        }

        val result = if (favoriteItem != null) listOf(favoriteItem) + sortedRest else sortedRest
        _libraryItems.value = LocalResource.Success(result)
    }

    private val _accountThumbnail = MutableStateFlow<String?>(null)
    val accountThumbnail: StateFlow<String?> get() = _accountThumbnail.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val youtubeLoggedIn = dataStoreManager.loggedIn.mapLatest { it == DataStoreManager.TRUE }

    init {
        viewModelScope.launch {
            dataStoreManager.cookie.distinctUntilChanged().collect {
                _accountThumbnail.value = dataStoreManager.getString("AccountThumbUrl").first().takeIf { !it.isNullOrEmpty() }
            }
        }
        loadCurrentFilterData()
    }

    fun setSelectedFilter(filter: LibraryFilter) {
        _selectedFilter.value = filter
        loadCurrentFilterData()
    }

    fun toggleGridView() {
        viewModelScope.launch {
            dataStoreManager.setLibraryGridView(!isGridView.value)
        }
    }

    fun toggleEditMode(enable: Boolean? = null) {
        _isEditMode.value = enable ?: !_isEditMode.value
    }

    fun removePin(id: String) {
        viewModelScope.launch {
            dataStoreManager.removePin(id)
        }
    }

    fun pinItem(item: PinnedItem) {
        viewModelScope.launch {
            dataStoreManager.addPin(item)
        }
    }

    fun isPinned(targetId: String): Boolean {
        return pinnedItems.value.any { it.id == targetId || it.targetId == targetId }
    }

    fun togglePin(item: LibraryCardItem) {
        viewModelScope.launch {
            val existing = pinnedItems.value.find { it.id == item.id || it.targetId == item.targetId }
            if (existing != null) {
                dataStoreManager.removePin(existing.id)
            } else {
                val pinnedType = when (item.type) {
                    LibraryCardType.FAVORITE_SONGS -> PinnedType.FAVORITE_SONGS
                    LibraryCardType.YOUTUBE_PLAYLIST -> PinnedType.PLAYLIST
                    LibraryCardType.ALBUM -> PinnedType.ALBUM
                    LibraryCardType.ARTIST -> PinnedType.ARTIST
                    LibraryCardType.DOWNLOADED_SONGS -> PinnedType.DOWNLOADED_SONGS
                    LibraryCardType.MOST_PLAYED -> PinnedType.MOST_PLAYED
                }
                dataStoreManager.addPin(
                    PinnedItem(
                        id = item.id,
                        title = item.title,
                        subtitle = item.subtitle,
                        thumbnailUrl = item.thumbnailUrl,
                        type = pinnedType,
                        targetId = item.targetId,
                    ),
                )
            }
        }
    }

    fun updatePins(items: List<PinnedItem>) {
        viewModelScope.launch {
            dataStoreManager.updatePins(items)
        }
    }

    fun createPlaylist(title: String, description: String? = null, privacyStatus: String = "PRIVATE") {
        viewModelScope.launch {
            playlistRepository.createPlaylist(
                title = title,
                description = description,
                privacyStatus = privacyStatus,
            ).collectLatest { res ->
                if (res is Resource.Success) {
                    loadCurrentFilterData()
                }
            }
        }
    }

    fun updatePlaylistTitle(newTitle: String, playlistId: String) {
        viewModelScope.launch {
            playlistRepository.editPlaylist(
                playlistId = playlistId,
                title = newTitle,
            ).collectLatest { res ->
                if (res is Resource.Success) {
                    loadCurrentFilterData()
                }
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId).collectLatest { res ->
                if (res is Resource.Success) {
                    loadCurrentFilterData()
                }
            }
        }
    }

    fun deleteSong(videoId: String) {
        viewModelScope.launch {
            songRepository.updateSongInLibrary(
                kotlinx.datetime.LocalDateTime(1970, 1, 1, 0, 0, 0),
                videoId,
            ).collectLatest {
                loadCurrentFilterData()
            }
        }
    }

    private var loadJob: Job? = null

    fun loadCurrentFilterData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _libraryItems.value = LocalResource.Loading()
            try {
                when (_selectedFilter.value) {
                    LibraryFilter.YOUR_LIBRARY -> loadYourLibraryAll()
                    LibraryFilter.PLAYLISTS -> loadPlaylists()
                    LibraryFilter.ALBUMS -> loadFavoriteAlbums()
                    LibraryFilter.ARTISTS -> loadFavoriteArtists()
                }
            } catch (e: Exception) {
                Logger.e("LibraryViewModel", "Error loading library data: ${e.message}")
                _libraryItems.value = LocalResource.Error(e.message ?: "Failed to load library")
            }
        }
    }

    private suspend fun loadYourLibraryAll() {
        combine(
            playlistRepository.getLibraryPlaylist().catch { emit(null) },
            albumRepository.getLibraryAlbums().catch { emit(emptyList()) },
            artistRepository.getLibraryArtists().catch { emit(emptyList()) },
        ) { ytPlaylists, libraryAlbums, libraryArtists ->
            val list = mutableListOf<LibraryCardItem>()

            // 1. Favorite Songs (Liked songs - LM) at top
            list.add(
                LibraryCardItem(
                    id = "favorite_songs",
                    title = "Liked Songs",
                    subtitle = "Playlist • YouTube Music",
                    thumbnailUrl = null,
                    type = LibraryCardType.FAVORITE_SONGS,
                    targetId = "LM",
                ),
            )

            // 2. YouTube Playlists (filtering out duplicate Liked Songs and podcasts / episodes)
            (ytPlaylists ?: emptyList()).filterNot { pl ->
                val t = pl.title.lowercase()
                val id = pl.browseId.lowercase()
                id == "lm" || id == "vllm" || id == "femusic_liked_videos" ||
                    t == "liked music" || t == "liked songs" || t == "liked videos" || t == "your likes" ||
                    t.contains("podcast") || t.contains("episode") || t.contains("new episodes") || t.contains("show")
            }.forEach { pl ->
                list.add(
                    LibraryCardItem(
                        id = "yt_${pl.browseId}",
                        title = pl.title,
                        subtitle = "Playlist • ${pl.author.ifEmpty { "YouTube Music" }}",
                        thumbnailUrl = pl.thumbnails.lastOrNull()?.url,
                        type = LibraryCardType.YOUTUBE_PLAYLIST,
                        targetId = pl.browseId,
                    ),
                )
            }

            // 3. Account Albums (from the signed-in YouTube Music account)
            (libraryAlbums ?: emptyList()).forEach { album ->
                list.add(
                    LibraryCardItem(
                        id = "album_${album.browseId}",
                        title = album.title,
                        subtitle = "Album • ${album.artists.joinToString(", ") { it.name }}",
                        thumbnailUrl = album.thumbnails.lastOrNull()?.url,
                        type = LibraryCardType.ALBUM,
                        targetId = album.browseId,
                    ),
                )
            }

            // 4. Account Artists (subscribed/followed on the signed-in account)
            (libraryArtists ?: emptyList()).forEach { artist ->
                list.add(
                    LibraryCardItem(
                        id = "artist_${artist.browseId}",
                        title = artist.artist,
                        subtitle = "Artist",
                        thumbnailUrl = artist.thumbnails.lastOrNull()?.url,
                        type = LibraryCardType.ARTIST,
                        targetId = artist.browseId,
                    ),
                )
            }

            list
        }.collect { items ->
            rawLibraryItems = items
            applySort()
        }
    }

    private suspend fun loadPlaylists() {
        playlistRepository.getLibraryPlaylist().catch { emit(null) }.collectLatest { ytPlaylists ->
            val list = mutableListOf<LibraryCardItem>()

            // 1. Favorite Songs at top
            list.add(
                LibraryCardItem(
                    id = "favorite_songs",
                    title = "Liked Songs",
                    subtitle = "Playlist • YouTube Music",
                    thumbnailUrl = null,
                    type = LibraryCardType.FAVORITE_SONGS,
                    targetId = "LM",
                ),
            )

            // 2. YouTube Playlists (filtering out duplicate Liked Songs and podcasts / episodes)
            (ytPlaylists ?: emptyList()).filterNot { pl ->
                val t = pl.title.lowercase()
                val id = pl.browseId.lowercase()
                id == "lm" || id == "vllm" || id == "femusic_liked_videos" ||
                    t == "liked music" || t == "liked songs" || t == "liked videos" || t == "your likes" ||
                    t.contains("podcast") || t.contains("episode") || t.contains("new episodes") || t.contains("show")
            }.forEach { pl ->
                list.add(
                    LibraryCardItem(
                        id = "yt_${pl.browseId}",
                        title = pl.title,
                        subtitle = "Playlist • ${pl.author.ifEmpty { "YouTube Music" }}",
                        thumbnailUrl = pl.thumbnails.lastOrNull()?.url,
                        type = LibraryCardType.YOUTUBE_PLAYLIST,
                        targetId = pl.browseId,
                    ),
                )
            }

            rawLibraryItems = list
            applySort()
        }
    }

    private suspend fun loadFavoriteAlbums() {
        albumRepository.getLibraryAlbums().catch { emit(emptyList()) }.collectLatest { libraryAlbums ->
            val list = (libraryAlbums ?: emptyList()).filterNot { album ->
                val t = album.title.lowercase()
                t.contains("podcast") || t.contains("episode")
            }.map { album ->
                LibraryCardItem(
                    id = "album_${album.browseId}",
                    title = album.title,
                    subtitle = "Album • ${album.artists.joinToString(", ") { it.name }}",
                    thumbnailUrl = album.thumbnails.lastOrNull()?.url,
                    type = LibraryCardType.ALBUM,
                    targetId = album.browseId,
                )
            }
            rawLibraryItems = list
            applySort()
        }
    }

    private suspend fun loadFavoriteArtists() {
        artistRepository.getLibraryArtists().catch { emit(emptyList()) }.collectLatest { libraryArtists ->
            val list = (libraryArtists ?: emptyList()).map { artist ->
                LibraryCardItem(
                    id = "artist_${artist.browseId}",
                    title = artist.artist,
                    subtitle = "Artist",
                    thumbnailUrl = artist.thumbnails.lastOrNull()?.url,
                    type = LibraryCardType.ARTIST,
                    targetId = artist.browseId,
                )
            }
            rawLibraryItems = list
            applySort()
        }
    }
}