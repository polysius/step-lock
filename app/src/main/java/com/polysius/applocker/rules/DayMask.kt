package com.polysius.applocker.rules

import java.time.DayOfWeek

/** Stores a set of weekdays as one integer: bit 0 is Monday, bit 6 is Sunday. */
fun Set<DayOfWeek>.toDayMask(): Int = fold(0) { mask, day -> mask or (1 shl day.ordinal) }

fun Int.toDays(): Set<DayOfWeek> =
    DayOfWeek.entries.filter { this and (1 shl it.ordinal) != 0 }.toSet()
