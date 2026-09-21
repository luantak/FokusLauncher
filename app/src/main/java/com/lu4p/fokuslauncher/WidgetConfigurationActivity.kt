package com.lu4p.fokuslauncher

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.lu4p.fokuslauncher.data.widget.WidgetHostManager

/**
 * Starts widget configuration through [AppWidgetHost], then forwards the provider's result to the
 * widget page.
 *
 * Providers do not have to export their configuration activity. Calling the activity directly can
 * therefore throw a [SecurityException]. AppWidgetHost asks the system to launch it on our behalf.
 */
class WidgetConfigurationActivity : Activity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId =
                intent.getIntExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID,
                )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finishWithResult(RESULT_CANCELED, null)
            return
        }

        if (savedInstanceState == null) {
            try {
                AppWidgetHost(applicationContext, WidgetHostManager.HOST_ID)
                        .startAppWidgetConfigureActivityForResult(
                                this,
                                appWidgetId,
                                0,
                                REQUEST_CONFIGURE,
                                null,
                        )
            } catch (_: ActivityNotFoundException) {
                finishWithResult(RESULT_CANCELED, null)
            } catch (_: SecurityException) {
                finishWithResult(RESULT_CANCELED, null)
            }
        }
    }

    @Deprecated("Deprecated in the framework, but required by AppWidgetHost's configuration API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE) {
            finishWithResult(resultCode, data)
        }
    }

    private fun finishWithResult(resultCode: Int, data: Intent?) {
        val result =
                data ?: Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(resultCode, result)
        finish()
    }

    companion object {
        private const val REQUEST_CONFIGURE = 1

        fun createIntent(context: Context, appWidgetId: Int): Intent =
                Intent(context, WidgetConfigurationActivity::class.java).putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        appWidgetId,
                )
    }
}
