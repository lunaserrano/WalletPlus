package com.walletplus.app.presentation.transactions

import com.walletplus.domain.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountInputTest {

    private fun type(keys: String): String = keys.fold("") { acc, c ->
        when (c) {
            '.' -> AmountInput.appendDecimal(acc)
            '<' -> AmountInput.backspace(acc)
            else -> AmountInput.appendDigit(acc, c)
        }
    }

    @Test
    fun `teclear un monto produce centavos exactos`() {
        assertEquals(Money(1250), AmountInput.toMoney(type("12.50")))
        assertEquals(Money(5), AmountInput.toMoney(type(".05")))
    }

    @Test
    fun `limita a dos decimales y evita ceros a la izquierda`() {
        assertEquals("12.34", type("12.345"))
        assertEquals("7", type("07"))
        assertEquals("0.", type("."))
        assertEquals("1.5", type("1..5"))
    }

    @Test
    fun `limita la parte entera y permite borrar`() {
        assertEquals("123456789", type("1234567890"))
        assertEquals("12", type("123<"))
        assertNull(AmountInput.toMoney(""))
    }

    @Test
    fun `formatea con separadores de miles mientras se escribe`() {
        assertEquals("0", AmountInput.display(""))
        assertEquals("1,234.5", AmountInput.display("1234.5"))
        assertEquals("1,000,000", AmountInput.display("1000000"))
    }

    @Test
    fun `convierte montos guardados para editarlos`() {
        assertEquals("25.5", AmountInput.fromMoney(Money(2550)))
        assertEquals("25.05", AmountInput.fromMoney(Money(2505)))
        assertEquals("30", AmountInput.fromMoney(Money(3000)))
    }
}
