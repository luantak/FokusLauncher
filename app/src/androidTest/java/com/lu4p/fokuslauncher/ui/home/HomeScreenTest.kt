package com.lu4p.fokuslauncher.ui.home

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.WeatherData
import com.lu4p.fokuslauncher.ui.theme.FokusLauncherTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testFavorites =
            listOf(
                    FavoriteApp(label = "Music", packageName = "com.lu4p.music", iconName = "music"),
                    FavoriteApp(label = "Work", packageName = "com.lu4p.work", iconName = "work"),
                    FavoriteApp(label = "Read", packageName = "com.lu4p.reader", iconName = "read"),
                    FavoriteApp(label = "Social", packageName = "com.lu4p.social", iconName = "chat"),
                    FavoriteApp(label = "Health", packageName = "com.lu4p.health", iconName = "fitness"),
                    FavoriteApp(label = "Finance", packageName = "com.lu4p.finance", iconName = "finance")
            )
    private val testRightSideShortcuts =
            testFavorites.map { HomeShortcut(iconName = it.iconName, target = it.resolvedIconTarget) }

    private fun clock(
            time: String = "3:56",
            date: String = "Fri. 12 Jul.",
            battery: Int = 88
    ) = HomeClockUiState(currentTime = time, currentDate = date, batteryPercent = battery)

    private val weatherOff = HomeWeatherUiState()

    @Test
    fun homeScreen_displaysClockWidget() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                HomeScreenContent(
                        uiState = HomeUiState(),
                        clockUiState = clock(),
                        weatherUiState = weatherOff,
                        favorites = testFavorites,
                        rightSideShortcuts = testRightSideShortcuts,
                        onLabelClick = {},
                        onLabelLongPress = {},
                        onIconClick = {}
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
                        uiState = HomeUiState(),
                        clockUiState = clock(),
                        weatherUiState = weatherOff,
                        favorites = testFavorites,
                        rightSideShortcuts = testRightSideShortcuts,
                        onLabelClick = {},
                        onLabelLongPress = {},
                        onIconClick = {}
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
                        uiState = HomeUiState(),
                        clockUiState = clock(),
                        weatherUiState = weatherOff,
                        favorites = testFavorites,
                        rightSideShortcuts = testRightSideShortcuts,
                        onLabelClick = {},
                        onLabelLongPress = {},
                        onIconClick = {}
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
                        uiState = HomeUiState(),
                        clockUiState =
                                clock(time = "12:00", date = "Mon. 1 Jan.", battery = 100),
                        weatherUiState = weatherOff,
                        favorites = emptyList(),
                        rightSideShortcuts = emptyList(),
                        onLabelClick = {},
                        onLabelLongPress = {},
                        onIconClick = {}
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
                        uiState = HomeUiState(),
                        clockUiState =
                                clock(time = "0:00", date = "Sun. 31 Dec.", battery = 0),
                        weatherUiState = weatherOff,
                        favorites = emptyList(),
                        rightSideShortcuts = emptyList(),
                        onLabelClick = {},
                        onLabelLongPress = {},
                        onIconClick = {}
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
                        uiState = HomeUiState(isDefaultLauncher = false),
                        clockUiState = clock(),
                        weatherUiState = weatherOff,
                        favorites = testFavorites,
                        rightSideShortcuts = testRightSideShortcuts,
                        onLabelClick = {},
                        onLabelLongPress = {},
                        onIconClick = {}
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
                        uiState = HomeUiState(isDefaultLauncher = true),
                        clockUiState = clock(),
                        weatherUiState = weatherOff,
                        favorites = testFavorites,
                        rightSideShortcuts = testRightSideShortcuts,
                        onLabelClick = {},
                        onLabelLongPress = {},
                        onIconClick = {}
                )
            }
        }

        composeTestRule.onAllNodesWithTag("set_default_launcher_button").assertCountEquals(0)
    }

    @Test
    fun homeScreen_weatherWidget_showsCelsiusAndHandlesClick() {
        var weatherClicked = false
        composeTestRule.setContent {
            FokusLauncherTheme {
                HomeScreenContent(
                        uiState = HomeUiState(showHomeWeather = true),
                        clockUiState = clock(),
                        weatherUiState =
                                HomeWeatherUiState(
                                        weather = WeatherData(temperature = 22, iconCode = "01d"),
                                        showWeatherWidget = true
                                ),
                        favorites = testFavorites,
                        rightSideShortcuts = testRightSideShortcuts,
                        onLabelClick = {},
                        onLabelLongPress = {},
                        onIconClick = {},
                        onWeatherClick = { weatherClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("weather_widget").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("22°C").assertIsDisplayed()
        assertTrue(weatherClicked)
    }

    @Test
    fun homeScreen_weatherWidget_showsFahrenheitWhenRequested() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                HomeScreenContent(
                        uiState = HomeUiState(showHomeWeather = true),
                        clockUiState = clock(),
                        weatherUiState =
                                HomeWeatherUiState(
                                        weather = WeatherData(temperature = 72, iconCode = "01d"),
                                        weatherUseFahrenheit = true,
                                        showWeatherWidget = true
                                ),
                        favorites = testFavorites,
                        rightSideShortcuts = testRightSideShortcuts,
                        onLabelClick = {},
                        onLabelLongPress = {},
                        onIconClick = {},
                        onWeatherClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("weather_widget").assertIsDisplayed()
        composeTestRule.onNodeWithText("72°F").assertIsDisplayed()
    }

    @Test
    fun homeScreen_hidesAllHomeInfo_whenAllItemTogglesOff() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                HomeScreenContent(
                        uiState =
                                HomeUiState(
                                        showHomeClock = false,
                                        showHomeDate = false,
                                        showHomeWeather = false,
                                        showHomeBattery = false
                                ),
                        clockUiState = clock(),
                        weatherUiState =
                                HomeWeatherUiState(
                                        weather = WeatherData(temperature = 22, iconCode = "01d"),
                                        showWeatherWidget = true
                                ),
                        favorites = testFavorites,
                        rightSideShortcuts = testRightSideShortcuts,
                        onLabelClick = {},
                        onLabelLongPress = {},
                        onIconClick = {}
                )
            }
        }

        composeTestRule.onAllNodesWithTag("clock_widget").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("date_battery_row").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("weather_widget").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Fri. 12 Jul.").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("88%").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("22°C").assertCountEquals(0)
    }
}
