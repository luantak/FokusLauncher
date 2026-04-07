package com.lu4p.fokuslauncher.ui.home

import android.text.format.DateFormat
import com.lu4p.fokuslauncher.data.model.HomeDateFormatStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatCompactDate(date: Date, locale: Locale): String {
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEE d MMM")
    return SimpleDateFormat(pattern, locale)
            .format(date)
            .replace(",", "")
            .replace(Regex("\\s+"), " ")
            .trim()
}

/** Formats the home screen date line according to [style] and [locale]. */
internal fun formatHomeDate(date: Date, locale: Locale, style: HomeDateFormatStyle): String {
    return when (style) {
        HomeDateFormatStyle.SYSTEM_DEFAULT -> formatCompactDate(date, locale)
        HomeDateFormatStyle.US_SLASHES -> SimpleDateFormat("MM/dd/yyyy", locale).format(date)
        HomeDateFormatStyle.EU_SLASHES -> SimpleDateFormat("dd/MM/yyyy", locale).format(date)
        HomeDateFormatStyle.EU_DOTS -> SimpleDateFormat("dd.MM.yyyy", locale).format(date)
        HomeDateFormatStyle.WEEKDAY_MONTH_ABBR ->
                SimpleDateFormat("EEE MMM d, yyyy", locale).format(date)
        HomeDateFormatStyle.MONTH_LONG -> SimpleDateFormat("MMMM d, yyyy", locale).format(date)
    }
}
