package com.lu4p.fokuslauncher.data.font

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import com.lu4p.fokuslauncher.data.model.LauncherFontPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class CustomFontImportResult {
    data class Success(val storageValue: String, val displayLabel: String) : CustomFontImportResult()
    data class Failure(val reason: CustomFontImportFailure) : CustomFontImportResult()
}

enum class CustomFontImportFailure {
    UNREADABLE_URI,
    INVALID_EXTENSION,
    INVALID_FONT,
    IO_ERROR,
}

/**
 * Copies user-selected `.ttf` files into app-private storage and resolves them for Compose
 * [androidx.compose.ui.text.font.FontFamily].
 */
@Singleton
class CustomFontStore
@Inject
constructor(@param:ApplicationContext private val context: Context) {
    private val fontsDir: File
        get() = File(context.filesDir, FONTS_DIR_NAME).apply { mkdirs() }

    fun hasStoredFont(): Boolean = storedFontFile().isFile

    /** Family name from the stored `.ttf` metadata, if a font file is present. */
    fun readStoredFontDisplayLabel(): String? {
        val file = storedFontFile()
        if (!file.isFile) return null
        return TtfFontMetadataReader.readFamilyDisplayName(file)
    }

    fun resolveFile(storageValue: String): File? {
        val relative = LauncherFontPreferences.customFontRelativePath(storageValue) ?: return null
        val file = File(fontsDir, relative)
        return file.takeIf { it.isFile }
    }

    suspend fun importFromUri(uri: Uri): CustomFontImportResult =
            withContext(Dispatchers.IO) {
                val displayName =
                        context.contentResolver.queryDisplayName(uri)
                                ?: return@withContext CustomFontImportResult.Failure(
                                        CustomFontImportFailure.UNREADABLE_URI
                                )
                if (!isTtfFileName(displayName)) {
                    return@withContext CustomFontImportResult.Failure(
                            CustomFontImportFailure.INVALID_EXTENSION
                    )
                }
                val target = storedFontFile()
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                            ?: return@withContext CustomFontImportResult.Failure(
                                    CustomFontImportFailure.UNREADABLE_URI
                            )
                } catch (_: IOException) {
                    target.delete()
                    return@withContext CustomFontImportResult.Failure(CustomFontImportFailure.IO_ERROR)
                }
                if (!isValidTtfFile(target)) {
                    target.delete()
                    return@withContext CustomFontImportResult.Failure(
                            CustomFontImportFailure.INVALID_FONT
                    )
                }
                val displayLabel =
                        TtfFontMetadataReader.readFamilyDisplayName(target)
                                ?: LauncherFontPreferences.displayLabelFromTtfFileName(
                                        displayName
                                )
                CustomFontImportResult.Success(
                        storageValue = LauncherFontPreferences.CUSTOM_FONT_STORAGE,
                        displayLabel = displayLabel,
                )
            }

    fun deleteStoredFont() {
        fontsDir.listFiles()?.forEach { it.delete() }
    }

    private fun storedFontFile(): File =
            File(fontsDir, LauncherFontPreferences.CUSTOM_FONT_ACTIVE_FILE)

    private fun isValidTtfFile(file: File): Boolean =
            runCatching { Typeface.createFromFile(file) }.getOrNull() != null

    private fun android.content.ContentResolver.queryDisplayName(uri: Uri): String? {
        query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst()) {
                        return cursor.getString(index)?.trim()?.takeIf { it.isNotEmpty() }
                    }
                }
        return null
    }

    companion object {
        const val FONTS_DIR_NAME = "fonts"

        internal fun isTtfFileName(name: String): Boolean =
                name.trim().endsWith(".ttf", ignoreCase = true)
    }
}
