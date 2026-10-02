package com.example.security

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class EncryptedTokenStorage(
    context: Context,
    private val keyStoreManager: KeyStoreManager
) {
    companion object {
        private const val PREFS_NAME = "ashian_secure_vault"
        private const val KEY_ACCESS_TOKEN = "enc_access_token"
        private const val KEY_REFRESH_TOKEN = "enc_refresh_token"
        private const val KEY_DEVICE_SESSION_ID = "device_session_id"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "enc_user_name"
        private const val KEY_BRANCH_ID = "branch_id"
        private const val KEY_USER_ROLE = "enc_user_role"
        private const val KEY_SCREENSHOT_PROTECTED = "screenshot_protected"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        if (prefs.getString(KEY_DEVICE_SESSION_ID, null) == null) {
            prefs.edit().putString(KEY_DEVICE_SESSION_ID, UUID.randomUUID().toString()).apply()
        }
    }

    fun getDeviceId(): String = prefs.getString(KEY_DEVICE_SESSION_ID, null)
        ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_DEVICE_SESSION_ID, it).apply()
        }

    fun getAccessToken(): String? = getEncrypted(KEY_ACCESS_TOKEN)
    fun getRefreshToken(): String? = getEncrypted(KEY_REFRESH_TOKEN)

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, keyStoreManager.encrypt(accessToken))
            .putString(KEY_REFRESH_TOKEN, keyStoreManager.encrypt(refreshToken))
            .apply()
    }

    fun saveUserProfile(
        userId: Long,
        fullName: String,
        branchId: Long,
        role: String
    ) {
        prefs.edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, keyStoreManager.encrypt(fullName))
            .putLong(KEY_BRANCH_ID, branchId)
            .putString(KEY_USER_ROLE, keyStoreManager.encrypt(role))
            .apply()
    }

    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, 0L)
    fun getUserName(): String = getEncrypted(KEY_USER_NAME).orEmpty()
    fun getBranchId(): Long = prefs.getLong(KEY_BRANCH_ID, 0L)
    fun getUserRole(): String = getEncrypted(KEY_USER_ROLE).orEmpty()

    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank() && !getRefreshToken().isNullOrBlank()

    fun clearAuth() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_BRANCH_ID)
            .remove(KEY_USER_ROLE)
            .apply()
    }

    fun isScreenshotProtectionEnabled(): Boolean =
        prefs.getBoolean(KEY_SCREENSHOT_PROTECTED, true)

    fun setScreenshotProtection(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCREENSHOT_PROTECTED, enabled).apply()
    }

    private fun getEncrypted(key: String): String? {
        val encrypted = prefs.getString(key, null) ?: return null
        return keyStoreManager.decrypt(encrypted).ifBlank { null }
    }
}
