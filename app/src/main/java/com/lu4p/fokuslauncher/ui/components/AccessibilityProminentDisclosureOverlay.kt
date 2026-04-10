package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R

private val DisclosureBackground = Color.Black
private val DisclosureOnBackground = Color.White

/**
 * Stand-alone, in-app disclosure for AccessibilityService use (Google Play User Data policy).
 * Not bundled with privacy policy or other data-collection notices.
 *
 * Uses an opaque background because [FokusLauncherTheme] sets Material backgrounds to transparent.
 */
@Composable
fun AccessibilityProminentDisclosureOverlay(
        onAccept: () -> Unit,
        onNotNow: () -> Unit,
        modifier: Modifier = Modifier,
) {
    var checked by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    Box(
            modifier =
                    modifier
                            .fillMaxSize()
                            .background(DisclosureBackground),
    ) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                                .verticalScroll(scroll)
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                    text = stringResource(R.string.accessibility_prominent_disclosure_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = DisclosureOnBackground,
            )
            Text(
                    text = stringResource(R.string.accessibility_prominent_disclosure_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DisclosureOnBackground,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        colors =
                                CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = DisclosureOnBackground.copy(alpha = 0.7f),
                                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                )
                Text(
                        text = stringResource(R.string.accessibility_prominent_disclosure_checkbox),
                        style = MaterialTheme.typography.bodyMedium,
                        color = DisclosureOnBackground,
                        modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                    onClick = onAccept,
                    enabled = checked,
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                            ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color.White.copy(alpha = 0.14f),
                                    disabledContentColor = Color.White.copy(alpha = 0.45f),
                            ),
            ) {
                Text(
                        text = stringResource(R.string.accessibility_prominent_disclosure_continue),
                        style = MaterialTheme.typography.labelLarge,
                        color =
                                if (checked) {
                                    Color.Black
                                } else {
                                    Color.White.copy(alpha = 0.45f)
                                },
                )
            }
            FokusTextButton(
                    onClick = onNotNow,
                    modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                        stringResource(R.string.accessibility_prominent_disclosure_not_now),
                        color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
