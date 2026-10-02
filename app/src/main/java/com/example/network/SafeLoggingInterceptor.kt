package com.example.network

import android.util.Log
import com.example.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class SafeLoggingInterceptor : Interceptor {

    companion object {
        private const val TAG = "AshianMelkNet"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!BuildConfig.DEBUG) return chain.proceed(request)

        val method = request.method
        val path = request.url.encodedPath
        val startNs = System.nanoTime()

        Log.d(TAG, "--> $method $path")
        return try {
            val response = chain.proceed(request)
            val tookMs = (System.nanoTime() - startNs) / 1_000_000
            Log.d(TAG, "<-- ${response.code} $method $path (${tookMs}ms)")
            response
        } catch (e: Exception) {
            Log.e(TAG, "<-- FAILED $method $path [${e.javaClass.simpleName}]")
            throw e
        }
    }
}
