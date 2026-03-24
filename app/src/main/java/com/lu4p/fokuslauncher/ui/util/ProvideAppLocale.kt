package com.lu4p.fokuslauncher.ui.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Wraps the activity [Context] so [getBaseContext] stays the real activity (Hilt / [LocalActivity]
 * resolution keep working) while [getResources] / [getAssets] use a locale-specific configuration.
 */
private class LocaleResourcesContextWrapper(
        baseContext: Context,
        private val localizedContext: Context
) : ContextWrapper(baseContext) {

    override fun getResources(): Resources = localizedContext.resources

    override fun getAssets(): AssetManager = localizedContext.assets
}

/**
 * Applies the in-app locale to Compose [stringResource] / [LocalConfiguration] without
 * [android.app.Activity.recreate]. When [localeTag] is blank, the activity’s configuration is
 * used (system default after [androidx.appcompat.app.AppCompatDelegate]).
 */
@Composable
fun ProvideAppLocale(localeTag: String, content: @Composable () -> Unit) {
    val activityContext = LocalContext.current
    val fontScale = LocalConfiguration.current.fontScale
    if (localeTag.isBlank()) {
        content()
    } else {
        val wrappedContext =
                remember(localeTag, fontScale) {
                    val locale = Locale.forLanguageTag(localeTag)
                    val config = Configuration(activityContext.resources.configuration)
                    config.setLocale(locale)
                    val localized = activityContext.createConfigurationContext(config)
                    LocaleResourcesContextWrapper(activityContext, localized)
                }
        val localizedConfiguration =
                remember(wrappedContext) {
                    Configuration(wrappedContext.resources.configuration)
                }
        CompositionLocalProvider(
                LocalContext provides wrappedContext,
                LocalConfiguration provides localizedConfiguration
        ) {
            content()
        }
    }
}
