package com.example.checkpoint

import android.app.Application
import com.example.checkpoint.di.AppContainer
import com.example.checkpoint.di.DefaultAppContainer

/**
 * Entry point dell'applicazione.
 * Inizializza il contenitore delle dipendenze al lancio dell'app.
 */
class CheckPointApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}