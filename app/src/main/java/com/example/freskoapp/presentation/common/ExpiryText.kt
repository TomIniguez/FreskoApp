package com.example.freskoapp.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.freskoapp.R
import com.example.freskoapp.domain.model.ExpirationStatus
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

// What the status line says about a product. The status (from the user's warning days) decides between
// "Vence en N días" and "Vigente · …"; the dates only give the number.
sealed interface ExpiryPhrase {
    data class ExpiredDaysAgo(val days: Int) : ExpiryPhrase
    data object Today : ExpiryPhrase
    data object Tomorrow : ExpiryPhrase
    data class InDays(val days: Int) : ExpiryPhrase
    data class FreshDays(val days: Int) : ExpiryPhrase
    data class FreshMonths(val months: Int) : ExpiryPhrase
    data class FreshYears(val years: Int) : ExpiryPhrase
}

fun expiryPhrase(status: ExpirationStatus, expirationDate: LocalDate, today: LocalDate): ExpiryPhrase {
    val days = ChronoUnit.DAYS.between(today, expirationDate).toInt()
    return when (status) {
        ExpirationStatus.EXPIRED -> ExpiryPhrase.ExpiredDaysAgo(maxOf(1, -days))
        ExpirationStatus.EXPIRING_SOON -> when (days) {
            0 -> ExpiryPhrase.Today
            1 -> ExpiryPhrase.Tomorrow
            else -> ExpiryPhrase.InDays(days)
        }
        ExpirationStatus.FRESH -> {
            val months = Period.between(today, expirationDate).toTotalMonths().toInt()
            when {
                months >= 12 -> ExpiryPhrase.FreshYears(months / 12)
                months >= 1 -> ExpiryPhrase.FreshMonths(months)
                else -> ExpiryPhrase.FreshDays(days)
            }
        }
    }
}

@Composable
fun expiryText(status: ExpirationStatus, expirationDate: LocalDate, today: LocalDate = LocalDate.now()): String =
    when (val p = expiryPhrase(status, expirationDate, today)) {
        is ExpiryPhrase.ExpiredDaysAgo -> pluralStringResource(R.plurals.expiry_expired_days_ago, p.days, p.days)
        ExpiryPhrase.Today -> stringResource(R.string.expiry_today)
        ExpiryPhrase.Tomorrow -> stringResource(R.string.expiry_tomorrow)
        is ExpiryPhrase.InDays -> pluralStringResource(R.plurals.expiry_in_days, p.days, p.days)
        is ExpiryPhrase.FreshDays -> pluralStringResource(R.plurals.expiry_fresh_days, p.days, p.days)
        is ExpiryPhrase.FreshMonths -> pluralStringResource(R.plurals.expiry_fresh_months, p.months, p.months)
        is ExpiryPhrase.FreshYears -> pluralStringResource(R.plurals.expiry_fresh_years, p.years, p.years)
    }
