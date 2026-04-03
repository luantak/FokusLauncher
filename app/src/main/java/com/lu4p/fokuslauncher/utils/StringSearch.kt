package com.lu4p.fokuslauncher.utils

import java.text.Normalizer
import java.util.Locale

private val combiningMarks = Regex("\\p{M}+")

/** Decomposes accents, strips combining marks, lowercases with a stable locale. */
fun String.normalizedForSearch(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD).replace(combiningMarks, "").lowercase(Locale.ROOT)

fun String.containsNormalizedSearch(substring: String): Boolean =
        normalizedForSearch().contains(substring.normalizedForSearch())
