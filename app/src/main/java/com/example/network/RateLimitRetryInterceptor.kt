package com.example.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class RateLimitRetryInterceptor(
    private val maxRetries: Int = 2
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val safeToRetry = request.method in setOf("GET", "HEAD", "OPTIONS") ||
            !request.header("Idempotency-Key").isNullOrBlank()

        if (!safeToRetry) return chain.proceed(request)

        var response: Response? = null
        var attempt = 0
        var delayMs = 750L

        while (attempt <= maxRetries) {
            try {
                response?.close()
                response = chain.proceed(request)

                if (response.code != 429 && response.code != 503) return response
                if (attempt >= maxRetries) return response

                val retryAfterMs = response.header("Retry-After")
                    ?.toLongOrNull()
                    ?.coerceIn(0L, 30L)
                    ?.times(1000L)

                Thread.sleep(retryAfterMs ?: delayMs)
                delayMs = (delayMs * 2).coerceAtMost(4_000L)
                attempt++
            } catch (e: IOException) {
                if (attempt >= maxRetries) throw e
                Thread.sleep(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(4_000L)
                attempt++
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Request retry was interrupted", e)
            }
        }

        return response ?: chain.proceed(request)
    }
}
