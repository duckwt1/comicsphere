package com.android.dacs3.utliz

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    fun saveLoginState(isLoggedIn: Boolean) {
        prefs.edit().putBoolean("is_logged_in", isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}