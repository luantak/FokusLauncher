package com.lu4p.fokuslauncher.ui.components

import com.lu4p.fokuslauncher.ui.util.clickableNoRippleWithSystemSound
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Scale for the AM/PM segment relative to [displayLarge] in 12h mode. Kept smaller than the time
 * on the same row at large launcher font scales.
 */
private const val AmPmLineScale = 0.30f

/** Last whitespace + Latin AM/PM / a.m. / p.m. at end of string (after normalizing spaces). */
private val TrailingAmPmLatin = Regex("""(?i)\s+([ap]\.?m\.?)$""")

/**
 * Splits [formattedTime] when the system inserts an explicit newline before the day period (e.g.
 * `"3:58\\nPM"`).
 */
internal fun splitClockMainAndPeriod(formattedTime: String): Pair<String, String>? {
    val normalized = formattedTime.replace("\r\n", "\n").trim()
    val nl = normalized.indexOf('\n')
    if (nl < 0) return null
    val main = normalized.substring(0, nl).trim()
    val period =
            normalized.substring(nl + 1)
                    .lineSequence()
                    .map { it.trim() }
                    .firstOrNull { it.isNotEmpty() }
                    ?: return null
    if (main.isEmpty()) return null
    if (main.none { it.isDigit() }) return null
    if (period.length > 14) return null
    if (period.any { it.isDigit() }) return null
    return main to period
}

/**
 * Collapses line breaks and unusual spaces, then takes the last AM/PM token (common US 12h:
 * `"3:58 PM"` on one line — Compose may wrap without `\n`, but we still shrink the suffix).
 */
internal fun extractTrailingAmPmLatin(collapsedTime: String): Pair<String, String>? {
    val match = TrailingAmPmLatin.find(collapsedTime) ?: return null
    val period = match.groupValues[1].trim()
    if (period.isEmpty()) return null
    val main = collapsedTime.substring(0, match.range.first).trim()
    if (main.isEmpty()) return null
    if (main.none { it.isDigit() }) return null
    return main to period
}

/**
 * Normalizes whitespace (including narrow no-break space often used before AM/PM) into single
 * spaces for parsing.
 */
internal fun collapseClockTimeForParsing(formattedTime: String): String =
        formattedTime
                .replace("\r\n", "\n")
                .replace('\u202f', ' ')
                .replace('\u00a0', ' ')
                .lines()
                .joinToString(" ") { it.trim() }
                .replace(Regex("\\s+"), " ")
                .trim()

/**
 * Resolves main clock text vs day-period for 12h shrinking. Returns null for 24h or when no
 * reliable split exists.
 */
internal fun resolveClockMainAndPeriod(
        formattedTime: String,
        is24HourFormat: Boolean,
): Pair<String, String>? {
    if (is24HourFormat) return null
    splitClockMainAndPeriod(formattedTime)?.let { return it }
    val collapsed = collapseClockTimeForParsing(formattedTime)
    return extractTrailingAmPmLatin(collapsed)
}

/** System-style time string with Latin AM/PM removed; 24h strings unchanged. */
internal fun clockDisplayTimeWithoutDayPeriod(
        formattedTime: String,
        is24HourFormat: Boolean,
): String {
    if (is24HourFormat) return formattedTime
    return resolveClockMainAndPeriod(formattedTime, is24HourFormat = false)?.first
            ?: formattedTime
}

/**
 * Large clock display showing the current time using the system time format (12h with AM/PM or 24h).
 * Clicking opens the clock / alarm app.
 */
@Composable
fun ClockWidget(
        time: String,
        is24HourFormat: Boolean = true,
        modifier: Modifier = Modifier,
        outlined: Boolean = false,
        onClick: () -> Unit = {}
) {
    val clockStyle = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.SemiBold)
    val color = MaterialTheme.colorScheme.onBackground
    val split =
            remember(time, is24HourFormat) {
                resolveClockMainAndPeriod(time, is24HourFormat)
            }
    val clickModifier = modifier.clickableNoRippleWithSystemSound(onClick = onClick)

    if (split == null) {
        val textModifier =
                clickModifier.semantics {
                    contentDescription = time.replace("\n", " ").trim()
                }
        if (outlined) {
            OutlinedText(
                    text = time,
                    style = clockStyle,
                    color = color,
                    modifier = textModifier,
                    outlineWidth = 3f,
            )
        } else {
            Text(text = time, style = clockStyle, color = color, modifier = textModifier)
        }
    } else {
        val (main, period) = split
        val periodStyle =
                clockStyle.copy(
                        fontSize = clockStyle.fontSize * AmPmLineScale,
                        lineHeight = clockStyle.lineHeight * AmPmLineScale,
                        letterSpacing = clockStyle.letterSpacing * AmPmLineScale,
                        fontWeight = FontWeight.Normal,
                )
        Row(
                verticalAlignment = Alignment.Bottom,
                modifier =
                        clickModifier.semantics {
                            contentDescription = "$main $period".trim()
                        }
        ) {
            if (outlined) {
                OutlinedText(
                        text = main,
                        style = clockStyle,
                        color = color,
                        maxLines = 1,
                        outlineWidth = 3f,
                )
            } else {
                Text(text = main, style = clockStyle, color = color, maxLines = 1)
            }
            Spacer(Modifier.width(8.dp))
            if (outlined) {
                OutlinedText(
                        text = period,
                        style = periodStyle,
                        color = color,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 4.dp),
                        outlineWidth = 1.5f,
                )
            } else {
                Text(
                        text = period,
                        style = periodStyle,
                        color = color,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}
