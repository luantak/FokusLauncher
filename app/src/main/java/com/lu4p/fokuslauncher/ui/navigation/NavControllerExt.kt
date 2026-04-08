package com.lu4p.fokuslauncher.ui.navigation

import androidx.navigation.NavController

fun NavController.navigateSingleTop(route: String) {
    navigate(route) { launchSingleTop = true }
}
