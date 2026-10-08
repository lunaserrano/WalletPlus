package com.walletplus.app.presentation.transactions

import com.walletplus.domain.money.Money

/**
 * Buffer de texto del teclado numérico. Pura lógica (sin Android) para poder probarla.
 */
object AmountInput {
    private const val MAX_INTEGER_DIGITS = 9

    fun appendDigit(current: String, digit: Char): String {
        require(digit.isDigit())
        val integer = current.substringBefore('.')
        val hasDecimal = current.contains('.')
        val decimals = current.substringAfter('.', "")
        return when {
            hasDecimal && decimals.length >= 2 -> current
            !hasDecimal && integer.length >= MAX_INTEGER_DIGITS -> current
            current == "0" -> digit.toString()
            else -> current + digit
        }
    }

    fun appendDecimal(current: String): String = when {
        current.contains('.') -> current
        current.isEmpty() -> "0."
        else -> "$current."
    }

    fun backspace(current: String): String = current.dropLast(1)

    fun toMoney(current: String): Money? = if (current.isEmpty()) null else Money.parse(current)

    fun fromMoney(money: Money): String {
        val units = money.cents / 100
        val cents = money.cents % 100
        return if (cents == 0L) units.toString() else "$units.${cents.toString().padStart(2, '0').trimEnd('0')}"
    }

    /** "1234.5" → "1,234.5" (respeta lo que el usuario está escribiendo). */
    fun display(current: String): String {
        if (current.isEmpty()) return "0"
        val integer = current.substringBefore('.').ifEmpty { "0" }
        val grouped = integer.reversed().chunked(3).joinToString(",").reversed()
        return if (current.contains('.')) "$grouped.${current.substringAfter('.')}" else grouped
    }
}
