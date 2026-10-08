package com.walletplus.domain.money

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Cantidad monetaria almacenada en centavos.
 *
 * Nunca se usa Double para dinero: toda la aritmética es entera y desborda con excepción
 * en lugar de producir un valor silenciosamente incorrecto.
 */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(Math.addExact(cents, other.cents))

    operator fun minus(other: Money): Money = Money(Math.subtractExact(cents, other.cents))

    operator fun times(factor: Int): Money = Money(Math.multiplyExact(cents, factor.toLong()))

    operator fun unaryMinus(): Money = Money(Math.negateExact(cents))

    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    val isZero: Boolean get() = cents == 0L
    val isPositive: Boolean get() = cents > 0L
    val isNegative: Boolean get() = cents < 0L

    fun abs(): Money = if (cents < 0) -this else this

    fun coerceAtLeast(minimum: Money): Money = if (this < minimum) minimum else this

    /**
     * Divide en [parts] partes redondeando hacia arriba al centavo, para que la suma de las
     * partes nunca quede por debajo del total (útil para el ahorro recomendado).
     */
    fun divideRoundingUp(parts: Long): Money {
        require(parts > 0) { "parts debe ser mayor que cero" }
        if (cents <= 0) return Money(cents / parts)
        return Money((cents + parts - 1) / parts)
    }

    fun toBigDecimal(): BigDecimal = BigDecimal.valueOf(cents, 2)

    override fun toString(): String = toBigDecimal().toPlainString()

    companion object {
        val ZERO = Money(0)

        fun ofUnits(units: Long, cents: Int = 0): Money {
            require(cents in 0..99) { "cents debe estar entre 0 y 99" }
            val sign = if (units < 0) -1 else 1
            return Money(Math.addExact(Math.multiplyExact(units, 100L), (sign * cents).toLong()))
        }

        fun fromBigDecimal(value: BigDecimal): Money =
            Money(value.setScale(2, RoundingMode.HALF_EVEN).unscaledValue().longValueExact())

        /**
         * Interpreta texto introducido por el usuario: "25", "25.7", "25,75", "1,234.56", "1.234,56".
         * Devuelve null si no es un monto válido no negativo con como máximo 2 decimales.
         */
        fun parse(text: String): Money? {
            val raw = text.trim().replace(" ", "").replace(" ", "").removePrefix("$")
            if (raw.isEmpty() || raw.startsWith("-")) return null
            if (raw.any { !it.isDigit() && it != '.' && it != ',' }) return null

            val lastDot = raw.lastIndexOf('.')
            val lastComma = raw.lastIndexOf(',')
            val normalized = when {
                lastDot >= 0 && lastComma >= 0 -> {
                    val decimalSep = if (lastDot > lastComma) '.' else ','
                    val groupSep = if (decimalSep == '.') ',' else '.'
                    raw.replace(groupSep.toString(), "").replace(decimalSep, '.')
                }
                lastComma >= 0 -> {
                    val decimals = raw.length - lastComma - 1
                    if (raw.count { it == ',' } == 1 && decimals in 0..2) raw.replace(',', '.')
                    else raw.replace(",", "")
                }
                else -> {
                    val decimals = raw.length - lastDot - 1
                    if (lastDot >= 0 && (raw.count { it == '.' } > 1 || decimals == 3)) {
                        raw.replace(".", "")
                    } else {
                        raw
                    }
                }
            }
            if (normalized.count { it == '.' } > 1) return null
            val decimals = normalized.substringAfter('.', "")
            if (decimals.length > 2) return null
            return runCatching { fromBigDecimal(BigDecimal(normalized)) }.getOrNull()
        }
    }
}

fun Iterable<Money>.sum(): Money = fold(Money.ZERO) { acc, money -> acc + money }

inline fun <T> Iterable<T>.sumOfMoney(selector: (T) -> Money): Money =
    fold(Money.ZERO) { acc, item -> acc + selector(item) }
