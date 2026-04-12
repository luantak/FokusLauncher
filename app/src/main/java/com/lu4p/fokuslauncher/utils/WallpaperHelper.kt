package com.lu4p.fokuslauncher.utils

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.core.graphics.ColorUtils
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
     * `true` if the **system** home wallpaper reads as uniform near-black when downsampled (not a
     * photo), `false` if it clearly is not, or `null` if the wallpaper could not be read (keep the
     * stored preference — do **not** treat `null` as non-black).
     */
    suspend fun homeWallpaperEffectivelyBlackOrNull(context: Context): Boolean? =
            withContext(Dispatchers.IO) {
                runCatching { classifyHomeWallpaperBlackness(context) }.getOrNull()
            }

    /**
     * Never throws — wallpaper APIs can fail with [SecurityException] on newer Android releases.
     */
    private fun classifyHomeWallpaperBlackness(context: Context): Boolean? {
        val wallpaperManager = WallpaperManager.getInstance(context.applicationContext)

        // From Android 13 (T), getWallpaperFile often returns the default system asset for
        // third-party apps — decoding it misclassifies black wallpapers. Prefer drawables on T+.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                        wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)
                    }
                    .getOrNull()
                    ?.use { pfd ->
                        runCatching { BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor) }
                                .getOrNull()
                    }
                    ?.let { full ->
                        try {
                            sampleFullWallpaperBitmapIsEffectivelyBlack(full)
                        } finally {
                            runCatching { full.recycle() }
                        }
                    }
                    ?.let { return it }
        }

        drawableWallpaperEffectivelyBlackOrNull(wallpaperManager)?.let { return it }

        return null
    }

    private fun sampleFullWallpaperBitmapIsEffectivelyBlack(full: Bitmap): Boolean? {
        val toSample =
                if (full.config == Bitmap.Config.HARDWARE) {
                    full.copy(Bitmap.Config.ARGB_8888, false) ?: return null
                } else {
                    full
                }
        try {
            val sampleW = 32
            val sampleH = 32
            if (toSample.width == sampleW && toSample.height == sampleH) {
                return bitmapSampleIsNearUniformBlack(toSample)
            }
            val scaled = Bitmap.createScaledBitmap(toSample, sampleW, sampleH, true)
            return try {
                bitmapSampleIsNearUniformBlack(scaled)
            } finally {
                scaled.recycle()
            }
        } finally {
            if (toSample !== full) toSample.recycle()
        }
    }

    private fun drawableWallpaperEffectivelyBlackOrNull(
            wallpaperManager: WallpaperManager
    ): Boolean? {
        return runCatching {
            val drawable =
                    runCatching { wallpaperManager.peekDrawable() }.getOrNull()
                            ?: runCatching { wallpaperManager.drawable }.getOrNull()
                            ?: return@runCatching null
            val sampleW = 32
            val sampleH = 32
            val bitmap = createBitmap(sampleW, sampleH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            // Do not require positive intrinsic size — many wallpaper drawables report -1×-1.
            drawable.setBounds(0, 0, sampleW, sampleH)
            drawable.draw(canvas)
            try {
                bitmapSampleIsNearUniformBlack(bitmap)
            } finally {
                bitmap.recycle()
            }
        }.getOrNull()
    }

    /**
     * Near-black via luminance (tolerates JPEG/scaling; stricter than treating gray photos as
     * black).
     */
    private fun bitmapSampleIsNearUniformBlack(bitmap: Bitmap): Boolean? {
        return runCatching {
            val luminanceCeiling = 0.045
            var opaqueSamples = 0
            val w = bitmap.width
            val h = bitmap.height
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val pixel = bitmap.getPixel(x, y)
                    if (AndroidColor.alpha(pixel) == 0) continue
                    opaqueSamples++
                    if (ColorUtils.calculateLuminance(pixel) > luminanceCeiling) {
                        return@runCatching false
                    }
                }
            }
            if (opaqueSamples > 0) true else null
        }.getOrNull()
    }
}
