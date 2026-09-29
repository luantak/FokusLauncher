package com.lu4p.fokuslauncher.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.ui.components.NoteWidget
import com.lu4p.fokuslauncher.ui.theme.FokusLauncherTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeContentLayoutTest {
    @get:Rule val composeTestRule = createComposeRule()

    private fun showHome(outlined: Boolean, fontScale: Float) {
        composeTestRule.setContent {
            FokusLauncherTheme(fontScale = fontScale) {
                HomeContentLayout(modifier = Modifier.size(width = 320.dp, height = 400.dp)) {
                    Box(Modifier.fillMaxWidth().height(80.dp).testTag("header"))
                    NoteWidget(
                            text = (1..1000).joinToString("\n") { "A long note, line $it" },
                            outlined = outlined,
                            modifier = Modifier.layoutId(HomeContentSlot.Note).testTag("preview"),
                    )
                    Box(Modifier.fillMaxWidth().height(80.dp).testTag("controls"))
                    Spacer(Modifier.layoutId(HomeContentSlot.Gap))
                    Box(Modifier.fillMaxWidth().height(160.dp)
                            .layoutId(HomeContentSlot.Favorites).testTag("favorites"))
                }
            }
        }
        val header = composeTestRule.onNodeWithTag("header").fetchSemanticsNode().boundsInRoot
        val note = composeTestRule.onNodeWithTag("preview").fetchSemanticsNode().boundsInRoot
        val controls = composeTestRule.onNodeWithTag("controls").fetchSemanticsNode().boundsInRoot
        val favorites = composeTestRule.onNodeWithTag("favorites").fetchSemanticsNode().boundsInRoot
        assertTrue(note.top >= header.bottom)
        assertTrue(note.bottom <= controls.top)
        assertTrue(controls.bottom < favorites.top)
    }

    @Test fun longPreviewReservesSpaceForControlsAndFavorites() = showHome(false, 1f)

    @Test fun largeTextPreviewReservesSpaceForControlsAndFavorites() = showHome(false, 2f)

    @Test fun outlinedPreviewReservesSpaceForControlsAndFavorites() = showHome(true, 2f)
}
