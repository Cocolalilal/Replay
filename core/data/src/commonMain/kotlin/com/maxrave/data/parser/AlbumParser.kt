package com.maxrave.data.parser

import com.maxrave.domain.data.model.browse.album.AlbumBrowse
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.browse.artist.ResultAlbum
import com.maxrave.domain.data.model.searchResult.songs.Album
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.data.mapping.toDomainFeedbackTokens
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.kotlinytmusicscraper.pages.AlbumPage
import com.maxrave.logger.Logger

internal fun parseAlbumData(data: AlbumPage): AlbumBrowse {
    val artist: ArrayList<Artist> = arrayListOf()
    Logger.w("AlbumParser", "Parsing album data \n$data")
    data.album.artists?.forEach {
        artist.add(Artist(it.id, it.name))
    }
    val songs: ArrayList<Track> = arrayListOf()
    data.songs.forEach { songItem ->
        songs.add(
            Track(
                album =
                    Album(
                        id = data.album.id,
                        name = data.album.title,
                    ),
                artists =
                    songItem.artists.map { artistItem ->
                        Artist(
                            id = artistItem.id,
                            name = artistItem.name,
                        )
                    },
                duration =
                    if (songItem.duration != null) {
                        "%02d:%02d".format(
                            (songItem.duration ?: 0) / 60,
                            (songItem.duration ?: 0) % 60,
                        )
                    } else {
                        ""
                    },
                durationSeconds = songItem.duration ?: 0,
                isAvailable = false,
                isExplicit = songItem.explicit,
                likeStatus = "INDIFFERENT",
                thumbnails = songItem.thumbnails?.thumbnails?.toListThumbnail() ?: listOf(),
                title = songItem.title,
                videoId = songItem.id,
                // Real YTM marker, not a hardcoded label — album rows are songs.
                videoType =
                    songItem.endpoint
                        ?.watchEndpointMusicSupportedConfigs
                        ?.watchEndpointMusicConfig
                        ?.musicVideoType,
                category = null,
                feedbackTokens = songItem.feedbackTokens?.toDomainFeedbackTokens(),
                resultType = null,
                year = data.album.year.toString(),
            ),
        )
    }

    return AlbumBrowse(
        artists = artist,
        audioPlaylistId = data.album.playlistId,
        description = data.description ?: "",
        duration = data.duration ?: "",
        durationSeconds = 0,
        thumbnails = data.thumbnails?.thumbnails?.toListThumbnail() ?: listOf(),
        title = data.album.title,
        trackCount = songs.size,
        tracks = songs,
        type = "Album",
        year = data.album.year.toString(),
        otherVersion =
            data.otherVersion.map {
                ResultAlbum(
                    browseId = it.browseId,
                    isExplicit = it.explicit,
                    thumbnails = listOf(Thumbnail(800, it.thumbnail, 800)),
                    title = it.title,
                    year = data.album.year.toString(),
                )
            },
    )
}