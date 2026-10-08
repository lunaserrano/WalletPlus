package com.walletplus.app.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.MaterialTheme
import com.walletplus.app.core.format.MoneyFormatter
import com.walletplus.app.core.format.PercentFormat
import com.walletplus.app.core.ui.theme.WalletTheme
import com.walletplus.domain.insights.Insight
import com.walletplus.domain.insights.InsightTone
import com.walletplus.domain.model.PaymentMethod
import com.walletplus.domain.validation.BudgetError
import com.walletplus.domain.validation.CategoryError
import com.walletplus.domain.validation.GoalError
import com.walletplus.domain.validation.SettingsError
import com.walletplus.domain.validation.TransactionError

fun PaymentMethod.label(): String = when (this) {
    PaymentMethod.CASH -> "Efectivo"
    PaymentMethod.DEBIT_CARD -> "Tarjeta de débito"
    PaymentMethod.CREDIT_CARD -> "Tarjeta de crédito"
    PaymentMethod.TRANSFER -> "Transferencia"
    PaymentMethod.OTHER -> "Otro"
}

fun PaymentMethod.shortLabel(): String = when (this) {
    PaymentMethod.CASH -> "Efectivo"
    PaymentMethod.DEBIT_CARD -> "Débito"
    PaymentMethod.CREDIT_CARD -> "Crédito"
    PaymentMethod.TRANSFER -> "Transferencia"
    PaymentMethod.OTHER -> "Otro"
}

fun PaymentMethod.icon(): ImageVector = when (this) {
    PaymentMethod.CASH -> Icons.Rounded.Payments
    PaymentMethod.DEBIT_CARD -> Icons.Rounded.CreditCard
    PaymentMethod.CREDIT_CARD -> Icons.Rounded.CreditCard
    PaymentMethod.TRANSFER -> Icons.Rounded.SwapHoriz
    PaymentMethod.OTHER -> Icons.Rounded.MoreHoriz
}

fun TransactionError.message(): String = when (this) {
    TransactionError.AMOUNT_REQUIRED -> "Introduce un monto"
    TransactionError.AMOUNT_NOT_POSITIVE -> "El monto debe ser mayor que cero"
    TransactionError.CATEGORY_REQUIRED -> "Elige una categoría"
    TransactionError.CATEGORY_INVALID -> "La categoría no es válida para este tipo de movimiento"
    TransactionError.CATEGORY_INACTIVE -> "Esta categoría está desactivada"
    TransactionError.DATE_IN_FUTURE -> "La fecha no puede ser futura"
    TransactionError.DATE_BEFORE_START -> "La fecha es anterior al inicio de tu cuenta"
    TransactionError.NO_ACCOUNT -> "Configura primero tu cuenta"
}

fun CategoryError.message(): String = when (this) {
    CategoryError.NAME_REQUIRED -> "Escribe un nombre"
    CategoryError.NAME_TOO_LONG -> "El nombre es demasiado largo"
    CategoryError.NAME_DUPLICATED -> "Ya existe una categoría con ese nombre"
    CategoryError.EMOJI_REQUIRED -> "Elige un ícono"
    CategoryError.NOT_FOUND -> "La categoría ya no existe"
}

fun BudgetError.message(): String = when (this) {
    BudgetError.CATEGORY_INVALID -> "Elige una categoría de gasto"
    BudgetError.LIMIT_NEGATIVE -> "El presupuesto no puede ser negativo"
    BudgetError.LIMIT_REQUIRED -> "Introduce un monto válido"
}

fun GoalError.message(): String = when (this) {
    GoalError.NAME_REQUIRED -> "Ponle un nombre a tu meta"
    GoalError.TARGET_NOT_POSITIVE -> "El objetivo debe ser mayor que cero"
    GoalError.SAVED_NEGATIVE -> "El monto ahorrado no puede ser negativo"
    GoalError.DATE_IN_PAST -> "La fecha objetivo debe ser futura"
    GoalError.NOT_FOUND -> "La meta ya no existe"
    GoalError.AMOUNT_NOT_POSITIVE -> "Introduce un monto mayor que cero"
    GoalError.INSUFFICIENT_SAVED -> "No puedes retirar más de lo ahorrado"
}

fun SettingsError.message(): String = when (this) {
    SettingsError.BALANCE_NEGATIVE -> "Introduce un saldo válido (cero o mayor)"
    SettingsError.DATE_IN_FUTURE -> "La fecha no puede ser futura"
    SettingsError.VALUE_NEGATIVE -> "Los montos no pueden ser negativos"
    SettingsError.CURRENCY_INVALID -> "Moneda no válida"
}

data class InsightCopy(val title: String, val body: String)

/**
 * Texto de cada observación. Tono educativo y prudente: "según tus registros",
 * "podrías considerar"... nunca como verdad absoluta ni asesoría profesional.
 */
fun Insight.copy(money: MoneyFormatter): InsightCopy = when (this) {
    is Insight.BudgetExceeded -> InsightCopy(
        "Presupuesto superado",
        "Has superado el presupuesto de ${category.name}: llevas ${money.format(spent)} de ${money.format(limit)}. " +
            "Podrías revisar los próximos gastos de esta categoría.",
    )
    is Insight.MonthOverspent -> InsightCopy(
        "Gastos por encima de tus ingresos",
        "Según tus registros, este mes has gastado ${money.format(expense)} frente a ${money.format(income)} de ingresos. " +
            "Una posible oportunidad es pausar gastos opcionales hasta fin de mes.",
    )
    is Insight.PlanSpendingExceeded -> InsightCopy(
        "Límite mensual superado",
        "Tus gastos (${money.format(spent)}) superan el máximo que definiste para el mes (${money.format(limit)}).",
    )
    is Insight.BudgetNearLimit -> InsightCopy(
        "${category.name} cerca del límite",
        "Has usado el ${PercentFormat.format(percent)} del presupuesto. Te quedan ${money.format(remaining.coerceAtLeast(com.walletplus.domain.money.Money.ZERO))} para el resto del mes.",
    )
    is Insight.CategoryIncreased -> InsightCopy(
        "${category.name} aumentó ${PercentFormat.format(change)}",
        "Tu gasto en ${category.name} pasó de ${money.format(previous)} a ${money.format(current)} respecto al mes anterior.",
    )
    is Insight.HighOptionalSpending -> InsightCopy(
        "Gastos opcionales: ${PercentFormat.format(share)}",
        "Una parte importante de tus gastos (${money.format(total)}) corresponde a gastos que marcaste como opcionales. " +
            "Podrías revisarlos para encontrar oportunidades de ahorro.",
    )
    is Insight.SmallExpensesAddUp -> InsightCopy(
        "Los gastos pequeños suman",
        "Según tus registros, $count gastos menores de ${money.format(limit, showCents = false)} representan ${money.format(total)} este mes.",
    )
    is Insight.GoalPace -> InsightCopy(
        "Ritmo para \"$goalName\"",
        "Reservando ${money.format(perWeek)} por semana podrías alcanzar tu meta a tiempo. Te faltan ${money.format(remaining)}.",
    )
    is Insight.TopCategory -> InsightCopy(
        "Tu mayor gasto: ${category.name}",
        "Tu categoría con mayor gasto este mes es ${category.name}, con ${money.format(amount)} (${PercentFormat.format(share)} del total).",
    )
    is Insight.OptionalSpendingSummary -> InsightCopy(
        "Gastos opcionales bajo control",
        "Este mes has gastado ${money.format(total)} en gastos que marcaste como opcionales" +
            (share?.let { " (${PercentFormat.format(it)} del total)." } ?: "."),
    )
    is Insight.SavingsRate -> InsightCopy(
        "Estás ahorrando ${PercentFormat.format(rate)}",
        "Tu porcentaje de ahorro actual es ${PercentFormat.format(rate)} de tus ingresos: ${money.format(savings)} este mes. ¡Buen trabajo!",
    )
}

fun Insight.icon(): ImageVector = when (this) {
    is Insight.SavingsRate -> Icons.Rounded.Savings
    is Insight.GoalPace -> Icons.Rounded.Savings
    is Insight.CategoryIncreased -> Icons.Rounded.TrendingUp
    is Insight.TopCategory -> Icons.Rounded.AccountBalance
    else -> when (tone) {
        InsightTone.ALERT -> Icons.Rounded.ReportProblem
        InsightTone.ATTENTION -> Icons.Rounded.WarningAmber
        else -> Icons.Rounded.Lightbulb
    }
}

@Composable
fun InsightTone.color(): Color = when (this) {
    InsightTone.POSITIVE -> WalletTheme.colors.income
    InsightTone.NEUTRAL -> MaterialTheme.colorScheme.tertiary
    InsightTone.ATTENTION -> WalletTheme.colors.caution
    InsightTone.ALERT -> WalletTheme.colors.exceeded
}

const val EDUCATIONAL_DISCLAIMER =
    "Estas sugerencias son educativas y se basan únicamente en tus registros. No constituyen asesoría financiera profesional."
