package com.example.security

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BiometricLockManager(
    context: Context,
    private val tokenStorage: EncryptedTokenStorage
) {
    private val prefs = context.getSharedPreferences("ashian_biometric_lock", Context.MODE_PRIVATE)
    private val _locked = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    fun isProtectedRole(): Boolean {
        if (!tokenStorage.isLoggedIn()) return false
        val role = tokenStorage.getUserRole().lowercase().trim()
        return role.isNotBlank() && role != "marketer"
    }

    private fun userKey(): String = "enabled_${tokenStorage.getUserId()}"

    fun isEnabledForCurrentUser(): Boolean =
        isProtectedRole() && prefs.getBoolean(userKey(), false)

    fun requiresSetup(): Boolean =
        isProtectedRole() && !isEnabledForCurrentUser()

    fun requiresUnlock(): Boolean =
        isEnabledForCurrentUser() && _locked.value

    fun enableForCurrentUser() {
        if (!isProtectedRole()) return
        prefs.edit().putBoolean(userKey(), true).apply()
        _locked.value = false
    }

    fun disableForCurrentUser() {
        prefs.edit().remove(userKey()).apply()
        _locked.value = false
    }

    fun markLocked() {
        if (isEnabledForCurrentUser()) _locked.value = true
    }

    fun markUnlocked() {
        _locked.value = false
    }

    fun resetTransientLock() {
        _locked.value = isEnabledForCurrentUser()
    }
}
