package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.ui.home.HomeWidgetAlignment
import com.lu4p.fokuslauncher.ui.theme.FokusLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MediaWidgetLayoutTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun centeredAlignmentCentersTransportControls() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                MediaWidget(
                    title = "Track",
                    artist = "Artist",
                    isPlaying = true,
                    canSkipToPrevious = true,
                    canSkipToNext = true,
                    alignment = HomeWidgetAlignment.CENTER,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        val widget = composeTestRule.onNodeWithTag("media_widget").getUnclippedBoundsInRoot()
        val previous = composeTestRule.onNodeWithTag("media_previous").getUnclippedBoundsInRoot()
        val next = composeTestRule.onNodeWithTag("media_next").getUnclippedBoundsInRoot()
        val controlsCenter = (previous.left + next.right) / 2f

        val widgetCenter = (widget.left + widget.right) / 2f
        assertEquals(widgetCenter.value, controlsCenter.value, 1f)
    }

    @Test
    fun largeLauncherFontScaleScalesControlIconsOnce() {
        composeTestRule.setContent {
            FokusLauncherTheme(fontScale = 1.5f) {
                MediaWidget(
                    title = "Track",
                    artist = null,
                    isPlaying = false,
                    canSkipToPrevious = true,
                    canSkipToNext = true,
                )
            }
        }

        val icon = composeTestRule.onNodeWithTag("media_previous").getUnclippedBoundsInRoot()

        val iconWidth = icon.right - icon.left
        assertEquals(36f, iconWidth.value, 1f)
        assertTrue(iconWidth < 40.dp)
    }
}
