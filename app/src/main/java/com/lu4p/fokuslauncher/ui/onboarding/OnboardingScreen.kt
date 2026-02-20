package com.lu4p.fokuslauncher.ui.onboarding

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.ShortcutTarget

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit = {},
    onNavigateToHomeWithEditOverlay: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val isLastStep by viewModel.isLastStep.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.recheckDefaultLauncher()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        val locationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* Granted or denied, we proceed to next step */ viewModel.onNext() }

        when (currentStep) {
            OnboardingStep.WELCOME -> WelcomeStep(
                onGetStarted = { viewModel.onNext() }
            )
            OnboardingStep.LOCATION -> LocationStep(
                onAllow = {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                },
                onSkip = { viewModel.onSkipLocation() }
            )
            OnboardingStep.SET_DEFAULT_LAUNCHER -> SetDefaultStep(
                onSetDefault = { viewModel.openDefaultLauncherSettings() },
                onNext = { viewModel.onNext() }
            )
            OnboardingStep.CUSTOMIZE_HOME -> CustomizeStep(
                onChooseApps = { viewModel.onChooseApps(onNavigateToHomeWithEditOverlay) },
                onSkip = { viewModel.onSkip() }
            )
            OnboardingStep.SWIPE_SHORTCUTS -> {
                val swipeState by viewModel.swipeShortcutsState.collectAsStateWithLifecycle()
                SwipeShortcutsStep(
                    swipeState = swipeState,
                    onSetSwipeLeft = { viewModel.setSwipeLeftTarget(it) },
                    onSetSwipeRight = { viewModel.setSwipeRightTarget(it) },
                    onSkip = { viewModel.onSkip() }
                )
            }
            OnboardingStep.QUICK_TIPS -> QuickTipsStep(
                onDone = { viewModel.onDone(onNavigateToHome) }
            )
            null -> { /* Loading / empty state */ }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (currentStep != OnboardingStep.CUSTOMIZE_HOME && currentStep != OnboardingStep.LOCATION && currentStep != OnboardingStep.SWIPE_SHORTCUTS && !isLastStep) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { viewModel.onNext() }) {
                    Text(
                        text = stringResource(R.string.onboarding_next),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        FilledTonalButton(
            onClick = onGetStarted,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(text = stringResource(R.string.onboarding_get_started))
        }
    }
}

@Composable
private fun LocationStep(
    onAllow: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp).padding(bottom = 16.dp)
        )
        Text(
            text = stringResource(R.string.onboarding_location_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_location_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        FilledTonalButton(
            onClick = onAllow,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(text = stringResource(R.string.onboarding_location_allow))
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onSkip) {
            Text(
                text = stringResource(R.string.onboarding_location_skip),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SetDefaultStep(
    onSetDefault: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_set_default_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_set_default_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        FilledTonalButton(
            onClick = onSetDefault,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(text = stringResource(R.string.onboarding_set_default_button))
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNext) {
            Text(
                text = stringResource(R.string.onboarding_next),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CustomizeStep(
    onChooseApps: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_customize_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_customize_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        FilledTonalButton(
            onClick = onChooseApps,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(text = stringResource(R.string.onboarding_choose_apps))
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onSkip) {
            Text(
                text = stringResource(R.string.onboarding_skip),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SwipeShortcutsStep(
    swipeState: SwipeShortcutsState,
    onSetSwipeLeft: (ShortcutTarget?) -> Unit,
    onSetSwipeRight: (ShortcutTarget?) -> Unit,
    onSkip: () -> Unit
) {
    var showAppPickerFor by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_swipe_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_swipe_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 320.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.onboarding_swipe_left),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = formatShortcutTarget(swipeState.swipeLeftTarget, swipeState.allApps, stringResource(R.string.onboarding_swipe_not_set)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            TextButton(onClick = { showAppPickerFor = "swipeLeft" }) {
                Text(stringResource(R.string.onboarding_swipe_change))
            }
            IconButton(onClick = { onSetSwipeLeft(null) }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 320.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.onboarding_swipe_right),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = formatShortcutTarget(swipeState.swipeRightTarget, swipeState.allApps, stringResource(R.string.onboarding_swipe_not_set)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            TextButton(onClick = { showAppPickerFor = "swipeRight" }) {
                Text(stringResource(R.string.onboarding_swipe_change))
            }
            IconButton(onClick = { onSetSwipeRight(null) }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onSkip) {
            Text(
                text = stringResource(R.string.onboarding_next),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }

    val pickerTarget = showAppPickerFor
    if (pickerTarget != null) {
        OnboardingAppPickerDialog(
            allApps = swipeState.allApps,
            onSelect = { packageName ->
                when (pickerTarget) {
                    "swipeLeft" -> onSetSwipeLeft(ShortcutTarget.App(packageName))
                    "swipeRight" -> onSetSwipeRight(ShortcutTarget.App(packageName))
                }
                showAppPickerFor = null
            },
            onDismiss = { showAppPickerFor = null }
        )
    }
}

@Composable
private fun OnboardingAppPickerDialog(
    allApps: List<AppInfo>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var filter by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.onboarding_swipe_pick_app), color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column {
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                val filtered = if (filter.isBlank()) allApps
                else allApps.filter { it.label.contains(filter, ignoreCase = true) }
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(filtered) { app ->
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(app.packageName) }
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.onboarding_swipe_cancel)) } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

private fun formatShortcutTarget(
    target: ShortcutTarget?,
    allApps: List<AppInfo>,
    notSetLabel: String
): String {
    return when (target) {
        null -> notSetLabel
        is ShortcutTarget.App -> allApps.find { it.packageName == target.packageName }?.label ?: target.packageName
        is ShortcutTarget.DeepLink -> "Deep link"
        is ShortcutTarget.LauncherShortcut -> {
            val appLabel = allApps.find { it.packageName == target.packageName }?.label ?: target.packageName
            "$appLabel - Shortcut"
        }
    }
}

@Composable
private fun QuickTipsStep(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_tips_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))
        TipRow(
            icon = Icons.Default.ArrowUpward,
            text = stringResource(R.string.onboarding_tip_swipe)
        )
        Spacer(modifier = Modifier.height(16.dp))
        TipRow(
            icon = Icons.Default.TouchApp,
            text = stringResource(R.string.onboarding_tip_longpress)
        )
        Spacer(modifier = Modifier.height(32.dp))
        FilledTonalButton(
            onClick = onDone,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(text = stringResource(R.string.onboarding_done))
        }
    }
}

@Composable
private fun TipRow(
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 320.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
