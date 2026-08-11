package com.lu4p.fokuslauncher.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.core.graphics.get
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SimplifiedAppIconBitmapTest {

    @Test
    fun resolveSimplifiedSource_usesFullDrawableForNonAdaptive() {
        val drawable = ColorDrawable(Color.RED)
        val source = resolveSimplifiedSource(drawable)
        assertFalse(source.isAdaptiveLayer)
        assertFalse(source.isAlreadyMonochrome)
        assertEquals(drawable, source.drawable)
    }

    @Test
    fun toTintableAlphaMask_mapsDarkOpaqueGlyphsViaContrast() {
        val src = Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888)
        src.setPixel(0, 0, Color.WHITE)
        src.setPixel(1, 0, Color.BLACK)

        val mask = src.toTintableAlphaMask(TintableMaskMode.LUMINANCE_CONTRAST)

        // Dark ink dominates → black pixel becomes opaque white silhouette.
        assertEquals(0, Color.alpha(mask[0, 0]))
        assertEquals(255, Color.alpha(mask[1, 0]))
        assertEquals(255, Color.red(mask[1, 0]))
    }

    @Test
    fun toTintableAlphaMask_preservesAlphaForMonochromeGlyphs() {
        val src = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        src.setPixel(0, 0, Color.argb(128, 10, 20, 30))

        val mask = src.toTintableAlphaMask(TintableMaskMode.PRESERVE_ALPHA)

        assertEquals(128, Color.alpha(mask[0, 0]))
        assertEquals(255, Color.red(mask[0, 0]))
        assertEquals(255, Color.green(mask[0, 0]))
        assertEquals(255, Color.blue(mask[0, 0]))
    }

    @Test
    fun buildSimplifiedAppIconBitmap_producesTintableWhiteRgb() {
        val drawable = ColorDrawable(Color.RED)
        drawable.setBounds(0, 0, 8, 8)

        val simplified = buildSimplifiedAppIconBitmap(drawable, sizePx = 8)

        assertEquals(8, simplified.width)
        assertEquals(8, simplified.height)
        val pixel = simplified[4, 4]
        assertTrue("expected visible silhouette alpha, got ${Color.alpha(pixel)}", Color.alpha(pixel) > 0)
        assertEquals(255, Color.red(pixel))
        assertEquals(255, Color.green(pixel))
        assertEquals(255, Color.blue(pixel))
    }

    @Test
    fun hasTransparentPixels_detectsPartialAlpha() {
        val src = Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888)
        src.setPixel(0, 0, Color.RED)
        src.setPixel(1, 0, Color.argb(0, 0, 0, 0))
        assertTrue(src.hasTransparentPixels())

        val opaque = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        opaque.setPixel(0, 0, Color.GREEN)
        assertFalse(opaque.hasTransparentPixels())
    }
}
