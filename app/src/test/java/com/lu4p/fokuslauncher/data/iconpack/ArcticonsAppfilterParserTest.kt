package com.lu4p.fokuslauncher.data.iconpack

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArcticonsAppfilterParserTest {

    @Test
    fun parse_mapsComponentToDrawable() {
        val xml =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <resources>
                    <item component="ComponentInfo{com.example.app/com.example.app.Main}" drawable="example" />
                    <item component="ComponentInfo{com.other/com.other.Main}" drawable="other" />
                </resources>
                """.trimIndent()

        val map = ArcticonsAppfilterParser.parse(ByteArrayInputStream(xml.toByteArray()))

        assertEquals(2, map.size)
        assertEquals(
                "example",
                map["ComponentInfo{com.example.app/com.example.app.Main}"],
        )
        assertEquals("other", map["ComponentInfo{com.other/com.other.Main}"])
    }

    @Test
    fun parse_firstMappingWinsForDuplicateComponent() {
        val xml =
                """
                <resources>
                    <item component="ComponentInfo{com.example/com.example.Main}" drawable="first" />
                    <item component="ComponentInfo{com.example/com.example.Main}" drawable="second" />
                </resources>
                """.trimIndent()

        val map = ArcticonsAppfilterParser.parse(ByteArrayInputStream(xml.toByteArray()))
        assertEquals("first", map["ComponentInfo{com.example/com.example.Main}"])
    }

    @Test
    fun componentKey_formatsLikeAppfilter() {
        assertEquals(
                "ComponentInfo{com.example/com.example.Main}",
                ArcticonsAppfilterParser.componentKey("com.example", "com.example.Main"),
        )
    }

    @Test
    fun packageNameFromComponentKey_extractsPackage() {
        assertEquals(
                "com.example",
                ArcticonsIconPackRepository.packageNameFromComponentKey(
                        "ComponentInfo{com.example/com.example.Main}"
                ),
        )
        assertNull(ArcticonsIconPackRepository.packageNameFromComponentKey("not-a-key"))
    }

    @Test
    fun whitelist_containsOfficialArcticonsPackagesOnly() {
        assertTrue(ArcticonsPackages.isWhitelisted(ArcticonsPackages.NORMAL))
        assertTrue(ArcticonsPackages.isWhitelisted(ArcticonsPackages.BLACK))
        assertTrue(ArcticonsPackages.isWhitelisted(ArcticonsPackages.YOU))
        assertTrue(ArcticonsPackages.isWhitelisted(ArcticonsPackages.DAY_NIGHT))
        assertTrue(!ArcticonsPackages.isWhitelisted("com.example.random.icons"))
    }
}
