package com.walletplus.domain.time

import com.walletplus.domain.TODAY
import com.walletplus.domain.usd
import com.walletplus.domain.usecase.convertBetweenPayPeriods
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PeriodTest {

    private fun range(from: String, to: String) = DateRange(LocalDate.parse(from), LocalDate.parse(to))

    @Test
    fun `quincenas del 1 al 15 y del 16 a fin de mes`() {
        assertEquals(range("2026-10-01", "2026-10-15"), Period.containing(PeriodType.BIWEEKLY, TODAY).range)
        assertEquals(range("2026-10-16", "2026-10-31"), Period.containing(PeriodType.BIWEEKLY, TODAY.plusDays(1)).range)
        // Febrero no bisiesto
        assertEquals(range("2027-02-16", "2027-02-28"), Period.containing(PeriodType.BIWEEKLY, LocalDate.parse("2027-02-20")).range)
    }

    @Test
    fun `quincena anterior y siguiente cruzan meses y anios`() {
        val first = Period.containing(PeriodType.BIWEEKLY, LocalDate.parse("2027-01-10"))
        assertEquals(range("2026-12-16", "2026-12-31"), first.previous().range)
        assertEquals(range("2027-01-16", "2027-01-31"), first.next().range)
    }

    @Test
    fun `semanas de lunes a domingo`() {
        val week = Period.containing(PeriodType.WEEKLY, TODAY) // jueves 15 oct 2026
        assertEquals(range("2026-10-12", "2026-10-18"), week.range)
        assertEquals(range("2026-10-05", "2026-10-11"), week.previous().range)
    }

    @Test
    fun `ultimos periodos en orden cronologico`() {
        val months = Period.containing(PeriodType.MONTHLY, TODAY).lastPeriods(3)
        assertEquals(listOf("2026-08-01", "2026-09-01", "2026-10-01").map(LocalDate::parse), months.map { it.start })
        assertTrue(months.all { it.type == PeriodType.MONTHLY })
    }

    @Test
    fun `quincenas restantes cuentan los dias de pago`() {
        // Desde el 15 oct hasta el 1 nov: pagos el 16 oct y el 1 nov.
        assertEquals(2, remainingPeriods(TODAY, LocalDate.parse("2026-11-01"))!!.fortnights)
        // Antes del siguiente pago: al menos una quincena.
        assertEquals(1, remainingPeriods(LocalDate.parse("2026-10-02"), LocalDate.parse("2026-10-10"))!!.fortnights)
    }

    @Test
    fun `conversion entre ciclos de pago`() {
        assertEquals(usd(150), convertBetweenPayPeriods(usd(300), PeriodType.MONTHLY, PeriodType.BIWEEKLY))
        assertEquals(usd(0, 51), convertBetweenPayPeriods(usd(1, 1), PeriodType.MONTHLY, PeriodType.BIWEEKLY))
        assertEquals(usd(600), convertBetweenPayPeriods(usd(300), PeriodType.BIWEEKLY, PeriodType.MONTHLY))
        assertEquals(usd(300), convertBetweenPayPeriods(usd(300), PeriodType.MONTHLY, PeriodType.MONTHLY))
    }
}
