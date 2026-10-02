package com.example.security

import android.view.Window
import android.view.WindowManager
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object SecurityUtils {

    fun calculateSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun maskPhoneNumber(phone: String): String {
        if (phone.length < 7) return "***"
        return phone.take(4) + "***" + phone.takeLast(3)
    }

    fun maskToken(token: String): String {
        if (token.length <= 8) return "******"
        return token.take(4) + "..." + token.takeLast(4)
    }

    fun setWindowSecure(window: Window?, isSecure: Boolean) {
        window?.let {
            if (isSecure) {
                it.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                it.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}
