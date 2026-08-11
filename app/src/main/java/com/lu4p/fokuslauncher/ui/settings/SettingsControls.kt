package com.lu4p.fokuslauncher.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.widget.Toast
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.data.model.DrawerAppSortMode
import com.lu4p.fokuslauncher.data.model.HomeAlignment
import com.lu4p.fokuslauncher.data.model.HomeDateFormatStyle
import com.lu4p.fokuslauncher.data.model.LauncherFontPreferences
import com.lu4p.fokuslauncher.data.model.LauncherFontScale
import com.lu4p.fokuslauncher.data.model.LauncherVisualStyle
import com.lu4p.fokuslauncher.data.model.NotificationIndicatorColorPreset
import com.lu4p.fokuslauncher.data.model.NotificationIndicatorStyle
import com.lu4p.fokuslauncher.data.model.PhotoWallpaperDrawerOverlayIntensity
import com.lu4p.fokuslauncher.data.model.PhotoWallpaperOutlineWidthDp
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.model.TemperatureUnit
import com.lu4p.fokuslauncher.data.model.WidgetTapTarget
import com.lu4p.fokuslauncher.ui.components.FokusIconButton
import com.lu4p.fokuslauncher.ui.components.FokusTextButton
import com.lu4p.fokuslauncher.ui.components.LauncherIcon
import com.lu4p.fokuslauncher.ui.settings.components.SettingsDropdown
import com.lu4p.fokuslauncher.ui.settings.components.SettingsRow
import com.lu4p.fokuslauncher.ui.settings.components.EmptySettingsStateText
import com.lu4p.fokuslauncher.ui.settings.components.SectionHeader
import com.lu4p.fokuslauncher.ui.theme.LocalLauncherFontScale
import com.lu4p.fokuslauncher.ui.theme.LocalLauncherIconGlow
import com.lu4p.fokuslauncher.ui.theme.composeFontFamilyFromStoredName
import com.lu4p.fokuslauncher.ui.theme.launcherIconDp
import com.lu4p.fokuslauncher.ui.theme.settingsPreviewColor
import com.lu4p.fokuslauncher.ui.theme.withoutLauncherTextGlow
import com.lu4p.fokuslauncher.ui.util.OnResumeEffect
import com.lu4p.fokuslauncher.ui.util.formatShortcutTargetDisplay
import com.lu4p.fokuslauncher.ui.util.rememberBooleanChangeWithSystemSound
import com.lu4p.fokuslauncher.ui.util.rememberClickWithSystemSound
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.launch

internal data class SubpageNavRow(
        @param:StringRes val labelRes: Int,
        val subtitle: String? = null,
        val onClick: () -> Unit,
)

internal data class SwipeTargetPick(
        val pickerKey: String,
        @param:StringRes val labelRes: Int,
        val target: ShortcutTarget?,
        val onClear: () -> Unit,
)

internal data class WidgetTapPickerRow(
        @param:StringRes val labelRes: Int,
        val tapTarget: WidgetTapTarget?,
        val pickerKey: String,
        val onClear: () -> Unit,
        val emptyLabel: (Context, Resources) -> String = ::formatWidgetAppEmptyLabel,
)

internal data class DeviceControlToggleRow(
        @param:StringRes val labelRes: Int,
        val subtitle: String,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
)

internal data class CommunityLink(
        val icon: ImageVector,
        val titleRes: Int,
        val subtitleRes: Int,
        val url: String,
)

internal val communityLinks =
        listOf(
                CommunityLink(
                        Icons.Filled.Star,
                        R.string.settings_github_title,
                        R.string.settings_github_subtitle,
                        "https://github.com/luantak/FokusLauncher",
                ),
                CommunityLink(
                        Icons.Outlined.Translate,
                        R.string.settings_weblate_title,
                        R.string.settings_weblate_subtitle,
                        "https://hosted.weblate.org/engage/fokus-launcher/",
                ),
                CommunityLink(
                        Icons.Filled.ChatBubble,
                        R.string.settings_matrix_title,
                        R.string.settings_matrix_subtitle,
                        "https://matrix.to/#/#fokus:matrix.org",
                ),
                CommunityLink(
                        Icons.Outlined.LocalCafe,
                        R.string.settings_paypal_title,
                        R.string.settings_paypal_subtitle,
                        "https://paypal.me/PScheduikat",
                ),
        )

internal fun Context.hasCoarseLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

internal fun <T> LazyListScope.manageableAppsSection(
        headerRes: Int,
        emptyTextRes: Int,
        apps: List<T>,
        key: (T) -> Any,
        label: (T) -> String,
        subtitle: (T) -> String,
        onRowClick: (T) -> Unit,
        trailingContent: @Composable RowScope.(T) -> Unit,
) {
    item { SectionHeader(stringResource(headerRes)) }
    if (apps.isEmpty()) {
        item { EmptySettingsStateText(text = stringResource(emptyTextRes)) }
    } else {
        items(apps, key = key) { app ->
            SettingsRow(
                    label = label(app),
                    subtitle = subtitle(app),
                    subtitleStyle = MaterialTheme.typography.labelMedium,
                    onClick = { onRowClick(app) },
                    trailing = { trailingContent(app) },
            )
        }
    }
}

@Composable
internal fun rememberCoarseLocationPermission(context: Context, activity: Activity?): Pair<Boolean, () -> Unit> {
    var granted by remember { mutableStateOf(context.hasCoarseLocationPermission()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    OnResumeEffect(lifecycleOwner) { granted = context.hasCoarseLocationPermission() }
    val launcher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
            ) {
                granted = context.hasCoarseLocationPermission()
                if (!granted &&
                                activity != null &&
                                !ActivityCompat.shouldShowRequestPermissionRationale(
                                        activity,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                )
                ) {
                    context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                    )
                }
            }
    val request = remember(launcher) { { launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) } }
    return granted to request
}

/**
 * Endonym: name of the language written in that language (e.g. English, Polski), independent of
 * app UI locale.
 */
internal fun languageAutonym(localeTag: String, allTags: List<String>): String {
    val locale = Locale.forLanguageTag(localeTag)
    val sameLanguageTags =
            allTags.filter { Locale.forLanguageTag(it).language == locale.language }
    val displayLocale =
            when {
                sameLanguageTags.size <= 1 -> locale
                localeTag == "pt" -> Locale.forLanguageTag("pt-PT")
                else -> locale
            }
    val raw =
            if (sameLanguageTags.size > 1) {
                displayLocale.getDisplayName(displayLocale).trim()
            } else {
                locale.getDisplayLanguage(locale).trim()
            }
    if (raw.isBlank()) return localeTag
    return raw.replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase(displayLocale) else ch.toString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeDateFormatDropdown(
        currentStyle: HomeDateFormatStyle,
        enabled: Boolean,
        onStyleSelected: (HomeDateFormatStyle) -> Unit
) {
    val options = remember { HomeDateFormatStyle.entries }
    var expanded by remember { mutableStateOf(false) }
    val onDateFormatExpandedChange =
            rememberBooleanChangeWithSystemSound { newExpanded ->
                if (enabled) expanded = newExpanded
            }
    SettingsDropdown(
            title = stringResource(R.string.settings_home_date_format),
            options = options,
            expanded = expanded,
            onExpandedChange = onDateFormatExpandedChange,
            selectedDisplayText = stringResource(currentStyle.labelRes),
            fieldEnabled = enabled,
            menuExpanded = expanded && enabled,
            itemContent = { style ->
                Text(
                        text = stringResource(style.labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                )
            },
            onItemSelected = onStyleSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TemperatureUnitDropdown(
        currentUnit: TemperatureUnit,
        enabled: Boolean,
        onUnitSelected: (TemperatureUnit) -> Unit
) {
    val options = remember { TemperatureUnit.entries }
    var expanded by remember { mutableStateOf(false) }
    val onExpandedChange =
            rememberBooleanChangeWithSystemSound { newExpanded ->
                if (enabled) expanded = newExpanded
            }
    SettingsDropdown(
            title = stringResource(R.string.settings_temperature_unit),
            options = options,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            selectedDisplayText = stringResource(currentUnit.labelRes),
            fieldEnabled = enabled,
            menuExpanded = expanded && enabled,
            itemContent = { unit ->
                Text(
                        text = stringResource(unit.labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                )
            },
            onItemSelected = onUnitSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationIndicatorStyleDropdown(
        currentStyle: NotificationIndicatorStyle,
        onStyleSelected: (NotificationIndicatorStyle) -> Unit,
) {
    val options = remember { NotificationIndicatorStyle.entries }
    var expanded by remember { mutableStateOf(false) }
    val onExpandedChange = rememberBooleanChangeWithSystemSound { expanded = it }
    SettingsDropdown(
            title = stringResource(R.string.settings_notification_indicator_style),
            options = options,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            selectedDisplayText = stringResource(currentStyle.labelRes),
            itemContent = { style ->
                Text(
                        text = stringResource(style.labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                )
            },
            onItemSelected = onStyleSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationIndicatorColorDropdown(
        currentColor: Int,
        onColorSelected: (NotificationIndicatorColorPreset) -> Unit,
) {
    val options = remember { NotificationIndicatorColorPreset.entries }
    val currentPreset = remember(currentColor) {
        NotificationIndicatorColorPreset.fromArgb(currentColor)
    }
    var expanded by remember { mutableStateOf(false) }
    val onExpandedChange = rememberBooleanChangeWithSystemSound { expanded = it }
    SettingsDropdown(
            title = stringResource(R.string.settings_notification_indicator_color),
            options = options,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            selectedDisplayText = stringResource(currentPreset.labelRes),
            fieldTextColor = Color(currentPreset.argb),
            menuItemTextColor = { Color(it.argb) },
            itemContent = { preset ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                            modifier =
                                    Modifier.size(12.dp)
                                            .background(Color(preset.argb), CircleShape),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                            text = stringResource(preset.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(preset.argb),
                    )
                }
            },
            onItemSelected = onColorSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppLanguageDropdown(
        currentTag: String,
        onTagSelected: (String) -> Unit
) {
    val systemDefaultLabel = stringResource(R.string.settings_language_system_default)
    val supportedLocaleTags =
            remember {
                listOf(
                        "ca",
                        "da",
                        "de",
                        "en",
                        "es",
                        "eu",
                        "fi",
                        "fr",
                        "in",
                        "it",
                        "pl",
                        "pt",
                        "pt-BR",
                        "ro",
                        "ru",
                        "ta",
                        "tr",
                        "uk",
                        "zh-CN",
                )
            }
    val options =
            remember(systemDefaultLabel) {
                val collator = Collator.getInstance(Locale.ROOT).apply { strength = Collator.PRIMARY }
                buildList {
                    add("" to systemDefaultLabel)
                    supportedLocaleTags
                            .map { tag -> tag to languageAutonym(tag, supportedLocaleTags) }
                            .sortedWith { a, b -> collator.compare(a.second, b.second) }
                            .forEach { add(it) }
                }
            }
    var expanded by remember { mutableStateOf(false) }
    val onLanguageExpandedChange = rememberBooleanChangeWithSystemSound { expanded = it }
    val selectedDisplayText =
            options.find { (tag, _) -> tag == currentTag }?.second
                    ?: if (currentTag.isBlank()) {
                        systemDefaultLabel
                    } else {
                        languageAutonym(currentTag, supportedLocaleTags)
                    }
    SettingsDropdown(
            title = stringResource(R.string.settings_language_label),
            options = options,
            expanded = expanded,
            onExpandedChange = onLanguageExpandedChange,
            selectedDisplayText = selectedDisplayText,
            itemContent = { (_, label) ->
                Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                )
            },
            onItemSelected = { (storageTag, _) -> onTagSelected(storageTag) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherFontFamilyDropdown(
        currentFamilyName: String,
        installedFamilies: List<String>,
        hasCustomFontFile: Boolean,
        customFontDisplayName: String,
        resolveCustomFontFile: (String) -> java.io.File?,
        onFamilySelected: (String) -> Unit
) {
    val systemDefault = stringResource(R.string.settings_weather_app_system_default)
    val customImportedFallback = stringResource(R.string.settings_font_custom_imported)
    val customFontLabel =
            customFontDisplayName.trim().ifBlank { customImportedFallback }
    val options =
            remember(
                    currentFamilyName,
                    installedFamilies,
                    systemDefault,
                    hasCustomFontFile,
                    customFontLabel,
            ) {
                buildList {
                    add("" to systemDefault)
                    if (hasCustomFontFile) {
                        add(LauncherFontPreferences.CUSTOM_FONT_STORAGE to customFontLabel)
                    }
                    val sorted = installedFamilies.sortedWith(String.CASE_INSENSITIVE_ORDER)
                    sorted.forEach { add(it to it) }
                    val cur = currentFamilyName.trim()
                    if (cur.isNotEmpty() &&
                                    sorted.none { it.equals(cur, ignoreCase = true) } &&
                                    !(hasCustomFontFile &&
                                            cur == LauncherFontPreferences.CUSTOM_FONT_STORAGE)
                    ) {
                        val label =
                                if (LauncherFontPreferences.isCustomFont(cur)) {
                                    customFontLabel
                                } else {
                                    cur
                                }
                        add(cur to label)
                    }
                }
            }
    var expanded by remember { mutableStateOf(false) }
    val onFontExpandedChange = rememberBooleanChangeWithSystemSound { expanded = it }
    val selectedLabel =
            options.find { (value, _) -> value == currentFamilyName }?.second
                    ?: currentFamilyName.ifBlank { systemDefault }
    SettingsDropdown(
            title = stringResource(R.string.settings_font_label),
            options = options,
            expanded = expanded,
            onExpandedChange = onFontExpandedChange,
            selectedDisplayText = selectedLabel,
            textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                            fontFamily =
                                    composeFontFamilyFromStoredName(currentFamilyName) {
                                        resolveCustomFontFile(it)
                                    }
                    ),
            itemContent = { (storageValue, label) ->
                Text(
                        text = label,
                        style =
                                MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily =
                                                composeFontFamilyFromStoredName(storageValue) {
                                                    resolveCustomFontFile(it)
                                                }
                                ),
                        color = MaterialTheme.colorScheme.onBackground,
                )
            },
            onItemSelected = { (storageValue, _) -> onFamilySelected(storageValue) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherFontSizeSlider(
        currentScale: Float,
        onScaleChange: (Float) -> Unit,
) {
    val synced = LauncherFontScale.snapToStep(currentScale)
    var pending by remember { mutableFloatStateOf(synced) }
    LaunchedEffect(synced) { pending = synced }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                    text = stringResource(R.string.settings_font_size_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
            )
            Text(
                    text = String.format(Locale.US, "%.1fx", pending),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
                text = stringResource(R.string.settings_font_size_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(12.dp))
        Slider(
                value = pending,
                onValueChange = { raw ->
                    pending = LauncherFontScale.snapToStep(raw)
                },
                onValueChangeFinished = {
                    val v = LauncherFontScale.snapToStep(pending)
                    if (v != synced) {
                        onScaleChange(v)
                    }
                },
                valueRange = LauncherFontScale.MIN..LauncherFontScale.MAX,
                steps = LauncherFontScale.SLIDER_STEPS,
                modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotoWallpaperOutlineWidthSlider(
        currentWidthDp: Float,
        onWidthDpChange: (Float) -> Unit,
) {
    val synced = PhotoWallpaperOutlineWidthDp.snapToStep(currentWidthDp)
    var pending by remember { mutableFloatStateOf(synced) }
    LaunchedEffect(synced) { pending = synced }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(
                text = stringResource(R.string.settings_photo_outline_strength_label),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
                text = stringResource(R.string.settings_photo_outline_strength_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(12.dp))
        Slider(
                value = pending,
                onValueChange = { raw ->
                    pending = raw.coerceIn(PhotoWallpaperOutlineWidthDp.MIN, PhotoWallpaperOutlineWidthDp.MAX)
                },
                onValueChangeFinished = {
                    val v = PhotoWallpaperOutlineWidthDp.snapToStep(pending)
                    if (v != synced) {
                        onWidthDpChange(v)
                    }
                },
                valueRange = PhotoWallpaperOutlineWidthDp.MIN..PhotoWallpaperOutlineWidthDp.MAX,
                steps = PhotoWallpaperOutlineWidthDp.SLIDER_STEPS,
                modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotoWallpaperDrawerOverlaySlider(
        currentIntensity: Float,
        onIntensityChange: (Float) -> Unit,
) {
    val synced = PhotoWallpaperDrawerOverlayIntensity.snapToStep(currentIntensity)
    var pending by remember { mutableFloatStateOf(synced) }
    LaunchedEffect(synced) { pending = synced }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                    text = stringResource(R.string.settings_photo_drawer_overlay_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
            )
            Text(
                    text = String.format(Locale.US, "%.1fx", pending),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
                text = stringResource(R.string.settings_photo_drawer_overlay_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(12.dp))
        Slider(
                value = pending,
                onValueChange = { raw ->
                    pending = PhotoWallpaperDrawerOverlayIntensity.snapToStep(raw)
                },
                onValueChangeFinished = {
                    val v = PhotoWallpaperDrawerOverlayIntensity.snapToStep(pending)
                    if (v != synced) {
                        onIntensityChange(v)
                    }
                },
                valueRange =
                        PhotoWallpaperDrawerOverlayIntensity.MIN..PhotoWallpaperDrawerOverlayIntensity.MAX,
                steps = PhotoWallpaperDrawerOverlayIntensity.SLIDER_STEPS,
                modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherVisualStyleDropdown(
        currentStyle: LauncherVisualStyle,
        onStyleSelected: (LauncherVisualStyle) -> Unit,
        homeUsesPhotoWallpaper: Boolean,
) {
    val options = remember { LauncherVisualStyle.entries.toList() }
    var expanded by remember { mutableStateOf(false) }
    val onExpandedChange = rememberBooleanChangeWithSystemSound { expanded = it }
    LaunchedEffect(homeUsesPhotoWallpaper) {
        if (homeUsesPhotoWallpaper) expanded = false
    }
    val displayStyle =
            if (homeUsesPhotoWallpaper) LauncherVisualStyle.CLASSIC else currentStyle
    val selectedLabel = stringResource(displayStyle.labelRes)
    val fieldPreviewColor = displayStyle.settingsPreviewColor()
    val lockedSubtitle = stringResource(R.string.settings_look_locked_image_wallpaper)
    val normalSubtitle = stringResource(R.string.settings_visual_style_subtitle)
    val menuGlowEnabled = LocalLauncherIconGlow.current.enabled
    val menuFontBlurBoost =
            LocalLauncherFontScale.current.coerceIn(LauncherFontScale.MIN, LauncherFontScale.MAX)
                    .coerceIn(0.85f, 1.45f)
    SettingsDropdown(
            title = stringResource(R.string.settings_visual_style_label),
            subtitle = if (homeUsesPhotoWallpaper) lockedSubtitle else normalSubtitle,
            options = options,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            fieldEnabled = !homeUsesPhotoWallpaper,
            selectedDisplayText = selectedLabel,
            fieldTextColor = fieldPreviewColor,
            menuItemTextColor = { it.settingsPreviewColor() },
            itemContent = { style ->
                val preview = style.settingsPreviewColor()
                val itemTextStyle =
                        if (menuGlowEnabled) {
                            MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.Unspecified,
                                    shadow =
                                            Shadow(
                                                    color = preview.copy(alpha = 1f),
                                                    offset = Offset.Zero,
                                                    blurRadius = 40f * menuFontBlurBoost,
                                            ),
                            )
                        } else {
                            MaterialTheme.typography.bodyLarge
                                    .copy(color = Color.Unspecified)
                                    .withoutLauncherTextGlow()
                        }
                Text(
                        text = stringResource(style.labelRes),
                        style = itemTextStyle,
                        color = preview,
                )
            },
            onItemSelected = onStyleSelected,
    )
}

@Composable
internal fun SettingsLabeledSegmentedSection(
        title: String,
        subtitle: String?,
        content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DrawerCategoryRailSideRow(
        railOnLeft: Boolean,
        onRailOnLeftChanged: (Boolean) -> Unit
) {
    SettingsLabeledSegmentedSection(
            title = stringResource(R.string.settings_drawer_category_rail_side),
            subtitle = stringResource(R.string.settings_drawer_category_rail_side_subtitle),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                    selected = railOnLeft,
                    onClick =
                            rememberClickWithSystemSound { onRailOnLeftChanged(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {},
            ) {
                Text(stringResource(R.string.settings_drawer_rail_position_left))
            }
            SegmentedButton(
                    selected = !railOnLeft,
                    onClick =
                            rememberClickWithSystemSound { onRailOnLeftChanged(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {},
            ) {
                Text(stringResource(R.string.settings_drawer_rail_position_right))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DrawerAppSortRow(
        currentMode: DrawerAppSortMode,
        showCustomSortOption: Boolean,
        onModeChanged: (DrawerAppSortMode) -> Unit
) {
    val modes =
            remember(showCustomSortOption) {
                if (showCustomSortOption) DrawerAppSortMode.entries.toList()
                else DrawerAppSortMode.entries.filterNot { it == DrawerAppSortMode.CUSTOM }
            }
    val coercedMode =
            remember(currentMode, showCustomSortOption, modes) {
                if (!showCustomSortOption && currentMode == DrawerAppSortMode.CUSTOM) {
                    DrawerAppSortMode.ALPHABETICAL
                } else {
                    currentMode
                }
            }
    SettingsLabeledSegmentedSection(
            title = stringResource(R.string.settings_drawer_app_sort),
            subtitle = stringResource(R.string.settings_drawer_app_sort_subtitle),
    ) {
        SingleChoiceSegmentedButtonRow(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(IntrinsicSize.Max)
        ) {
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                        selected = coercedMode == mode,
                        onClick = rememberClickWithSystemSound { onModeChanged(mode) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape =
                                SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = modes.size
                                ),
                        icon = {},
                ) {
                    Box(
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                            contentAlignment = Alignment.Center,
                    ) {
                        Text(
                                text = stringResource(mode.labelRes),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LongLockThresholdRow(
        currentMinutes: Int,
        onMinutesSelected: (Int) -> Unit
) {
    val options = remember { listOf(1, 5, 15, 30) }
    var expanded by remember { mutableStateOf(false) }
    val onLongLockExpandedChange = rememberBooleanChangeWithSystemSound { expanded = it }
    val selectedLabel =
            pluralStringResource(
                    R.plurals.settings_long_lock_duration_minutes,
                    currentMinutes,
                    currentMinutes
            )
    SettingsDropdown(
            title = stringResource(R.string.settings_long_lock_duration),
            subtitle = stringResource(R.string.settings_long_lock_duration_subtitle),
            options = options,
            expanded = expanded,
            onExpandedChange = onLongLockExpandedChange,
            selectedDisplayText = selectedLabel,
            itemContent = { minutes ->
                Text(
                        text =
                                pluralStringResource(
                                        R.plurals.settings_long_lock_duration_minutes,
                                        minutes,
                                        minutes
                                ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                )
            },
            onItemSelected = onMinutesSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeAlignmentRow(
        currentAlignment: HomeAlignment,
        onAlignmentChanged: (HomeAlignment) -> Unit
) {
    SettingsLabeledSegmentedSection(
            title = stringResource(R.string.home_alignment_title),
            subtitle = stringResource(R.string.home_alignment_subtitle),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            HomeAlignment.entries.forEachIndexed { index, alignment ->
                SegmentedButton(
                        selected = currentAlignment == alignment,
                        onClick =
                                rememberClickWithSystemSound {
                                    onAlignmentChanged(alignment)
                                },
                        shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = HomeAlignment.entries.size
                        ),
                        icon = {},
                ) {
                    Text(stringResource(alignment.labelRes))
                }
            }
        }
    }
}

@Composable
internal fun ExportLogsRow(
        context: Context,
        createLogShareIntent: suspend () -> Intent?
) {
    val scope = rememberCoroutineScope()
    val activity = LocalActivity.current
    val shareChooserTitle = stringResource(R.string.settings_export_logs_share_chooser)
    val exportLogsFailedToast = stringResource(R.string.toast_export_logs_failed)
    SettingsRow(
            label = stringResource(R.string.settings_export_logs_title),
            subtitle = stringResource(R.string.settings_export_logs_subtitle),
            verticalPadding = 14.dp,
            onClick = {
                scope.launch {
                    val shareIntent = createLogShareIntent()
                    if (shareIntent != null && activity != null) {
                        activity.startActivity(
                                Intent.createChooser(shareIntent, shareChooserTitle)
                        )
                    } else {
                        Toast.makeText(context, exportLogsFailedToast, Toast.LENGTH_SHORT).show()
                    }
                }
            },
    )
}

// --- Weather app (location gate + shortcut row) ---

@Composable
internal fun WeatherAppSettingRow(
        hasCoarseLocationPermission: Boolean,
        onRequestLocationPermission: () -> Unit,
        context: Context,
        resources: Resources,
        preferredWeatherTap: WidgetTapTarget?,
        allApps: List<AppInfo>,
        allShortcutActions: List<AppShortcutAction>,
        onPickApp: () -> Unit,
        onClear: () -> Unit,
) {
    Column {
        if (!hasCoarseLocationPermission) {
            SettingsRow(
                    label = stringResource(R.string.settings_weather_location_disabled),
                    subtitle = stringResource(R.string.settings_weather_location_disabled_subtitle),
                    onClick = onRequestLocationPermission,
                    leading = {
                        LauncherIcon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                iconSize = 24.dp,
                        )
                    },
                    trailing = {
                        FokusTextButton(onClick = onRequestLocationPermission) {
                            Text(stringResource(R.string.settings_weather_location_enable_button))
                        }
                    },
            )
        } else {
            val weatherAppLabel =
                    formatWidgetTapTarget(
                            context = context,
                            resources = resources,
                            binding = preferredWeatherTap,
                            allApps = allApps,
                            allActions = allShortcutActions,
                            emptyLabel = ::formatWeatherAppEmptyLabel,
                    )
            ShortcutTargetRow(
                    label = stringResource(R.string.settings_weather_app),
                    currentTarget = weatherAppLabel,
                    onPickApp = onPickApp,
                    onClear = onClear,
            )
        }
    }
}

// --- Swipe shortcut row ---

@Composable
internal fun ShortcutTargetRow(
        label: String,
        currentTarget: String,
        onPickApp: () -> Unit,
        onClear: () -> Unit
) {
    Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                    currentTarget,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
            )
        }
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp),
        ) {
            FokusIconButton(
                    onClick = onPickApp,
                    modifier = Modifier.size(36.dp.launcherIconDp()),
            ) {
                LauncherIcon(
                        Icons.Outlined.Edit,
                        stringResource(R.string.action_change),
                        tint = MaterialTheme.colorScheme.primary,
                        iconSize = 20.dp,
                )
            }
            FokusIconButton(
                    onClick = onClear,
                    modifier = Modifier.size(36.dp.launcherIconDp()),
            ) {
                LauncherIcon(
                        Icons.Default.Close,
                        stringResource(R.string.action_clear),
                        tint = MaterialTheme.colorScheme.error,
                        iconSize = 18.dp,
                )
            }
        }
    }
}

// =====================  DIALOGS  =====================

internal fun formatWidgetAppEmptyLabel(
        _context: Context,
        resources: Resources,
): String = resources.getString(R.string.settings_weather_app_system_default)

internal fun formatWeatherAppEmptyLabel(context: Context, resources: Resources): String {
    val hasSystemWeatherApp =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Intent(Intent.ACTION_MAIN)
                        .apply { addCategory(Intent.CATEGORY_APP_WEATHER) }
                        .resolveActivity(context.packageManager) != null
            } else {
                false
            }
    return if (hasSystemWeatherApp) {
        resources.getString(R.string.settings_weather_app_system_default)
    } else {
        resources.getString(R.string.settings_weather_app_not_configured)
    }
}

internal fun formatWidgetTapTarget(
        context: Context,
        resources: Resources,
        binding: WidgetTapTarget?,
        allApps: List<AppInfo>,
        allActions: List<AppShortcutAction>,
        emptyLabel: (Context, Resources) -> String,
): String {
    if (binding == null) return emptyLabel(context, resources)
    val resolvedLabel =
            allActions.find {
                it.target == binding.target && it.profileKey == binding.profileKey
            }?.actionLabel
    return formatShortcutTargetDisplay(
            context = context,
            target = binding.target,
            allApps = allApps,
            notSetLabel = emptyLabel(context, resources),
            resolvedLauncherActionLabel = resolvedLabel,
            profileKey = binding.profileKey,
    )
}

internal fun formatShortcutTarget(
        context: Context,
        resources: Resources,
        target: ShortcutTarget?,
        allApps: List<AppInfo>
): String {
    return formatShortcutTargetDisplay(
            context = context,
            target = target,
            allApps = allApps,
            notSetLabel = resources.getString(R.string.shortcut_target_not_set),
            resolvedLauncherActionLabel = null
    )
}
