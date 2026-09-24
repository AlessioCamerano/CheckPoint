package com.example.checkpoint

import android.app.Application
import com.example.checkpoint.di.AppContainer
import com.example.checkpoint.di.DefaultAppContainer

/**
 * Punto di ingresso dell'applicazione (Application class).
 * Inizializza il contenitore delle dipendenze [AppContainer] all'avvio dell'app.
 */
class CheckPointApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
