package com.lu4p.fokuslauncher.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DotSearchSyntaxTest {

    @Test
    fun `default pattern requires whitespace after dot`() {
        val r = DotSearchSyntax.parse(". cats")
        assertEquals(DotSearchParsed.Default("cats"), r)
    }

    @Test
    fun `default pattern multiple spaces`() {
        assertEquals(DotSearchParsed.Default("x"), DotSearchSyntax.parse(".   x"))
    }

    @Test
    fun `default rejects dot without space before query`() {
        assertNull(DotSearchSyntax.parse(".cats"))
    }

    @Test
    fun `alias pattern letter and space`() {
        assertEquals(
                DotSearchParsed.Alias('a', "buy soap"),
                DotSearchSyntax.parse(".a buy soap")
        )
    }

    @Test
    fun `alias rejects digit`() {
        assertNull(DotSearchSyntax.parse(".1 two"))
    }

    @Test
    fun `alias rejects uppercase letter`() {
        assertNull(DotSearchSyntax.parse(".A soap"))
    }

    @Test
    fun `alias takes precedence when second char is alphanumeric`() {
        assertTrue(DotSearchSyntax.parse(".a stuff") is DotSearchParsed.Alias)
    }

    @Test
    fun `dot space then text is default not alias`() {
        assertEquals(DotSearchParsed.Default("a b"), DotSearchSyntax.parse(". a b"))
    }

    @Test
    fun `empty body no match`() {
        assertNull(DotSearchSyntax.parse(". "))
        assertNull(DotSearchSyntax.parse(".   "))
        assertNull(DotSearchSyntax.parse(".a "))
        assertNull(DotSearchSyntax.parse(".a  "))
    }

    @Test
    fun `non dot prefix no match`() {
        assertNull(DotSearchSyntax.parse("foo"))
        assertNull(DotSearchSyntax.parse(" . x"))
    }

    @Test
    fun isPossibleDotSearchPrefix() {
        assertTrue(DotSearchSyntax.isPossibleDotSearchPrefix("."))
        assertTrue(DotSearchSyntax.isPossibleDotSearchPrefix(".a"))
    }
}
