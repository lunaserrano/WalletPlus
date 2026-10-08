package com.walletplus.app.presentation.tour

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletplus.domain.repository.SettingsRepository
import com.walletplus.domain.usecase.UpdatePreferencesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Identificadores de los elementos que señala el recorrido de primer uso. */
object TourIds {
    const val BALANCE = "balance"
    const val ADD = "add"
    const val TRANSACTIONS = "tab_transactions"
    const val PLAN = "tab_plan"
    const val ANALYSIS = "tab_analysis"
    const val SETTINGS = "settings"
}

val FirstUseTourSteps = listOf(
    TourStep(
        targetId = null,
        title = "¿Te mostramos cómo funciona?",
        body = "Un recorrido de un minuto por lo esencial de WalletPlus. Puedes omitirlo cuando quieras y repetirlo desde Configuración.",
        icon = Icons.Rounded.Explore,
    ),
    TourStep(
        targetId = TourIds.BALANCE,
        title = "Tu saldo, siempre al día",
        body = "Se calcula solo: saldo inicial + ingresos − gastos. Aquí también ves cuánto entró y salió este mes.",
        icon = Icons.Rounded.AccountBalanceWallet,
        corner = 38.dp,
    ),
    TourStep(
        targetId = TourIds.ADD,
        title = "Registra en segundos",
        body = "Toca + para anotar un gasto o un ingreso. Escribe el monto, elige la categoría y listo. Si te equivocas, puedes deshacerlo.",
        icon = Icons.Rounded.AddCircle,
        circular = true,
    ),
    TourStep(
        targetId = TourIds.TRANSACTIONS,
        title = "Tu historial completo",
        body = "Busca y filtra por fecha, categoría, tipo o método de pago. Toca un movimiento para editarlo o deslízalo para eliminarlo.",
        icon = Icons.Rounded.ReceiptLong,
    ),
    TourStep(
        targetId = TourIds.PLAN,
        title = "Presupuestos y metas",
        body = "Pon límites mensuales por categoría y te avisaremos al acercarte. Crea metas y te diremos cuánto apartar por semana.",
        icon = Icons.Rounded.TrackChanges,
    ),
    TourStep(
        targetId = TourIds.ANALYSIS,
        title = "Entiende tus hábitos",
        body = "Descubre en qué gastas, qué parte es opcional y cómo vas frente al mes anterior, con sugerencias para ahorrar.",
        icon = Icons.Rounded.Insights,
    ),
    TourStep(
        targetId = TourIds.SETTINGS,
        title = "Todo bajo tu control",
        body = "Moneda, categorías, bloqueo con PIN y datos de demostración. Desde aquí puedes volver a ver este recorrido.",
        icon = Icons.Rounded.Settings,
        circular = true,
    ),
)

class TourViewModel(
    settings: SettingsRepository,
    private val preferences: UpdatePreferencesUseCase,
) : ViewModel() {

    /** null mientras carga, para no mostrar el recorrido por error. */
    val shouldShow: StateFlow<Boolean?> = settings.observe()
        .map { !it.tourCompleted }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun finish() {
        viewModelScope.launch { preferences.tourCompleted(true) }
    }
}
