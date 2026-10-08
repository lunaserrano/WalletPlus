package com.walletplus.app.presentation.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.walletplus.app.core.di.LocalAppContainer
import com.walletplus.app.core.ui.components.AmountKeypad
import com.walletplus.app.core.ui.theme.Spacing
import com.walletplus.app.presentation.onboarding.WalletMark
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

const val PIN_LENGTH = 4

/** Pantalla de desbloqueo con PIN (estructura lista para sumar biometría). */
@Composable
fun LockScreen() {
    val appLock = LocalAppContainer.current.appLock
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var attempts by remember { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }

    LaunchedEffect(attempts) {
        if (attempts > 0) {
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
            for (x in listOf(18f, -16f, 10f, -6f, 0f)) shake.animateTo(x)
        }
    }

    fun tryUnlock(candidate: String) {
        scope.launch {
            if (!appLock.verify(candidate)) {
                error = true
                attempts++
                pin = ""
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.xxxl, vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(72.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) { WalletMark(Modifier.size(40.dp), accent = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)) }
        Spacer(Modifier.height(Spacing.xxl))
        Text("Introduce tu PIN", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            if (error) "PIN incorrecto. Inténtalo de nuevo." else "Tus finanzas están protegidas",
            style = MaterialTheme.typography.bodyMedium,
            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xxl))
        PinDots(pin.length, error, Modifier.offset { IntOffset(shake.value.roundToInt(), 0) })
        Spacer(Modifier.weight(1f))
        AmountKeypad(
            onDigit = { d ->
                if (pin.length < PIN_LENGTH) {
                    error = false
                    pin += d
                    if (pin.length == PIN_LENGTH) tryUnlock(pin)
                }
            },
            onDecimal = {},
            onBackspace = { pin = pin.dropLast(1) },
            onClear = { pin = "" },
            showDecimal = false,
            keyHeight = 64.dp,
        )
    }
}

@Composable
fun PinDots(filled: Int, error: Boolean, modifier: Modifier = Modifier, total: Int = PIN_LENGTH) {
    Row(
        modifier.semantics { contentDescription = "$filled dígitos introducidos" },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        repeat(total) { index ->
            val color by animateColorAsState(
                when {
                    error -> MaterialTheme.colorScheme.error
                    index < filled -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                label = "pinDot",
            )
            Box(Modifier.size(14.dp).clip(CircleShape).background(color))
        }
    }
}
