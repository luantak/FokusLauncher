package com.lu4p.fokuslauncher.ui.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.data.model.LauncherFontScale
import com.lu4p.fokuslauncher.ui.components.LauncherIcon
import com.lu4p.fokuslauncher.ui.theme.LocalLauncherFontScale
import com.lu4p.fokuslauncher.ui.theme.LocalLauncherIconGlow
import com.lu4p.fokuslauncher.ui.theme.withoutLauncherTextGlow
import com.lu4p.fokuslauncher.ui.util.clickableWithSystemSound
import com.lu4p.fokuslauncher.ui.util.rememberBooleanChangeWithSystemSound
import com.lu4p.fokuslauncher.ui.util.rememberClickWithSystemSound

private val SettingsPickerCorner = RoundedCornerShape(12.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsToggleRow(
        label: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        subtitle: String? = null,
        enabled: Boolean = true
) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    Modifier.fillMaxWidth()
                            .clickableWithSystemSound(enabled = enabled) { onCheckedChange(!checked) }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color =
                            if (enabled) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f)
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(
                checked = checked,
                onCheckedChange = rememberBooleanChangeWithSystemSound(onCheckedChange),
                enabled = enabled,
        )
    }
}

@Composable
internal fun SettingsRow(
        label: String,
        modifier: Modifier = Modifier,
        subtitle: String? = null,
        horizontalPadding: Dp = 24.dp,
        verticalPadding: Dp = 12.dp,
        labelStyle: TextStyle = MaterialTheme.typography.bodyLarge,
        subtitleStyle: TextStyle = MaterialTheme.typography.labelSmall,
        labelColor: Color = MaterialTheme.colorScheme.onBackground,
        subtitleColor: Color = MaterialTheme.colorScheme.secondary,
        leading: (@Composable RowScope.() -> Unit)? = null,
        trailing: (@Composable RowScope.() -> Unit)? = null,
        onClick: (() -> Unit)? = null,
        clickableEnabled: Boolean = true,
) {
    val padded =
            Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = verticalPadding)
    val rowModifier =
            modifier.then(
                    if (onClick != null) {
                        padded.clickableWithSystemSound(
                                enabled = clickableEnabled,
                                onClick = onClick,
                        )
                    } else {
                        padded
                    }
            )
    Row(verticalAlignment = Alignment.Top, modifier = rowModifier) {
        leading?.invoke(this)
        if (leading != null) {
            Spacer(Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = labelStyle, color = labelColor)
            if (!subtitle.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(text = subtitle, style = subtitleStyle, color = subtitleColor)
            }
        }
        trailing?.invoke(this)
    }
}

@Composable
private fun settingsPickerMenuItemColors() =
        MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.onBackground,
                leadingIconColor = MaterialTheme.colorScheme.onBackground,
                trailingIconColor = MaterialTheme.colorScheme.onBackground,
        )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsReadOnlyExposedDropdown(
        expanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
        selectedDisplayText: String,
        fieldEnabled: Boolean = true,
        menuExpanded: Boolean = expanded,
        textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
        textFieldModifier: Modifier = Modifier,
        fieldTextColor: Color? = null,
        menuContent: @Composable ColumnScope.() -> Unit
) {
    val launcherGlowEnabled = LocalLauncherIconGlow.current.enabled
    // Match a standard single-line outlined field (~56dp); typography already scales with font
    // size — avoid multiplying shell dp by [LocalLauncherFontScale] or the bar becomes very tall.
    val fieldHeight = if (launcherGlowEnabled) 58.dp else 56.dp
    val fieldHorizontalPadding = PaddingValues(start = 16.dp, end = 4.dp)
    val selectedTextGlowPadding = if (launcherGlowEnabled) 2.dp else 0.dp
    val resolvedTextStyle =
            if (launcherGlowEnabled) textStyle else textStyle.withoutLauncherTextGlow()
    val outlineColor =
            when {
                !fieldEnabled -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f)
                menuExpanded -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f)
            }
    val outlineWidth = if (menuExpanded && fieldEnabled) 2.dp else 1.dp
    ExposedDropdownMenuBox(
            expanded = menuExpanded,
            onExpandedChange = onExpandedChange
    ) {
        // Plain Text (not OutlinedTextField) so soft text shadow / glow is not clipped to a rect.
        Box(
                modifier =
                        textFieldModifier
                                .menuAnchor(
                                        ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                        enabled = fieldEnabled
                                )
                                .fillMaxWidth()
        ) {
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .height(fieldHeight)
                                    .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            SettingsPickerCorner,
                                    )
                                    .border(
                                            BorderStroke(outlineWidth, outlineColor),
                                            SettingsPickerCorner,
                                    )
                                    .padding(fieldHorizontalPadding),
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                        text = selectedDisplayText,
                        modifier =
                                Modifier.weight(1f)
                                        .padding(vertical = selectedTextGlowPadding),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = resolvedTextStyle,
                        color =
                                when {
                                    fieldTextColor != null && fieldEnabled -> fieldTextColor
                                    fieldTextColor != null -> fieldTextColor.copy(alpha = 0.55f)
                                    !fieldEnabled ->
                                            MaterialTheme.colorScheme.onBackground.copy(
                                                    alpha = 0.55f
                                            )
                                    else -> Color.Unspecified
                                },
                )
                IconButton(
                        onClick = { onExpandedChange(!menuExpanded) },
                        enabled = fieldEnabled,
                ) {
                    LauncherIcon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            iconSize = 24.dp,
                            tint =
                                    if (fieldEnabled) MaterialTheme.colorScheme.onBackground
                                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                    )
                }
            }
        }
        ExposedDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onExpandedChange(false) },
                shape = SettingsPickerCorner,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
                border =
                        BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f)
                        ),
        ) {
            menuContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsLabeledDropdown(
        title: String,
        subtitle: String? = null,
        expanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
        selectedDisplayText: String,
        fieldEnabled: Boolean = true,
        menuExpanded: Boolean = expanded,
        textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
        textFieldModifier: Modifier = Modifier,
        fieldTextColor: Color? = null,
        menuContent: @Composable ColumnScope.() -> Unit,
) {
    Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
            )
        }
        Spacer(Modifier.height(12.dp))
        SettingsReadOnlyExposedDropdown(
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                selectedDisplayText = selectedDisplayText,
                fieldEnabled = fieldEnabled,
                menuExpanded = menuExpanded,
                textStyle = textStyle,
                textFieldModifier = textFieldModifier,
                fieldTextColor = fieldTextColor,
                menuContent = menuContent,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> SettingsDropdown(
        title: String,
        subtitle: String? = null,
        options: List<T>,
        expanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
        selectedDisplayText: String,
        fieldEnabled: Boolean = true,
        menuExpanded: Boolean = expanded,
        textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
        textFieldModifier: Modifier = Modifier,
        fieldTextColor: Color? = null,
        menuItemTextColor: ((T) -> Color)? = null,
        itemContent: @Composable (T) -> Unit,
        onItemSelected: (T) -> Unit,
) {
    SettingsLabeledDropdown(
            title = title,
            subtitle = subtitle,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            selectedDisplayText = selectedDisplayText,
            fieldEnabled = fieldEnabled,
            menuExpanded = menuExpanded,
            textStyle = textStyle,
            textFieldModifier = textFieldModifier,
            fieldTextColor = fieldTextColor,
    ) {
        val menuGlowEnabled = LocalLauncherIconGlow.current.enabled
        val menuFontScale =
                LocalLauncherFontScale.current.coerceIn(LauncherFontScale.MIN, LauncherFontScale.MAX)
        val menuItemVerticalPadding =
                ((if (menuGlowEnabled) 14f else 8f) * menuFontScale).dp
        val menuItemHorizontalPadding = (16f * menuFontScale).dp
        val menuItemContentPadding =
                PaddingValues(
                        horizontal = menuItemHorizontalPadding,
                        vertical = menuItemVerticalPadding,
                )
        options.forEach { option ->
            val itemColors =
                    if (menuItemTextColor != null) {
                        val c = menuItemTextColor(option)
                        MenuDefaults.itemColors(
                                textColor = c,
                                leadingIconColor = c,
                                trailingIconColor = c,
                        )
                    } else {
                        settingsPickerMenuItemColors()
                    }
            DropdownMenuItem(
                    text = { itemContent(option) },
                    onClick =
                            rememberClickWithSystemSound {
                                if (!fieldEnabled) return@rememberClickWithSystemSound
                                onItemSelected(option)
                                onExpandedChange(false)
                            },
                    colors = itemColors,
                    contentPadding = menuItemContentPadding,
            )
        }
    }
}
