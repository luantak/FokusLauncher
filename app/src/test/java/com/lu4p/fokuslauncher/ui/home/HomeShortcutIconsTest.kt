package com.lu4p.fokuslauncher.ui.home

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.graphics.drawable.ColorDrawable
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeShortcutIconsTest {
    private val context = RuntimeEnvironment.getApplication()
    private val host = AppInfo("com.example.browser", "Browser", null)
    private val shortcutApp = host.copy(launcherShortcutId = "web-app")

    @Test
    fun appTargetUsesHostRatherThanPinnedShortcut() = runBlocking {
        val icon = ColorDrawable()
        val result = loadHomeShortcutIcon(context, HomeShortcut(target = ShortcutTarget.App(host.packageName)),
            listOf(shortcutApp, host)) { app ->
            assertEquals(host, app)
            icon
        }
        assertEquals(icon, result)
    }

    @Test
    fun launcherShortcutUsesMatchingShortcut() = runBlocking {
        val icon = ColorDrawable()
        val result = loadHomeShortcutIcon(context,
            HomeShortcut(target = ShortcutTarget.LauncherShortcut(host.packageName, "web-app")),
            listOf(host, shortcutApp)) { app ->
            assertEquals(shortcutApp, app)
            icon
        }
        assertEquals(icon, result)
    }

    @Test
    fun missingProfileNeverUsesOwnerApp() = runBlocking {
        assertNull(loadHomeShortcutIcon(context,
            HomeShortcut(target = ShortcutTarget.App(host.packageName), profileKey = "work"),
            listOf(host)) { error("Wrong profile icon requested") })
    }

    @Test
    fun builtInWidgetPageKeepsCustomIcon() = runBlocking {
        assertNull(loadHomeShortcutIcon(context, HomeShortcut(target = ShortcutTarget.WidgetPage),
            listOf(host)) { error("Widget page is not an installed app") })
    }

    @Test
    fun phoneUsesResolvedDialer() = runBlocking {
        val intent = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:"))
        shadowOf(context.packageManager).addResolveInfoForIntent(intent, ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = host.packageName
                name = "DialerActivity"
            }
        })
        val icon = ColorDrawable()
        assertEquals(icon, loadHomeShortcutIcon(context,
            HomeShortcut(target = ShortcutTarget.PhoneDial), listOf(host)) { app ->
            assertEquals(host.packageName, app.packageName)
            assertEquals(ComponentName(host.packageName, "DialerActivity"), app.componentName)
            icon
        })
    }

    @Test
    fun explicitDeepLinkUsesTargetComponent() = runBlocking {
        val component = ComponentName(host.packageName, "DeepLinkActivity")
        val target = ShortcutTarget.DeepLink(Intent().setComponent(component).toUri(Intent.URI_INTENT_SCHEME))
        val icon = ColorDrawable()
        assertEquals(icon, loadHomeShortcutIcon(context, HomeShortcut(target = target), listOf(host)) { app ->
            assertEquals(component, app.componentName)
            icon
        })
    }

    @Test
    fun malformedDeepLinkFallsBackWithoutLoading() = runBlocking {
        assertNull(loadHomeShortcutIcon(context,
            HomeShortcut(target = ShortcutTarget.DeepLink("intent:#Intent;component=broken;end")),
            emptyList()) { error("Invalid target icon requested") })
    }
}
