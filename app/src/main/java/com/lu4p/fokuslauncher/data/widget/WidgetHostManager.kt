package com.lu4p.fokuslauncher.data.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetHostManager
@Inject
constructor(@param:ApplicationContext private val context: Context) {
    private val appWidgetHost = AppWidgetHost(context, HOST_ID)
    private val appWidgetManager = AppWidgetManager.getInstance(context)

    fun startListening() {
        runCatching { appWidgetHost.startListening() }
    }

    fun stopListening() {
        runCatching { appWidgetHost.stopListening() }
    }

    fun allocateAppWidgetId(): Int = appWidgetHost.allocateAppWidgetId()

    fun getAppWidgetIds(): IntArray = runCatching { appWidgetHost.appWidgetIds }.getOrDefault(intArrayOf())

    fun deleteAppWidgetId(appWidgetId: Int) {
        runCatching { appWidgetHost.deleteAppWidgetId(appWidgetId) }
    }

    fun bindAppWidgetIdIfAllowed(appWidgetId: Int, provider: ComponentName): Boolean =
            appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider)

    fun getAppWidgetInfo(appWidgetId: Int): AppWidgetProviderInfo? =
            appWidgetManager.getAppWidgetInfo(appWidgetId)

    fun createView(appWidgetId: Int): AppWidgetHostView? {
        val info = getAppWidgetInfo(appWidgetId) ?: return null
        return appWidgetHost.createView(context, appWidgetId, info).apply {
            setAppWidget(appWidgetId, info)
        }
    }

    companion object {
        const val HOST_ID = 1001
    }
}
