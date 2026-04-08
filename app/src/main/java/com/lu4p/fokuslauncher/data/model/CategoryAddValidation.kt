package com.lu4p.fokuslauncher.data.model

import android.content.Context
import com.lu4p.fokuslauncher.R

/**
 * Returns a failure if [normalized] is a reserved drawer category, including localized labels from
 * string resources (must match [AppRepository.addCategoryDefinition]).
 */
fun reservedCategoryAddFailure(context: Context, normalized: String): AddCategoryResult.Failure? {
    if (normalized.isBlank()) return null
    if (normalized.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true) ||
            normalized.equals(context.getString(R.string.drawer_filter_all_apps), ignoreCase = true)) {
        return AddCategoryResult.Failure.ReservedAllApps
    }
    if (normalized.equals(ReservedCategoryNames.UNCATEGORIZED, ignoreCase = true) ||
            normalized.equals(context.getString(R.string.drawer_filter_uncategorized), ignoreCase = true)
    ) {
        return AddCategoryResult.Failure.ReservedUncategorized
    }
    if (normalized.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true) ||
            normalized.equals(context.getString(R.string.drawer_filter_private), ignoreCase = true)) {
        return AddCategoryResult.Failure.ReservedPrivate
    }
    if (normalized.equals(ReservedCategoryNames.WORK, ignoreCase = true) ||
            normalized.equals(context.getString(R.string.drawer_filter_work), ignoreCase = true)) {
        return AddCategoryResult.Failure.ReservedWork
    }
    return null
}

/**
 * Reasons to block **Add** and show under the text field. Order and rules must match
 * [com.lu4p.fokuslauncher.data.repository.AppRepository.addCategoryDefinition].
 *
 * @param rawFieldText exact field contents (whitespace-only yields [AddCategoryResult.Failure.Blank]);
 * empty field yields null so the screen stays quiet before the user types.
 */
fun categoryAddFieldFailure(
        context: Context,
        rawFieldText: String,
        existingCategoryDefinitionNames: List<String>
): AddCategoryResult.Failure? {
    val trimmed = rawFieldText.trim()
    if (rawFieldText.isNotEmpty() && trimmed.isEmpty()) {
        return AddCategoryResult.Failure.Blank
    }
    if (trimmed.isEmpty()) return null
    val normalized = SystemCategoryKeys.normalize(context, trimmed)
    reservedCategoryAddFailure(context, normalized)?.let { return it }
    val duplicate =
            existingCategoryDefinitionNames.any { defined ->
                SystemCategoryKeys.normalize(context, defined)
                        .equals(normalized, ignoreCase = true)
            }
    return if (duplicate) {
        AddCategoryResult.Failure.Duplicate(normalized)
    } else {
        null
    }
}
