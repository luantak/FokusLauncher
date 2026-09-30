package com.lu4p.fokuslauncher.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.components.FokusAlertDialog
import com.lu4p.fokuslauncher.ui.components.FokusTextButton
import com.lu4p.fokuslauncher.ui.settings.components.SettingsRow

@Composable
fun ConfigurationBackupRow(viewModel: ConfigurationBackupViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var showActions by rememberSaveable { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        if (it != null) viewModel.exportTo(it)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) viewModel.prepareImport(it)
    }
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }
    SettingsRow(
        label = stringResource(R.string.configuration_backup_title),
        subtitle = stringResource(if (busy) R.string.configuration_backup_busy else R.string.configuration_backup_subtitle),
        verticalPadding = 14.dp,
        onClick = { if (!busy) showActions = true },
    )
    if (showActions) {
        FokusAlertDialog(
            onDismissRequest = { showActions = false },
            title = { Text(stringResource(R.string.configuration_backup_title)) },
            text = { Text(stringResource(R.string.configuration_backup_details)) },
            confirmButton = {
                Column {
                    FokusTextButton(onClick = {
                        showActions = false
                        export.launch("fokus-launcher-configuration.json")
                    }) { Text(stringResource(R.string.configuration_export)) }
                    FokusTextButton(onClick = {
                        showActions = false
                        importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                    }) { Text(stringResource(R.string.configuration_import)) }
                }
            },
            dismissButton = {
                FokusTextButton(onClick = { showActions = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
    if (pending != null) {
        FokusAlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text(stringResource(R.string.configuration_import)) },
            text = { Text(stringResource(R.string.configuration_import_confirm)) },
            confirmButton = {
                FokusTextButton(onClick = viewModel::confirmImport) { Text(stringResource(R.string.configuration_import)) }
            },
            dismissButton = {
                FokusTextButton(onClick = viewModel::cancelImport) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
