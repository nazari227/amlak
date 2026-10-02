package com.example.diagnostics

import android.content.Context
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

class AppDiagnostics(context: Context) {

    companion object {
        private const val PREFS = "ashian_diagnostics"
        private const val KEY_CRASH_TRACE = "last_crash_trace"
        private const val KEY_CRASH_AT = "last_crash_at"
        private const val KEY_AUTH_STAGE = "last_auth_stage"
        private const val KEY_AUTH_STAGE_AT = "last_auth_stage_at"
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setAuthStage(stage: String) {
        prefs.edit()
            .putString(KEY_AUTH_STAGE, stage.take(120))
            .putString(KEY_AUTH_STAGE_AT, Instant.now().toString())
            .apply()
    }

    fun getAuthStage(): String = prefs.getString(KEY_AUTH_STAGE, "").orEmpty()
    fun getAuthStageAt(): String = prefs.getString(KEY_AUTH_STAGE_AT, "").orEmpty()

    fun recordCrash(throwable: Throwable) {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        prefs.edit()
            .putString(KEY_CRASH_TRACE, writer.toString().take(20_000))
            .putString(KEY_CRASH_AT, Instant.now().toString())
            .apply()
    }

    fun hasCrash(): Boolean = !prefs.getString(KEY_CRASH_TRACE, null).isNullOrBlank()
    fun getCrashTrace(): String = prefs.getString(KEY_CRASH_TRACE, "").orEmpty()
    fun getCrashAt(): String = prefs.getString(KEY_CRASH_AT, "").orEmpty()

    fun clearCrash() {
        prefs.edit()
            .remove(KEY_CRASH_TRACE)
            .remove(KEY_CRASH_AT)
            .apply()
    }
}
