package com.lu4p.fokuslauncher.ui.home

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.HomeAlignment
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.ui.theme.FokusLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeShortcutIconsUiTest {
    @get:Rule val compose = createComposeRule()

    private fun checkLayout(alignment: HomeAlignment, fontScale: Float) {
        val app = AppInfo("com.example.music", "Music", null)
        val shortcut = HomeShortcut(iconName = "music", target = ShortcutTarget.App(app.packageName))
        val state = mutableStateOf(HomeUiState(homeAlignment = alignment, launcherFontScale = fontScale, arcticonsPackage = "com.donnnno.arcticons"))
        val requests = java.util.concurrent.atomic.AtomicInteger()
        var clicks = 0
        val loader: suspend (AppInfo) -> android.graphics.drawable.Drawable? = {
            requests.incrementAndGet()
            ColorDrawable(AndroidColor.WHITE)
        }
        compose.setContent {
            FokusLauncherTheme(fontScale = fontScale) {
                CompositionLocalProvider(LocalHomeIconLoader provides loader) {
                    HomeScreenContent(
                        uiState = state.value, clockUiState = HomeClockUiState(), weatherUiState = HomeWeatherUiState(),
                        favorites = emptyList(), installedApps = listOf(app), rightSideShortcuts = listOf(shortcut),
                        onLabelClick = {}, onIconClick = { assertEquals(shortcut, it); clicks++ },
                    )
                }
            }
        }
        compose.waitUntil {
            compose.onAllNodesWithTag("shortcut_arcticon", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
        compose.onNodeWithTag("shortcut_arcticon", useUnmergedTree = true)
            .assertWidthIsEqualTo((32 * fontScale).dp).assertHeightIsEqualTo((32 * fontScale).dp)
        compose.onNodeWithTag("right_shortcut_icon_0")
            .assertWidthIsEqualTo((48 * fontScale).dp).performClick()
        assertEquals(1, clicks)

        compose.runOnIdle { state.value = state.value.copy(arcticonsPackage = null) }
        compose.onNodeWithTag("shortcut_custom_icon", useUnmergedTree = true)
            .assertWidthIsEqualTo((24 * fontScale).dp)
        val disabledRequests = requests.get()
        compose.runOnIdle { state.value = state.value.copy(showHomeBattery = false) }
        compose.waitForIdle()
        assertEquals(disabledRequests, requests.get())

        compose.runOnIdle { state.value = state.value.copy(arcticonsPackage = "com.donnnno.arcticons.black") }
        compose.waitUntil {
            compose.onAllNodesWithTag("shortcut_arcticon", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(disabledRequests + 1, requests.get())
        compose.waitForIdle()
        compose.onNodeWithTag("shortcut_arcticon", useUnmergedTree = true)
            .assertWidthIsEqualTo((32 * fontScale).dp)
    }

    @Test fun leftAlignedShortcutsSwitchIconsWithoutChangingClickTarget() = checkLayout(HomeAlignment.LEFT, 1f)
    @Test fun rightAlignedShortcutsUseLargerArcticons() = checkLayout(HomeAlignment.RIGHT, 1f)
    @Test fun centeredShortcutsUseLargerArcticons() = checkLayout(HomeAlignment.CENTER, 1f)
    @Test fun middleAlignedShortcutsUseLargerArcticons() = checkLayout(HomeAlignment.MIDDLE, 1f)
    @Test fun launcherScaleIsAppliedOnlyOnce() = checkLayout(HomeAlignment.LEFT, 1.5f)

    @Test
    fun installedArcticonsPackRendersRealShortcutDrawables() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val repository = com.lu4p.fokuslauncher.data.iconpack.ArcticonsIconPackRepository(context)
        org.junit.Assume.assumeTrue(repository.isArcticonsInstalled())
        val app = AppInfo("com.android.settings", "Settings", null)
        val state = mutableStateOf(HomeUiState(arcticonsPackage = repository.installedPackage.value))
        val loader: suspend (AppInfo) -> android.graphics.drawable.Drawable? = { repository.getIcon(it) }
        compose.setContent {
            FokusLauncherTheme {
                CompositionLocalProvider(LocalHomeIconLoader provides loader) {
                    HomeScreenContent(
                        uiState = state.value, clockUiState = HomeClockUiState(), weatherUiState = HomeWeatherUiState(),
                        favorites = listOf(com.lu4p.fokuslauncher.data.model.FavoriteApp("Settings", app.packageName)),
                        installedApps = listOf(app),
                        rightSideShortcuts = listOf(HomeShortcut(iconName = "settings", target = ShortcutTarget.App(app.packageName))),
                        onLabelClick = {}, onIconClick = {},
                    )
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("shortcut_arcticon", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("shortcut_arcticon", useUnmergedTree = true).assertWidthIsEqualTo(32.dp)
        saveScreenshot(context, "arcticons-enabled.png")
        compose.runOnIdle { state.value = state.value.copy(arcticonsPackage = null) }
        compose.onNodeWithTag("shortcut_custom_icon", useUnmergedTree = true).assertWidthIsEqualTo(24.dp)
        saveScreenshot(context, "arcticons-disabled.png")
    }

    private fun saveScreenshot(context: android.content.Context, name: String) {
        val directory = context.getExternalFilesDir("shortcut-icon-evidence")!!
        directory.mkdirs()
        java.io.File(directory, name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun failedLoadKeepsSelectedCustomIcon() {
        val app = AppInfo("com.example.music", "Music", null)
        compose.setContent {
            FokusLauncherTheme {
                HomeScreenContent(
                    uiState = HomeUiState(arcticonsPackage = "com.donnnno.arcticons"),
                    clockUiState = HomeClockUiState(), weatherUiState = HomeWeatherUiState(),
                    favorites = emptyList(), installedApps = listOf(app),
                    rightSideShortcuts = listOf(HomeShortcut(iconName = "music", target = ShortcutTarget.App(app.packageName))),
                    onLabelClick = {}, onIconClick = {},
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("shortcut_custom_icon", useUnmergedTree = true).assertWidthIsEqualTo(24.dp)
    }
}
