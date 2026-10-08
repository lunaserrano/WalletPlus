package com.walletplus.domain.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * Granularidad de un periodo. Semanas de lunes a domingo; quincenas del 1 al 15 y del 16 a fin de mes.
 */
enum class PeriodType {
    WEEKLY,
    BIWEEKLY,
    MONTHLY;

    /** Solo la quincena y el mes pueden ser ciclo de pago (presupuestos y plan). */
    val isPayPeriod: Boolean get() = this != WEEKLY
}

/** Un periodo concreto del calendario: "semana del 6 al 12 oct", "1.ª quincena de oct", "octubre". */
data class Period(val type: PeriodType, val range: DateRange) {
    val start: LocalDate get() = range.start
    val end: LocalDate get() = range.endInclusive

    /** Mes al que pertenece el periodo (por su fecha de inicio). */
    val yearMonth: YearMonth get() = YearMonth.from(start)

    operator fun contains(date: LocalDate): Boolean = date in range

    fun previous(): Period = containing(type, start.minusDays(1))

    fun next(): Period = containing(type, end.plusDays(1))

    /** Los últimos [count] periodos terminando en este (incluido), del más antiguo al más reciente. */
    fun lastPeriods(count: Int): List<Period> {
        require(count > 0)
        return generateSequence(this) { it.previous() }.take(count).toList().asReversed()
    }

    companion object {
        fun containing(type: PeriodType, date: LocalDate): Period = when (type) {
            PeriodType.WEEKLY -> {
                val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                Period(type, DateRange(monday, monday.plusDays(6)))
            }
            PeriodType.BIWEEKLY -> {
                val month = YearMonth.from(date)
                val range = if (date.dayOfMonth <= FIRST_FORTNIGHT_LAST_DAY) {
                    DateRange(month.atDay(1), month.atDay(FIRST_FORTNIGHT_LAST_DAY))
                } else {
                    DateRange(month.atDay(FIRST_FORTNIGHT_LAST_DAY + 1), month.atEndOfMonth())
                }
                Period(type, range)
            }
            PeriodType.MONTHLY -> month(YearMonth.from(date))
        }

        fun month(month: YearMonth): Period = Period(PeriodType.MONTHLY, month.toDateRange())

        private const val FIRST_FORTNIGHT_LAST_DAY = 15
    }
}
