package com.walletplus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.walletplus.app.core.di.LocalAppContainer
import com.walletplus.app.core.ui.theme.WalletPlusTheme
import com.walletplus.app.presentation.root.RootState
import com.walletplus.app.presentation.root.RootViewModel
import com.walletplus.app.presentation.root.WalletPlusRoot

class MainActivity : ComponentActivity() {

    private val container by lazy { (application as WalletPlusApplication).container }

    private val rootViewModel: RootViewModel by lazy {
        ViewModelProvider(
            this,
            viewModelFactory { initializer { RootViewModel(container.settings, container.appLock) } },
        )[RootViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { rootViewModel.state.value is RootState.Loading }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                WalletPlusTheme {
                    WalletPlusRoot(rootViewModel)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Al salir de la app se vuelve a pedir el PIN (si el bloqueo está activo).
        if (!isChangingConfigurations) container.appLock.lock()
    }
}
