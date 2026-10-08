package com.walletplus.app.core.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer no disponible") }

/** Crea un ViewModel con acceso al contenedor de dependencias. */
@Composable
inline fun <reified VM : ViewModel> walletViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = LocalAppContainer.current
    return viewModel(key = key) { create(container) }
}
