package com.lu4p.fokuslauncher.ui.drawer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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

    private fun singleProfileState(
        apps: List<AppInfo>,
        searchQuery: String = "",
        autoOpenKeyboard: Boolean = true
    ) = AppDrawerUiState(
        allApps = apps,
        filteredProfileSections =
            listOf(DrawerProfileSectionUi(id = "owner", title = "Personal", apps = apps)),
        searchQuery = searchQuery,
        autoOpenKeyboard = autoOpenKeyboard,
        categories = listOf("All apps", "Productivity", "Social")
    )

    @Test
    fun appDrawer_displaysAppList() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                AppDrawerContent(
                    uiState = singleProfileState(testApps),
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
                    uiState = singleProfileState(testApps),
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
    fun appDrawer_keepsSearchBarVisibleWhenAutoOpenDisabled() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                AppDrawerContent(
                    uiState =
                        singleProfileState(testApps, autoOpenKeyboard = false),
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
                    uiState = singleProfileState(testApps),
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
                    uiState = singleProfileState(testApps),
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
                    uiState =
                        singleProfileState(testApps, searchQuery = "cal").copy(
                            filteredProfileSections =
                                listOf(
                                    DrawerProfileSectionUi(
                                        id = "owner",
                                        title = "Personal",
                                        apps = filteredApps
                                    )
                                )
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
        composeTestRule.onAllNodesWithText("Chrome").fetchSemanticsNodes().also {
            assertEquals(0, it.size)
        }
        composeTestRule.onAllNodesWithText("Atom").fetchSemanticsNodes().also {
            assertEquals(0, it.size)
        }
    }

    @Test
    fun appDrawer_categoryChipClick_triggersCallback() {
        var selectedCategory = ""

        composeTestRule.setContent {
            FokusLauncherTheme {
                AppDrawerContent(
                    uiState = singleProfileState(testApps),
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
                    uiState = singleProfileState(testApps),
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
                    uiState = singleProfileState(testApps),
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
                    uiState =
                        AppDrawerUiState(
                            allApps = emptyList(),
                            filteredProfileSections = emptyList()
                        ),
                    onSearchQueryChanged = {},
                    onCategorySelected = {},
                    onAppClick = {},
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("app_list").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Atom").fetchSemanticsNodes().also {
            assertEquals(0, it.size)
        }
    }

    @Test
    fun appDrawer_hidesProfileHeader_whenOnlyPersonalSectionHasApps() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                AppDrawerContent(
                    uiState = singleProfileState(testApps),
                    onSearchQueryChanged = {},
                    onCategorySelected = {},
                    onAppClick = {},
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onAllNodesWithText("PERSONAL").fetchSemanticsNodes().also {
            assertEquals(0, it.size)
        }
    }

    @Test
    fun appDrawer_showsNonOwnerProfileLabel_whenPersonalAndWorkSectionsHaveApps() {
        val personalApps = listOf(AppInfo("com.lu4p.atom", "Atom", null))
        val workApps = listOf(AppInfo("com.work.slack", "Slack", null))

        composeTestRule.setContent {
            FokusLauncherTheme {
                AppDrawerContent(
                    uiState =
                        AppDrawerUiState(
                            allApps = personalApps + workApps,
                            filteredProfileSections =
                                listOf(
                                    DrawerProfileSectionUi(
                                        id = "owner",
                                        title = "Personal",
                                        apps = personalApps
                                    ),
                                    DrawerProfileSectionUi(
                                        id = "u_1",
                                        title = "Work profile",
                                        apps = workApps
                                    )
                                ),
                            categories = listOf("All apps", "Productivity", "Social")
                        ),
                    onSearchQueryChanged = {},
                    onCategorySelected = {},
                    onAppClick = {},
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onAllNodesWithText("PERSONAL").fetchSemanticsNodes().also {
            assertEquals(0, it.size)
        }
        composeTestRule.onNodeWithText("WORK PROFILE").assertIsDisplayed()
    }
}
