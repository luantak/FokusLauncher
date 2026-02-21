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
import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.activity.compose.LocalActivity
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.view.WindowManager
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.ui.drawer.AppDrawerScreen
import com.lu4p.fokuslauncher.ui.home.HomeScreen
import com.lu4p.fokuslauncher.ui.home.HomeViewModel
import com.lu4p.fokuslauncher.ui.onboarding.OnboardingScreen
import com.lu4p.fokuslauncher.ui.settings.CategoryAppsScreen
import com.lu4p.fokuslauncher.ui.settings.CategorySettingsScreen
import com.lu4p.fokuslauncher.ui.settings.EditHomeAppsScreen
import com.lu4p.fokuslauncher.ui.settings.EditShortcutsScreen
import com.lu4p.fokuslauncher.ui.settings.SettingsScreen
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.util.function.Consumer

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val SETTINGS_CATEGORIES = "settings_categories"
    const val SETTINGS_CATEGORY_APPS = "settings_category_apps"
    const val SETTINGS_EDIT_HOME_APPS = "settings_edit_home_apps"
    const val SETTINGS_EDIT_SHORTCUTS = "settings_edit_shortcuts"
}

private const val SWIPE_THRESHOLD = 200f
private const val ANIM_DURATION = 350
private const val HORIZONTAL_MAX_SLIDE_RATIO = 0.6f
private const val HORIZONTAL_TRIGGER_RATIO = 0.6f
private const val HORIZONTAL_DRAG_GAIN = 1.8f

private fun snapBackAnimationSpec() = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

@Composable
fun FokusNavGraph(
    navGraphViewModel: FokusNavGraphViewModel = hiltViewModel()
) {
    val hasCompletedOnboarding by navGraphViewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
    val showWallpaper by navGraphViewModel.showWallpaper.collectAsStateWithLifecycle()
    var pendingOpenEditHomeApps by remember { mutableStateOf(false) }

    if (!hasCompletedOnboarding) {
        OnboardingScreen(
            onNavigateToHome = { /* ViewModel sets hasCompletedOnboarding */ },
            onNavigateToHomeWithEditOverlay = {
                pendingOpenEditHomeApps = true
            }
        )
        return
    }

    val navController = rememberNavController()
    var showDrawer by remember { mutableStateOf(false) }
    var horizontalSwipeActive by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val isHome = navBackStackEntry?.destination?.route == Routes.HOME
    val shouldBlurAndDim = showDrawer || !isHome || horizontalSwipeActive
    // Never apply Android window-level blur/dim while on Home.
    val shouldApplyWindowEffects = shouldBlurAndDim && !isHome

    val activity = LocalActivity.current
    var crossWindowBlurEnabled by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activity?.getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled == true
            } else {
                false
            }
        )
    }
    DisposableEffect(activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || activity == null) {
            onDispose { }
        } else {
            val windowManager = activity.getSystemService(WindowManager::class.java)
            val listener = Consumer<Boolean> { enabled ->
                crossWindowBlurEnabled = enabled
            }
            windowManager?.addCrossWindowBlurEnabledListener(listener)
            onDispose {
                windowManager?.removeCrossWindowBlurEnabledListener(listener)
            }
        }
    }
    val overlayScrimColor = FokusBackdrop.scrimColor(crossWindowBlurEnabled)

    LaunchedEffect(shouldApplyWindowEffects, crossWindowBlurEnabled, activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val window = activity?.window
            if (window != null) {
                if (shouldApplyWindowEffects) {
                    window.setBackgroundBlurRadius(
                        if (crossWindowBlurEnabled) FokusBackdrop.WINDOW_BACKGROUND_BLUR_RADIUS else 0
                    )
                    if (crossWindowBlurEnabled) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                        window.attributes.blurBehindRadius = FokusBackdrop.WINDOW_BLUR_BEHIND_RADIUS
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                        window.attributes.blurBehindRadius = 0
                    }
                    window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    window.setDimAmount(FokusBackdrop.windowDimAmount(crossWindowBlurEnabled))
                    window.attributes = window.attributes
                } else {
                    window.setBackgroundBlurRadius(0)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    window.attributes.blurBehindRadius = 0
                    window.setDimAmount(0f)
                    window.attributes = window.attributes
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (showWallpaper) Color.Transparent else Color.Black)
    ) {
        // ── Main navigation (Home + Settings) ──────────────────────
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            enterTransition = { fadeIn(tween(ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(ANIM_DURATION)) }
        ) {
            // =====================  HOME  =====================
            composable(
                Routes.HOME,
                exitTransition = { androidx.compose.animation.ExitTransition.KeepUntilTransitionsFinished },
                popEnterTransition = { androidx.compose.animation.EnterTransition.None }
            ) {
                BackHandler(enabled = true) { /* launcher: no-op */ }

                val homeViewModel: HomeViewModel = hiltViewModel()
                val lifecycleOwner = LocalLifecycleOwner.current

                // Open edit-home-apps screen after onboarding "Choose apps".
                LaunchedEffect(pendingOpenEditHomeApps) {
                    if (pendingOpenEditHomeApps) {
                        navController.navigate(Routes.SETTINGS_EDIT_HOME_APPS) { launchSingleTop = true }
                        pendingOpenEditHomeApps = false
                    }
                }
                val swipeLeftTarget by homeViewModel.swipeLeftTarget.collectAsStateWithLifecycle()
                val swipeRightTarget by homeViewModel.swipeRightTarget.collectAsStateWithLifecycle()
                val activity = LocalActivity.current

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    val density = LocalDensity.current
                    val maxSlidePx = with(density) { (maxWidth * HORIZONTAL_MAX_SLIDE_RATIO).toPx() }
                    val triggerPx = with(density) { (maxWidth * HORIZONTAL_TRIGGER_RATIO).toPx() }
                    var horizontalOffsetPx by remember { mutableFloatStateOf(0f) }
                    val coroutineScope = rememberCoroutineScope()
                    val snapBackSpec = snapBackAnimationSpec()
                    var launchTriggered by remember { mutableStateOf(false) }
                    val isHorizontalGestureActive = abs(horizontalOffsetPx) > 0.5f || launchTriggered

                    LaunchedEffect(isHorizontalGestureActive) {
                        horizontalSwipeActive = isHorizontalGestureActive
                    }

                    // Track the current snap-back job so we can cancel it on resume
                    var snapBackJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

                    // Reset horizontal offset when returning from launched app
                    DisposableEffect(lifecycleOwner, coroutineScope) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                // Cancel any pending snap-back animation and reset immediately
                                snapBackJob?.cancel()
                                horizontalOffsetPx = 0f
                                launchTriggered = false
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    LaunchedEffect(launchTriggered) {
                        if (launchTriggered) {
                            // Launch in a separate job we can track and cancel
                            snapBackJob = coroutineScope.launch {
                                // Keep the panel at the swiped position briefly so launch feels continuous.
                                delay(260)
                                Animatable(horizontalOffsetPx).animateTo(
                                    targetValue = 0f,
                                    animationSpec = snapBackSpec
                                ) {
                                    horizontalOffsetPx = value
                                }
                                launchTriggered = false
                                snapBackJob = null
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                var verticalDragOffset = 0f
                                detectVerticalDragGestures(
                                    onDragStart = { },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        verticalDragOffset += dragAmount
                                    },
                                    onDragEnd = {
                                        when {
                                            verticalDragOffset < -SWIPE_THRESHOLD -> showDrawer = true
                                            verticalDragOffset > SWIPE_THRESHOLD -> activity?.let {
                                                MainActivity.expandStatusBar(it)
                                            }
                                        }
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
                                                        if (!launchTriggered) {
                                                            coroutineScope.launch {
                                                                Animatable(horizontalOffsetPx).animateTo(
                                                                    targetValue = 0f,
                                                                    animationSpec = snapBackSpec
                                                                ) {
                                                                    horizontalOffsetPx = value
                                                                }
                                                            }
                                                        }
                                                    },
                                                    onDragCancel = {
                                                        if (!launchTriggered) {
                                                            coroutineScope.launch {
                                                                Animatable(horizontalOffsetPx).animateTo(
                                                                    targetValue = 0f,
                                                                    animationSpec = snapBackSpec
                                                                ) {
                                                                    horizontalOffsetPx = value
                                                                }
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        } else Modifier
                                    )
                            
                    ) {
                        HomeScreen(
                            viewModel = homeViewModel,
                            onOpenSettings = {
                                navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                            },
                            onOpenEditHomeApps = {
                                navController.navigate(Routes.SETTINGS_EDIT_HOME_APPS) { launchSingleTop = true }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationX = horizontalOffsetPx
                                    alpha = if (showDrawer) 0f else 1f
                                }
                        )
                    }
                }
                
                // ── App Drawer overlay ─────────────────────────────────────
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
                            .background(overlayScrimColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {}
                    ) {
                        AppDrawerScreen(
                            onSettingsClick = {
                                navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                            },
                            onEditCategoryApps = { category ->
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

            // =====================  SETTINGS  =====================
            composable(
                Routes.SETTINGS,
                enterTransition = {
                    slideInHorizontally(tween(ANIM_DURATION)) { it }
                },
                exitTransition = {
                    slideOutHorizontally(tween(ANIM_DURATION)) { -it }
                },
                popEnterTransition = {
                    slideInHorizontally(tween(ANIM_DURATION)) { -it }
                },
                popExitTransition = {
                    slideOutHorizontally(tween(ANIM_DURATION)) { it }
                }
            ) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onEditHomeScreen = {
                        navController.navigate(Routes.SETTINGS_EDIT_HOME_APPS) { launchSingleTop = true }
                    },
                    onEditRightShortcuts = {
                        navController.navigate(Routes.SETTINGS_EDIT_SHORTCUTS) { launchSingleTop = true }
                    },
                    onEditCategories = {
                        navController.navigate(Routes.SETTINGS_CATEGORIES) { launchSingleTop = true }
                    },
                    backgroundScrim = overlayScrimColor
                )
            }

            composable(
                Routes.SETTINGS_CATEGORIES,
                enterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { it } },
                exitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { -it } },
                popEnterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { -it } },
                popExitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { it } }
            ) {
                CategorySettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onEditCategoryApps = { category ->
                        navController.navigate(
                            "${Routes.SETTINGS_CATEGORY_APPS}/${Uri.encode(category)}"
                        ) {
                            launchSingleTop = true
                        }
                    },
                    backgroundScrim = overlayScrimColor
                )
            }

            composable(
                "${Routes.SETTINGS_CATEGORY_APPS}/{category}",
                enterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { it } },
                exitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { -it } },
                popEnterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { -it } },
                popExitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { it } }
            ) { entry ->
                val encodedCategory = entry.arguments?.getString("category").orEmpty()
                CategoryAppsScreen(
                    category = Uri.decode(encodedCategory),
                    onNavigateBack = { navController.popBackStack() },
                    backgroundScrim = overlayScrimColor
                )
            }

            composable(
                Routes.SETTINGS_EDIT_HOME_APPS,
                enterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { it } },
                exitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { -it } },
                popEnterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { -it } },
                popExitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { it } }
            ) {
                val homeBackStackEntry = remember(navBackStackEntry) {
                    navController.getBackStackEntry(Routes.HOME)
                }
                val homeViewModel: HomeViewModel = hiltViewModel(homeBackStackEntry)
                EditHomeAppsScreen(
                    viewModel = homeViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    backgroundScrim = overlayScrimColor
                )
            }

            composable(
                Routes.SETTINGS_EDIT_SHORTCUTS,
                enterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { it } },
                exitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { -it } },
                popEnterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { -it } },
                popExitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { it } }
            ) {
                val homeBackStackEntry = remember(navBackStackEntry) {
                    navController.getBackStackEntry(Routes.HOME)
                }
                val homeViewModel: HomeViewModel = hiltViewModel(homeBackStackEntry)
                EditShortcutsScreen(
                    viewModel = homeViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    backgroundScrim = overlayScrimColor
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
            Intent(Intent.ACTION_VIEW, intentUri.toUri())
        } catch (_: Exception) {
            null
        }
    }
}
