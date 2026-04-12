package com.lu4p.fokuslauncher.utils

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WallpaperHelper {
    suspend fun setBlackWallpaper(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val width = context.resources.displayMetrics.widthPixels
                val height = context.resources.displayMetrics.heightPixels
                val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(AndroidColor.BLACK)

                val wallpaperManager = WallpaperManager.getInstance(context)
                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                try {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                } catch (_: Exception) {
                    // Lock screen wallpaper may fail on some devices, ignore
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * True if the **system** home wallpaper reads as uniform near-black when downsampled (not a
     * photo). Used to unlock neon colors and glow; any brighter pixels or read failure → false.
     */
    suspend fun isHomeWallpaperEffectivelyBlack(context: Context): Boolean =
            withContext(Dispatchers.IO) {
                runCatching {
                    val wallpaperManager = WallpaperManager.getInstance(context)
                    val drawable = wallpaperManager.drawable ?: return@runCatching false
                    val iw = drawable.intrinsicWidth
                    val ih = drawable.intrinsicHeight
                    if (iw <= 0 || ih <= 0) return@runCatching false
                    val sampleW = 32
                    val sampleH = 32
                    val bitmap = createBitmap(sampleW, sampleH, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, sampleW, sampleH)
                    drawable.draw(canvas)
                    // Above this per channel → treat as a real image, not solid black.
                    val channelCeiling = 28
                    for (y in 0 until sampleH) {
                        for (x in 0 until sampleW) {
                            val pixel = bitmap.getPixel(x, y)
                            if (AndroidColor.alpha(pixel) < 255) return@runCatching false
                            val r = AndroidColor.red(pixel)
                            val g = AndroidColor.green(pixel)
                            val b = AndroidColor.blue(pixel)
                            if (r > channelCeiling || g > channelCeiling || b > channelCeiling) {
                                return@runCatching false
                            }
                        }
                    }
                    true
                }.getOrElse { false }
            }
}
