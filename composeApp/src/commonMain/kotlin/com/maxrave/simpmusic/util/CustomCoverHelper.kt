package com.maxrave.simpmusic.util

import com.maxrave.common.CUSTOM_COVERS_FOLDER
import com.maxrave.data.io.fileDir
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import java.io.File

object CustomCoverHelper {
    private const val TAG = "CustomCoverHelper"

    /**
     * Every ID the Liked Songs playlist is known by. All of them are the same canonical playlist,
     * so a custom cover set under one alias must resolve under any other — this is what keeps a
     * cover picked on the old local "Liked Songs" playlist alive now that everything routes to `LM`.
     */
    private val LIKED_SONGS_ALIASES =
        setOf(
            "LM",
            "VLLM",
            "FEmusic_liked_videos",
            "favorite_songs",
            "liked_songs",
        )

    /**
     * All cover-map / filename candidate keys for [playlistId], including the `VL`-stripped and
     * `local_`-prefixed variants and (for the Liked Songs playlist) every alias it is known by.
     */
    internal fun coverCandidateKeys(playlistId: String): List<String> {
        val clean = playlistId.removePrefix("VL")
        val keys = linkedSetOf(playlistId, clean, "VL$clean", "local_$playlistId", "local_$clean")
        if (clean.lowercase() in LIKED_SONGS_ALIASES.map { it.lowercase() }) {
            LIKED_SONGS_ALIASES.forEach { keys.add(it) }
        }
        return keys.toList()
    }

    /**
     * Sanitizes a playlist ID or key so it can be safely used as a filename on all filesystems.
     */
    fun sanitizeId(id: String): String {
        return id.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    /**
     * Returns the dedicated directory where custom playlist covers are stored.
     * Guaranteed to exist.
     */
    fun getCustomCoversDirectory(): File {
        val dir = File(fileDir(), CUSTOM_COVERS_FOLDER)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Saves the custom cover image bytes into persistent internal app storage and records it in DataStore.
     * Returns the absolute path of the saved image file.
     */
    suspend fun saveCustomCover(
        playlistId: String,
        imageBytes: ByteArray,
        dataStoreManager: DataStoreManager,
    ): String = withContext(Dispatchers.IO) {
        val dir = getCustomCoversDirectory()
        val safeId = sanitizeId(playlistId)
        val targetFile = File(dir, "cover_$safeId.jpg")
        targetFile.outputStream().use { it.write(imageBytes) }
        val absolutePath = targetFile.absolutePath
        dataStoreManager.setCustomPlaylistCover(playlistId, absolutePath)
        Logger.d(TAG, "Saved custom cover for $playlistId at $absolutePath (${imageBytes.size} bytes)")
        absolutePath
    }

    /**
     * Removes the custom cover for a playlist from disk and DataStore.
     */
    suspend fun removeCustomCover(
        playlistId: String,
        dataStoreManager: DataStoreManager,
    ) = withContext(Dispatchers.IO) {
        val dir = getCustomCoversDirectory()
        // Remove every candidate file and map entry, including Liked Songs aliases, so no stale
        // cover survives under a different key after removal.
        for (candidate in coverCandidateKeys(playlistId)) {
            val safeId = sanitizeId(candidate)
            val file = File(dir, "cover_$safeId.jpg")
            if (file.exists()) {
                file.delete()
            }
            val localFile = File(dir, "cover_local_$safeId.jpg")
            if (localFile.exists()) {
                localFile.delete()
            }
            dataStoreManager.setCustomPlaylistCover(candidate, null)
        }
        Logger.d(TAG, "Removed custom cover for $playlistId")
    }

    /**
     * Checks if a custom cover file exists on disk for the given playlist ID or saved path.
     */
    fun getCustomCoverFile(
        playlistId: String?,
        savedPath: String? = null,
    ): File? {
        if (playlistId.isNullOrEmpty() && savedPath.isNullOrEmpty()) return null
        val coversDir = getCustomCoversDirectory()

        // 1. Direct savedPath check
        if (!savedPath.isNullOrEmpty()) {
            val file = File(savedPath)
            if (file.exists() && file.isFile && file.length() > 0) return file

            // Check filename in coversDir in case base path changed across devices / restore
            val byName = File(coversDir, file.name)
            if (byName.exists() && byName.isFile && byName.length() > 0) return byName
        }

        // 2. Playlist ID checks — every alias and prefix variant, so a restored cover whose key
        //    changed (e.g. set as "favorite_songs", looked up as "LM") still resolves.
        if (!playlistId.isNullOrEmpty()) {
            for (candidate in coverCandidateKeys(playlistId)) {
                val safeId = sanitizeId(candidate)
                val byId = File(coversDir, "cover_$safeId.jpg")
                if (byId.exists() && byId.isFile && byId.length() > 0) return byId

                val byLocalId = File(coversDir, "cover_local_$safeId.jpg")
                if (byLocalId.exists() && byLocalId.isFile && byLocalId.length() > 0) return byLocalId
            }
        }

        return null
    }

    /**
     * Returns true if a custom cover exists for the given playlist.
     */
    fun hasCustomCover(
        playlistId: String?,
        customCoversMap: Map<String, String>,
    ): Boolean {
        if (playlistId.isNullOrEmpty()) return false
        val savedPath = coverMapPathFor(playlistId, customCoversMap)
        return getCustomCoverFile(playlistId, savedPath) != null
    }
}

/**
 * Resolves the stored cover path for [playlistId] from [customCoversMap], trying the exact ID,
 * its `VL`-stripped / `local_`-prefixed variants and every Liked Songs alias.
 */
internal fun coverMapPathFor(
    playlistId: String,
    customCoversMap: Map<String, String>,
): String? {
    for (key in CustomCoverHelper.coverCandidateKeys(playlistId)) {
        customCoversMap[key]?.let { return it }
    }
    return null
}

/**
 * Resolves custom playlist cover from customCoversMap if present, matching either exact playlistId,
 * stripped "VL" prefix, added "VL" prefix, local prefix, or any Liked Songs alias, and validating
 * file existence.
 */
fun resolvePlaylistCover(
    playlistId: String?,
    defaultThumbnail: String?,
    customCoversMap: Map<String, String>,
): String? {
    if (playlistId.isNullOrEmpty()) return defaultThumbnail
    val savedPath = coverMapPathFor(playlistId, customCoversMap)

    val customFile = CustomCoverHelper.getCustomCoverFile(playlistId, savedPath)
    if (customFile != null) {
        return customFile.absolutePath
    }

    if (!savedPath.isNullOrEmpty() && (savedPath.startsWith("http://") || savedPath.startsWith("https://"))) {
        return savedPath
    }

    return defaultThumbnail
}

/**
 * Returns true if the playlist ID or title identifies it as the Liked Songs playlist.
 */
fun isLikedSongsPlaylist(playlistId: String?, title: String? = null): Boolean {
    if (!playlistId.isNullOrEmpty()) {
        val clean = playlistId.removePrefix("VL").lowercase()
        if (clean == "lm" ||
            clean == "vllm" ||
            clean == "femusic_liked_videos" ||
            clean == "favorite_songs" ||
            clean == "liked_songs"
        ) {
            return true
        }
    }
    if (!title.isNullOrEmpty()) {
        val lower = title.lowercase().trim()
        if (lower == "liked songs" ||
            lower == "liked music" ||
            lower == "liked videos" ||
            lower == "your likes" ||
            lower == "favorite" ||
            lower == "favorites"
        ) {
            return true
        }
    }
    return false
}

