package com.walletplus.domain.money

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Porcentaje expresado en puntos básicos (1 % = 100 pb) para evitar NaN, Infinity y errores
 * de redondeo. 61.8 % se representa como 6180.
 */
@JvmInline
value class Percent(val basisPoints: Long) : Comparable<Percent> {

    override fun compareTo(other: Percent): Int = basisPoints.compareTo(other.basisPoints)

    /** Valor con 2 decimales, por ejemplo 61.80. */
    fun toBigDecimal(): BigDecimal = BigDecimal.valueOf(basisPoints, 2)

    /** Fracción 0..∞ para animaciones o barras de progreso; solo para presentación. */
    fun toFraction(): Float = basisPoints / 10_000f

    val isPositive: Boolean get() = basisPoints > 0

    operator fun compareTo(wholePercent: Int): Int = basisPoints.compareTo(wholePercent * 100L)

    companion object {
        val ZERO = Percent(0)
        val HUNDRED = Percent(10_000)

        fun ofWhole(value: Int): Percent = Percent(value * 100L)

        /**
         * part / whole. Devuelve null cuando el total no es positivo (no hay base de comparación),
         * en lugar de dividir por cero.
         */
        fun of(part: Money, whole: Money): Percent? {
            if (!whole.isPositive) return null
            val bp = BigDecimal.valueOf(part.cents)
                .multiply(BigDecimal.valueOf(10_000))
                .divide(BigDecimal.valueOf(whole.cents), 0, RoundingMode.HALF_UP)
            return Percent(bp.longValueExact())
        }

        /** Variación relativa (current - previous) / previous. Null si previous no es positivo. */
        fun change(previous: Money, current: Money): Percent? = of(current - previous, previous)
    }
}
