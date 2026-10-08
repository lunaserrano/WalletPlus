package com.walletplus.app.core.format

import androidx.compose.runtime.staticCompositionLocalOf
import com.walletplus.domain.money.Money
import com.walletplus.domain.money.Percent
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Currency
import java.util.Locale

val SpanishLocale: Locale = Locale.forLanguageTag("es-419")

/** Monedas ofrecidas en la configuración. */
val SupportedCurrencies = listOf("USD", "EUR", "MXN", "COP", "ARS", "CLP", "PEN", "GTQ", "CRC", "DOP", "HNL", "BOB", "PYG", "UYU")

/**
 * Formatea montos como "$1,234.56". El dinero siempre se maneja en centavos ([Money]);
 * aquí solo se convierte a texto.
 */
class MoneyFormatter(val currencyCode: String) {

    val symbol: String = runCatching { Currency.getInstance(currencyCode).getSymbol(Locale.US) }
        .getOrDefault(currencyCode)
        .let { if (it == "US$") "$" else it }

    private val symbols = DecimalFormatSymbols(Locale.US)
    private val full = DecimalFormat("#,##0.00", symbols)
    private val whole = DecimalFormat("#,##0", symbols)
    private val compact = DecimalFormat("#,##0.#", symbols)

    private fun withSymbol(number: String): String = if (symbol.length == 1) "$symbol$number" else "$symbol $number"

    fun format(money: Money, signed: Boolean = false, showCents: Boolean = true): String {
        val number = (if (showCents) full else whole).format(money.abs().toBigDecimal())
        val sign = when {
            money.isNegative -> "−"
            signed && money.isPositive -> "+"
            else -> ""
        }
        return sign + withSymbol(number)
    }

    /** Solo el número, sin símbolo: "1,234.56". */
    fun number(money: Money): String = full.format(money.abs().toBigDecimal())

    /** Formato compacto para ejes de gráficos: "$1.2k". */
    fun compact(money: Money): String {
        val value = money.abs().toBigDecimal()
        val text = when {
            value >= BigDecimal(1_000_000) -> compact.format(value.divide(BigDecimal(1_000_000), 1, RoundingMode.HALF_UP)) + "M"
            value >= BigDecimal(1_000) -> compact.format(value.divide(BigDecimal(1_000), 1, RoundingMode.HALF_UP)) + "k"
            else -> whole.format(value)
        }
        return (if (money.isNegative) "−" else "") + withSymbol(text)
    }
}

val LocalMoneyFormatter = staticCompositionLocalOf { MoneyFormatter("USD") }

object PercentFormat {
    private val oneDecimal = DecimalFormat("#,##0.#", DecimalFormatSymbols(Locale.US))

    fun format(percent: Percent?, signed: Boolean = false): String {
        if (percent == null) return "—"
        val sign = if (signed && percent.basisPoints > 0) "+" else if (percent.basisPoints < 0) "−" else ""
        return sign + oneDecimal.format(percent.toBigDecimal().abs()) + "%"
    }
}

object DateFormats {
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", SpanishLocale)
    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", SpanishLocale)
    private val longDate = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", SpanishLocale)
    private val numeric = DateTimeFormatter.ofPattern("dd/MM/yyyy", SpanishLocale)

    fun relativeDay(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> if (date.year == today.year) dayMonth.format(date).clean() else dayMonthYear.format(date).clean()
    }

    /** Encabezado de grupo: "Hoy", "Ayer" o "miércoles, 15 de octubre". */
    fun dayHeader(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> longDate.format(date).replaceFirstChar { it.titlecase(SpanishLocale) } +
            if (date.year != today.year) " ${date.year}" else ""
    }

    /** "Miércoles, 15 de octubre". */
    fun long(date: LocalDate): String = longDate.format(date).replaceFirstChar { it.titlecase(SpanishLocale) }

    fun short(date: LocalDate): String = dayMonthYear.format(date).clean()

    fun numeric(date: LocalDate): String = numeric.format(date)

    fun month(month: YearMonth, today: LocalDate = LocalDate.now()): String {
        val name = month.month.getDisplayName(TextStyle.FULL_STANDALONE, SpanishLocale)
            .replaceFirstChar { it.titlecase(SpanishLocale) }
        return if (month.year == today.year) name else "$name ${month.year}"
    }

    fun monthShort(month: YearMonth): String =
        month.month.getDisplayName(TextStyle.SHORT_STANDALONE, SpanishLocale).trimEnd('.')
            .replaceFirstChar { it.titlecase(SpanishLocale) }

    private fun String.clean() = replace(".", "")
}
