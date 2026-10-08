package com.walletplus.app.presentation.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walletplus.app.core.di.LocalAppContainer
import com.walletplus.app.core.di.walletViewModel
import com.walletplus.app.core.format.DateFormats
import com.walletplus.app.core.format.LocalMoneyFormatter
import com.walletplus.app.core.ui.components.ActionChip
import com.walletplus.app.core.ui.components.EmptyState
import com.walletplus.app.core.ui.components.LocalMessenger
import com.walletplus.app.core.ui.components.MoneyText
import com.walletplus.app.core.ui.components.PrimaryButton
import com.walletplus.app.core.ui.components.ScreenTitle
import com.walletplus.app.core.ui.components.WalletSheet
import com.walletplus.app.core.ui.theme.Spacing
import com.walletplus.app.core.ui.theme.WalletTheme
import com.walletplus.app.core.ui.theme.tabular
import com.walletplus.app.presentation.common.TransactionRow
import com.walletplus.app.presentation.common.icon
import com.walletplus.app.presentation.common.label
import com.walletplus.domain.model.Category
import com.walletplus.domain.model.PaymentMethod
import com.walletplus.domain.model.TransactionType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset

private enum class FilterSheet { DATE, CATEGORY, METHOD }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    contentPadding: PaddingValues,
    onEditTransaction: (Long) -> Unit,
    onAddTransaction: () -> Unit,
) {
    val viewModel = walletViewModel { TransactionsViewModel(it.transactions, it.categories, it.deleteTransaction, it.clock) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val container = LocalAppContainer.current
    val messenger = LocalMessenger.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val focus = LocalFocusManager.current
    var sheet by remember { mutableStateOf<FilterSheet?>(null) }
    val listState = rememberLazyListState()

    fun delete(id: Long) {
        scope.launch {
            val removed = viewModel.deleteTransaction(id) ?: return@launch
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            messenger.show("Movimiento eliminado", "Deshacer") {
                container.appScope.launch { container.restoreTransaction(removed) }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + Spacing.xxl),
    ) {
        item { Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars)) }
        item { ScreenTitle("Movimientos", modifier = Modifier.padding(horizontal = Spacing.screen)) }

        if (!state.loading && !state.hasAnyTransaction) {
            item {
                EmptyState(
                    emoji = "🧾",
                    title = "Aún no hay movimientos",
                    message = "Cada ingreso y gasto que registres aparecerá aquí, ordenado por día.",
                    actionLabel = "Registrar movimiento",
                    onAction = onAddTransaction,
                )
            }
            return@LazyColumn
        }

        item {
            SearchField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                onDone = { focus.clearFocus() },
                modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm),
            )
        }
        item {
            FilterChipsRow(
                state = state,
                onToggleType = viewModel::toggleType,
                onOpen = { sheet = it },
                onClear = viewModel::clearFilters,
            )
        }
        item {
            ResultsSummary(state, Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.md))
        }

        if (state.groups.isEmpty() && !state.loading) {
            item {
                EmptyState(
                    emoji = "🔍",
                    title = "Sin resultados",
                    message = "No hay movimientos con estos filtros. Prueba con otra búsqueda.",
                    actionLabel = "Limpiar filtros",
                    onAction = viewModel::clearFilters,
                )
            }
        }

        state.groups.forEach { group ->
            stickyHeader(key = "h-${group.date}") {
                DayHeader(DateFormats.dayHeader(group.date, state.today), group.net)
            }
            items(group.items, key = { it.id }) { tx ->
                SwipeToDelete(onDelete = { delete(tx.id) }, modifier = Modifier.animateItem()) {
                    TransactionRow(
                        transaction = tx,
                        category = state.categoriesById[tx.categoryId],
                        today = state.today,
                        onClick = { onEditTransaction(tx.id) },
                        showDate = false,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = Spacing.lg),
                    )
                }
            }
        }
    }

    when (sheet) {
        FilterSheet.CATEGORY -> CategoryFilterSheet(
            categories = state.categories,
            selected = state.filter.categoryIds,
            onApply = {
                viewModel.setCategories(it)
                sheet = null
            },
            onDismiss = { sheet = null },
        )
        FilterSheet.METHOD -> MethodFilterSheet(
            selected = state.filter.paymentMethods,
            onApply = {
                viewModel.setMethods(it)
                sheet = null
            },
            onDismiss = { sheet = null },
        )
        FilterSheet.DATE -> DateFilterSheet(
            current = state.datePreset,
            onSelect = { preset, from, to ->
                viewModel.setPreset(preset, from, to)
                sheet = null
            },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Buscar movimientos", maxLines = 1) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            AnimatedVisibility(value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) { Icon(Icons.Rounded.Close, contentDescription = "Borrar búsqueda") }
            }
        },
        singleLine = true,
        shape = CircleShape,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun FilterChipsRow(
    state: TransactionsState,
    onToggleType: (TransactionType) -> Unit,
    onOpen: (FilterSheet) -> Unit,
    onClear: () -> Unit,
) {
    val filter = state.filter
    val chevron: @Composable () -> Unit = { Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp)) }
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.screen),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item { ActionChip("Ingresos", { onToggleType(TransactionType.INCOME) }, selected = filter.types == setOf(TransactionType.INCOME)) }
        item { ActionChip("Gastos", { onToggleType(TransactionType.EXPENSE) }, selected = filter.types == setOf(TransactionType.EXPENSE)) }
        item {
            val from = filter.from
            val to = filter.to
            val label = if (state.datePreset == DatePreset.CUSTOM && from != null && to != null) {
                "${DateFormats.short(from)} – ${DateFormats.short(to)}"
            } else if (state.datePreset == DatePreset.ALL) "Fecha" else state.datePreset.label
            ActionChip(label, { onOpen(FilterSheet.DATE) }, selected = state.datePreset != DatePreset.ALL, leading = chevron)
        }
        item {
            val count = filter.categoryIds.size
            ActionChip(if (count == 0) "Categoría" else "Categorías · $count", { onOpen(FilterSheet.CATEGORY) }, selected = count > 0, leading = chevron)
        }
        item {
            val count = filter.paymentMethods.size
            ActionChip(if (count == 0) "Método" else "Métodos · $count", { onOpen(FilterSheet.METHOD) }, selected = count > 0, leading = chevron)
        }
        if (filter.isActive || state.query.isNotEmpty()) {
            item {
                TextButton(onClick = onClear) { Text("Limpiar") }
            }
        }
    }
}

@Composable
private fun ResultsSummary(state: TransactionsState, modifier: Modifier = Modifier) {
    val formatter = LocalMoneyFormatter.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${state.resultCount} ${if (state.resultCount == 1) "movimiento" else "movimientos"}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (state.income.isPositive) {
            Text(formatter.format(state.income, signed = true), style = MaterialTheme.typography.labelMedium.tabular(), color = WalletTheme.colors.income)
            Spacer(Modifier.size(Spacing.md))
        }
        if (state.expense.isPositive) {
            Text(formatter.format(-state.expense), style = MaterialTheme.typography.labelMedium.tabular(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DayHeader(label: String, net: com.walletplus.domain.money.Money) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Spacing.screen, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        MoneyText(net, signed = true, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDelete(onDelete: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        onDismiss = { value -> if (value == SwipeToDismissBoxValue.EndToStart) onDelete() },
        backgroundContent = {
            val color by animateColorAsState(
                if (state.targetValue == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer,
                label = "swipe",
            )
            Box(
                Modifier.fillMaxSize().padding(horizontal = Spacing.lg, vertical = 4.dp).clip(MaterialTheme.shapes.medium).background(color).padding(horizontal = Spacing.xl),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.onError)
            }
        },
    ) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryFilterSheet(
    categories: List<Category>,
    selected: Set<Long>,
    onApply: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var current by remember { mutableStateOf(selected) }
    WalletSheet(title = "Filtrar por categoría", onDismiss = onDismiss) {
        listOf(TransactionType.EXPENSE to "Gastos", TransactionType.INCOME to "Ingresos").forEach { (type, title) ->
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                categories.filter { it.type == type }.forEach { category ->
                    val isSelected = category.id in current
                    ActionChip(
                        text = "${category.emoji} ${category.name}",
                        onClick = { current = if (isSelected) current - category.id else current + category.id },
                        selected = isSelected,
                    )
                }
            }
        }
        Row {
            TextButton(onClick = { current = emptySet() }) { Text("Quitar selección") }
        }
        PrimaryButton("Aplicar", onClick = { onApply(current) })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MethodFilterSheet(selected: Set<PaymentMethod>, onApply: (Set<PaymentMethod>) -> Unit, onDismiss: () -> Unit) {
    var current by remember { mutableStateOf(selected) }
    WalletSheet(title = "Filtrar por método de pago", onDismiss = onDismiss) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            PaymentMethod.entries.forEach { method ->
                val isSelected = method in current
                ActionChip(
                    text = method.label(),
                    onClick = { current = if (isSelected) current - method else current + method },
                    selected = isSelected,
                    leading = { Icon(method.icon(), contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
        }
        PrimaryButton("Aplicar", onClick = { onApply(current) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateFilterSheet(
    current: DatePreset,
    onSelect: (DatePreset, java.time.LocalDate?, java.time.LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    var showRange by remember { mutableStateOf(false) }
    WalletSheet(title = "Filtrar por fecha", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            DatePreset.entries.forEach { preset ->
                ActionChip(
                    text = preset.label,
                    onClick = { if (preset == DatePreset.CUSTOM) showRange = true else onSelect(preset, null, null) },
                    selected = preset == current,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
    }
    if (showRange) {
        val state = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showRange = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null,
                    onClick = {
                        val from = state.selectedStartDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                        val to = (state.selectedEndDateMillis ?: state.selectedStartDateMillis)
                            ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                        showRange = false
                        onSelect(DatePreset.CUSTOM, from, to)
                    },
                ) { Text("Aplicar") }
            },
            dismissButton = { TextButton(onClick = { showRange = false }) { Text("Cancelar") } },
        ) {
            DateRangePicker(state = state, showModeToggle = false, modifier = Modifier.height(480.dp))
        }
    }
}
