package com.lu4p.fokuslauncher.data.iconpack

import java.io.InputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Parses an Arcticons-style `appfilter.xml` into component → drawable-name mappings.
 *
 * Expected item shape:
 * ```
 * <item component="ComponentInfo{pkg/cls}" drawable="icon_name" />
 * ```
 */
object ArcticonsAppfilterParser {
    fun parse(input: InputStream): Map<String, String> {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(input, "utf-8")
        return parse(parser)
    }

    fun parse(parser: XmlPullParser): Map<String, String> {
        val map = HashMap<String, String>(8192)
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name.equals("item", ignoreCase = true)) {
                var component: String? = null
                var drawable: String? = null
                for (i in 0 until parser.attributeCount) {
                    when (parser.getAttributeName(i)) {
                        "component" -> component = parser.getAttributeValue(i)
                        "drawable" -> drawable = parser.getAttributeValue(i)
                    }
                }
                val componentKey = component?.trim().orEmpty()
                val drawableName = drawable?.trim().orEmpty()
                if (componentKey.isNotEmpty() && drawableName.isNotEmpty()) {
                    // First mapping wins (matches typical launcher behavior).
                    map.putIfAbsent(componentKey, drawableName)
                }
            }
            event = parser.next()
        }
        return map
    }

    /** Builds the ComponentInfo key used in appfilter entries. */
    fun componentKey(packageName: String, className: String): String =
            "ComponentInfo{$packageName/$className}"
}
