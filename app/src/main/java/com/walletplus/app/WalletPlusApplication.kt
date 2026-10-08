package com.walletplus.app

import android.app.Application
import com.walletplus.app.core.di.AppContainer

class WalletPlusApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
