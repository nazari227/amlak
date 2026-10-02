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
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_BRANCH_ID = "branch_id"
        private const val KEY_BRANCH_NAME = "branch_name"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_SCREENSHOT_PROTECTED = "screenshot_protected"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Ensure consistent device session ID
        if (prefs.getString(KEY_DEVICE_SESSION_ID, null) == null) {
            val deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_SESSION_ID, deviceId).apply()
        }
    }

    fun getDeviceId(): String {
        return prefs.getString(KEY_DEVICE_SESSION_ID, "") ?: UUID.randomUUID().toString()
    }

    fun getAccessToken(): String? {
        val encrypted = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val decrypted = keyStoreManager.decrypt(encrypted)
        return decrypted.ifEmpty { null }
    }

    fun saveAccessToken(token: String) {
        val encrypted = keyStoreManager.encrypt(token)
        prefs.edit().putString(KEY_ACCESS_TOKEN, encrypted).apply()
    }

    fun getRefreshToken(): String? {
        val encrypted = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val decrypted = keyStoreManager.decrypt(encrypted)
        return decrypted.ifEmpty { null }
    }

    fun saveRefreshToken(token: String) {
        val encrypted = keyStoreManager.encrypt(token)
        prefs.edit().putString(KEY_REFRESH_TOKEN, encrypted).apply()
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        saveAccessToken(accessToken)
        saveRefreshToken(refreshToken)
    }

    fun saveUserProfile(
        userId: Long,
        fullName: String,
        phone: String,
        branchId: Long,
        branchName: String,
        role: String
    ) {
        prefs.edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, fullName)
            .putString(KEY_USER_PHONE, phone)
            .putLong(KEY_BRANCH_ID, branchId)
            .putString(KEY_BRANCH_NAME, branchName)
            .putString(KEY_USER_ROLE, role)
            .apply()
    }

    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, 0L)
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "مشاور املاک") ?: "مشاور املاک"
    fun getUserPhone(): String = prefs.getString(KEY_USER_PHONE, "") ?: ""
    fun getBranchId(): Long = prefs.getLong(KEY_BRANCH_ID, 1L)
    fun getBranchName(): String = prefs.getString(KEY_BRANCH_NAME, "شعبه مرکزی") ?: "شعبه مرکزی"
    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "consultant") ?: "consultant"

    fun isLoggedIn(): Boolean {
        return !getAccessToken().isNullOrBlank()
    }

    fun clearAuth() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_PHONE)
            .apply()
    }

    fun isScreenshotProtectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_SCREENSHOT_PROTECTED, false)
    }

    fun setScreenshotProtection(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCREENSHOT_PROTECTED, enabled).apply()
    }
}
