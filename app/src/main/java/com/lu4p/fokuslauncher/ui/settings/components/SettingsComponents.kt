package com.lu4p.fokuslauncher.ui.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
                enabled = enabled
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
    Row(verticalAlignment = Alignment.CenterVertically, modifier = rowModifier) {
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
private fun settingsPickerOutlinedFieldColors() =
        OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                disabledTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f),
                disabledBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f),
                focusedTrailingIconColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTrailingIconColor = MaterialTheme.colorScheme.onBackground,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                cursorColor = MaterialTheme.colorScheme.primary,
        )

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
        menuContent: @Composable ColumnScope.() -> Unit
) {
    ExposedDropdownMenuBox(
            expanded = menuExpanded,
            onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
                modifier =
                        textFieldModifier
                                .menuAnchor(
                                        ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                        enabled = fieldEnabled
                                )
                                .fillMaxWidth(),
                value = selectedDisplayText,
                onValueChange = { _ -> },
                readOnly = true,
                enabled = fieldEnabled,
                singleLine = true,
                shape = SettingsPickerCorner,
                textStyle = textStyle,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded)
                },
                colors = settingsPickerOutlinedFieldColors()
        )
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
    ) {
        options.forEach { option ->
            DropdownMenuItem(
                    text = { itemContent(option) },
                    onClick =
                            rememberClickWithSystemSound {
                                onItemSelected(option)
                                onExpandedChange(false)
                            },
                    colors = settingsPickerMenuItemColors(),
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
            )
        }
    }
}
