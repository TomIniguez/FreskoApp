package com.example.freskoapp.presentation.common

import java.time.LocalDate

// Spanish month abbreviations as the design writes them ("29 sep"); fixed so they don't depend on the JDK's CLDR
// data, which writes "sept." on some devices.
private val MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

// "29 sep" within the current year, "15 mar 2027" otherwise (or always, with withYear).
fun LocalDate.shortDate(today: LocalDate = LocalDate.now(), withYear: Boolean = false): String {
    val dayMonth = "$dayOfMonth ${MONTHS[monthValue - 1]}"
    return if (year == today.year && !withYear) dayMonth else "$dayMonth $year"
}
