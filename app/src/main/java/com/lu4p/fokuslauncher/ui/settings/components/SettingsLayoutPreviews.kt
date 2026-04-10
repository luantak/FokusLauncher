package com.lu4p.fokuslauncher.ui.settings.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.lu4p.fokuslauncher.ui.theme.FokusLauncherTheme

@Preview(name = "Toggle row subtitle wraps", fontScale = 1.5f, showBackground = true)
@Composable
private fun PreviewSettingsToggleRowLargeFont() {
    FokusLauncherTheme {
        SettingsToggleRow(
                label = "Example setting with a long label that may wrap on smaller widths",
                subtitle = "Secondary text that also grows with font scale",
                checked = true,
                onCheckedChange = {},
        )
    }
}

@Preview(name = "Settings row multi-line", fontScale = 1.5f, showBackground = true)
@Composable
private fun PreviewSettingsRowLargeFont() {
    FokusLauncherTheme {
        SettingsRow(
                label = "Open another screen from settings",
                subtitle = "This subtitle should stay aligned when text is large",
                onClick = {},
        )
    }
}

@Preview(name = "Dropdown label", fontScale = 1.5f, showBackground = true)
@Composable
private fun PreviewSettingsLabeledDropdownLargeFont() {
    FokusLauncherTheme {
        SettingsLabeledDropdown(
                title = "Font size",
                subtitle = "Stacks with your device font size",
                expanded = false,
                onExpandedChange = {},
                selectedDisplayText = "1.0x",
        ) {
            androidx.compose.material3.DropdownMenuItem(
                    text = { Text("1.0x", color = MaterialTheme.colorScheme.onBackground) },
                    onClick = {},
            )
        }
    }
}
