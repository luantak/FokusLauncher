package com.lu4p.fokuslauncher.data.model

import android.content.Context
import android.content.res.Configuration
import com.lu4p.fokuslauncher.R
import java.util.Locale

/**
 * Stable keys for Android-inferred app categories.
 *
 * These values are persisted and compared internally. UI should translate them at display time.
 */
object SystemCategoryKeys {
    const val UTILITIES = "Utilities"
    const val GAMES = "Games"
    const val PRODUCTIVITY = "Productivity"
    const val SOCIAL = "Social"
    const val MEDIA = "Media"

    private val supportedLocaleTags = listOf("en", "de", "pl")

    private data class CategoryDef(
        val key: String,
        val labelResId: Int
    )

    private val inferredCategories =
        listOf(
            CategoryDef(UTILITIES, R.string.inferred_category_utilities),
            CategoryDef(GAMES, R.string.inferred_category_games),
            CategoryDef(PRODUCTIVITY, R.string.inferred_category_productivity),
            CategoryDef(SOCIAL, R.string.inferred_category_social),
            CategoryDef(MEDIA, R.string.inferred_category_media)
        )

    /** English keys in the standard order used for default category definitions (drawer / settings). */
    fun defaultOrderedCategoryNames(): List<String> = inferredCategories.map { it.key }

    fun normalize(context: Context, rawCategory: String): String {
        val trimmed = rawCategory.trim()
        if (trimmed.isBlank()) return ""

        if (trimmed.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true)) {
            return ReservedCategoryNames.ALL_APPS
        }
        if (trimmed.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true)) {
            return ReservedCategoryNames.PRIVATE
        }
        if (trimmed.equals(ReservedCategoryNames.WORK, ignoreCase = true)) {
            return ReservedCategoryNames.WORK
        }
        if (trimmed.equals(ReservedCategoryNames.UNCATEGORIZED, ignoreCase = true)) {
            return ReservedCategoryNames.UNCATEGORIZED
        }

        inferredCategories.firstOrNull { def ->
            trimmed.equals(def.key, ignoreCase = true)
        }?.let { def ->
            return def.key
        }

        inferredCategories.forEach { def ->
            if (matchesAnySupportedLabel(context, trimmed, def.labelResId)) return def.key
        }

        return trimmed
    }

    fun displayLabel(context: Context, category: String): String {
        return when (normalize(context, category)) {
            ReservedCategoryNames.UNCATEGORIZED -> context.getString(R.string.drawer_filter_uncategorized)
            UTILITIES -> context.getString(R.string.inferred_category_utilities)
            GAMES -> context.getString(R.string.inferred_category_games)
            PRODUCTIVITY -> context.getString(R.string.inferred_category_productivity)
            SOCIAL -> context.getString(R.string.inferred_category_social)
            MEDIA -> context.getString(R.string.inferred_category_media)
            else -> category.trim()
        }
    }

    private fun matchesAnySupportedLabel(
        context: Context,
        category: String,
        labelResId: Int
    ): Boolean {
        return supportedLocaleTags.any { tag ->
            try {
                localizedString(context, tag, labelResId).equals(category, ignoreCase = true)
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun localizedString(context: Context, localeTag: String, resId: Int): String {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(localeTag))
        return context.createConfigurationContext(config).getString(resId)
    }
}
