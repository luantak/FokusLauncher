package com.lu4p.fokuslauncher.utils

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper for reading the system wallpaper and providing it
 * as a background behind the launcher home screen.
 */
@Singleton
class WallpaperHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val wallpaperManager: WallpaperManager by lazy {
        WallpaperManager.getInstance(context)
    }

    /**
     * Returns the current system wallpaper as a Bitmap, or null if unavailable.
     * Requires READ_EXTERNAL_STORAGE on older APIs; newer APIs provide it without.
     */
    fun getWallpaperBitmap(): Bitmap? {
        return try {
            val drawable = wallpaperManager.drawable
            if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks if wallpaper access is available on this device.
     */
    fun isWallpaperAvailable(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.isSetWallpaperAllowed
            } else {
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
