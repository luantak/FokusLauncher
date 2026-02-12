package com.lu4p.fokuslauncher.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.WeatherData
import com.lu4p.fokuslauncher.ui.theme.FokusLauncherTheme
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testFavorites = listOf(
        FavoriteApp(label = "Music", packageName = "com.lu4p.music", iconName = "music"),
        FavoriteApp(label = "Work", packageName = "com.lu4p.work", iconName = "work"),
        FavoriteApp(label = "Read", packageName = "com.lu4p.reader", iconName = "read"),
        FavoriteApp(label = "Social", packageName = "com.lu4p.social", iconName = "chat"),
        FavoriteApp(label = "Health", packageName = "com.lu4p.health", iconName = "fitness"),
        FavoriteApp(label = "Finance", packageName = "com.lu4p.finance", iconName = "finance")
    )
    private val testRightSideShortcuts = testFavorites.map {
        HomeShortcut(iconName = it.iconName, target = it.resolvedIconTarget)
    }

    @Test
    fun homeScreen_displaysClockWidget() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                HomeScreenContent(
                    uiState = HomeUiState(
                        currentTime = "3:56",
                        currentDate = "Fri. 12 Jul.",
                        batteryPercent = 88
                    ),
                    favorites = testFavorites,
                    rightSideShortcuts = testRightSideShortcuts,
                    onLabelClick = {},
                    onLabelLongPress = {},
                    onIconClick = {},
                    onSwipeUp = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("clock_widget").assertIsDisplayed()
        composeTestRule.onNodeWithText("3:56").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysDateAndBattery() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                HomeScreenContent(
                    uiState = HomeUiState(
                        currentTime = "3:56",
                        currentDate = "Fri. 12 Jul.",
                        batteryPercent = 88
                    ),
                    favorites = testFavorites,
                    rightSideShortcuts = testRightSideShortcuts,
                    onLabelClick = {},
                    onLabelLongPress = {},
                    onIconClick = {},
                    onSwipeUp = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("date_battery_row").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fri. 12 Jul.").assertIsDisplayed()
        composeTestRule.onNodeWithText("88%").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysFavoriteApps() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                HomeScreenContent(
                    uiState = HomeUiState(
                        currentTime = "3:56",
                        currentDate = "Fri. 12 Jul.",
                        batteryPercent = 88
                    ),
                    favorites = testFavorites,
                    rightSideShortcuts = testRightSideShortcuts,
                    onLabelClick = {},
                    onLabelLongPress = {},
                    onIconClick = {},
                    onSwipeUp = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Music").assertIsDisplayed()
        composeTestRule.onNodeWithText("Work").assertIsDisplayed()
        composeTestRule.onNodeWithText("Read").assertIsDisplayed()
        composeTestRule.onNodeWithText("Social").assertIsDisplayed()
        composeTestRule.onNodeWithText("Health").assertIsDisplayed()
        composeTestRule.onNodeWithText("Finance").assertIsDisplayed()
    }

    @Test
    fun homeScreen_emptyFavorites_showsNoItems() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                HomeScreenContent(
                    uiState = HomeUiState(
                        currentTime = "12:00",
                        currentDate = "Mon. 1 Jan.",
                        batteryPercent = 100
                    ),
                    favorites = emptyList(),
                    rightSideShortcuts = emptyList(),
                    onLabelClick = {},
                    onLabelLongPress = {},
                    onIconClick = {},
                    onSwipeUp = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("favorites_list").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clock_widget").assertIsDisplayed()
    }

    @Test
    fun homeScreen_zeroBattery_displaysCorrectly() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                HomeScreenContent(
                    uiState = HomeUiState(
                        currentTime = "0:00",
                        currentDate = "Sun. 31 Dec.",
                        batteryPercent = 0
                    ),
                    favorites = emptyList(),
                    rightSideShortcuts = emptyList(),
                    onLabelClick = {},
                    onLabelLongPress = {},
                    onIconClick = {},
                    onSwipeUp = {}
                )
            }
        }

        composeTestRule.onNodeWithText("0%").assertIsDisplayed()
        composeTestRule.onNodeWithText("0:00").assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsDefaultLauncherBanner_whenNotDefault() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                HomeScreenContent(
                    uiState = HomeUiState(
                        currentTime = "3:56",
                        currentDate = "Fri. 12 Jul.",
                        batteryPercent = 88,
                        isDefaultLauncher = false
                    ),
                    favorites = testFavorites,
                    rightSideShortcuts = testRightSideShortcuts,
                    onLabelClick = {},
                    onLabelLongPress = {},
                    onIconClick = {},
                    onSwipeUp = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("set_default_launcher_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Set as default launcher").assertIsDisplayed()
    }

    @Test
    fun homeScreen_hidesDefaultLauncherBanner_whenIsDefault() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                HomeScreenContent(
                    uiState = HomeUiState(
                        currentTime = "3:56",
                        currentDate = "Fri. 12 Jul.",
                        batteryPercent = 88,
                        isDefaultLauncher = true
                    ),
                    favorites = testFavorites,
                    rightSideShortcuts = testRightSideShortcuts,
                    onLabelClick = {},
                    onLabelLongPress = {},
                    onIconClick = {},
                    onSwipeUp = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("set_default_launcher_button").assertDoesNotExist()
    }

    @Test
    fun homeScreen_weatherWidget_showsCelsiusAndHandlesClick() {
        var weatherClicked = false
        composeTestRule.setContent {
            FokusLauncherTheme {
                HomeScreenContent(
                    uiState = HomeUiState(
                        currentTime = "3:56",
                        currentDate = "Fri. 12 Jul.",
                        batteryPercent = 88,
                        weather = WeatherData(temperature = 22, iconCode = "01d"),
                        showWeatherWidget = true
                    ),
                    favorites = testFavorites,
                    rightSideShortcuts = testRightSideShortcuts,
                    onLabelClick = {},
                    onLabelLongPress = {},
                    onIconClick = {},
                    onSwipeUp = {},
                    onWeatherClick = { weatherClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("weather_widget").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("22°C").assertIsDisplayed()
        assertTrue(weatherClicked)
    }
}
