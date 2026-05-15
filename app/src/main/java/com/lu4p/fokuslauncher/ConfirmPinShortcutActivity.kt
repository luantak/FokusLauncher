package com.lu4p.fokuslauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Bundle

/**
 * Accepts browser PWA/shortcut pin requests so browsers expose "Add to Home screen" while Fokus
 * is the default launcher. The pinned shortcut is stored by Android's launcher service and picked
 * up by AppRepository through LauncherApps pinned shortcuts.
 */
class ConfirmPinShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT) {
            val request = intent?.pinItemRequest()
            if (request?.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
                request.accept()
            }
        }

        finish()
    }

    @Suppress("DEPRECATION")
    private fun Intent.pinItemRequest(): LauncherApps.PinItemRequest? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableExtra(
                        LauncherApps.EXTRA_PIN_ITEM_REQUEST,
                        LauncherApps.PinItemRequest::class.java,
                )
            } else {
                getParcelableExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST)
            }
}
