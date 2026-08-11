package com.lu4p.fokuslauncher.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.font.CustomFontImportFailure
import com.lu4p.fokuslauncher.media.MediaNotificationHelper
import com.lu4p.fokuslauncher.ui.settings.components.SectionHeader
import com.lu4p.fokuslauncher.ui.settings.components.SettingsDivider
import com.lu4p.fokuslauncher.ui.settings.components.SettingsRow
import com.lu4p.fokuslauncher.ui.settings.components.SettingsToggleRow
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import com.lu4p.fokuslauncher.ui.util.OnResumeEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit = {},
        onNavigateToHome: () -> Unit = {},
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val installedFontFamilies by viewModel.installedFontFamilies.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mediaNotificationAccessTick by remember { mutableIntStateOf(0) }
    var pendingNotificationIndicatorsEnable by remember { mutableStateOf(false) }
    OnResumeEffect(lifecycleOwner) { mediaNotificationAccessTick++ }
    val mediaNotificationAccessEnabled =
            remember(mediaNotificationAccessTick) {
                MediaNotificationHelper.isListenerEnabled(context)
            }
    LaunchedEffect(
            mediaNotificationAccessTick,
            uiState.showNotificationIndicators,
            pendingNotificationIndicatorsEnable,
    ) {
        if (pendingNotificationIndicatorsEnable && mediaNotificationAccessEnabled) {
            pendingNotificationIndicatorsEnable = false
            viewModel.setShowNotificationIndicators(true)
        } else if (uiState.showNotificationIndicators && !mediaNotificationAccessEnabled) {
            viewModel.setShowNotificationIndicators(false)
        }
    }

    val wallpaperPickerLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
                    uri ->
                uri?.let {
                    viewModel.setSystemWallpaper(it)
                    onNavigateToHome()
                }
            }

    val fontImportFailedUnreadable =
            stringResource(R.string.settings_font_import_failed_unreadable)
    val fontImportFailedExtension =
            stringResource(R.string.settings_font_import_failed_extension)
    val fontImportFailedInvalid = stringResource(R.string.settings_font_import_failed_invalid)
    val fontImportFailedIo = stringResource(R.string.settings_font_import_failed_io)
    val fontPickerLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {
                    uri ->
                uri?.let { picked ->
                    viewModel.importCustomFont(picked) { failure ->
                        val message =
                                when (failure) {
                                    CustomFontImportFailure.UNREADABLE_URI ->
                                            fontImportFailedUnreadable
                                    CustomFontImportFailure.INVALID_EXTENSION ->
                                            fontImportFailedExtension
                                    CustomFontImportFailure.INVALID_FONT -> fontImportFailedInvalid
                                    CustomFontImportFailure.IO_ERROR -> fontImportFailedIo
                                    null -> null
                                }
                        if (message != null) {
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .background(backgroundScrim)
                            .navigationBarsPadding()
                            .testTag("appearance_settings_screen")
    ) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.settings_look_and_feel_title),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                    listOf(
                            Triple(
                                    R.string.settings_show_status_bar,
                                    uiState.showStatusBar,
                                    viewModel::setShowStatusBar,
                            ),
                            Triple(
                                    R.string.settings_allow_landscape_rotation,
                                    uiState.allowLandscapeRotation,
                                    viewModel::setAllowLandscapeRotation,
                            ),
                    ),
                    key = { it.first },
            ) { (labelRes, checked, onChange) ->
                SettingsToggleRow(
                        label = stringResource(labelRes),
                        checked = checked,
                        onCheckedChange = onChange,
                )
            }
            item {
                AppLanguageDropdown(
                        currentTag = uiState.appLocaleTag,
                        onTagSelected = viewModel::setAppLocaleTag,
                )
            }
            item {
                LauncherFontFamilyDropdown(
                        currentFamilyName = uiState.launcherFontFamilyName,
                        installedFamilies = installedFontFamilies,
                        hasCustomFontFile = uiState.hasCustomFontFile,
                        customFontDisplayName = uiState.customFontDisplayName,
                        resolveCustomFontFile = viewModel::resolveCustomFontFile,
                        onFamilySelected = viewModel::setLauncherFontFamilyName,
                )
            }
            item {
                SettingsRow(
                        label = stringResource(R.string.settings_font_import_ttf),
                        subtitle = stringResource(R.string.settings_font_import_ttf_subtitle),
                        verticalPadding = 14.dp,
                        onClick = {
                            fontPickerLauncher.launch(
                                    arrayOf(
                                            "font/ttf",
                                            "application/x-font-ttf",
                                            "application/font-sfnt",
                                            "application/octet-stream",
                                    )
                            )
                        },
                )
            }
            if (uiState.hasCustomFontFile) {
                item {
                    SettingsRow(
                            label = stringResource(R.string.settings_font_clear_custom),
                            verticalPadding = 14.dp,
                            onClick = { viewModel.clearCustomFont() },
                    )
                }
            }
            item {
                LauncherFontSizeSlider(
                        currentScale = uiState.launcherFontScale,
                        onScaleChange = viewModel::setLauncherFontScale,
                )
            }
            item {
                LauncherVisualStyleDropdown(
                        currentStyle = uiState.launcherVisualStyle,
                        onStyleSelected = viewModel::setLauncherVisualStyle,
                        homeUsesPhotoWallpaper = uiState.homeUsesPhotoWallpaper,
                )
            }
            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_glow_label),
                        checked =
                                uiState.launcherGlowEnabled && !uiState.homeUsesPhotoWallpaper,
                        onCheckedChange = viewModel::setLauncherGlowEnabled,
                        subtitle =
                                stringResource(
                                        if (uiState.homeUsesPhotoWallpaper) {
                                            R.string.settings_look_locked_image_wallpaper
                                        } else {
                                            R.string.settings_glow_subtitle
                                        }
                                ),
                        enabled = !uiState.homeUsesPhotoWallpaper,
                )
            }
            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_simplified_app_icons),
                        checked = uiState.showSimplifiedAppIcons,
                        onCheckedChange = viewModel::setShowSimplifiedAppIcons,
                        subtitle = stringResource(R.string.settings_simplified_app_icons_subtitle),
                )
            }
            item {
                SettingsRow(
                        label = stringResource(R.string.settings_set_background_image),
                        verticalPadding = 14.dp,
                        onClick = { wallpaperPickerLauncher.launch("image/*") },
                )
            }
            item {
                SettingsRow(
                        label = stringResource(R.string.settings_set_black_wallpaper),
                        verticalPadding = 14.dp,
                        onClick = {
                            viewModel.setBlackWallpaper()
                            onNavigateToHome()
                        },
                )
            }
            if (uiState.homeUsesPhotoWallpaper) {
                item {
                    SectionHeader(
                            stringResource(R.string.settings_section_image_wallpaper_accessibility)
                    )
                }
                item {
                    PhotoWallpaperOutlineWidthSlider(
                            currentWidthDp = uiState.photoWallpaperOutlineWidthDp,
                            onWidthDpChange = viewModel::setPhotoWallpaperOutlineWidthDp,
                    )
                }
                item {
                    PhotoWallpaperDrawerOverlaySlider(
                            currentIntensity = uiState.photoWallpaperDrawerOverlayIntensity,
                            onIntensityChange = viewModel::setPhotoWallpaperDrawerOverlayIntensity,
                    )
                }
            }
            item { SettingsDivider() }
            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_notification_indicators),
                        subtitle =
                                if (mediaNotificationAccessEnabled) {
                                    stringResource(R.string.settings_notification_indicators_subtitle)
                                } else {
                                    stringResource(
                                            R.string
                                                    .settings_notification_indicators_subtitle_grant_access
                                    )
                                },
                        checked = uiState.showNotificationIndicators,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (mediaNotificationAccessEnabled) {
                                    viewModel.setShowNotificationIndicators(true)
                                } else {
                                    pendingNotificationIndicatorsEnable = true
                                    MediaNotificationHelper.openListenerSettings(context)
                                }
                            } else {
                                pendingNotificationIndicatorsEnable = false
                                viewModel.setShowNotificationIndicators(false)
                            }
                        },
                )
            }
            if (uiState.showNotificationIndicators) {
                item {
                    NotificationIndicatorStyleDropdown(
                            currentStyle = uiState.notificationIndicatorStyle,
                            onStyleSelected = viewModel::setNotificationIndicatorStyle,
                    )
                }
                item {
                    NotificationIndicatorColorDropdown(
                            currentColor = uiState.notificationIndicatorColor,
                            onColorSelected = viewModel::setNotificationIndicatorColorPreset,
                    )
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
