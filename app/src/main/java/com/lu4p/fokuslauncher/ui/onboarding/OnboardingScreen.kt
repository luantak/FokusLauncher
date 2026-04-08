package com.lu4p.fokuslauncher.ui.onboarding

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.lu4p.fokuslauncher.ui.components.FokusIconButton
import com.lu4p.fokuslauncher.ui.components.FokusTextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.util.OnResumeEffect
import com.lu4p.fokuslauncher.ui.util.formatShortcutTargetDisplay
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.ui.settings.ShortcutActionPickerDialog
import com.lu4p.fokuslauncher.ui.home.HomeViewModel
import com.lu4p.fokuslauncher.ui.settings.EditHomeAppsScreen

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val isLastStep by viewModel.isLastStep.collectAsStateWithLifecycle()
    val showEditHomeApps by viewModel.showEditHomeApps.collectAsStateWithLifecycle()

    OnResumeEffect(lifecycleOwner) { viewModel.recheckDefaultLauncher() }

    // Create HomeViewModel for the edit screen when needed
    val homeViewModel: HomeViewModel? = if (showEditHomeApps) hiltViewModel() else null

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(32.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            val locationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ ->
                viewModel.onNext()
            }

            when (currentStep) {
                OnboardingStep.WELCOME -> WelcomeStep(
                    onGetStarted = { viewModel.onNext() }
                )
                OnboardingStep.BACKGROUND -> BackgroundStep(
                    onChooseBlack = {
                        viewModel.setBlackWallpaper()
                        viewModel.onNext()
                    },
                    onChooseWallpaper = {
                        viewModel.onNext()
                    }
                )
                OnboardingStep.LOCATION -> LocationStep(
                    onAllow = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    onSkip = { viewModel.onNext() }
                )
                OnboardingStep.SET_DEFAULT_LAUNCHER -> SetDefaultStep(
                    onSetDefault = { viewModel.openDefaultLauncherSettings() },
                    onNext = { viewModel.onNext() }
                )
                OnboardingStep.CUSTOMIZE_HOME -> CustomizeStep(
                    onChooseApps = { viewModel.onChooseApps() },
                    onSkip = { viewModel.onNext() }
                )
                OnboardingStep.SWIPE_SHORTCUTS -> {
                    val swipeState by viewModel.swipeShortcutsState.collectAsStateWithLifecycle()
                    SwipeShortcutsStep(
                        swipeState = swipeState,
                        onSetSwipeLeft = { viewModel.setSwipeLeftTarget(it) },
                        onSetSwipeRight = { viewModel.setSwipeRightTarget(it) },
                        onSkip = { viewModel.onNext() }
                    )
                }
                OnboardingStep.QUICK_TIPS -> QuickTipsStep(
                    onDone = { viewModel.onDone(onNavigateToHome) }
                )
                null -> { /* Loading / empty state */ }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (currentStep != OnboardingStep.BACKGROUND && currentStep != OnboardingStep.CUSTOMIZE_HOME && currentStep != OnboardingStep.LOCATION && currentStep != OnboardingStep.SWIPE_SHORTCUTS && !isLastStep) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FokusTextButton(onClick = { viewModel.onNext() }) {
                        Text(
                            text = stringResource(R.string.onboarding_next),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Show EditHomeAppsScreen as full-screen overlay during onboarding
        if (showEditHomeApps && homeViewModel != null) {
            LaunchedEffect(homeViewModel) { homeViewModel.startEditingHomeApps() }
            EditHomeAppsScreen(
                viewModel = homeViewModel,
                onNavigateBack = { viewModel.onEditHomeAppsDismissed() },
                backgroundScrim = Color.Black
            )
        }
    }
}

@Composable
private fun OnboardingStepTitle(text: String) {
    Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
    )
}

@Composable
private fun WelcomeStep(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OnboardingStepTitle(stringResource(R.string.onboarding_welcome_title))
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
private fun BackgroundStep(
    onChooseBlack: () -> Unit,
    onChooseWallpaper: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OnboardingStepTitle(stringResource(R.string.onboarding_background_title))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_background_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))

        // Option 1: Black background
        androidx.compose.material3.OutlinedCard(
            onClick = onChooseBlack,
            modifier = Modifier.fillMaxWidth(0.85f),
            colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.material3.CardDefaults.outlinedCardBorder()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.onboarding_background_black),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Option 2: Keep wallpaper
        androidx.compose.material3.OutlinedCard(
            onClick = onChooseWallpaper,
            modifier = Modifier.fillMaxWidth(0.85f),
            colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.material3.CardDefaults.outlinedCardBorder()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.onboarding_background_wallpaper),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
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
        OnboardingStepTitle(stringResource(R.string.onboarding_location_title))
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
        FokusTextButton(onClick = onSkip) {
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
        OnboardingStepTitle(stringResource(R.string.onboarding_set_default_title))
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
        FokusTextButton(onClick = onNext) {
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
        OnboardingStepTitle(stringResource(R.string.onboarding_customize_title))
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
        FokusTextButton(onClick = onSkip) {
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
    val showAppPickerFor = remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val notSetSwipe = stringResource(R.string.onboarding_swipe_not_set)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OnboardingStepTitle(stringResource(R.string.onboarding_swipe_title))
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
                    text =
                            formatShortcutTargetDisplay(
                                    context = context,
                                    target = swipeState.swipeLeftTarget,
                                    allApps = swipeState.allApps,
                                    notSetLabel = notSetSwipe
                            ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            FokusTextButton(onClick = { showAppPickerFor.value = "swipeLeft" }) {
                Text(stringResource(R.string.onboarding_swipe_change))
            }
            FokusIconButton(onClick = { onSetSwipeLeft(null) }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_clear),
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
                    text =
                            formatShortcutTargetDisplay(
                                    context = context,
                                    target = swipeState.swipeRightTarget,
                                    allApps = swipeState.allApps,
                                    notSetLabel = notSetSwipe
                            ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            FokusTextButton(onClick = { showAppPickerFor.value = "swipeRight" }) {
                Text(stringResource(R.string.onboarding_swipe_change))
            }
            FokusIconButton(onClick = { onSetSwipeRight(null) }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_clear),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        FokusTextButton(onClick = onSkip) {
            Text(
                text = stringResource(R.string.onboarding_next),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }

    showAppPickerFor.value?.let { pickerTarget ->
        ShortcutActionPickerDialog(
            allActions = swipeState.allShortcutActions,
            allApps = swipeState.allApps,
            title = stringResource(R.string.edit_shortcuts_section_all_actions),
            onSelect = { action ->
                when (pickerTarget) {
                    "swipeLeft" -> onSetSwipeLeft(action.target)
                    "swipeRight" -> onSetSwipeRight(action.target)
                }
                showAppPickerFor.value = null
            },
            onDismiss = { showAppPickerFor.value = null },
            containerColor = Color.Black
        )
    }
}

@Composable
private fun QuickTipsStep(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OnboardingStepTitle(stringResource(R.string.onboarding_tips_title))
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
