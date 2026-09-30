package com.lu4p.fokuslauncher.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.backup.ConfigurationBackup
import com.lu4p.fokuslauncher.data.repository.AppRepository
import com.lu4p.fokuslauncher.data.util.AppLocaleHelper
import com.lu4p.fokuslauncher.utils.WallpaperHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ConfigurationBackupViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val backup: ConfigurationBackup,
    private val appRepository: AppRepository,
) : ViewModel() {
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _pending = MutableStateFlow<ConfigurationBackup.Snapshot?>(null)
    val pending = _pending.asStateFlow()
    private val _message = MutableStateFlow<Int?>(null)
    val message = _message.asStateFlow()

    fun clearMessage() { _message.value = null }
    fun cancelImport() { _pending.value = null }

    fun exportTo(uri: Uri) = operation(R.string.configuration_export_success) {
        val text = backup.export()
        context.contentResolver.openOutputStream(uri, "wt")?.use {
            it.write(text.toByteArray(Charsets.UTF_8))
        } ?: error("Cannot open destination")
    }

    fun prepareImport(uri: Uri) = operation(null) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= ConfigurationBackup.MAX_BYTES)
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: error("Cannot open backup")
        require(bytes.size <= ConfigurationBackup.MAX_BYTES)
        _pending.value = backup.parse(bytes.toString(Charsets.UTF_8))
    }

    fun confirmImport() {
        val snapshot = _pending.value ?: return
        _pending.value = null
        operation(R.string.configuration_import_success) {
            backup.restore(snapshot)
            appRepository.invalidateCache()
            withContext(Dispatchers.Main) { AppLocaleHelper.applyLocaleTag(snapshot.localeTag) }
            if (!snapshot.usesPhotoWallpaper) WallpaperHelper.setBlackWallpaper(context)
        }
    }

    private fun operation(success: Int?, action: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                action()
                _message.value = success
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _pending.value = null
                _message.value = R.string.configuration_backup_failed
            } finally {
                _busy.value = false
            }
        }
    }
}
