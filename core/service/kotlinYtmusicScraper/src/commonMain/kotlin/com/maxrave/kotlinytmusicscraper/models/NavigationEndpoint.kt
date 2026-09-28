package com.maxrave.kotlinytmusicscraper.models

import kotlinx.serialization.Serializable

@Serializable
data class NavigationEndpoint(
    val watchEndpoint: WatchEndpoint? = null,
    val watchPlaylistEndpoint: WatchEndpoint? = null,
    val browseEndpoint: BrowseEndpoint? = null,
    val searchEndpoint: SearchEndpoint? = null,
    val queueAddEndpoint: QueueAddEndpoint? = null,
    val shareEntityEndpoint: ShareEntityEndpoint? = null,
    val playlistEditEndpoint: PlaylistEditEndpoint? = null,
    val feedbackEndpoint: FeedbackEndpoint? = null,
    val commandExecutorCommand: CommandExecutorCommand? = null,
) {
    val endpoint: Endpoint?
        get() =
            watchEndpoint
                ?: watchPlaylistEndpoint
                ?: browseEndpoint
                ?: searchEndpoint
                ?: queueAddEndpoint
                ?: shareEntityEndpoint
                ?: feedbackEndpoint

    /** Resolve a YT Music feedback token, including nested commandExecutorCommand wrappers. */
    fun resolveFeedbackToken(): String? {
        feedbackEndpoint?.feedbackToken?.let { return it }
        commandExecutorCommand?.commands?.forEach { cmd ->
            cmd.feedbackEndpoint?.feedbackToken?.let { return it }
        }
        return null
    }
}

@Serializable
data class CommandExecutorCommand(
    val commands: List<NavigationEndpoint>? = null,
)
