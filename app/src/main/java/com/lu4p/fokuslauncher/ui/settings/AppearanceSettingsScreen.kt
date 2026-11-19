package com.lu4p.fokuslauncher.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.ui.Alignment
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
import androidx.compose.material3.Text
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
import com.lu4p.fokuslauncher.data.iconpack.IconPackInfo
import com.lu4p.fokuslauncher.data.iconpack.ArcticonsPackages
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.font.CustomFontImportFailure
import com.lu4p.fokuslauncher.media.MediaNotificationHelper
import com.lu4p.fokuslauncher.ui.components.FokusAlertDialog
import com.lu4p.fokuslauncher.ui.components.FokusTextButton
import com.lu4p.fokuslauncher.ui.home.HomeAppIconMode
import com.lu4p.fokuslauncher.ui.settings.components.SettingsDropdown
import com.lu4p.fokuslauncher.ui.util.rememberBooleanChangeWithSystemSound
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
    var showArcticonsInstallDialog by remember { mutableStateOf(false) }
    var showIconPackDisclaimer by remember { mutableStateOf(false) }
    var pendingIconPackSelection by remember { mutableStateOf<String?>(null) }
    val iconPackDisclaimerAccepted by
            viewModel.iconPackDisclaimerAccepted.collectAsStateWithLifecycle()
    OnResumeEffect(lifecycleOwner) {
        mediaNotificationAccessTick++
        viewModel.refreshArcticonsInstallState()
    }
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
            item {
                SectionHeader(stringResource(R.string.settings_look_section_display))
            }
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
                HomeAlignmentRow(
                        currentAlignment = uiState.homeAlignment,
                        onAlignmentChanged = viewModel::setHomeAlignment,
                )
            }
            item {
                AppLanguageDropdown(
                        currentTag = uiState.appLocaleTag,
                        onTagSelected = viewModel::setAppLocaleTag,
                )
            }

            item { SettingsDivider() }
            item { SectionHeader(stringResource(R.string.settings_look_section_text)) }
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
                                            "font/otf",
                                            "application/x-font-ttf",
                                            "application/x-font-otf",
                                            "application/x-font-opentype",
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

            item { SettingsDivider() }
            item { SectionHeader(stringResource(R.string.settings_look_section_colors)) }
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

            item { SettingsDivider() }
            item { SectionHeader(stringResource(R.string.settings_look_section_icons)) }
            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_arcticons_drawer_icons),
                        checked = uiState.useArcticonsDrawerIcons && uiState.arcticonsInstalled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (!viewModel.setUseArcticonsDrawerIcons(true)) {
                                    showArcticonsInstallDialog = true
                                }
                            } else {
                                viewModel.setUseArcticonsDrawerIcons(false)
                            }
                        },
                        subtitle =
                                stringResource(
                                        if (uiState.arcticonsInstalled) {
                                            R.string.settings_arcticons_drawer_icons_subtitle
                                        } else {
                                            R.string.settings_arcticons_drawer_icons_subtitle_missing
                                        }
                                ),
                )
            }

            if (uiState.arcticonsInstalled) item {
                var packExpanded by remember { mutableStateOf(false) }
                val onPackExpandedChange =
                        rememberBooleanChangeWithSystemSound { packExpanded = it }
                val packOptions = uiState.installedIconPacks
                // Nothing selected = automatic: show the pack currently in effect.
                val selectedPackLabel =
                        packOptions
                                .firstOrNull { it.packageName == uiState.selectedIconPack }
                                ?.label
                                ?: packOptions
                                        .firstOrNull {
                                            it.packageName == uiState.activeIconPack
                                        }
                                        ?.label.orEmpty()
                SettingsDropdown(
                        title = stringResource(R.string.settings_icon_pack_picker),
                        options = packOptions,
                        expanded = packExpanded,
                        onExpandedChange = onPackExpandedChange,
                        selectedDisplayText = selectedPackLabel,
                        fieldEnabled = uiState.installedIconPacks.isNotEmpty(),
                        itemContent = { pack -> Text(pack.label) },
                        onItemSelected = { pack ->
                            if (!iconPackDisclaimerAccepted &&
                                            !ArcticonsPackages.isWhitelisted(pack.packageName)) {
                                pendingIconPackSelection = pack.packageName
                                showIconPackDisclaimer = true
                            } else {
                                viewModel.setSelectedIconPack(pack.packageName)
                            }
                        },
                )
            }

            if (uiState.useArcticonsDrawerIcons && uiState.arcticonsInstalled) item {
                var expanded by remember { mutableStateOf(false) }
                val onExpandedChange = rememberBooleanChangeWithSystemSound { expanded = it }
                val packOptions = uiState.installedIconPacks
                val packName =
                        packOptions
                                .firstOrNull { it.packageName == uiState.activeIconPack }
                                ?.label
                                ?: stringResource(R.string.app_name)
                val labels = mapOf(
                    HomeAppIconMode.TEXT to
                            stringResource(R.string.settings_home_app_icons_text),
                    HomeAppIconMode.WITH_LABEL to
                            stringResource(
                                    R.string.settings_home_app_icons_with_label,
                                    packName,
                            ),
                    HomeAppIconMode.ICON_ONLY to
                            stringResource(
                                    R.string.settings_home_app_icons_only,
                                    packName,
                            ),
                )
                SettingsDropdown(
                    title = stringResource(R.string.settings_home_app_icons),
                    options = HomeAppIconMode.entries,
                    expanded = expanded,
                    onExpandedChange = onExpandedChange,
                    selectedDisplayText = labels.getValue(uiState.homeAppIconMode),
                    itemContent = { mode -> Text(labels.getValue(mode)) },
                    onItemSelected = { mode ->
                        if (!viewModel.setHomeAppIconMode(mode)) showArcticonsInstallDialog = true
                    },
                )
            }

            item { SettingsDivider() }
            item { SectionHeader(stringResource(R.string.settings_look_section_wallpaper)) }
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
                SectionHeader(stringResource(R.string.settings_look_section_notifications))
            }
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
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showArcticonsInstallDialog) {
        FokusAlertDialog(
                onDismissRequest = { showArcticonsInstallDialog = false },
                title = {
                    Text(
                            stringResource(R.string.settings_arcticons_install_title),
                            color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                text = {
                    Text(
                            stringResource(R.string.settings_arcticons_install_message),
                            color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                confirmButton = {
                    FokusTextButton(
                            onClick = {
                                showArcticonsInstallDialog = false
                                viewModel.openArcticonsFdroidInstall()
                            }
                    ) {
                        Text(
                                stringResource(R.string.settings_arcticons_install_fdroid),
                                color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                dismissButton = {
                    FokusTextButton(
                            onClick = {
                                showArcticonsInstallDialog = false
                                viewModel.openArcticonsPlayStoreInstall()
                            }
                    ) {
                        Text(
                                stringResource(R.string.settings_arcticons_install_play),
                                color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
        )
    }

    if (showIconPackDisclaimer) {
        var disclaimerChecked by remember { mutableStateOf(false) }
        FokusAlertDialog(
                onDismissRequest = {
                    showIconPackDisclaimer = false
                    pendingIconPackSelection = null
                },
                title = {
                    Text(
                            stringResource(R.string.settings_icon_pack_disclaimer_title),
                            color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                text = {
                    Column {
                        Text(
                                stringResource(R.string.settings_icon_pack_disclaimer_body),
                                color = MaterialTheme.colorScheme.onBackground,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                    checked = disclaimerChecked,
                                    onCheckedChange = { disclaimerChecked = it },
                            )
                            Text(
                                    stringResource(R.string.settings_icon_pack_disclaimer_checkbox),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                            )
                        }
                    }
                },
                confirmButton = {
                    FokusTextButton(
                            onClick = {
                                showIconPackDisclaimer = false
                                pendingIconPackSelection?.let {
                                    viewModel.acceptIconPackDisclaimerAndSelect(it)
                                }
                                pendingIconPackSelection = null
                            },
                            enabled = disclaimerChecked,
                    ) {
                        Text(
                                stringResource(R.string.settings_icon_pack_disclaimer_accept),
                                color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                dismissButton = {
                    FokusTextButton(
                            onClick = {
                                showIconPackDisclaimer = false
                                pendingIconPackSelection = null
                            }
                    ) {
                        Text(
                                stringResource(R.string.action_cancel),
                                color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
        )
    }
}
