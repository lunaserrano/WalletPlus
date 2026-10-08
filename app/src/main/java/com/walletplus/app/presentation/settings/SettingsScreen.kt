package com.walletplus.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walletplus.app.core.di.walletViewModel
import com.walletplus.app.core.format.DateFormats
import com.walletplus.app.core.format.LocalMoneyFormatter
import com.walletplus.app.core.format.SupportedCurrencies
import com.walletplus.app.core.ui.components.ActionChip
import com.walletplus.app.core.ui.components.AmountField
import com.walletplus.app.core.ui.components.AmountKeypad
import com.walletplus.app.core.ui.components.CalendarIcon
import com.walletplus.app.core.ui.components.IconBadge
import com.walletplus.app.core.ui.components.LocalMessenger
import com.walletplus.app.core.ui.components.PrimaryButton
import com.walletplus.app.core.ui.components.WalletCard
import com.walletplus.app.core.ui.components.WalletDatePickerDialog
import com.walletplus.app.core.ui.components.WalletSheet
import com.walletplus.app.core.ui.components.walletFieldColors
import com.walletplus.app.core.ui.theme.Spacing
import com.walletplus.app.presentation.common.label
import com.walletplus.app.presentation.common.message
import com.walletplus.app.presentation.planning.PlanSettingsSheet
import com.walletplus.app.presentation.transactions.AmountInput
import com.walletplus.domain.money.Money
import com.walletplus.domain.validation.Outcome
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class SettingsSheet { NAME, CURRENCY, ACCOUNT, PLAN, PIN }
private enum class Confirm { DEMO, RESET }

@Composable
fun SettingsScreen(onBack: () -> Unit, onReplayTour: () -> Unit, onOpenCategories: () -> Unit) {
    val viewModel = walletViewModel { c ->
        SettingsViewModel(
            c.settings, c.accounts, c.budgets, c.updatePreferences, c.updateAccount, c.updatePlan,
            c.loadDemoData, c.resetAllData, c.appLock, c.clock,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val messenger = LocalMessenger.current
    val formatter = LocalMoneyFormatter.current
    var sheet by remember { mutableStateOf<SettingsSheet?>(null) }
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    val current = state ?: return Box(Modifier.fillMaxSize())
    val settings = current.settings

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = Spacing.screen)
            .padding(bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(Modifier.padding(top = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver") }
            Text("Configuración", style = MaterialTheme.typography.headlineSmall)
        }

        Group("Perfil") {
            SettingRow(Icons.Rounded.Person, "Nombre", settings.displayName ?: "Opcional · solo para el saludo") { sheet = SettingsSheet.NAME }
        }

        Group("Cuenta") {
            SettingRow(Icons.Rounded.Payments, "Moneda", settings.currencyCode) { sheet = SettingsSheet.CURRENCY }
            current.account?.let { account ->
                SettingRow(
                    Icons.Rounded.AccountBalanceWallet,
                    "Saldo inicial",
                    "${formatter.format(account.initialBalance)} · desde ${DateFormats.short(account.startDate)}",
                ) { sheet = SettingsSheet.ACCOUNT }
            }
        }

        Group("Planificación") {
            SettingRow(
                Icons.Rounded.Tune,
                "Presupuesto general",
                settings.payPeriod.label() + " · " +
                    if (settings.plan.isConfigured) "gastos máx. ${formatter.format(settings.plan.spendingLimit)} · ahorro ${formatter.format(settings.plan.savingsTarget)}"
                    else "ingreso esperado, gasto máximo y ahorro",
            ) { sheet = SettingsSheet.PLAN }
            SettingRow(Icons.Rounded.Category, "Categorías", "Crear, editar o desactivar", onClick = onOpenCategories)
        }

        Group("Ayuda") {
            SettingRow(Icons.Rounded.Explore, "Ver recorrido guiado", "Repasa las funciones principales paso a paso") {
                scope.launch {
                    viewModel.replayTour()
                    onReplayTour()
                }
            }
        }

        Group("Seguridad") {
            SettingRow(
                icon = Icons.Rounded.Lock,
                title = "Bloqueo con PIN",
                subtitle = if (settings.lockEnabled) "Activado · se pide al volver a la app" else "Protege tus datos con un PIN de 4 dígitos",
                trailing = {
                    Switch(checked = settings.lockEnabled, onCheckedChange = { enabled ->
                        if (enabled) {
                            sheet = SettingsSheet.PIN
                        } else {
                            scope.launch {
                                viewModel.disableLock()
                                messenger.show("Bloqueo desactivado")
                            }
                        }
                    })
                },
            ) { if (!settings.lockEnabled) sheet = SettingsSheet.PIN }
        }

        Group("Datos") {
            SettingRow(
                Icons.Rounded.Science,
                if (settings.demoMode) "Datos de demostración activos" else "Cargar datos de demostración",
                if (settings.demoMode) "Reemplazan tus datos. Bórralos para empezar de verdad." else "Para explorar gráficos y análisis",
            ) { confirm = Confirm.DEMO }
            SettingRow(Icons.Rounded.FileDownload, "Exportar CSV, PDF o copia de seguridad", "Próximamente", enabled = false) {}
            SettingRow(
                Icons.Rounded.DeleteForever,
                if (settings.demoMode) "Borrar datos de demostración" else "Borrar todos los datos",
                "Vuelve a empezar desde cero",
                tint = MaterialTheme.colorScheme.error,
            ) { confirm = Confirm.RESET }
        }

        WalletCard(color = MaterialTheme.colorScheme.primaryContainer) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Column {
                    Text("Privacidad", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "WalletPlus funciona sin Internet y sin cuenta. Tus datos financieros se guardan solo en este dispositivo y no se envían a ningún servicio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        Text(
            "WalletPlus 1.0.0 · Las recomendaciones son educativas y no constituyen asesoría financiera profesional.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.xs),
        )
    }

    when (sheet) {
        SettingsSheet.NAME -> NameSheet(settings.displayName.orEmpty(), onDismiss = { sheet = null }) { name ->
            scope.launch { viewModel.setName(name) }
            sheet = null
        }
        SettingsSheet.CURRENCY -> CurrencySheet(settings.currencyCode, onDismiss = { sheet = null }) { code ->
            scope.launch { viewModel.setCurrency(code) }
            sheet = null
        }
        SettingsSheet.ACCOUNT -> current.account?.let { account ->
            AccountSheet(account.initialBalance, account.startDate, current.today, viewModel, onDismiss = { sheet = null })
        }
        SettingsSheet.PLAN -> PlanSettingsSheet(
            plan = settings.plan,
            payPeriod = settings.payPeriod,
            hasCategoryBudgets = current.hasBudgets,
            onSave = viewModel::savePlan,
            onDismiss = { sheet = null },
        )
        SettingsSheet.PIN -> PinSetupSheet(onDismiss = { sheet = null }) { pin ->
            scope.launch {
                viewModel.setPin(pin)
                messenger.show("Bloqueo con PIN activado")
            }
            sheet = null
        }
        null -> Unit
    }

    confirm?.let { kind ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (kind == Confirm.DEMO) "¿Cargar datos de demostración?" else "¿Borrar todos los datos?") },
            text = {
                Text(
                    if (kind == Confirm.DEMO) "Se reemplazarán tus movimientos, presupuestos y metas por datos de ejemplo claramente marcados. Esta acción no se puede deshacer."
                    else "Se eliminarán movimientos, presupuestos, metas y categorías personalizadas. Volverás a la configuración inicial. Esta acción no se puede deshacer.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    scope.launch {
                        if (kind == Confirm.DEMO) {
                            viewModel.loadDemoData()
                            messenger.show("Datos de demostración cargados")
                        } else {
                            viewModel.resetAll()
                        }
                    }
                }) { Text(if (kind == Confirm.DEMO) "Cargar" else "Borrar", color = if (kind == Confirm.RESET) MaterialTheme.colorScheme.error else Color.Unspecified) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.xs, top = Spacing.md, bottom = Spacing.sm),
        )
        WalletCard(contentPadding = PaddingValues(vertical = Spacing.xs)) { content() }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, enabled = enabled, color = Color.Transparent) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            IconBadge(icon, if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant, size = 36.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailing != null) trailing()
            else if (enabled) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NameSheet(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    WalletSheet("Tu nombre", onDismiss = onDismiss) {
        Text("Solo se usa para saludarte. Nunca sale de tu teléfono.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(24) },
            label = { Text("Nombre (opcional)") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = walletFieldColors(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton("Guardar", onClick = { onSave(name) })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CurrencySheet(current: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    WalletSheet("Moneda", onDismiss = onDismiss) {
        Text(
            "Cambiar la moneda solo cambia cómo se muestran los montos; no convierte valores.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SupportedCurrencies.forEach { code -> ActionChip(code, onClick = { onSelect(code) }, selected = code == current) }
        }
    }
}

@Composable
private fun AccountSheet(
    balance: Money,
    start: LocalDate,
    today: LocalDate,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val messenger = LocalMessenger.current
    var amount by remember { mutableStateOf(AmountInput.fromMoney(balance)) }
    var date by remember { mutableStateOf(start) }
    var picker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    WalletSheet("Saldo inicial", onDismiss = onDismiss) {
        Text(
            "Tu saldo actual se calcula como saldo inicial + ingresos − gastos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AmountField(amount, { amount = it; error = null }, "Saldo inicial", error = error, imeAction = ImeAction.Done)
        ActionChip("Desde ${DateFormats.short(date)}", onClick = { picker = true }, leading = { CalendarIcon() })
        PrimaryButton("Guardar", onClick = {
            scope.launch {
                when (val result = viewModel.saveAccount(Money.parse(amount), date)) {
                    is Outcome.Success -> {
                        messenger.show("Saldo inicial actualizado")
                        onDismiss()
                    }
                    is Outcome.Invalid -> error = result.errors.first().message()
                }
            }
        })
    }
    if (picker) {
        WalletDatePickerDialog(date, onDismiss = { picker = false }, onConfirm = { date = it; picker = false }, today = today)
    }
}

/** Configuración de PIN en dos pasos: introducir y confirmar. */
@Composable
private fun PinSetupSheet(onDismiss: () -> Unit, onConfirmed: (String) -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }
    WalletSheet(if (first == null) "Crea tu PIN" else "Confirma tu PIN", onDismiss = onDismiss) {
        Text(
            when {
                mismatch -> "Los PIN no coinciden. Vuelve a intentarlo."
                first == null -> "Elige 4 dígitos. Se guardan cifrados con un hash, nunca en texto plano."
                else -> "Introduce el mismo PIN otra vez."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (mismatch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(Modifier.fillMaxWidth().padding(vertical = Spacing.lg), contentAlignment = Alignment.Center) {
            PinDots(pin.length, mismatch)
        }
        AmountKeypad(
            onDigit = { d ->
                if (pin.length < PIN_LENGTH) {
                    mismatch = false
                    pin += d
                    if (pin.length == PIN_LENGTH) {
                        val stored = first
                        when {
                            stored == null -> {
                                first = pin
                                pin = ""
                            }
                            stored == pin -> onConfirmed(pin)
                            else -> {
                                mismatch = true
                                first = null
                                pin = ""
                            }
                        }
                    }
                }
            },
            onDecimal = {},
            onBackspace = { pin = pin.dropLast(1) },
            onClear = { pin = "" },
            showDecimal = false,
        )
    }
}

