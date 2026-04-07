package com.lu4p.fokuslauncher.utils

import java.text.Normalizer
import java.util.Locale

private val combiningMarks = Regex("\\p{M}+")
/** Letters and numbers across all Unicode scripts; everything else is ignored for matching. */
private val nonLetterOrDigit = Regex("[^\\p{L}\\p{N}]+")

/**
 * Decomposes accents, strips combining marks, lowercases with a stable locale, then removes
 * punctuation/separators so e.g. "F-Droid" matches "fdroid" (issue #48).
 */
fun String.normalizedForSearch(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD)
                .replace(combiningMarks, "")
                .lowercase(Locale.ROOT)
                .replace(nonLetterOrDigit, "")

fun String.containsNormalizedSearch(substring: String): Boolean =
        normalizedForSearch().contains(substring.normalizedForSearch())
