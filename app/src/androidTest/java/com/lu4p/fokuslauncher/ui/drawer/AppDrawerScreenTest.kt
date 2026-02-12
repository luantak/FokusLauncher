package com.lu4p.fokuslauncher.ui.drawer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.ui.theme.FokusLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppDrawerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testApps = listOf(
        AppInfo("com.lu4p.atom", "Atom", null),
        AppInfo("com.lu4p.calculator", "Calculator", null),
        AppInfo("com.lu4p.calendar", "Calendar", null),
        AppInfo("com.lu4p.camera", "Camera", null),
        AppInfo("com.lu4p.chrome", "Chrome", null),
        AppInfo("com.lu4p.gmail", "Gmail", null),
        AppInfo("com.lu4p.maps", "Maps", null)
    )

    @Test
    fun appDrawer_displaysAppList() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                AppDrawerContent(
                    uiState = AppDrawerUiState(
                        allApps = testApps,
                        filteredApps = testApps
                    ),
                    onSearchQueryChanged = {},
                    onCategorySelected = {},
                    onAppClick = {},
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Atom").assertIsDisplayed()
        composeTestRule.onNodeWithText("Calculator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chrome").assertIsDisplayed()
    }

    @Test
    fun appDrawer_displaysSearchBar() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                AppDrawerContent(
                    uiState = AppDrawerUiState(
                        allApps = testApps,
                        filteredApps = testApps
                    ),
                    onSearchQueryChanged = {},
                    onCategorySelected = {},
                    onAppClick = {},
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("search_bar").assertIsDisplayed()
    }

    @Test
    fun appDrawer_displaysCategoryChips() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                AppDrawerContent(
                    uiState = AppDrawerUiState(
                        allApps = testApps,
                        filteredApps = testApps
                    ),
                    onSearchQueryChanged = {},
                    onCategorySelected = {},
                    onAppClick = {},
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("All apps").assertIsDisplayed()
        composeTestRule.onNodeWithText("Productivity").assertIsDisplayed()
    }

    @Test
    fun appDrawer_searchFiltersCallback() {
        var capturedQuery = ""

        composeTestRule.setContent {
            FokusLauncherTheme {
                AppDrawerContent(
                    uiState = AppDrawerUiState(
                        allApps = testApps,
                        filteredApps = testApps
                    ),
                    onSearchQueryChanged = { capturedQuery = it },
                    onCategorySelected = {},
                    onAppClick = {},
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("search_bar").performTextInput("cal")
        assertEquals("cal", capturedQuery)
    }

    @Test
    fun appDrawer_filteredResults_showsOnlyMatchingApps() {
        val filteredApps = testApps.filter { it.label.contains("Cal", ignoreCase = true) }

        composeTestRule.setContent {
            FokusLauncherTheme {
                AppDrawerContent(
                    uiState = AppDrawerUiState(
                        allApps = testApps,
                        filteredApps = filteredApps,
                        searchQuery = "cal"
                    ),
                    onSearchQueryChanged = {},
                    onCategorySelected = {},
                    onAppClick = {},
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Calculator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Calendar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chrome").assertDoesNotExist()
        composeTestRule.onNodeWithText("Atom").assertDoesNotExist()
    }

    @Test
    fun appDrawer_categoryChipClick_triggersCallback() {
        var selectedCategory = ""

        composeTestRule.setContent {
            FokusLauncherTheme {
                AppDrawerContent(
                    uiState = AppDrawerUiState(
                        allApps = testApps,
                        filteredApps = testApps
                    ),
                    onSearchQueryChanged = {},
                    onCategorySelected = { selectedCategory = it },
                    onAppClick = {},
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Social").performClick()
        assertEquals("Social", selectedCategory)
    }

    @Test
    fun appDrawer_appClick_triggersCallback() {
        var clickedTarget: LaunchTarget? = null

        composeTestRule.setContent {
            FokusLauncherTheme {
                AppDrawerContent(
                    uiState = AppDrawerUiState(
                        allApps = testApps,
                        filteredApps = testApps
                    ),
                    onSearchQueryChanged = {},
                    onCategorySelected = {},
                    onAppClick = { clickedTarget = it },
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Chrome").performClick()
        assertEquals(LaunchTarget.MainApp("com.lu4p.chrome"), clickedTarget)
    }

    @Test
    fun appDrawer_settingsButton_isDisplayed() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                AppDrawerContent(
                    uiState = AppDrawerUiState(
                        allApps = testApps,
                        filteredApps = testApps
                    ),
                    onSearchQueryChanged = {},
                    onCategorySelected = {},
                    onAppClick = {},
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_button").assertIsDisplayed()
    }

    @Test
    fun appDrawer_emptyAppList_showsNoItems() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                AppDrawerContent(
                    uiState = AppDrawerUiState(
                        allApps = emptyList(),
                        filteredApps = emptyList()
                    ),
                    onSearchQueryChanged = {},
                    onCategorySelected = {},
                    onAppClick = {},
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("app_list").assertIsDisplayed()
        composeTestRule.onNodeWithText("Atom").assertDoesNotExist()
    }
}
