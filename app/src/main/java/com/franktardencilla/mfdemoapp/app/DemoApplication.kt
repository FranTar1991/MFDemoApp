package com.franktardencilla.mfdemoapp.app

import android.app.Application
import com.franktardencilla.mfdemoapp.BuildConfig

class DemoApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(
            context = this,
            runtimeMode = if (BuildConfig.USE_REAL_YSDK) {
                AppRuntimeMode.REAL_YSDK
            } else {
                AppRuntimeMode.MOCK
            }
        )
    }
}
