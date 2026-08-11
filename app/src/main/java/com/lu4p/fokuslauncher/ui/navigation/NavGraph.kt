package com.lu4p.fokuslauncher.ui.navigation

import android.app.Activity
import android.app.ActivityOptions
import com.lu4p.fokuslauncher.MainActivity
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Build
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.activity.compose.LocalActivity
import androidx.core.net.toUri
import androidx.compose.animation.AnimatedContentScope
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.ui.util.OnResumeEffect
import com.lu4p.fokuslauncher.ui.util.rememberSystemAnimationsEnabled
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.view.WindowManager
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.ui.drawer.AppDrawerScreen
import com.lu4p.fokuslauncher.ui.drawer.AppDrawerViewModel
import com.lu4p.fokuslauncher.ui.home.HomeScreen
import com.lu4p.fokuslauncher.ui.home.HomeViewModel
import com.lu4p.fokuslauncher.ui.onboarding.OnboardingScreen
import com.lu4p.fokuslauncher.ui.settings.AppearanceSettingsScreen
import com.lu4p.fokuslauncher.ui.settings.AppsManagementSettingsScreen
import com.lu4p.fokuslauncher.ui.settings.CategoryAppsScreen
import com.lu4p.fokuslauncher.ui.settings.CategoryIconPickerScreen
import com.lu4p.fokuslauncher.ui.settings.CategorySettingsScreen
import com.lu4p.fokuslauncher.ui.settings.EditHomeAppsScreen
import com.lu4p.fokuslauncher.ui.settings.EditShortcutsScreen
import com.lu4p.fokuslauncher.ui.settings.DeviceControlSettingsScreen
import com.lu4p.fokuslauncher.ui.settings.DrawerBehaviorSettingsScreen
import com.lu4p.fokuslauncher.ui.settings.DrawerDotSearchSettingsScreen
import com.lu4p.fokuslauncher.ui.settings.HomeWidgetsSettingsScreen
import com.lu4p.fokuslauncher.ui.settings.PomodoroSettingsScreen
import com.lu4p.fokuslauncher.ui.settings.IconPickerScreen
import com.lu4p.fokuslauncher.ui.settings.ProfileNamesSettingsScreen
import com.lu4p.fokuslauncher.ui.settings.SettingsScreen
import com.lu4p.fokuslauncher.ui.settings.SettingsViewModel
import androidx.compose.ui.res.stringResource
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import com.lu4p.fokuslauncher.ui.widgets.WidgetPageScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.util.function.Consumer

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val SETTINGS_DEVICE_CONTROL = "settings_device_control"
    const val SETTINGS_CATEGORIES = "settings_categories"
    const val SETTINGS_CATEGORY_APPS = "settings_category_apps"
    const val SETTINGS_EDIT_HOME_APPS = "settings_edit_home_apps"
    const val SETTINGS_EDIT_SHORTCUTS = "settings_edit_shortcuts"
    const val SETTINGS_ICON_PICKER_SHORTCUT = "settings_icon_picker/shortcut/{index}"
    const val SETTINGS_ICON_PICKER_CATEGORY = "settings_icon_picker/category/{category}"
    const val SETTINGS_HOME_WIDGETS = "settings_home_widgets"
    const val SETTINGS_POMODORO = "settings_pomodoro"
    const val SETTINGS_DRAWER_DOT_SEARCH = "settings_drawer_dot_search"
    const val SETTINGS_PROFILE_NAMES = "settings_profile_names"
    const val SETTINGS_APPEARANCE = "settings_appearance"
    const val SETTINGS_DRAWER_BEHAVIOR = "settings_drawer_behavior"
    const val SETTINGS_APPS_MANAGEMENT = "settings_apps_management"

    fun iconPickerShortcut(index: Int): String = "settings_icon_picker/shortcut/$index"

    fun iconPickerCategory(category: String): String =
            "settings_icon_picker/category/${Uri.encode(category)}"
}

private const val SWIPE_THRESHOLD = 100f
private const val ANIM_DURATION = 200
/** Hold at full slide after triggering a shortcut launch, then snap home (ms). */
private const val SWIPE_LAUNCH_HOLD_MS = 40L
private const val HORIZONTAL_MAX_SLIDE_RATIO = 0.6f
private const val HORIZONTAL_TRIGGER_RATIO = 0.3f
private const val HORIZONTAL_DRAG_GAIN = 1.8f

private enum class SwipeSide { LEFT, RIGHT }

private fun snapBackAnimationSpec() = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessHigh
)

/** Animates [from] → [to], or snaps instantly when system animations are disabled. */
private suspend fun animateHorizontalOffset(
    from: Float,
    to: Float,
    animationsEnabled: Boolean,
    onValue: (Float) -> Unit,
) {
    if (!animationsEnabled || from == to) {
        onValue(to)
        return
    }
    Animatable(from).animateTo(
        targetValue = to,
        animationSpec = snapBackAnimationSpec(),
    ) {
        onValue(value)
    }
}

@Composable
private fun settingsViewModel(activity: ComponentActivity): SettingsViewModel =
        hiltViewModel(viewModelStoreOwner = activity)

private fun NavGraphBuilder.fokusSettingsComposable(
        route: String,
        arguments: List<NamedNavArgument> = emptyList(),
        deepLinks: List<NavDeepLink> = emptyList(),
        content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable(
            route = route,
            arguments = arguments,
            deepLinks = deepLinks,
            enterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { it } },
            exitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { -it } },
            popEnterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { -it } },
            popExitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { it } },
            content = content,
    )
}

@Composable
fun FokusNavGraph(
    navGraphViewModel: FokusNavGraphViewModel = hiltViewModel()
) {
    val hasCompletedOnboarding by navGraphViewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()

    if (!hasCompletedOnboarding) {
        OnboardingScreen(
            onNavigateToHome = { /* ViewModel sets hasCompletedOnboarding */ }
        )
        return
    }

    val navController = rememberNavController()
    var showDrawer by remember { mutableStateOf(false) }
    var widgetPageSide by remember { mutableStateOf<SwipeSide?>(null) }
    val horizontalSwipeActive = remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val keyboardControllerUpdated = rememberUpdatedState(keyboardController)

    val componentActivity = LocalActivity.current as ComponentActivity
    val launcherHomeCoordinator =
        hiltViewModel<LauncherHomeCoordinatorViewModel>(viewModelStoreOwner = componentActivity)

    LaunchedEffect(launcherHomeCoordinator, navController) {
        launcherHomeCoordinator.goHomeRequests.collect {
            // Hide IME with the drawer exit; goHome bypasses AppDrawer close helpers.
            keyboardControllerUpdated.value?.hide()
            showDrawer = false
            widgetPageSide = null
            navController.popBackStack(Routes.HOME, inclusive = false)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val isHome = navBackStackEntry?.destination?.route == Routes.HOME
    val shouldBlurAndDim = showDrawer || widgetPageSide != null || !isHome || horizontalSwipeActive.value
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
    val photoDrawerOverlay by
            navGraphViewModel.photoWallpaperDrawerOverlayUiState.collectAsStateWithLifecycle()
    val overlayScrimIntensity =
            if (photoDrawerOverlay.usesPhotoWallpaper) photoDrawerOverlay.intensityMultiplier
            else 1f
    val overlayScrimColor =
            remember(crossWindowBlurEnabled, overlayScrimIntensity) {
                FokusBackdrop.drawerOverlayScrimColor(
                        blurEnabled = crossWindowBlurEnabled,
                        intensityMultiplier = overlayScrimIntensity,
                )
            }

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

    Box(Modifier.fillMaxSize()) {
        Box(
                modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
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
                exitTransition = { ExitTransition.KeepUntilTransitionsFinished },
                popEnterTransition = { EnterTransition.None }
            ) {
                BackHandler(enabled = true) { /* launcher: no-op */ }

                // Eager scope: start loading apps and pre-warming drawer caches as soon as Home is
                // shown, not on first drawer composition (faster first open).
                val appDrawerViewModel: AppDrawerViewModel = hiltViewModel()

                val homeViewModel: HomeViewModel = hiltViewModel()
                val lifecycleOwner = LocalLifecycleOwner.current

                // Home / middle nav button: dismiss empty-space long-press menu (and app menu).
                LaunchedEffect(launcherHomeCoordinator, homeViewModel) {
                    launcherHomeCoordinator.goHomeRequests.collect {
                        homeViewModel.dismissHomeOverlays()
                    }
                }

                val swipeLeftTarget by homeViewModel.swipeLeftTarget.collectAsStateWithLifecycle()
                val swipeRightTarget by homeViewModel.swipeRightTarget.collectAsStateWithLifecycle()
                val systemAnimationsEnabled = rememberSystemAnimationsEnabled()

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    val density = LocalDensity.current
                    val pageWidthPx = with(density) { maxWidth.toPx() }
                    val maxSlidePx = with(density) { (maxWidth * HORIZONTAL_MAX_SLIDE_RATIO).toPx() }
                    val triggerPx = with(density) { (maxWidth * HORIZONTAL_TRIGGER_RATIO).toPx() }
                    var horizontalOffsetPx by remember { mutableFloatStateOf(0f) }
                    var widgetDragSide by remember { mutableStateOf<SwipeSide?>(null) }
                    val coroutineScope = rememberCoroutineScope()
                    var launchTriggered by remember { mutableStateOf(false) }
                    // With animations off, only show settled positions (no finger-follow slide).
                    val displayedHorizontalOffsetPx =
                        when {
                            systemAnimationsEnabled -> horizontalOffsetPx
                            widgetPageSide == SwipeSide.RIGHT -> pageWidthPx
                            widgetPageSide == SwipeSide.LEFT -> -pageWidthPx
                            else -> 0f
                        }
                    val isHorizontalGestureActive =
                        abs(displayedHorizontalOffsetPx) > 0.5f ||
                            launchTriggered ||
                            widgetPageSide != null

                    LaunchedEffect(isHorizontalGestureActive) {
                        horizontalSwipeActive.value = isHorizontalGestureActive
                    }

                    // Track the current snap-back job so we can cancel it on resume
                    var snapBackJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

                    // Reset horizontal offset when returning from launched app
                    OnResumeEffect(lifecycleOwner, coroutineScope) {
                        snapBackJob?.cancel()
                        horizontalOffsetPx = 0f
                        widgetPageSide = null
                        widgetDragSide = null
                        launchTriggered = false
                    }

                    LaunchedEffect(launchTriggered, systemAnimationsEnabled) {
                        if (launchTriggered) {
                            if (!systemAnimationsEnabled) {
                                horizontalOffsetPx = 0f
                                return@LaunchedEffect
                            }
                            // Launch in a separate job we can track and cancel
                            snapBackJob = coroutineScope.launch {
                                // Keep the panel at the swiped position briefly so launch feels continuous.
                                delay(SWIPE_LAUNCH_HOLD_MS)
                                animateHorizontalOffset(
                                    from = horizontalOffsetPx,
                                    to = 0f,
                                    animationsEnabled = true,
                                ) {
                                    horizontalOffsetPx = it
                                }
                                launchTriggered = false
                                snapBackJob = null
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(widgetPageSide, widgetDragSide) {
                                var verticalDragOffset = 0f
                                var drawerTriggered = false
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        verticalDragOffset = 0f
                                        drawerTriggered = false
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        if (widgetPageSide != null || widgetDragSide != null) {
                                            return@detectVerticalDragGestures
                                        }
                                        change.consume()
                                        verticalDragOffset += dragAmount
                                        if (!drawerTriggered && verticalDragOffset < -SWIPE_THRESHOLD) {
                                            drawerTriggered = true
                                            verticalDragOffset = 0f
                                            showDrawer = true
                                        }
                                    },
                                    onDragEnd = {
                                        when {
                                            verticalDragOffset > SWIPE_THRESHOLD -> activity?.let {
                                                MainActivity.expandStatusBar(it)
                                            }
                                        }
                                        verticalDragOffset = 0f
                                        drawerTriggered = false
                                    },
                                    onDragCancel = {
                                        verticalDragOffset = 0f
                                        drawerTriggered = false
                                    }
                                )
                            }
                            .then(
                                        if (swipeLeftTarget != null || swipeRightTarget != null) {
                                            val minSlidePx =
                                                if (swipeLeftTarget != null) {
                                                    if (swipeLeftTarget is ShortcutTarget.WidgetPage) -pageWidthPx
                                                    else -maxSlidePx
                                                } else 0f
                                            val maxSlidePxVal =
                                                if (swipeRightTarget != null) {
                                                    if (swipeRightTarget is ShortcutTarget.WidgetPage) pageWidthPx
                                                    else maxSlidePx
                                                } else 0f
                                            Modifier.pointerInput(
                                                swipeLeftTarget,
                                                swipeRightTarget,
                                                pageWidthPx,
                                                maxSlidePx,
                                                triggerPx,
                                                minSlidePx,
                                                maxSlidePxVal,
                                                systemAnimationsEnabled,
                                            ) {
                                                val settleHorizontalDrag: () -> Unit = {
                                                    if (!launchTriggered) {
                                                        coroutineScope.launch {
                                                            val side = widgetDragSide
                                                            if (side != null && abs(horizontalOffsetPx) >= triggerPx) {
                                                                val targetValue =
                                                                    if (side == SwipeSide.RIGHT) pageWidthPx
                                                                    else -pageWidthPx
                                                                animateHorizontalOffset(
                                                                        from = horizontalOffsetPx,
                                                                        to = targetValue,
                                                                        animationsEnabled = systemAnimationsEnabled,
                                                                ) {
                                                                    horizontalOffsetPx = it
                                                                }
                                                                widgetPageSide = side
                                                            } else {
                                                                animateHorizontalOffset(
                                                                        from = horizontalOffsetPx,
                                                                        to = 0f,
                                                                        animationsEnabled = systemAnimationsEnabled,
                                                                ) {
                                                                    horizontalOffsetPx = it
                                                                }
                                                            }
                                                            widgetDragSide = null
                                                        }
                                                    }
                                                }
                                                detectHorizontalDragGestures(
                                                    onDragStart = {
                                                        if (widgetPageSide != null) return@detectHorizontalDragGestures
                                                        launchTriggered = false
                                                        widgetDragSide = null
                                                    },
                                                    onHorizontalDrag = { change, dragAmount ->
                                                        if (widgetPageSide != null) return@detectHorizontalDragGestures
                                                        if (launchTriggered) return@detectHorizontalDragGestures
                                                        if (horizontalOffsetPx == 0f) {
                                                            if (dragAmount > 0 && swipeRightTarget == null) return@detectHorizontalDragGestures
                                                            if (dragAmount < 0 && swipeLeftTarget == null) return@detectHorizontalDragGestures
                                                        }
                                                        change.consume()
                                                        horizontalOffsetPx =
                                                            (horizontalOffsetPx + (dragAmount * HORIZONTAL_DRAG_GAIN))
                                                                .coerceIn(minSlidePx, maxSlidePxVal)
                                                        val target = if (horizontalOffsetPx > 0f) swipeRightTarget else swipeLeftTarget
                                                        if (target is ShortcutTarget.WidgetPage) {
                                                            widgetDragSide =
                                                                if (horizontalOffsetPx > 0f) SwipeSide.RIGHT
                                                                else SwipeSide.LEFT
                                                        } else if (abs(horizontalOffsetPx) >= triggerPx) {
                                                            if (target != null) {
                                                                launchTriggered = true
                                                                horizontalOffsetPx =
                                                                    if (systemAnimationsEnabled) {
                                                                        if (horizontalOffsetPx > 0f) maxSlidePx else -maxSlidePx
                                                                    } else {
                                                                        0f
                                                                    }
                                                                activity?.launchWithBottomReveal(target)
                                                            }
                                                        }
                                                    },
                                                    onDragEnd = settleHorizontalDrag,
                                                    onDragCancel = settleHorizontalDrag
                                                )
                                            }
                                        } else Modifier
                                    )
                            
                    ) {
                        // Preview the widget page only while animating a drag, or once settled open.
                        val previewSide = widgetPageSide ?: widgetDragSide
                        if (previewSide != null &&
                                        (widgetPageSide != null || systemAnimationsEnabled)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        translationX =
                                            when (previewSide) {
                                                SwipeSide.RIGHT -> displayedHorizontalOffsetPx - pageWidthPx
                                                SwipeSide.LEFT -> displayedHorizontalOffsetPx + pageWidthPx
                                            }
                                    }
                            ) {
                                WidgetPageScreen(
                                    onClose = {
                                        coroutineScope.launch {
                                            animateHorizontalOffset(
                                                from = horizontalOffsetPx,
                                                to = 0f,
                                                animationsEnabled = systemAnimationsEnabled,
                                            ) {
                                                horizontalOffsetPx = it
                                            }
                                            widgetPageSide = null
                                            widgetDragSide = null
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(
                                            widgetPageSide,
                                            systemAnimationsEnabled,
                                            pageWidthPx,
                                            triggerPx,
                                        ) {
                                            fun settleWidgetPageClose() {
                                                val currentSide = widgetPageSide ?: return
                                                val openOffset =
                                                    if (currentSide == SwipeSide.RIGHT) {
                                                        pageWidthPx
                                                    } else {
                                                        -pageWidthPx
                                                    }
                                                // Same travel as opening: drag ~triggerPx toward home.
                                                val shouldClose =
                                                    abs(openOffset - horizontalOffsetPx) >= triggerPx
                                                coroutineScope.launch {
                                                    val targetValue =
                                                        when {
                                                            shouldClose -> 0f
                                                            else -> openOffset
                                                        }
                                                    animateHorizontalOffset(
                                                        from = horizontalOffsetPx,
                                                        to = targetValue,
                                                        animationsEnabled = systemAnimationsEnabled,
                                                    ) {
                                                        horizontalOffsetPx = it
                                                    }
                                                    if (shouldClose) {
                                                        widgetPageSide = null
                                                        widgetDragSide = null
                                                    }
                                                }
                                            }
                                            detectHorizontalDragGestures(
                                                onDragStart = { },
                                                onHorizontalDrag = { change, dragAmount ->
                                                    val currentSide = widgetPageSide
                                                        ?: return@detectHorizontalDragGestures
                                                    val closes =
                                                        (currentSide == SwipeSide.RIGHT && dragAmount < 0f) ||
                                                                (currentSide == SwipeSide.LEFT && dragAmount > 0f)
                                                    if (!closes) return@detectHorizontalDragGestures
                                                    change.consume()
                                                    val next =
                                                        horizontalOffsetPx +
                                                            (dragAmount * HORIZONTAL_DRAG_GAIN)
                                                    horizontalOffsetPx =
                                                        when (currentSide) {
                                                            SwipeSide.RIGHT ->
                                                                next.coerceIn(0f, pageWidthPx)
                                                            SwipeSide.LEFT ->
                                                                next.coerceIn(-pageWidthPx, 0f)
                                                        }
                                                },
                                                onDragEnd = { settleWidgetPageClose() },
                                                onDragCancel = { settleWidgetPageClose() },
                                            )
                                        }
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationX = displayedHorizontalOffsetPx
                                    alpha = if (showDrawer) 0f else 1f
                                }
                        ) {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onOpenSettings = {
                                    navController.navigateSingleTop(Routes.SETTINGS)
                                },
                                onOpenEditHomeApps = {
                                    navController.navigateSingleTop(Routes.SETTINGS_EDIT_HOME_APPS)
                                },
                                onOpenEditShortcuts = {
                                    navController.navigateSingleTop(Routes.SETTINGS_EDIT_SHORTCUTS)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // ── App Drawer overlay ─────────────────────────────────────
                // Full-screen scrim must not use the drawer's slide-in: Home is hidden immediately
                // (alpha 0) while the overlay used to start fully below the screen, which briefly
                // showed the wallpaper at full brightness before the scrim covered it.
                AnimatedVisibility(
                    visible = showDrawer,
                    enter = EnterTransition.None,
                    exit = fadeOut(tween(220)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(overlayScrimColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {}
                    )
                }
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
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        targetOffsetY = { it }     // slide back down
                    )
                ) {
                    AppDrawerScreen(
                        viewModel = appDrawerViewModel,
                        onSettingsClick = {
                            navController.navigateSingleTop(Routes.SETTINGS)
                        },
                        onEditCategoryApps = { category ->
                            navController.navigateSingleTop(
                                    "${Routes.SETTINGS_CATEGORY_APPS}/${Uri.encode(category)}"
                            )
                        },
                        onClose = { showDrawer = false }
                    )
                }
            }

            // =====================  SETTINGS  =====================
            fokusSettingsComposable(Routes.SETTINGS) {
                val settingsVm = settingsViewModel(componentActivity)
                SettingsScreen(
                    viewModel = settingsVm,
                    onNavigateBack = { navController.popBackStack() },
                    onEditHomeScreen = {
                        navController.navigateSingleTop(Routes.SETTINGS_EDIT_HOME_APPS)
                    },
                    onEditRightShortcuts = {
                        navController.navigateSingleTop(Routes.SETTINGS_EDIT_SHORTCUTS)
                    },
                    onOpenDeviceControlSettings = {
                        navController.navigateSingleTop(Routes.SETTINGS_DEVICE_CONTROL)
                    },
                    onEditCategories = {
                        navController.navigateSingleTop(Routes.SETTINGS_CATEGORIES)
                    },
                    onDrawerDotSearchSettings = {
                        navController.navigateSingleTop(Routes.SETTINGS_DRAWER_DOT_SEARCH)
                    },
                    onOpenHomeWidgetsSettings = {
                        navController.navigateSingleTop(Routes.SETTINGS_HOME_WIDGETS)
                    },
                    onOpenAppearanceSettings = {
                        navController.navigateSingleTop(Routes.SETTINGS_APPEARANCE)
                    },
                    onOpenDrawerBehaviorSettings = {
                        navController.navigateSingleTop(Routes.SETTINGS_DRAWER_BEHAVIOR)
                    },
                    onOpenAppsManagementSettings = {
                        navController.navigateSingleTop(Routes.SETTINGS_APPS_MANAGEMENT)
                    },
                    backgroundScrim = Color.Black
                )
            }

            fokusSettingsComposable(Routes.SETTINGS_APPEARANCE) {
                AppearanceSettingsScreen(
                        viewModel = settingsViewModel(componentActivity),
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToHome = {
                            keyboardController?.hide()
                            showDrawer = false
                            navController.popBackStack(Routes.HOME, inclusive = false)
                        },
                        backgroundScrim = Color.Black,
                )
            }

            fokusSettingsComposable(Routes.SETTINGS_DRAWER_BEHAVIOR) {
                DrawerBehaviorSettingsScreen(
                        viewModel = settingsViewModel(componentActivity),
                        onNavigateBack = { navController.popBackStack() },
                        backgroundScrim = Color.Black,
                )
            }

            fokusSettingsComposable(Routes.SETTINGS_APPS_MANAGEMENT) {
                AppsManagementSettingsScreen(
                        viewModel = settingsViewModel(componentActivity),
                        onNavigateBack = { navController.popBackStack() },
                        backgroundScrim = Color.Black,
                )
            }

            fokusSettingsComposable(Routes.SETTINGS_HOME_WIDGETS) {
                HomeWidgetsSettingsScreen(
                        viewModel = settingsViewModel(componentActivity),
                        onNavigateBack = { navController.popBackStack() },
                        onOpenPomodoroSettings = {
                            navController.navigateSingleTop(Routes.SETTINGS_POMODORO)
                        },
                        backgroundScrim = Color.Black
                )
            }

            fokusSettingsComposable(Routes.SETTINGS_POMODORO) {
                PomodoroSettingsScreen(
                        viewModel = settingsViewModel(componentActivity),
                        onNavigateBack = { navController.popBackStack() },
                        backgroundScrim = Color.Black,
                )
            }

            fokusSettingsComposable(Routes.SETTINGS_DRAWER_DOT_SEARCH) {
                DrawerDotSearchSettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        backgroundScrim = Color.Black
                )
            }

            fokusSettingsComposable(Routes.SETTINGS_PROFILE_NAMES) {
                ProfileNamesSettingsScreen(
                        viewModel = settingsViewModel(componentActivity),
                        onNavigateBack = { navController.popBackStack() },
                        backgroundScrim = Color.Black
                )
            }

            fokusSettingsComposable(Routes.SETTINGS_DEVICE_CONTROL) {
                DeviceControlSettingsScreen(
                        viewModel = settingsViewModel(componentActivity),
                        onNavigateBack = { navController.popBackStack() },
                        backgroundScrim = Color.Black
                )
            }

            fokusSettingsComposable(Routes.SETTINGS_CATEGORIES) {
                CategorySettingsScreen(
                        viewModel = settingsViewModel(componentActivity),
                        onNavigateBack = { navController.popBackStack() },
                        onEditCategoryApps = { category ->
                            navController.navigateSingleTop(
                                    "${Routes.SETTINGS_CATEGORY_APPS}/${Uri.encode(category)}"
                            )
                        },
                        onOpenCategoryIconPicker = { category ->
                            navController.navigateSingleTop(Routes.iconPickerCategory(category))
                        },
                        onOpenProfileNamesSettings = {
                            navController.navigateSingleTop(Routes.SETTINGS_PROFILE_NAMES)
                        },
                        backgroundScrim = Color.Black
                )
            }

            fokusSettingsComposable(Routes.SETTINGS_ICON_PICKER_CATEGORY) { entry ->
                val category =
                        Uri.decode(entry.arguments?.getString("category").orEmpty())
                val settingsVm = settingsViewModel(componentActivity)
                val uiState by settingsVm.uiState.collectAsStateWithLifecycle()
                CategoryIconPickerScreen(
                        category = category,
                        iconOverrides = uiState.categoryDrawerIconOverrides,
                        onSelect = { name ->
                            settingsVm.setCategoryDrawerIcon(category, name)
                            navController.popBackStack()
                        },
                        onNavigateBack = { navController.popBackStack() },
                        backgroundScrim = Color.Black,
                )
            }

            fokusSettingsComposable("${Routes.SETTINGS_CATEGORY_APPS}/{category}") { entry ->
                CategoryAppsScreen(
                        category =
                                Uri.decode(entry.arguments?.getString("category").orEmpty()),
                        viewModel = settingsViewModel(componentActivity),
                        onNavigateBack = { navController.popBackStack() },
                        backgroundScrim = Color.Black
                )
            }

            fokusSettingsComposable(Routes.SETTINGS_EDIT_HOME_APPS) { editBackStackEntry ->
                val homeBackStackEntry = remember(editBackStackEntry) {
                    navController.getBackStackEntry(Routes.HOME)
                }
                val homeViewModel: HomeViewModel = hiltViewModel(homeBackStackEntry)
                // Run before first frame so edit lists are not briefly empty (only "All apps" visible).
                remember(editBackStackEntry.id) {
                    homeViewModel.startEditingHomeApps()
                    true
                }
                EditHomeAppsScreen(
                    viewModel = homeViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    backgroundScrim = Color.Black
                )
            }

            fokusSettingsComposable(Routes.SETTINGS_EDIT_SHORTCUTS) { editBackStackEntry ->
                val homeBackStackEntry = remember(editBackStackEntry) {
                    navController.getBackStackEntry(Routes.HOME)
                }
                val homeViewModel: HomeViewModel = hiltViewModel(homeBackStackEntry)
                remember(editBackStackEntry.id) {
                    homeViewModel.startEditingShortcuts()
                    true
                }
                EditShortcutsScreen(
                    viewModel = homeViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenIconPicker = { index ->
                        navController.navigateSingleTop(Routes.iconPickerShortcut(index))
                    },
                    backgroundScrim = Color.Black
                )
            }

            fokusSettingsComposable(Routes.SETTINGS_ICON_PICKER_SHORTCUT) { entry ->
                val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
                val homeBackStackEntry = remember(entry) {
                    navController.getBackStackEntry(Routes.HOME)
                }
                val homeViewModel: HomeViewModel = hiltViewModel(homeBackStackEntry)
                val editShortcuts by homeViewModel.editRightShortcuts.collectAsStateWithLifecycle()
                val storedIconKey = editShortcuts.getOrNull(index)?.iconName ?: "circle"
                IconPickerScreen(
                        storedIconKey = storedIconKey,
                        titleText = stringResource(R.string.edit_shortcuts_choose_icon),
                        onSelect = { name ->
                            homeViewModel.updateShortcutIcon(index, name)
                            navController.popBackStack()
                        },
                        onNavigateBack = { navController.popBackStack() },
                        backgroundScrim = Color.Black,
                )
            }
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
        is ShortcutTarget.PhoneDial ->
            Intent(Intent.ACTION_DIAL, "tel:".toUri()).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        is ShortcutTarget.LauncherShortcut -> null
        is ShortcutTarget.WidgetPage -> null
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
