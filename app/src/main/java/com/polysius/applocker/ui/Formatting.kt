package com.polysius.applocker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

val timeFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

val dateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)

private val weekdays = setOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
)

/** Kept out of composables so the locale is read once per call, not on every recomposition. */
fun shortDayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.SHORT, Locale.getDefault())

fun daysSummary(days: Set<DayOfWeek>): String = when (days) {
    DayOfWeek.entries.toSet() -> "Every day"
    weekdays -> "Weekdays"
    setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Weekends"
    else -> days.sorted().joinToString(", ") { shortDayName(it) }
}

/** The current time, refreshed so countdowns and "locked until" text stay correct on screen. */
@Composable
fun rememberNow(): State<LocalDateTime> = produceState(LocalDateTime.now()) {
    while (true) {
        delay(NOW_REFRESH_MILLIS)
        value = LocalDateTime.now()
    }
}

/** How often on-screen time text refreshes. Tuning constant, not a product setting. */
private const val NOW_REFRESH_MILLIS = 20_000L
