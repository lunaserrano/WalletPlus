package com.walletplus.domain.time

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Rango de fechas cerrado [start, endInclusive]. */
data class DateRange(val start: LocalDate, val endInclusive: LocalDate) {
    init {
        require(!endInclusive.isBefore(start)) { "endInclusive no puede ser anterior a start" }
    }

    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(endInclusive)
}

fun YearMonth.toDateRange(): DateRange = DateRange(atDay(1), atEndOfMonth())

fun YearMonth.previous(): YearMonth = minusMonths(1)

/** Los últimos [count] meses terminando en este (incluido), del más antiguo al más reciente. */
fun YearMonth.lastMonths(count: Int): List<YearMonth> {
    require(count > 0)
    return (count - 1 downTo 0).map { minusMonths(it.toLong()) }
}

/** Periodos restantes hasta una fecha objetivo; nunca cero para evitar divisiones por cero. */
data class RemainingPeriods(val days: Long, val weeks: Long, val months: Long)

/**
 * Calcula días, semanas y meses (redondeando hacia arriba) entre [from] (exclusivo) y [to]
 * (inclusivo). Devuelve null si la fecha objetivo ya pasó o es hoy.
 */
fun remainingPeriods(from: LocalDate, to: LocalDate): RemainingPeriods? {
    val days = ChronoUnit.DAYS.between(from, to)
    if (days <= 0) return null
    val weeks = (days + 6) / 7
    val wholeMonths = ChronoUnit.MONTHS.between(from, to)
    val months = if (from.plusMonths(wholeMonths).isBefore(to)) wholeMonths + 1 else wholeMonths
    return RemainingPeriods(days = days, weeks = weeks, months = months.coerceAtLeast(1))
}
