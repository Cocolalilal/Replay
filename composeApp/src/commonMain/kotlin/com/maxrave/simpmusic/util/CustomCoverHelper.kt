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
        val safeId = sanitizeId(playlistId)
        val file = File(dir, "cover_$safeId.jpg")
        if (file.exists()) {
            file.delete()
        }
        val cleanId = playlistId.removePrefix("VL")
        val cleanFile = File(dir, "cover_${sanitizeId(cleanId)}.jpg")
        if (cleanFile.exists()) {
            cleanFile.delete()
        }
        val localFile = File(dir, "cover_local_$safeId.jpg")
        if (localFile.exists()) {
            localFile.delete()
        }
        dataStoreManager.setCustomPlaylistCover(playlistId, null)
        if (playlistId != cleanId) {
            dataStoreManager.setCustomPlaylistCover(cleanId, null)
        }
        dataStoreManager.setCustomPlaylistCover("local_$playlistId", null)
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

        // 2. Playlist ID checks
        if (!playlistId.isNullOrEmpty()) {
            val safeId = sanitizeId(playlistId)
            val byId = File(coversDir, "cover_$safeId.jpg")
            if (byId.exists() && byId.isFile && byId.length() > 0) return byId

            val cleanId = playlistId.removePrefix("VL")
            val byCleanId = File(coversDir, "cover_${sanitizeId(cleanId)}.jpg")
            if (byCleanId.exists() && byCleanId.isFile && byCleanId.length() > 0) return byCleanId

            val byLocalId = File(coversDir, "cover_local_$safeId.jpg")
            if (byLocalId.exists() && byLocalId.isFile && byLocalId.length() > 0) return byLocalId
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
        val cleanId = playlistId.removePrefix("VL")
        val savedPath = customCoversMap[playlistId]
            ?: customCoversMap[cleanId]
            ?: customCoversMap["VL$cleanId"]
            ?: customCoversMap["local_$playlistId"]
            ?: customCoversMap["local_$cleanId"]
        return getCustomCoverFile(playlistId, savedPath) != null
    }
}

/**
 * Resolves custom playlist cover from customCoversMap if present, matching either exact playlistId,
 * stripped "VL" prefix, added "VL" prefix, or local prefix, and validating file existence.
 */
fun resolvePlaylistCover(
    playlistId: String?,
    defaultThumbnail: String?,
    customCoversMap: Map<String, String>,
): String? {
    if (playlistId.isNullOrEmpty()) return defaultThumbnail
    val cleanId = playlistId.removePrefix("VL")
    val savedPath = customCoversMap[playlistId]
        ?: customCoversMap[cleanId]
        ?: customCoversMap["VL$cleanId"]
        ?: customCoversMap["local_$playlistId"]
        ?: customCoversMap["local_$cleanId"]

    val customFile = CustomCoverHelper.getCustomCoverFile(playlistId, savedPath)
    if (customFile != null) {
        return customFile.absolutePath
    }

    if (!savedPath.isNullOrEmpty() && (savedPath.startsWith("http://") || savedPath.startsWith("https://"))) {
        return savedPath
    }

    return defaultThumbnail
}
