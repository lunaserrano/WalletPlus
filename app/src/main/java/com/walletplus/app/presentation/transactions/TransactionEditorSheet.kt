package com.walletplus.app.presentation.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walletplus.app.core.di.LocalAppContainer
import com.walletplus.app.core.di.ScopedViewModelStore
import com.walletplus.app.core.di.walletViewModel
import com.walletplus.app.core.format.DateFormats
import com.walletplus.app.core.format.LocalMoneyFormatter
import com.walletplus.app.core.ui.components.ActionChip
import com.walletplus.app.core.ui.components.AmountKeypad
import com.walletplus.app.core.ui.components.CalendarIcon
import com.walletplus.app.core.ui.components.LocalMessenger
import com.walletplus.app.core.ui.components.PrimaryButton
import com.walletplus.app.core.ui.components.SegmentedControl
import com.walletplus.app.core.ui.components.WalletDatePickerDialog
import com.walletplus.app.core.ui.components.categoryColor
import com.walletplus.app.core.ui.components.walletFieldColors
import com.walletplus.app.core.ui.theme.Spacing
import com.walletplus.app.core.ui.theme.WalletTheme
import com.walletplus.app.core.ui.theme.tabular
import com.walletplus.app.presentation.common.icon
import com.walletplus.app.presentation.common.label
import com.walletplus.app.presentation.common.message
import com.walletplus.app.presentation.common.shortLabel
import com.walletplus.app.presentation.navigation.EditorRequest
import com.walletplus.domain.model.Category
import com.walletplus.domain.model.PaymentMethod
import com.walletplus.domain.model.TransactionType
import com.walletplus.domain.validation.TransactionError
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Registro de movimientos en una sola hoja: pensado para anotar un gasto en segundos. */
@Composable
fun TransactionEditorSheet(request: EditorRequest, onDismiss: () -> Unit) {
    ScopedViewModelStore(request.nonce) {
        val viewModel = walletViewModel { c ->
            TransactionEditorViewModel(
                editId = request.id,
                initialType = request.type,
                transactions = c.transactions,
                categoryRepository = c.categories,
                save = c.saveTransaction,
                delete = c.deleteTransaction,
                clock = c.clock,
            )
        }
        EditorSheetContent(viewModel, onDismiss)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorSheetContent(viewModel: TransactionEditorViewModel, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val container = LocalAppContainer.current
    val messenger = LocalMessenger.current
    val formatter = LocalMoneyFormatter.current
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val shake = remember { Animatable(0f) }

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is EditorEvent.Saved -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    close()
                    if (event.isNew) {
                        val kind = if (event.type == TransactionType.EXPENSE) "Gasto" else "Ingreso"
                        messenger.show("$kind guardado · ${formatter.format(event.amount)}", "Deshacer") {
                            container.appScope.launch { container.deleteTransaction(event.id) }
                        }
                    } else {
                        messenger.show("Cambios guardados")
                    }
                }
                is EditorEvent.Deleted -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    close()
                    messenger.show("Movimiento eliminado", "Deshacer") {
                        container.appScope.launch { container.restoreTransaction(event.transaction) }
                    }
                }
            }
        }
    }

    LaunchedEffect(state.errors) {
        if (state.errors.isNotEmpty()) {
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
            for (x in listOf(14f, -12f, 8f, -6f, 0f)) shake.animateTo(x)
        }
    }

    var descriptionFocused by remember { mutableStateOf(false) }
    var showNotes by rememberSaveable { mutableStateOf(state.notes.isNotEmpty()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isExpense = state.type == TransactionType.EXPENSE
    val accent by animateColorAsState(
        if (isExpense) MaterialTheme.colorScheme.onSurface else WalletTheme.colors.income,
        label = "accent",
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.xl)
                .padding(bottom = Spacing.lg),
        ) {
            // Encabezado: tipo + acciones
            Row(verticalAlignment = Alignment.CenterVertically) {
                SegmentedControl(
                    options = listOf("Gasto", "Ingreso"),
                    selectedIndex = if (isExpense) 0 else 1,
                    onSelect = { viewModel.setType(if (it == 0) TransactionType.EXPENSE else TransactionType.INCOME) },
                    modifier = Modifier.weight(1f),
                )
                if (state.isEdit) {
                    IconButton(onClick = viewModel::deleteCurrent) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar movimiento", tint = MaterialTheme.colorScheme.error)
                    }
                }
                IconButton(onClick = ::close) { Icon(Icons.Rounded.Close, contentDescription = "Cerrar") }
            }

            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Monto: el protagonista
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.xl, bottom = Spacing.md)
                        .offset { IntOffset(shake.value.roundToInt(), 0) }
                        .animateContentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val display = AmountInput.display(state.amountText)
                    val style = when {
                        display.length > 11 -> MaterialTheme.typography.displaySmall
                        display.length > 8 -> MaterialTheme.typography.displayMedium
                        else -> MaterialTheme.typography.displayLarge
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = (if (isExpense) "−" else "+") + formatter.symbol,
                            style = MaterialTheme.typography.headlineSmall,
                            color = accent.copy(alpha = 0.55f),
                            modifier = Modifier.padding(top = 8.dp, end = 4.dp),
                        )
                        Text(
                            text = display,
                            style = style.tabular(),
                            color = if (state.amountText.isEmpty()) accent.copy(alpha = 0.3f) else accent,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                    val amountError = state.errors.firstOrNull { it == TransactionError.AMOUNT_REQUIRED || it == TransactionError.AMOUNT_NOT_POSITIVE }
                    AnimatedVisibility(amountError != null) {
                        Text(amountError?.message().orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Fecha y método
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    item {
                        ActionChip(
                            text = DateFormats.relativeDay(state.date, state.today),
                            onClick = { showDatePicker = true },
                            leading = { CalendarIcon() },
                        )
                    }
                    item { PaymentMethodChip(state.paymentMethod, viewModel::setMethod) }
                }

                Spacer(Modifier.height(Spacing.lg))
                Text("Categoría", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.sm))
                CategoryPicker(state.categories, state.categoryId, viewModel::selectCategory)
                val categoryError = state.errors.firstOrNull {
                    it == TransactionError.CATEGORY_REQUIRED || it == TransactionError.CATEGORY_INVALID || it == TransactionError.CATEGORY_INACTIVE
                }
                AnimatedVisibility(categoryError != null) {
                    Text(categoryError?.message().orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                state.errors.firstOrNull { it == TransactionError.DATE_IN_FUTURE || it == TransactionError.NO_ACCOUNT }?.let {
                    Text(it.message(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                AnimatedVisibility(isExpense, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Column {
                        Spacer(Modifier.height(Spacing.lg))
                        Text("¿Es necesario?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(Spacing.sm))
                        SegmentedControl(
                            options = listOf("Necesario", "Opcional"),
                            selectedIndex = if (state.isNecessary) 0 else 1,
                            onSelect = { viewModel.setNecessary(it == 0) },
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.lg))
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::setDescription,
                    placeholder = { Text("Descripción (opcional)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = walletFieldColors(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().onFocusChanged { descriptionFocused = it.isFocused },
                )
                AnimatedVisibility(showNotes) {
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = viewModel::setNotes,
                        placeholder = { Text("Notas") },
                        minLines = 2,
                        shape = MaterialTheme.shapes.medium,
                        colors = walletFieldColors(),
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm).onFocusChanged { descriptionFocused = it.isFocused },
                    )
                }
                if (!showNotes) {
                    TextButton(onClick = { showNotes = true }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Agregar nota")
                    }
                }
            }

            // Teclado propio (se oculta mientras se escribe texto)
            AnimatedVisibility(!descriptionFocused, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                AmountKeypad(
                    onDigit = viewModel::digit,
                    onDecimal = viewModel::decimal,
                    onBackspace = viewModel::backspace,
                    onClear = viewModel::clear,
                    keyHeight = 52.dp,
                    modifier = Modifier.padding(top = Spacing.md),
                )
            }
            Spacer(Modifier.height(Spacing.md))
            PrimaryButton(
                text = when {
                    state.isEdit -> "Guardar cambios"
                    isExpense -> "Guardar gasto"
                    else -> "Guardar ingreso"
                },
                onClick = {
                    focusManager.clearFocus()
                    viewModel.save()
                },
                enabled = !state.saving && !state.loading,
            )
        }
    }

    if (showDatePicker) {
        WalletDatePickerDialog(
            initial = state.date,
            today = state.today,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                viewModel.setDate(it)
                showDatePicker = false
            },
        )
    }
}

@Composable
private fun PaymentMethodChip(selected: PaymentMethod, onSelect: (PaymentMethod) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ActionChip(
            text = selected.shortLabel(),
            onClick = { expanded = true },
            leading = { Icon(selected.icon(), contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, shape = MaterialTheme.shapes.medium) {
            PaymentMethod.entries.forEach { method ->
                DropdownMenuItem(
                    text = { Text(method.label(), fontWeight = if (method == selected) FontWeight.SemiBold else null) },
                    leadingIcon = { Icon(method.icon(), contentDescription = null) },
                    onClick = {
                        onSelect(method)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CategoryPicker(categories: List<Category>, selectedId: Long?, onSelect: (Long) -> Unit) {
    val haptics = LocalHapticFeedback.current
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth().height(96.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(categories, key = { it.id }) { category ->
            val selected = category.id == selectedId
            val color = categoryColor(category.colorKey)
            val container by animateColorAsState(
                if (selected) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                label = "categoryChip",
            )
            Surface(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onSelect(category.id)
                },
                shape = CircleShape,
                color = container,
                border = if (selected) BorderStroke(1.5.dp, color) else null,
                modifier = Modifier.height(44.dp),
            ) {
                Row(
                    Modifier.padding(start = 10.dp, end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(category.emoji, style = MaterialTheme.typography.titleMedium)
                    Text(
                        category.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

