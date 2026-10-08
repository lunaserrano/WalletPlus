package com.walletplus.app.presentation.root

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walletplus.app.core.format.LocalMoneyFormatter
import com.walletplus.app.core.format.MoneyFormatter
import com.walletplus.app.core.ui.theme.Motion
import com.walletplus.app.presentation.navigation.MainScaffold
import com.walletplus.app.presentation.onboarding.OnboardingScreen
import com.walletplus.app.presentation.settings.LockScreen

@Composable
fun WalletPlusRoot(viewModel: RootViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = {
                (fadeIn(Motion.emphasized(Motion.MEDIUM)) + scaleIn(Motion.emphasized(Motion.MEDIUM), initialScale = 0.97f))
                    .togetherWith(fadeOut(Motion.emphasized(Motion.SHORT)))
            },
            label = "root",
        ) { current ->
            when (current) {
                RootState.Loading -> Box(Modifier.fillMaxSize())
                RootState.Onboarding -> OnboardingScreen()
                RootState.Locked -> LockScreen()
                is RootState.Ready -> {
                    val formatter = remember(current.currencyCode) { MoneyFormatter(current.currencyCode) }
                    CompositionLocalProvider(LocalMoneyFormatter provides formatter) {
                        MainScaffold()
                    }
                }
            }
        }
    }
}
