package com.maxrave.simpmusic.viewModel

import com.eygraber.uri.Uri
import com.maxrave.common.CUSTOM_COVERS_FOLDER
import com.maxrave.common.DB_NAME
import com.maxrave.common.SETTINGS_FILENAME
import com.maxrave.data.io.getHomeFolderPath
import com.maxrave.domain.repository.CacheRepository
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.extension.zipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.getString
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.restore_success
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.system.exitProcess

actual suspend fun calculateDataFraction(cacheRepository: CacheRepository): SettingsStorageSectionFraction? = null

actual suspend fun restoreNative(
    commonRepository: CommonRepository,
    uri: Uri,
    getData: () -> Unit,
) {
    ZipInputStream(
        FileInputStream(File(uri.toString())),
    ).use { inputStream ->
        var entry =
            try {
                inputStream.nextEntry
            } catch (e: Exception) {
                null
            }
        var customCoversFolderCleared = false
        while (entry != null) {
            Logger.d("BackupRestore", "Processing entry: ${entry.name}")
            when {
                entry.name == "$SETTINGS_FILENAME.preferences_pb" -> {
                    File(getHomeFolderPath(listOf(".simpmusic")), "$SETTINGS_FILENAME.preferences_pb")
                        .outputStream()
                        .use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                }

                entry.name == DB_NAME -> {
                    runBlocking(Dispatchers.IO) {
                        commonRepository.databaseDaoCheckpoint()
                        commonRepository.closeDatabase()
                    }
                    FileOutputStream(commonRepository.getDatabasePath()).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                entry.name.startsWith("$CUSTOM_COVERS_FOLDER/") -> {
                    Logger.d("BackupRestore", "Found custom covers entry: ${entry.name}")
                    if (!customCoversFolderCleared) {
                        val coversFolder = File(getHomeFolderPath(listOf(".simpmusic", CUSTOM_COVERS_FOLDER)))
                        clearFolderJvm(coversFolder)
                        customCoversFolderCleared = true
                    }
                    restoreFolderJvm(entry.name, inputStream, CUSTOM_COVERS_FOLDER)
                }
            }
            entry = inputStream.nextEntry
        }
        withContext(Dispatchers.Main) {
            showToast(getString(Res.string.restore_success), ToastGravity.Bottom)
            showToast("App will restart to apply changes", ToastGravity.Bottom)
            delay(2000)
            exitProcess(0)
        }
    }
}

actual suspend fun backupNative(
    commonRepository: CommonRepository,
    uri: Uri,
    backupDownloaded: Boolean,
) {
    FileOutputStream(File(uri.toString())).use {
        it.buffered().zipOutputStream().use { outputStream ->
            File(getHomeFolderPath(listOf(".simpmusic")), "$SETTINGS_FILENAME.preferences_pb")
                .inputStream()
                .buffered()
                .use { inputStream ->
                    outputStream.putNextEntry(ZipEntry("$SETTINGS_FILENAME.preferences_pb"))
                    inputStream.copyTo(outputStream)
                }
            runBlocking(Dispatchers.IO) {
                commonRepository.databaseDaoCheckpoint()
            }
            FileInputStream(commonRepository.getDatabasePath()).use { inputStream ->
                outputStream.putNextEntry(ZipEntry(DB_NAME))
                inputStream.copyTo(outputStream)
            }
            val customCoversFolder = File(getHomeFolderPath(listOf(".simpmusic", CUSTOM_COVERS_FOLDER)))
            if (customCoversFolder.exists() && customCoversFolder.isDirectory) {
                backupFolderJvm(customCoversFolder, CUSTOM_COVERS_FOLDER, outputStream)
            }
        }
    }
}

private fun backupFolderJvm(
    folder: File,
    baseName: String,
    zipOutputStream: ZipOutputStream,
) {
    if (!folder.exists() || !folder.isDirectory) return

    Logger.d("BackupRestore", "Backing up folder: ${folder.absolutePath} as $baseName")
    folder.listFiles()?.forEach { file ->
        if (file.isFile) {
            val entryName = "$baseName/${file.name}"
            Logger.d("BackupRestore", "Backing up file: $entryName")
            zipOutputStream.putNextEntry(ZipEntry(entryName))
            file.inputStream().buffered().use { inputStream ->
                inputStream.copyTo(zipOutputStream)
            }
            zipOutputStream.closeEntry()
        } else if (file.isDirectory) {
            backupFolderJvm(file, "$baseName/${file.name}", zipOutputStream)
        }
    }
}

private fun clearFolderJvm(folder: File) {
    if (folder.exists() && folder.isDirectory) {
        Logger.d("BackupRestore", "Clearing folder: ${folder.absolutePath}")
        folder.listFiles()?.forEach { file ->
            if (file.isFile) {
                file.delete()
            } else if (file.isDirectory) {
                clearFolderJvm(file)
                file.delete()
            }
        }
    }
}

private fun restoreFolderJvm(
    entryName: String,
    zipInputStream: ZipInputStream,
    baseFolderName: String,
) {
    val homeFolder = File(getHomeFolderPath(listOf(".simpmusic")))
    val relativePath = entryName.removePrefix("$baseFolderName/")
    val targetFile = File(File(homeFolder, baseFolderName), relativePath)

    targetFile.parentFile?.mkdirs()
    targetFile.outputStream().use { outputStream ->
        zipInputStream.copyTo(outputStream)
    }
    Logger.d("BackupRestore", "Restored file to: ${targetFile.absolutePath}")
}

actual fun getPackageName(): String = ""

actual fun getFileDir(): String = File(getHomeFolderPath(listOf(".simpmusic"))).absolutePath

actual fun changeLanguageNative(code: String) {
    Locale.setDefault(
        Locale.forLanguageTag(
            if (code == "id-ID") {
                "in-ID"
            } else {
                code
            },
        ),
    )
}