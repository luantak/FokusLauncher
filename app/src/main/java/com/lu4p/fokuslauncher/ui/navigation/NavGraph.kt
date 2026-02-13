package com.lu4p.fokuslauncher.ui.navigation

import android.app.Activity
import android.app.ActivityOptions
import com.lu4p.fokuslauncher.MainActivity
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Build
import android.os.Process
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.ui.drawer.AppDrawerScreen
import com.lu4p.fokuslauncher.ui.home.HomeScreen
import com.lu4p.fokuslauncher.ui.home.HomeViewModel
import com.lu4p.fokuslauncher.ui.onboarding.OnboardingScreen
import com.lu4p.fokuslauncher.ui.settings.CategoryAppsScreen
import com.lu4p.fokuslauncher.ui.settings.CategorySettingsScreen
import com.lu4p.fokuslauncher.ui.settings.SettingsScreen
import kotlinx.coroutines.delay
import kotlin.math.abs

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val SETTINGS_CATEGORIES = "settings_categories"
    const val SETTINGS_CATEGORY_APPS = "settings_category_apps"
    const val ONBOARDING = "onboarding"
}

private const val SWIPE_THRESHOLD = 200f
private const val ANIM_DURATION = 350
private const val HORIZONTAL_MAX_SLIDE_RATIO = 0.6f
private const val HORIZONTAL_TRIGGER_RATIO = 0.6f
private const val HORIZONTAL_DRAG_GAIN = 1.8f

@Composable
fun FokusNavGraph(
    navGraphViewModel: FokusNavGraphViewModel = hiltViewModel()
) {
    val hasCompletedOnboarding by navGraphViewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
    var requestOpenEditOverlay by remember { mutableStateOf(false) }
    var tempShowHomeForEdit by remember { mutableStateOf(false) }

    if (!hasCompletedOnboarding && !tempShowHomeForEdit) {
        OnboardingScreen(
            onNavigateToHome = { /* ViewModel sets hasCompletedOnboarding */ },
            onNavigateToHomeWithEditOverlay = {
                tempShowHomeForEdit = true
                requestOpenEditOverlay = true
            }
        )
        return
    }

    val navController = rememberNavController()
    var showDrawer by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Main navigation (Home + Settings) ──────────────────────
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            enterTransition = { fadeIn(tween(ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(ANIM_DURATION)) }
        ) {
            // =====================  HOME  =====================
            composable(Routes.HOME) { backStackEntry ->
                BackHandler(enabled = true) { /* launcher: no-op */ }

                val homeViewModel: HomeViewModel = hiltViewModel()
                val showEditOverlay by homeViewModel.showEditOverlay.collectAsStateWithLifecycle()

                // Open edit overlay when returning from onboarding "Choose apps" or from Settings
                LaunchedEffect(requestOpenEditOverlay) {
                    if (requestOpenEditOverlay) {
                        homeViewModel.openEditOverlay()
                        requestOpenEditOverlay = false
                    }
                }
                // Open edit overlay when returning from Settings via "Edit home screen"
                val savedStateHandle = backStackEntry.savedStateHandle
                LaunchedEffect(Unit) {
                    savedStateHandle.getStateFlow("openEditOverlay", false)
                        .collect { shouldOpen ->
                            if (shouldOpen) {
                                homeViewModel.openEditOverlay()
                                savedStateHandle["openEditOverlay"] = false
                            }
                        }
                }
                LaunchedEffect(Unit) {
                    savedStateHandle.getStateFlow("openEditShortcutsOverlay", false)
                        .collect { shouldOpen ->
                            if (shouldOpen) {
                                homeViewModel.openEditShortcutsOverlay()
                                savedStateHandle["openEditShortcutsOverlay"] = false
                            }
                        }
                }
                val swipeLeftTarget by homeViewModel.swipeLeftTarget.collectAsStateWithLifecycle()
                val swipeRightTarget by homeViewModel.swipeRightTarget.collectAsStateWithLifecycle()
                val activity = LocalContext.current as? Activity

                var vertDrag by remember { mutableFloatStateOf(0f) }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    val density = LocalDensity.current
                    val maxSlidePx = with(density) { (maxWidth * HORIZONTAL_MAX_SLIDE_RATIO).toPx() }
                    val triggerPx = with(density) { (maxWidth * HORIZONTAL_TRIGGER_RATIO).toPx() }
                    var horizontalOffsetPx by remember { mutableFloatStateOf(0f) }
                    var launchTriggered by remember { mutableStateOf(false) }

                    LaunchedEffect(launchTriggered) {
                        if (launchTriggered) {
                            // Keep the panel at the swiped position briefly so launch feels continuous.
                            delay(260)
                            horizontalOffsetPx = 0f
                            launchTriggered = false
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (!showEditOverlay) Modifier
                                    .pointerInput(Unit) {
                                        detectVerticalDragGestures(
                                            onDragStart = { vertDrag = 0f },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                vertDrag += dragAmount
                                            },
                                            onDragEnd = {
                                                when {
                                                    vertDrag < -SWIPE_THRESHOLD -> showDrawer = true
                                                    vertDrag > SWIPE_THRESHOLD -> activity?.let {
                                                        MainActivity.expandStatusBar(it)
                                                    }
                                                }
                                                vertDrag = 0f
                                            }
                                        )
                                    }
                                    .then(
                                        if (swipeLeftTarget != null || swipeRightTarget != null) {
                                            val minSlidePx = if (swipeLeftTarget != null) -maxSlidePx else 0f
                                            val maxSlidePxVal = if (swipeRightTarget != null) maxSlidePx else 0f
                                            Modifier.pointerInput(
                                                swipeLeftTarget,
                                                swipeRightTarget,
                                                maxSlidePx,
                                                triggerPx,
                                                minSlidePx,
                                                maxSlidePxVal
                                            ) {
                                                detectHorizontalDragGestures(
                                                    onDragStart = { launchTriggered = false },
                                                    onHorizontalDrag = { change, dragAmount ->
                                                        if (launchTriggered) return@detectHorizontalDragGestures
                                                        if (horizontalOffsetPx == 0f) {
                                                            if (dragAmount > 0 && swipeRightTarget == null) return@detectHorizontalDragGestures
                                                            if (dragAmount < 0 && swipeLeftTarget == null) return@detectHorizontalDragGestures
                                                        }
                                                        change.consume()
                                                        horizontalOffsetPx =
                                                            (horizontalOffsetPx + (dragAmount * HORIZONTAL_DRAG_GAIN))
                                                                .coerceIn(minSlidePx, maxSlidePxVal)
                                                        if (abs(horizontalOffsetPx) >= triggerPx) {
                                                            val target = if (horizontalOffsetPx > 0f) swipeRightTarget else swipeLeftTarget
                                                            if (target != null) {
                                                                launchTriggered = true
                                                                horizontalOffsetPx =
                                                                    if (horizontalOffsetPx > 0f) maxSlidePx else -maxSlidePx
                                                                activity?.launchWithBottomReveal(target)
                                                            }
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        if (!launchTriggered) horizontalOffsetPx = 0f
                                                    },
                                                    onDragCancel = {
                                                        if (!launchTriggered) horizontalOffsetPx = 0f
                                                    }
                                                )
                                            }
                                        } else Modifier
                                    )
                                else Modifier
                            )
                    ) {
                        HomeScreen(
                            viewModel = homeViewModel,
                            onSwipeUp = { showDrawer = true },
                            onOpenSettings = {
                                navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                            },
                            onEditOverlayClosed = { tempShowHomeForEdit = false },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { translationX = horizontalOffsetPx }
                        )
                    }
                }
            }

            // =====================  SETTINGS  =====================
            composable(
                Routes.SETTINGS,
                enterTransition = {
                    slideInHorizontally(tween(ANIM_DURATION)) { it }
                },
                exitTransition = {
                    slideOutHorizontally(tween(ANIM_DURATION)) { it }
                }
            ) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onEditHomeScreen = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("openEditOverlay", true)
                        navController.popBackStack()
                    },
                    onEditRightShortcuts = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("openEditShortcutsOverlay", true)
                        navController.popBackStack()
                    },
                    onEditCategories = {
                        navController.navigate(Routes.SETTINGS_CATEGORIES) { launchSingleTop = true }
                    }
                )
            }

            composable(
                Routes.SETTINGS_CATEGORIES,
                enterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { it } },
                exitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { it } }
            ) {
                CategorySettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onEditCategoryApps = { category ->
                        navController.navigate(
                            "${Routes.SETTINGS_CATEGORY_APPS}/${Uri.encode(category)}"
                        ) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                "${Routes.SETTINGS_CATEGORY_APPS}/{category}",
                enterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { it } },
                exitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { it } }
            ) { entry ->
                val encodedCategory = entry.arguments?.getString("category").orEmpty()
                CategoryAppsScreen(
                    category = Uri.decode(encodedCategory),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // ── App Drawer overlay ─────────────────────────────────────
        // Rendered *after* the NavHost so it's always on top.
        // The home screen stays completely static underneath.

        AnimatedVisibility(
            visible = showDrawer,
            enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                initialOffsetY = { it }   // slide up from below the screen
            ),
            exit = slideOutVertically(
                animationSpec = tween(250),
                targetOffsetY = { it }     // slide back down
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                AppDrawerScreen(
                    onSettingsClick = {
                        showDrawer = false
                        navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                    },
                    onEditCategoryApps = { category ->
                        showDrawer = false
                        navController.navigate(
                            "${Routes.SETTINGS_CATEGORY_APPS}/${Uri.encode(category)}"
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onClose = { showDrawer = false }
                )
            }
        }
    }
}

// ---- Swipe app launch ----

/**
 * Launches an app using a bottom-edge clip reveal.
 * This is kept direction-agnostic so the swipe gesture controls launcher movement,
 * while app open remains consistently vertical.
 */
private fun Activity.launchWithBottomReveal(target: ShortcutTarget) {
    if (target is ShortcutTarget.LauncherShortcut) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                val launcherApps = getSystemService(LauncherApps::class.java)
                launcherApps?.startShortcut(
                    target.packageName,
                    target.shortcutId,
                    null,
                    null,
                    Process.myUserHandle()
                )
            } catch (_: Exception) {
                // ignore launch failures
            }
        }
        return
    }

    val intent = when (target) {
        is ShortcutTarget.App -> packageManager.getLaunchIntentForPackage(target.packageName)
        is ShortcutTarget.DeepLink -> parseDeepLinkIntent(target.intentUri)
        is ShortcutTarget.LauncherShortcut -> null
    } ?: return

    val root = window.decorView
    val width = if (root.width > 0) root.width else resources.displayMetrics.widthPixels
    val height = if (root.height > 0) root.height else resources.displayMetrics.heightPixels
    val centerX = (width / 2).coerceAtLeast(0)
    val bottomY = (height - 2).coerceAtLeast(0)

    val options = ActivityOptions.makeClipRevealAnimation(root, centerX, bottomY, 1, 1)
    startActivity(intent, options.toBundle())
}

private fun parseDeepLinkIntent(intentUri: String): Intent? {
    return try {
        Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME)
    } catch (_: Exception) {
        try {
            Intent(Intent.ACTION_VIEW, Uri.parse(intentUri))
        } catch (_: Exception) {
            null
        }
    }
}
