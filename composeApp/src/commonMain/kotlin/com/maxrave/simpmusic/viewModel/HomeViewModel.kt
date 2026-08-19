package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.common.SELECTED_LANGUAGE
import com.maxrave.common.SUPPORTED_LANGUAGE
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.data.model.home.HomeDataCombine
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.home.chart.Chart
import com.maxrave.domain.data.model.mood.Mood
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.manager.DataStoreManager.Values.TRUE
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.toTrack
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.util.isListenAgainSection
import com.maxrave.simpmusic.util.isQuickPicksSection
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.music_video
import simpmusic.composeapp.generated.resources.new_release
import simpmusic.composeapp.generated.resources.song
import simpmusic.composeapp.generated.resources.view_count

class HomeViewModel(
    private val dataStoreManager: DataStoreManager,
    private val homeRepository: HomeRepository,
) : BaseViewModel() {
    private val _homeItemList: MutableStateFlow<List<HomeItem>> =
        MutableStateFlow(arrayListOf())
    val homeItemList: StateFlow<List<HomeItem>> = _homeItemList

    private var _homeListState = MutableStateFlow<ListState>(ListState.IDLE)
    val homeListState: StateFlow<ListState> = _homeListState

    private var _continuation = MutableStateFlow<String?>(null)
    val continuation: StateFlow<String?> = _continuation

    private val _exploreMoodItem: MutableStateFlow<Mood?> = MutableStateFlow(null)
    val exploreMoodItem: StateFlow<Mood?> = _exploreMoodItem
    private val _accountInfo: MutableStateFlow<Pair<String?, String?>?> = MutableStateFlow(null)
    val accountInfo: StateFlow<Pair<String?, String?>?> = _accountInfo

    private var homeJob: Job? = null

    val showSnackBarErrorState = MutableSharedFlow<String>()

    private val _chart: MutableStateFlow<Chart?> = MutableStateFlow(null)
    val chart: StateFlow<Chart?> = _chart
    private val _newRelease: MutableStateFlow<List<HomeItem>> = MutableStateFlow(arrayListOf())
    val newRelease: StateFlow<List<HomeItem>> = _newRelease

    val featuredCarouselItems: StateFlow<List<Content>> =
        combine(_homeItemList, _newRelease) { homeList, newReleaseList ->
            // 1. Take items directly from YouTube Music's "Listen again" section if present
            val listenAgainSection = homeList.firstOrNull { isListenAgainSection(it.title) }
            if (listenAgainSection != null) {
                val contents = listenAgainSection.contents.filterNotNull().filter { it.thumbnails.isNotEmpty() }
                if (contents.isNotEmpty()) {
                    return@combine contents
                }
            }

            // Fallback when "Listen again" is not present (e.g., signed-out user / fresh session)
            val featured = mutableListOf<Content>()
            homeList.forEach { homeItem ->
                if (!isQuickPicksSection(homeItem.title) && !isListenAgainSection(homeItem.title)) {
                    val titleLower = homeItem.title.lowercase()
                    if (!titleLower.contains("podcast") &&
                        !titleLower.contains("episode") &&
                        !titleLower.contains("show")
                    ) {
                        homeItem.contents.filterNotNull().take(5).forEach { item ->
                            if (item.thumbnails.isNotEmpty() && featured.none { it.title == item.title || (it.browseId != null && it.browseId == item.browseId) }) {
                                featured.add(item)
                            }
                        }
                    }
                }
            }

            // Fallback to New Releases
            if (featured.isEmpty()) {
                newReleaseList.forEach { releaseHomeItem ->
                    releaseHomeItem.contents.filterNotNull().take(5).forEach { item ->
                        if (item.thumbnails.isNotEmpty() && featured.none { it.title == item.title || (it.browseId != null && it.browseId == item.browseId) }) {
                            featured.add(item)
                        }
                    }
                }
            }

            if (featured.isEmpty()) {
                newReleaseList.forEach { releaseHomeItem ->
                    featured.addAll(releaseHomeItem.contents.filterNotNull())
                }
            }

            featured.take(25)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    var regionCodeChart: MutableStateFlow<String?> = MutableStateFlow(null)

    val isOffline = MutableStateFlow<Boolean>(false)
    val isRetrying = MutableStateFlow<Boolean>(false)

    val loading = MutableStateFlow<Boolean>(true)
    val loadingChart = MutableStateFlow<Boolean>(true)
    private var regionCode: String = ""
    private var language: String = ""

    private val _songEntity: MutableStateFlow<SongEntity?> = MutableStateFlow(null)
    val songEntity: StateFlow<SongEntity?> = _songEntity

    private var _params: MutableStateFlow<String?> = MutableStateFlow(null)
    val params: StateFlow<String?> = _params

    // For showing alert that should log in to YouTube
    private val _showLogInAlert: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showLogInAlert: StateFlow<Boolean> = _showLogInAlert

    val dataSyncId =
        dataStoreManager
            .dataSyncId
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _mainHomeThumbnail: MutableStateFlow<String?> = MutableStateFlow(null)
    val mainHomeThumbnail: StateFlow<String?> = _mainHomeThumbnail

    init {
        homeJob = Job()
        viewModelScope.launch {
            if (dataStoreManager.cookie.first().isEmpty() &&
                dataStoreManager.shouldShowLogInRequiredAlert.first() == TRUE
            ) {
                _showLogInAlert.update { true }
            }
            regionCodeChart.value = dataStoreManager.chartKey.first()
            exploreChart(regionCodeChart.value ?: "ZZ")

            // Single unified trigger flow to prevent concurrent startup race conditions
            combine(
                dataStoreManager.location.distinctUntilChanged(),
                dataStoreManager.language.distinctUntilChanged(),
                dataStoreManager.cookie.distinctUntilChanged(),
                params,
            ) { loc, lang, cook, par ->
                QueryParams(
                    location = loc,
                    language = lang.ifEmpty {
                        dataStoreManager.getString(SELECTED_LANGUAGE).first()
                            ?: SUPPORTED_LANGUAGE.codes.first()
                    },
                    cookie = cook,
                    params = par,
                )
            }
                .distinctUntilChanged()
                .collectLatest { query ->
                    regionCode = query.location
                    language = query.language
                    if (query.cookie.isNotEmpty()) {
                        _accountInfo.emit(
                            Pair(
                                dataStoreManager.getString("AccountName").first(),
                                dataStoreManager.getString("AccountThumbUrl").first(),
                            ),
                        )
                    }
                    getHomeItemList(query.params)
                }
        }
        viewModelScope.launch {
            homeItemList.collectLatest { list ->
                _mainHomeThumbnail.value =
                    list
                        .firstOrNull()
                        ?.contents
                        ?.firstOrNull()
                        ?.thumbnails
                        ?.lastOrNull()
                        ?.url
            }
        }
    }

    fun doneShowLogInAlert(neverShowAgain: Boolean = false) {
        viewModelScope.launch {
            _showLogInAlert.update { false }
            if (neverShowAgain) {
                dataStoreManager.setShouldShowLogInRequiredAlert(false)
            }
        }
    }

    fun retryHome() {
        isRetrying.value = true
        getHomeItemList(params.value)
    }

    fun getHomeItemList(params: String? = null) {
        if (_homeItemList.value.isEmpty()) {
            loading.value = true
        }
        _homeListState.value = ListState.LOADING
        homeJob?.cancel()
        homeJob =
            viewModelScope.launch {
                // 1. Home Feed Flow - Updates immediately when cache or network data arrives
                launch {
                    try {
                        homeRepository
                            .getHomeData(
                                params,
                                getString(Res.string.view_count),
                                getString(Res.string.song),
                            ).collect { home ->
                                when (home) {
                                    is Resource.Success -> {
                                        val newContinuation = home.data?.first
                                        val raw = home.data?.second ?: listOf()
                                        val filtered = raw.filterNot { item ->
                                            val t = item.title.lowercase()
                                            t.contains("podcast") || t.contains("shows for you") || t.contains("episodes") || t.contains("show")
                                        }.distinct()
                                        if (filtered.isNotEmpty() || _homeItemList.value.isEmpty()) {
                                            _homeItemList.value = filtered
                                        }
                                        _continuation.value = newContinuation
                                        _homeListState.value = if (newContinuation.isNullOrEmpty() && filtered.isEmpty()) {
                                            ListState.PAGINATION_EXHAUST
                                        } else {
                                            ListState.IDLE
                                        }
                                        isOffline.value = false
                                        loading.value = false
                                        isRetrying.value = false
                                    }

                                    is Resource.Error -> {
                                        if (_homeItemList.value.isEmpty()) {
                                            _homeItemList.value = listOf()
                                            showSnackBarErrorState.emit(home.message ?: "Failed to load home")
                                        }
                                        isOffline.value = _homeItemList.value.isNotEmpty()
                                        loading.value = false
                                        isRetrying.value = false
                                        if (_homeListState.value == ListState.LOADING) {
                                            _homeListState.value = if (continuation.value.isNullOrEmpty()) ListState.PAGINATION_EXHAUST else ListState.IDLE
                                        }
                                    }

                                    else -> {}
                                }
                            }
                    } catch (e: Exception) {
                        Logger.e("HomeViewModel", "getHomeItemList homeData error: ${e.message}")
                        if (_homeItemList.value.isEmpty()) {
                            showSnackBarErrorState.emit(e.message ?: "Failed to load home")
                        }
                        loading.value = false
                        isRetrying.value = false
                    }
                }

                // 2. Mood & Moments Flow
                launch {
                    try {
                        homeRepository.getMoodAndMomentsData().collect { moodRes ->
                            if (moodRes is Resource.Success) {
                                _exploreMoodItem.value = moodRes.data
                            }
                        }
                    } catch (e: Exception) {
                        Logger.w("HomeViewModel", "getMoodAndMomentsData error: ${e.message}")
                    }
                }

                // 3. Chart Data Flow
                launch {
                    try {
                        homeRepository.getChartData(dataStoreManager.chartKey.first()).collect { chartRes ->
                            if (chartRes is Resource.Success) {
                                _chart.value = chartRes.data
                            }
                        }
                    } catch (e: Exception) {
                        Logger.w("HomeViewModel", "getChartData error: ${e.message}")
                    }
                }

                // 4. New Release Flow
                launch {
                    try {
                        homeRepository.getNewRelease(
                            getString(Res.string.new_release),
                            getString(Res.string.music_video),
                        ).collect { releaseRes ->
                            if (releaseRes is Resource.Success) {
                                val data = releaseRes.data ?: arrayListOf()
                                if (data.isNotEmpty() || _newRelease.value.isEmpty()) {
                                    _newRelease.value = data
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Logger.w("HomeViewModel", "getNewRelease error: ${e.message}")
                    }
                }
            }
    }

    fun getContinueHomeItem(continuation: String?) {
        viewModelScope.launch {
            if (continuation.isNullOrEmpty()) {
                _homeListState.value = ListState.PAGINATION_EXHAUST
                return@launch
            } else {
                log("Get more home item with continuation: $continuation")
                _homeListState.value = ListState.PAGINATING
                homeRepository
                    .getHomeDataContinue(
                        continuation,
                        getString(Res.string.view_count),
                        getString(Res.string.song),
                    ).collect { home ->
                        when (home) {
                            is Resource.Success -> {
                                _continuation.value = home.data?.first
                                val rawNewItems = home.data?.second ?: listOf()
                                val newItems = rawNewItems.filterNot { item ->
                                    val t = item.title.lowercase()
                                    t.contains("podcast") || t.contains("shows for you") || t.contains("episodes") || t.contains("show")
                                }.distinct()
                                _homeItemList.update { currentList ->
                                    val existing = currentList.toSet()
                                    currentList + newItems.filterNot { it in existing }
                                }
                                if (home.data?.first.isNullOrEmpty()) {
                                    _homeListState.value = ListState.PAGINATION_EXHAUST
                                } else {
                                    _homeListState.value = ListState.IDLE
                                }
                            }

                            is Resource.Error -> {
                                _continuation.value = null
                                Logger.w(tag, "getContinueHomeItem: ${home.message}")
                                showSnackBarErrorState.emit(home.message ?: "Unknown error")
                                _homeListState.value = ListState.PAGINATION_EXHAUST
                            }
                        }
                    }
            }
        }
    }

    fun exploreChart(region: String) {
        viewModelScope.launch {
            loadingChart.value = true
            homeRepository
                .getChartData(
                    region,
                ).collect { values ->
                    regionCodeChart.value = region
                    dataStoreManager.setChartKey(region)
                    when (values) {
                        is Resource.Success -> {
                            _chart.value = values.data
                        }

                        else -> {
                            _chart.value = null
                        }
                    }
                    loadingChart.value = false
                }
        }
    }

    fun setParams(params: String?) {
        _params.value = params
    }

    fun playAllQuickPicks(quickPicks: HomeItem) {
        val tracks: List<Track> = quickPicks.contents.filterNotNull().filter { !it.videoId.isNullOrEmpty() }.map { it.toTrack() }
        if (tracks.isNotEmpty()) {
            val first = tracks.first()
            setQueueData(
                QueueData.Data(
                    listTracks = ArrayList(tracks),
                    firstPlayedTrack = first,
                    playlistId = "RDAMVM${first.videoId}",
                    playlistName = "Quick Picks",
                    playlistType = PlaylistType.RADIO,
                    continuation = null,
                ),
            )
            loadMediaItem(first, Config.SONG_CLICK)
        }
    }

    fun playQuickPickTrack(quickPicks: HomeItem, item: Content) {
        val tracks: List<Track> = quickPicks.contents.filterNotNull().filter { !it.videoId.isNullOrEmpty() }.map { it.toTrack() }
        val track = item.toTrack()
        val index = tracks.indexOfFirst { it.videoId == track.videoId }.takeIf { it >= 0 } ?: 0
        val vid = track.videoId
        setQueueData(
            QueueData.Data(
                listTracks = ArrayList(tracks),
                firstPlayedTrack = track,
                playlistId = "RDAMVM$vid",
                playlistName = "Quick Picks",
                playlistType = PlaylistType.RADIO,
                continuation = null,
            ),
        )
        loadMediaItem(track, Config.SONG_CLICK, index)
    }

    override fun onCleared() {
        super.onCleared()
        homeJob?.cancel()
    }

    companion object {
        // Home params
        const val HOME_PARAMS_RELAX = "ggM8SgQIBxADSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_SLEEP = "ggM8SgQIBxABSgQIBRADSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_ENERGIZE = "ggM8SgQIBxABSgQIBRABSgQICRADSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_SAD = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChADSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_ROMANCE = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRADSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_FEEL_GOOD = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBADSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_WORKOUT = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBADSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_PARTY = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhADSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_COMMUTE = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxADSgQIBhAB"
        const val HOME_PARAMS_FOCUS = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAD"
    }
}

private data class QueryParams(
    val location: String,
    val language: String,
    val cookie: String,
    val params: String?,
)