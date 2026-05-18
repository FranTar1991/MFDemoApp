package com.franktardencilla.mfdemoapp.app

import android.app.Application

class DemoApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer()
    }
}
