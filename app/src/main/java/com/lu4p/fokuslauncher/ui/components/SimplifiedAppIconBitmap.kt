package com.lu4p.fokuslauncher.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.get
import androidx.core.graphics.set

/**
 * Builds a tintable bitmap for simplified (monochrome) app icons.
 *
 * Preference order:
 * 1. Adaptive icon monochrome layer (API 33+) when present
 * 2. Adaptive icon foreground (zoomed to the safe zone)
 * 3. Full drawable as-is
 *
 * Non-monochrome sources are converted to a luminance alpha mask so Compose can tint them
 * with the launcher text color. Apps without a dedicated monochrome layer still render as a
 * readable silhouette rather than blank or full-color branding.
 */
internal fun buildSimplifiedAppIconBitmap(drawable: Drawable, sizePx: Int): Bitmap {
    val safeSize = sizePx.coerceAtLeast(1)
    val source = resolveSimplifiedSource(drawable)
    val rendered = renderSimplifiedSource(source, safeSize)
    return rendered.toTintableAlphaMask(
            mode =
                    when {
                        source.isAlreadyMonochrome -> TintableMaskMode.PRESERVE_ALPHA
                        rendered.hasTransparentPixels() -> TintableMaskMode.PRESERVE_ALPHA
                        else -> TintableMaskMode.LUMINANCE_CONTRAST
                    }
    )
}

internal enum class TintableMaskMode {
    /** Use the source alpha channel; set RGB to white for Compose tinting. */
    PRESERVE_ALPHA,
    /**
     * Fully opaque bitmaps: encode shape via luminance contrast (dark or light glyphs both
     * become visible silhouettes).
     */
    LUMINANCE_CONTRAST,
}

internal data class SimplifiedIconSource(
        val drawable: Drawable,
        val isAdaptiveLayer: Boolean,
        val isAlreadyMonochrome: Boolean,
)

internal fun resolveSimplifiedSource(drawable: Drawable): SimplifiedIconSource {
    if (drawable is AdaptiveIconDrawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            drawable.monochrome?.let { mono ->
                return SimplifiedIconSource(
                        drawable = mono,
                        isAdaptiveLayer = true,
                        isAlreadyMonochrome = true,
                )
            }
        }
        return SimplifiedIconSource(
                drawable = drawable.foreground,
                isAdaptiveLayer = true,
                isAlreadyMonochrome = false,
        )
    }
    val isVector =
            drawable is VectorDrawable || drawable.javaClass.name.contains("VectorDrawable")
    return SimplifiedIconSource(
            drawable = drawable,
            isAdaptiveLayer = false,
            isAlreadyMonochrome = isVector,
    )
}

private fun renderSimplifiedSource(source: SimplifiedIconSource, sizePx: Int): Bitmap {
    if (!source.isAdaptiveLayer) {
        return source.drawable.toBitmap(width = sizePx, height = sizePx)
    }
    // Adaptive icons are 108dp with a 72dp safe zone; scale up to fill the list slot.
    val scale = 1.42f
    val drawSize = (sizePx * scale).toInt().coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val offset = (sizePx - drawSize) / 2
    source.drawable.setBounds(offset, offset, sizePx - offset, sizePx - offset)
    source.drawable.draw(canvas)
    return bmp
}

internal fun Bitmap.hasTransparentPixels(): Boolean {
    val width = this.width
    val height = this.height
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (Color.alpha(this[x, y]) < 255) return true
        }
    }
    return false
}

/**
 * Converts [this] into a white RGB bitmap whose alpha encodes the glyph so Compose [Icon]
 * tinting matches launcher text color.
 */
internal fun Bitmap.toTintableAlphaMask(mode: TintableMaskMode): Bitmap {
    val width = this.width.coerceAtLeast(1)
    val height = this.height.coerceAtLeast(1)
    val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    if (mode == TintableMaskMode.PRESERVE_ALPHA) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                val srcAlpha = Color.alpha(this[x, y])
                out[x, y] =
                        if (srcAlpha == 0) Color.TRANSPARENT
                        else Color.argb(srcAlpha, 255, 255, 255)
            }
        }
        return out
    }

    // Opaque bitmap: pick the polarity (dark-on-light vs light-on-dark) that yields more ink.
    var darkInk = 0L
    var lightInk = 0L
    val luminances = IntArray(width * height)
    var i = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = this[x, y]
            val luminance =
                    (0.299f * Color.red(pixel) +
                                    0.587f * Color.green(pixel) +
                                    0.114f * Color.blue(pixel))
                            .toInt()
                            .coerceIn(0, 255)
            luminances[i++] = luminance
            darkInk += (255 - luminance)
            lightInk += luminance
        }
    }
    val useDarkGlyphs = darkInk >= lightInk
    i = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val luminance = luminances[i++]
            val alpha = if (useDarkGlyphs) 255 - luminance else luminance
            out[x, y] =
                    if (alpha == 0) Color.TRANSPARENT else Color.argb(alpha, 255, 255, 255)
        }
    }
    return out
}
