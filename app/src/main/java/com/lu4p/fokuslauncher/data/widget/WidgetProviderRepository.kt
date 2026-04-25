package com.lu4p.fokuslauncher.data.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Process
import com.lu4p.fokuslauncher.data.model.HOSTED_WIDGET_DEFAULT_HEIGHT_DP
import com.lu4p.fokuslauncher.data.model.clampHostedWidgetHeightDp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class WidgetProviderInfo(
        val provider: ComponentName,
        val label: String,
        val appLabel: String,
        val preview: Drawable?,
        val appIcon: Drawable?,
        val defaultHeightDp: Int,
        val appWidgetProviderInfo: AppWidgetProviderInfo,
) {
    val stableKey: String = "${provider.packageName}/${provider.className}"
}

@Singleton
class WidgetProviderRepository
@Inject
constructor(@param:ApplicationContext private val context: Context) {
    private val appWidgetManager = AppWidgetManager.getInstance(context)
    private val packageManager = context.packageManager

    fun getOwnerProfileProviders(): List<WidgetProviderInfo> =
            appWidgetManager.installedProviders
                    .filter { it.profile == null || it.profile == Process.myUserHandle() }
                    .mapNotNull { info ->
                        val provider = info.provider ?: return@mapNotNull null
                        val widgetLabel = info.loadLabel(packageManager)?.trim()
                                ?: provider.packageName
                        val appLabel =
                                runCatching {
                                            packageManager.getApplicationInfo(
                                                    provider.packageName,
                                                    0,
                                            )
                                    }
                                    .getOrNull()
                                    ?.let(packageManager::getApplicationLabel)
                                    ?.toString()
                                    ?.trim()
                                    ?: provider.packageName
                        val appIcon =
                                runCatching {
                                            info.loadIcon(context, context.resources.displayMetrics.densityDpi)
                                                    ?: packageManager.getApplicationIcon(
                                                            provider.packageName
                                                    )
                                    }
                                    .getOrNull()
                        val preview =
                                runCatching {
                                            info.loadPreviewImage(
                                                    context,
                                                    context.resources.displayMetrics.densityDpi,
                                            )
                                    }
                                    .getOrNull()
                        WidgetProviderInfo(
                                provider = provider,
                                label = widgetLabel,
                                appLabel = appLabel,
                                preview = preview,
                                appIcon = appIcon,
                                defaultHeightDp = defaultHeightDp(info),
                                appWidgetProviderInfo = info,
                        )
                    }
                    .sortedWith(
                            compareBy<WidgetProviderInfo> { it.appLabel.lowercase() }
                                    .thenBy { it.label.lowercase() }
                                    .thenBy { it.provider.className }
                    )

    private fun defaultHeightDp(info: AppWidgetProviderInfo): Int {
        val heightDp =
                listOf(info.minResizeHeight, info.minHeight)
                        .firstOrNull { it > 0 }
                        ?: return HOSTED_WIDGET_DEFAULT_HEIGHT_DP
        return clampHostedWidgetHeightDp(heightDp)
    }
}
