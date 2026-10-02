package com.example.diagnostics

import android.content.Context
import java.io.PrintWriter
import java.io.StringWriter
import java.security.MessageDigest

data class CrashSnapshot(
    val code: String,
    val message: String,
    val stack: String,
    val timestamp: Long
)

class CrashDiagnostics(private val context: Context) {

    companion object {
        private const val PREFS = "ashian_crash_diagnostics"
        private const val KEY_MESSAGE = "last_crash_message"
        private const val KEY_STACK = "last_crash_stack"
        private const val KEY_TIME = "last_crash_time"
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                record(throwable)
            } catch (_: Throwable) {
            } finally {
                previous?.uncaughtException(thread, throwable)
            }
        }
    }

    fun peek(): CrashSnapshot? {
        val stack = prefs.getString(KEY_STACK, null)?.takeIf { it.isNotBlank() } ?: return null
        val message = prefs.getString(KEY_MESSAGE, "خطای ناشناخته") ?: "خطای ناشناخته"
        val timestamp = prefs.getLong(KEY_TIME, 0L)
        return CrashSnapshot(
            code = fingerprint(stack),
            message = message,
            stack = stack,
            timestamp = timestamp
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun record(throwable: Throwable) {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        val stack = writer.toString()
            .replace(Regex("(?i)(password|token|authorization|cookie)[^\\n]{0,120}"), "[redacted]")
            .take(12000)

        val message = buildString {
            append(throwable::class.java.simpleName)
            val m = throwable.message?.trim().orEmpty()
            if (m.isNotEmpty()) append(": ").append(m.take(500))
        }

        prefs.edit()
            .putString(KEY_MESSAGE, message)
            .putString(KEY_STACK, stack)
            .putLong(KEY_TIME, System.currentTimeMillis())
            .commit()
    }

    private fun fingerprint(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return digest.take(4).joinToString("") { "%02X".format(it) }
    }
}
