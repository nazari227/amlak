package com.example.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class SafeLoggingInterceptor : Interceptor {

    companion object {
        private const val TAG = "AshianMelkNet"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val method = request.method

        // Safe log - never print raw headers that might contain tokens or request bodies with passwords
        val hasAuth = request.header("Authorization") != null
        val deviceId = request.header("X-Device-Id")?.take(6) ?: "none"
        Log.d(TAG, "--> $method $url [Auth: $hasAuth, DevId: $deviceId...]")

        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            Log.e(TAG, "<-- $method $url FAILED: ${e.message}")
            throw e
        }

        val tookMs = (System.nanoTime() - startNs) / 1e6
        Log.d(TAG, "<-- ${response.code} ${response.message} $url (${tookMs}ms)")

        return response
    }
}
