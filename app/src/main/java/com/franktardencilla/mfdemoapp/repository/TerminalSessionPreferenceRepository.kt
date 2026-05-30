package com.franktardencilla.mfdemoapp.repository

import android.content.Context

class TerminalSessionPreferenceRepository(
    context: Context
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun shouldRestoreConnectedSession(): Boolean {
        return preferences.getBoolean(KEY_SHOULD_RESTORE_CONNECTED_SESSION, false)
    }

    fun setShouldRestoreConnectedSession(shouldRestore: Boolean) {
        preferences.edit()
            .putBoolean(KEY_SHOULD_RESTORE_CONNECTED_SESSION, shouldRestore)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "terminal_session"
        const val KEY_SHOULD_RESTORE_CONNECTED_SESSION = "should_restore_connected_session"
    }
}
