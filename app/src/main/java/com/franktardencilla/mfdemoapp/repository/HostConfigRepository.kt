package com.franktardencilla.mfdemoapp.repository

import android.content.Context
import com.franktardencilla.mfdemoapp.domain.model.HostConfig

class HostConfigRepository(
    context: Context
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun getHostConfig(): HostConfig {
        return HostConfig(
            primaryHost = preferences.getString(KEY_PRIMARY_HOST, null)
                ?: HostConfig.DEFAULT_PRIMARY_HOST,
            fallbackHost = preferences.getString(KEY_FALLBACK_HOST, null).orEmpty(),
            port = preferences.getInt(KEY_PORT, HostConfig.DEFAULT_PORT),
            timeoutMillis = preferences.getInt(KEY_TIMEOUT_MILLIS, HostConfig.DEFAULT_TIMEOUT_MILLIS)
        )
    }

    fun saveHostConfig(config: HostConfig) {
        preferences.edit()
            .putString(KEY_PRIMARY_HOST, config.primaryHost)
            .putString(KEY_FALLBACK_HOST, config.fallbackHost)
            .putInt(KEY_PORT, config.port)
            .putInt(KEY_TIMEOUT_MILLIS, config.timeoutMillis)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "host_config"
        const val KEY_PRIMARY_HOST = "primary_host"
        const val KEY_FALLBACK_HOST = "fallback_host"
        const val KEY_PORT = "port"
        const val KEY_TIMEOUT_MILLIS = "timeout_millis"
    }
}
