package com.walletplus.app.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.walletplus.app.core.di.walletViewModel
import com.walletplus.app.core.format.DateFormats
import com.walletplus.app.core.format.LocalMoneyFormatter
import com.walletplus.app.core.format.MoneyFormatter
import com.walletplus.app.core.format.SupportedCurrencies
import com.walletplus.app.core.ui.components.ActionChip
import com.walletplus.app.core.ui.components.AmountField
import com.walletplus.app.core.ui.components.CalendarIcon
import com.walletplus.app.core.ui.components.PrimaryButton
import com.walletplus.app.core.ui.components.WalletDatePickerDialog
import com.walletplus.app.core.ui.components.walletFieldColors
import com.walletplus.app.core.ui.theme.Motion
import com.walletplus.app.core.ui.theme.Spacing
import com.walletplus.app.core.ui.theme.WalletTheme
import com.walletplus.app.presentation.common.message
import com.walletplus.domain.money.Money
import com.walletplus.domain.usecase.CompleteOnboardingUseCase
import com.walletplus.domain.usecase.LoadDemoDataUseCase
import com.walletplus.domain.usecase.UpdatePreferencesUseCase
import com.walletplus.domain.validation.Outcome
import com.walletplus.domain.validation.SettingsError
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class OnboardingState(
    val step: Int = 0,
    val name: String = "",
    val currency: String = "USD",
    val balanceText: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    val errors: Set<SettingsError> = emptySet(),
    val busy: Boolean = false,
)

class OnboardingViewModel(
    private val complete: CompleteOnboardingUseCase,
    private val loadDemo: LoadDemoDataUseCase,
    private val preferences: UpdatePreferencesUseCase,
    clock: Clock,
) : ViewModel() {
    private val today = LocalDate.now(clock)
    private val _state = MutableStateFlow(OnboardingState(startDate = today, today = today))
    val state = _state.asStateFlow()

    fun goTo(step: Int) = _state.update { it.copy(step = step) }
    fun setName(value: String) = _state.update { it.copy(name = value.take(24)) }
    fun setCurrency(value: String) = _state.update { it.copy(currency = value) }
    fun setBalance(value: String) = _state.update { it.copy(balanceText = value, errors = emptySet()) }
    fun setStartDate(value: LocalDate) = _state.update { it.copy(startDate = value) }

    fun finish() {
        val current = _state.value
        val balance = if (current.balanceText.isBlank()) Money.ZERO else Money.parse(current.balanceText)
        if (balance == null) {
            _state.update { it.copy(errors = setOf(SettingsError.BALANCE_NEGATIVE)) }
            return
        }
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = complete(balance, current.startDate, current.currency, current.name)
            if (result is Outcome.Invalid) _state.update { it.copy(errors = result.errors, busy = false) }
        }
    }

    fun exploreDemo() {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            preferences.currency(_state.value.currency)
            loadDemo()
        }
    }
}

@Composable
fun OnboardingScreen() {
    val viewModel = walletViewModel {
        OnboardingViewModel(it.completeOnboarding, it.loadDemoData, it.updatePreferences, it.clock)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.step > 0) { viewModel.goTo(0) }

    AnimatedContent(
        targetState = state.step,
        transitionSpec = {
            val forward = targetState > initialState
            (slideInHorizontally(Motion.emphasized()) { if (forward) it / 4 else -it / 4 } + fadeIn(Motion.emphasized()))
                .togetherWith(slideOutHorizontally(Motion.emphasized()) { if (forward) -it / 4 else it / 4 } + fadeOut(Motion.emphasized(Motion.SHORT)))
        },
        label = "onboarding",
    ) { step ->
        when (step) {
            0 -> WelcomeStep(busy = state.busy, onStart = { viewModel.goTo(1) }, onDemo = viewModel::exploreDemo)
            else -> SetupStep(state, viewModel)
        }
    }
}

@Composable
private fun WelcomeStep(busy: Boolean, onStart: () -> Unit, onDemo: () -> Unit) {
    val colors = WalletTheme.colors
    LightStatusBarIcons()
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge.copy(topStart = androidx.compose.foundation.shape.CornerSize(0), topEnd = androidx.compose.foundation.shape.CornerSize(0)))
                .background(Brush.linearGradient(listOf(colors.heroStart, colors.heroEnd)))
                .statusBarsPadding()
                .padding(horizontal = Spacing.xxl, vertical = Spacing.xxxl),
        ) {
            Column {
                WalletMark(Modifier.size(72.dp))
                Spacer(Modifier.height(Spacing.xxl))
                Text("WalletPlus", style = MaterialTheme.typography.labelLarge, color = colors.onHeroMuted)
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Tu dinero,\nclaro y en calma.",
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.onHero,
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    "Registra ingresos y gastos en segundos, entiende en qué gastas y descubre cuánto puedes ahorrar.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onHeroMuted,
                )
            }
        }

        Column(
            Modifier.padding(horizontal = Spacing.xxl, vertical = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            Feature(Icons.Rounded.Lock, "Privado por diseño", "Sin cuentas ni registros. Tus datos nunca salen de tu teléfono.")
            Feature(Icons.Rounded.CloudOff, "Funciona sin Internet", "Todo se guarda y se calcula en tu dispositivo.")
            Feature(Icons.Rounded.AutoAwesome, "Un asistente, no solo un registro", "Análisis y sugerencias de ahorro basadas en tus hábitos.")
        }

        Spacer(Modifier.weight(1f))
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = Spacing.xxl, vertical = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PrimaryButton("Comenzar", onClick = onStart, enabled = !busy)
            Spacer(Modifier.height(Spacing.sm))
            if (busy) {
                CircularProgressIndicator(Modifier.size(28.dp).padding(4.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = onDemo, modifier = Modifier.fillMaxWidth()) {
                    Text("Explorar con datos de demostración")
                }
            }
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Box(
            Modifier.size(44.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    var showDatePicker by remember { mutableStateOf(false) }
    val formatter = remember(state.currency) { MoneyFormatter(state.currency) }
    CompositionLocalProvider(LocalMoneyFormatter provides formatter) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            IconButton(onClick = { viewModel.goTo(0) }, modifier = Modifier.padding(start = Spacing.sm, top = Spacing.sm)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                Column {
                    Text("Configura tu billetera", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        "Solo necesitamos tu saldo actual para empezar. Podrás cambiarlo después.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    label = { Text("¿Cómo te llamamos? (opcional)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = walletFieldColors(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )

                Column {
                    Text("Moneda", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Spacing.sm))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SupportedCurrencies.forEach { code ->
                            ActionChip(text = code, onClick = { viewModel.setCurrency(code) }, selected = code == state.currency)
                        }
                    }
                }

                AmountField(
                    value = state.balanceText,
                    onValueChange = viewModel::setBalance,
                    label = "Saldo inicial",
                    error = state.errors.firstOrNull { it == SettingsError.BALANCE_NEGATIVE }?.message(),
                    imeAction = ImeAction.Done,
                )

                Column {
                    Text("Fecha de inicio", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Spacing.sm))
                    ActionChip(
                        text = if (state.startDate == state.today) "Hoy" else DateFormats.short(state.startDate),
                        onClick = { showDatePicker = true },
                        leading = { CalendarIcon() },
                    )
                    state.errors.firstOrNull { it == SettingsError.DATE_IN_FUTURE }?.let {
                        Text(it.message(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Column(Modifier.padding(horizontal = Spacing.xxl, vertical = Spacing.lg)) {
                PrimaryButton("Empezar", onClick = viewModel::finish, enabled = !state.busy)
            }
        }
    }

    if (showDatePicker) {
        WalletDatePickerDialog(
            initial = state.startDate,
            today = state.today,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                viewModel.setStartDate(it)
                showDatePicker = false
            },
        )
    }
}

/** Marca de la app dibujada en Compose (misma silueta que el ícono del lanzador). */
@Composable
fun WalletMark(modifier: Modifier = Modifier, body: Color = Color.White, accent: Color = Color(0xFF0B9470)) {
    Canvas(modifier) {
        val w = size.width
        val unit = w / 48f
        drawRoundRect(
            color = body.copy(alpha = 0.55f),
            topLeft = Offset(6 * unit, 6 * unit),
            size = Size(30 * unit, 10 * unit),
            cornerRadius = CornerRadius(4 * unit),
        )
        drawRoundRect(
            color = body,
            topLeft = Offset(0f, 12 * unit),
            size = Size(48 * unit, 34 * unit),
            cornerRadius = CornerRadius(9 * unit),
        )
        drawCircle(accent, radius = 8 * unit, center = Offset(38 * unit, 29 * unit))
        drawRect(body, topLeft = Offset(37 * unit, 25 * unit), size = Size(2 * unit, 8 * unit))
        drawRect(body, topLeft = Offset(34 * unit, 28 * unit), size = Size(8 * unit, 2 * unit))
    }
}


/** Íconos claros en la barra de estado mientras la cabecera oscura está visible. */
@Composable
private fun LightStatusBarIcons() {
    val view = androidx.compose.ui.platform.LocalView.current
    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    androidx.compose.runtime.DisposableEffect(view) {
        val window = (view.context as? android.app.Activity)?.window
        val controller = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = false
        onDispose { controller?.isAppearanceLightStatusBars = !darkTheme }
    }
}
