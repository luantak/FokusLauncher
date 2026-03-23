package com.lu4p.fokuslauncher.data.font

import android.graphics.fonts.Font
import android.graphics.fonts.SystemFonts
import android.os.Build
import android.util.Xml
import java.io.File
import java.io.FileInputStream
import org.xmlpull.v1.XmlPullParser

/**
 * Resolves font family names suitable for [android.graphics.Typeface.create] /
 * Compose [androidx.compose.ui.text.font.DeviceFontFamilyName].
 *
 * Reads device [fonts.xml] configs when readable, and on API 31+ merges names from
 * [SystemFonts] (via [Font.getFamilyName] when present on the runtime classpath).
 * Returns an empty list if nothing could be read.
 */
object SystemFontFamiliesProvider {

    fun loadSortedDistinct(): List<String> {
        val names = LinkedHashSet<String>()
        for (path in CONFIG_PATHS) {
            val file = File(path)
            if (file.isFile && file.canRead()) {
                runCatching { FileInputStream(file).use { parseFontsConfig(it, names) } }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { addFamiliesFromSystemFonts(names) }
        }
        return names.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    private fun parseFontsConfig(stream: FileInputStream, out: MutableSet<String>) {
        val parser = Xml.newPullParser()
        parser.setInput(stream, null)
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "family" ->
                            parser.getAttributeValue(null, "name")?.trim()?.takeIf { it.isNotEmpty() }
                                    ?.let { out.add(it) }
                    "alias" ->
                            parser.getAttributeValue(null, "name")?.trim()?.takeIf { it.isNotEmpty() }
                                    ?.let { out.add(it) }
                }
            }
            event = parser.next()
        }
    }

    private fun addFamiliesFromSystemFonts(out: MutableSet<String>) {
        val fonts: Set<Font> = SystemFonts.getAvailableFonts()
        for (font in fonts) {
            extractFamilyName(font)?.trim()?.takeIf { it.isNotEmpty() }?.let { out.add(it) }
        }
    }

    private fun extractFamilyName(font: Font): String? {
        return runCatching {
                    val m = font.javaClass.getMethod("getFamilyName")
                    m.invoke(font) as? String
                }
                .getOrNull()
                ?: runCatching {
                    val m = font.javaClass.getDeclaredMethod("getFamilyName")
                    m.isAccessible = true
                    m.invoke(font) as? String
                }
                .getOrNull()
    }

    private val CONFIG_PATHS =
            listOf(
                    "/system/etc/fonts.xml",
                    "/system_ext/etc/fonts.xml",
                    "/product/etc/fonts_customization.xml",
            )
}
