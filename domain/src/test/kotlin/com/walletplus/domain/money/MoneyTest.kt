package com.walletplus.domain.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `se almacena en centavos sin errores de punto flotante`() {
        val total = Money.parse("0.10")!! + Money.parse("0.20")!!
        assertEquals(Money(30), total)
        assertEquals(2575L, Money.parse("25.75")!!.cents)
    }

    @Test
    fun `parse acepta formatos habituales`() {
        assertEquals(Money(2500), Money.parse("25"))
        assertEquals(Money(2570), Money.parse("25.7"))
        assertEquals(Money(2575), Money.parse("25,75"))
        assertEquals(Money(123456), Money.parse("1,234.56"))
        assertEquals(Money(123456), Money.parse("1.234,56"))
        assertEquals(Money(100000), Money.parse("1,000"))
        assertEquals(Money(1999), Money.parse("$ 19.99"))
    }

    @Test
    fun `parse rechaza valores invalidos`() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("-5"))
        assertNull(Money.parse("1,2,3.4.5"))
        assertNull(Money.parse("12.345.678,999"))
        assertNull(Money.parse("NaN"))
        assertNull(Money.parse("Infinity"))
    }

    @Test
    fun `division redondea hacia arriba para no quedarse corto`() {
        assertEquals(Money(334), Money(1000).divideRoundingUp(3))
        assertEquals(Money(250), Money(1000).divideRoundingUp(4))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `division por cero no esta permitida`() {
        Money(1000).divideRoundingUp(0)
    }

    @Test(expected = ArithmeticException::class)
    fun `el desbordamiento falla en lugar de dar un valor incorrecto`() {
        Money(Long.MAX_VALUE) + Money(1)
    }

    @Test
    fun `porcentaje evita division por cero`() {
        assertNull(Percent.of(Money(100), Money.ZERO))
        assertEquals(Percent(6188), Percent.of(usd(742, 50), usd(1200)))
        assertEquals(Percent(1800), Percent.change(usd(100), usd(118)))
    }

    @Test
    fun `ofUnits respeta el signo`() {
        assertEquals(Money(-550), Money.ofUnits(-5, 50))
        assertEquals("25.75", Money(2575).toString())
    }

    private fun usd(units: Long, cents: Int = 0) = Money.ofUnits(units, cents)
}
