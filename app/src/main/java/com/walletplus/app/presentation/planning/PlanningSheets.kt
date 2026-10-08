package com.walletplus.app.presentation.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.walletplus.app.core.format.DateFormats
import com.walletplus.app.core.format.LocalMoneyFormatter
import com.walletplus.app.core.ui.components.ActionChip
import com.walletplus.app.core.ui.components.AmountField
import com.walletplus.app.core.ui.components.CalendarIcon
import com.walletplus.app.core.ui.components.LocalMessenger
import com.walletplus.app.core.ui.components.PrimaryButton
import com.walletplus.app.core.ui.components.SegmentedControl
import com.walletplus.app.core.ui.components.WalletDatePickerDialog
import com.walletplus.app.core.ui.components.WalletSheet
import com.walletplus.app.core.ui.components.walletFieldColors
import com.walletplus.app.core.ui.theme.Spacing
import com.walletplus.app.presentation.common.label
import com.walletplus.app.presentation.common.message
import com.walletplus.app.presentation.common.words
import com.walletplus.app.presentation.transactions.AmountInput
import com.walletplus.domain.calc.BudgetProgress
import com.walletplus.domain.calc.GoalProgress
import com.walletplus.domain.model.Category
import com.walletplus.domain.model.SpendingPlan
import com.walletplus.domain.money.Money
import com.walletplus.domain.time.PeriodType
import com.walletplus.domain.usecase.convertBetweenPayPeriods
import com.walletplus.domain.validation.Outcome
import kotlinx.coroutines.launch
import java.time.LocalDate

private fun Money.asInput(): String = if (isZero) "" else AmountInput.fromMoney(this)
private fun parseOrZero(text: String): Money? = if (text.isBlank()) Money.ZERO else Money.parse(text)

private val PayPeriodOptions = listOf(PeriodType.MONTHLY, PeriodType.BIWEEKLY)

@Composable
fun PlanSettingsSheet(
    plan: SpendingPlan,
    payPeriod: PeriodType,
    hasCategoryBudgets: Boolean,
    onSave: suspend (SpendingPlan, PeriodType) -> Outcome<Unit, com.walletplus.domain.validation.SettingsError>,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val messenger = LocalMessenger.current
    var period by remember { mutableStateOf(payPeriod) }
    var income by remember { mutableStateOf(plan.expectedIncome.asInput()) }
    var limit by remember { mutableStateOf(plan.spendingLimit.asInput()) }
    var savings by remember { mutableStateOf(plan.savingsTarget.asInput()) }
    var error by remember { mutableStateOf<String?>(null) }
    val words = period.words

    // Al cambiar de ciclo se convierten los montos escritos (un mes = dos quincenas).
    fun switchTo(target: PeriodType) {
        if (target == period) return
        fun convert(text: String) = parseOrZero(text)?.let { convertBetweenPayPeriods(it, period, target).asInput() } ?: text
        income = convert(income)
        limit = convert(limit)
        savings = convert(savings)
        period = target
        error = null
    }

    WalletSheet(title = "Presupuesto general", onDismiss = onDismiss) {
        Text("¿Cada cuánto cobras?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SegmentedControl(
            options = PayPeriodOptions.map { it.label() },
            selectedIndex = PayPeriodOptions.indexOf(period),
            onSelect = { switchTo(PayPeriodOptions[it]) },
        )
        Text(
            if (period == PeriodType.BIWEEKLY) {
                "Tu presupuesto se reinicia cada quincena: del 1 al 15 y del 16 a fin de mes. Deja en blanco lo que no quieras usar."
            } else {
                "Compara cada mes lo que planeas con lo que realmente pasa. Deja en blanco lo que no quieras usar."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AmountField(income, { income = it }, "Ingreso ${words.adjective} esperado")
        AmountField(limit, { limit = it }, "Gastos máximos ${words.ofThe}")
        AmountField(savings, { savings = it }, "Meta de ahorro ${words.adjective}", imeAction = ImeAction.Done, error = error)
        if (period != payPeriod && hasCategoryBudgets) {
            Text(
                if (period == PeriodType.BIWEEKLY) "Tus presupuestos por categoría se dividirán entre dos para cada quincena."
                else "Tus presupuestos por categoría se multiplicarán por dos para todo el mes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PrimaryButton("Guardar", onClick = {
            val values = listOf(parseOrZero(income), parseOrZero(limit), parseOrZero(savings))
            if (values.any { it == null }) {
                error = "Revisa los montos introducidos"
                return@PrimaryButton
            }
            scope.launch {
                when (val result = onSave(SpendingPlan(values[0]!!, values[1]!!, values[2]!!), period)) {
                    is Outcome.Success -> {
                        messenger.show("Presupuesto general actualizado")
                        onDismiss()
                    }
                    is Outcome.Invalid -> error = result.errors.first().message()
                }
            }
        })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetSheet(
    progress: BudgetProgress?,
    categories: List<Category>,
    payPeriod: PeriodType,
    viewModel: PlanningViewModel,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val messenger = LocalMessenger.current
    var categoryId by remember { mutableStateOf(progress?.category?.id ?: categories.firstOrNull()?.id) }
    var amount by remember { mutableStateOf(progress?.budget?.limit?.asInput().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    WalletSheet(
        title = if (progress == null) "Nuevo presupuesto" else "Presupuesto de ${progress.category.name}",
        onDismiss = onDismiss,
        trailing = progress?.let {
            {
                IconButton(onClick = {
                    scope.launch {
                        viewModel.deleteBudget(it.budget.id)
                        messenger.show("Presupuesto eliminado")
                        onDismiss()
                    }
                }) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar presupuesto", tint = MaterialTheme.colorScheme.error) }
            }
        },
    ) {
        if (progress == null) {
            Text("Categoría", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                categories.forEach { category ->
                    ActionChip(
                        text = "${category.emoji} ${category.name}",
                        onClick = { categoryId = category.id },
                        selected = category.id == categoryId,
                    )
                }
            }
        }
        AmountField(amount, { amount = it; error = null }, "Límite ${payPeriod.words.adjective}", error = error, imeAction = ImeAction.Done)
        PrimaryButton("Guardar presupuesto", enabled = categoryId != null, onClick = {
            val id = categoryId ?: return@PrimaryButton
            scope.launch {
                when (val result = viewModel.saveBudget(id, Money.parse(amount))) {
                    is Outcome.Success -> {
                        messenger.show("Presupuesto guardado")
                        onDismiss()
                    }
                    is Outcome.Invalid -> error = result.errors.first().message()
                }
            }
        })
    }
}

private val GoalEmojis = listOf("🛟", "🏖️", "✈️", "🏠", "🚗", "🎓", "💍", "💻", "📱", "🎁", "🩺", "🐶", "🎸", "🌱", "💰", "🎯")

@Composable
fun GoalSheet(progress: GoalProgress?, today: LocalDate, viewModel: PlanningViewModel, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val messenger = LocalMessenger.current
    val goal = progress?.goal
    var name by remember { mutableStateOf(goal?.name.orEmpty()) }
    var emoji by remember { mutableStateOf(goal?.emoji ?: GoalEmojis.first()) }
    var target by remember { mutableStateOf(goal?.target?.asInput().orEmpty()) }
    var saved by remember { mutableStateOf(goal?.saved?.asInput().orEmpty()) }
    var hasDate by remember { mutableStateOf(goal?.targetDate != null) }
    var date by remember { mutableStateOf(goal?.targetDate ?: today.plusMonths(6)) }
    var showPicker by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }

    WalletSheet(
        title = if (goal == null) "Nueva meta" else "Editar meta",
        onDismiss = onDismiss,
        trailing = goal?.let {
            {
                IconButton(onClick = {
                    scope.launch {
                        viewModel.deleteGoal(it.id)
                        messenger.show("Meta eliminada")
                        onDismiss()
                    }
                }) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar meta", tint = MaterialTheme.colorScheme.error) }
            }
        },
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items(GoalEmojis) { option ->
                ActionChip(option, onClick = { emoji = option }, selected = option == emoji)
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            label = { Text("Nombre de la meta") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = walletFieldColors(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        AmountField(target, { target = it }, "Objetivo")
        AmountField(saved, { saved = it }, "Ahorrado hasta ahora", imeAction = ImeAction.Done)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Fecha objetivo", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Calcularemos cuánto apartar por semana y por mes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = hasDate, onCheckedChange = { hasDate = it })
        }
        if (hasDate) {
            ActionChip(DateFormats.short(date), onClick = { showPicker = true }, leading = { CalendarIcon() })
        }
        errors.forEach { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        PrimaryButton(if (goal == null) "Crear meta" else "Guardar cambios", onClick = {
            scope.launch {
                val result = viewModel.saveGoal(
                    id = goal?.id ?: 0,
                    name = name,
                    emoji = emoji,
                    target = Money.parse(target),
                    saved = if (saved.isBlank()) Money.ZERO else Money.parse(saved) ?: Money(-1),
                    date = if (hasDate) date else null,
                )
                when (result) {
                    is Outcome.Success -> {
                        messenger.show(if (goal == null) "Meta creada" else "Meta actualizada")
                        onDismiss()
                    }
                    is Outcome.Invalid -> errors = result.errors.map { it.message() }
                }
            }
        })
    }
    if (showPicker) {
        WalletDatePickerDialog(
            initial = date,
            today = today,
            allowFuture = true,
            allowPast = false,
            onDismiss = { showPicker = false },
            onConfirm = {
                date = it
                showPicker = false
            },
        )
    }
}

@Composable
fun ContributeSheet(progress: GoalProgress, payPeriod: PeriodType, viewModel: PlanningViewModel, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val messenger = LocalMessenger.current
    val formatter = LocalMoneyFormatter.current
    var deposit by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    WalletSheet(title = "${progress.goal.emoji} ${progress.goal.name}", onDismiss = onDismiss) {
        Text(
            "Ahorrado: ${formatter.format(progress.goal.saved)} de ${formatter.format(progress.goal.target)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SegmentedControl(listOf("Aportar", "Retirar"), if (deposit) 0 else 1, { deposit = it == 0 })
        AmountField(amount, { amount = it; error = null }, if (deposit) "Monto a aportar" else "Monto a retirar", error = error, imeAction = ImeAction.Done)
        val suggested = if (payPeriod == PeriodType.BIWEEKLY) progress.perFortnight else progress.perWeek
        suggested?.takeIf { deposit }?.let { suggestion ->
            ActionChip("Sugerido: ${formatter.format(suggestion)}", onClick = { amount = AmountInput.fromMoney(suggestion) })
        }
        PrimaryButton(if (deposit) "Aportar" else "Retirar", onClick = {
            scope.launch {
                when (val result = viewModel.contribute(progress.goal.id, Money.parse(amount), deposit)) {
                    is Outcome.Success -> {
                        val done = result.value.saved >= result.value.target
                        messenger.show(if (done && deposit) "¡Meta cumplida! 🎉" else if (deposit) "Aporte registrado" else "Retiro registrado")
                        onDismiss()
                    }
                    is Outcome.Invalid -> error = result.errors.first().message()
                }
            }
        })
        Text(
            "Los aportes a metas son un apartado dentro de tu saldo: no se registran como gastos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

