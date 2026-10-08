package com.walletplus.app.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.walletplus.app.core.format.LocalMoneyFormatter
import com.walletplus.app.core.ui.theme.Motion
import com.walletplus.app.core.ui.theme.tabular
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Selector segmentado con indicador deslizante animado. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    height: Dp = 44.dp,
) {
    val haptics = LocalHapticFeedback.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
    ) {
        val segmentWidth = maxWidth / options.size
        val offset by animateDpAsState(segmentWidth * selectedIndex, Motion.gentle(), label = "segment")
        Box(
            Modifier
                .offset(x = offset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(selectedColor),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { index, label ->
                val color by animateColorAsState(
                    if (index == selectedIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "segmentText",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .combinedClickable(onClick = {
                            if (index != selectedIndex) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSelect(index)
                        }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = color)
                }
            }
        }
    }
}

/** Teclado numérico propio: registra montos sin abrir el teclado del sistema. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AmountKeypad(
    onDigit: (Char) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    keyHeight: Dp = 56.dp,
    showDecimal: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", "⌫"))
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    val enabled = key != "." || showDecimal
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(keyHeight)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (enabled && key != "⌫" && key != ".") MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
                            .combinedClickable(
                                enabled = enabled,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                                    when (key) {
                                        "⌫" -> onBackspace()
                                        "." -> onDecimal()
                                        else -> onDigit(key.first())
                                    }
                                },
                                onLongClick = if (key == "⌫") {
                                    {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onClear()
                                    }
                                } else {
                                    null
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            key == "⌫" -> Icon(
                                Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = "Borrar (mantén para borrar todo)",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            enabled -> Text(
                                key,
                                style = MaterialTheme.typography.headlineSmall.tabular(),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Campo de monto con símbolo de moneda; el texto se interpreta con Money.parse. */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    val symbol = LocalMoneyFormatter.current.symbol
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onValueChange(text.filter { it.isDigit() || it == '.' || it == ',' }.take(14)) },
        label = { Text(label) },
        prefix = { Text("$symbol ") },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        textStyle = MaterialTheme.typography.bodyLarge.tabular(),
        shape = MaterialTheme.shapes.medium,
        colors = walletFieldColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun walletFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
)

/** Chip de acción redondeado (fecha, método de pago...). */
@Composable
fun ActionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
    selected: Boolean = false,
) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "chip",
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
        label = "chipText",
    )
    Surface(onClick = onClick, modifier = modifier.height(40.dp), shape = CircleShape, color = container, contentColor = content) {
        Row(
            Modifier.padding(PaddingValues(horizontal = 14.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            leading?.invoke()
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

/** Diálogo de fecha que solo permite días válidos según [allowFuture]/[allowPast]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    today: LocalDate = LocalDate.now(),
    allowFuture: Boolean = false,
    allowPast: Boolean = true,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return (allowFuture || !date.isAfter(today)) && (allowPast || date.isAfter(today))
            }
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
            }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    ) {
        DatePicker(state = state, showModeToggle = false)
    }
}

@Composable
fun CalendarIcon() = Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))

@Composable
fun VerticalGap(height: Dp) = Spacer(Modifier.height(height))
