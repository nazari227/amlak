package com.example.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class RateLimitRetryInterceptor(
    private val maxRetries: Int = 3
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var attempt = 0
        var delayMs = 1000L

        while (attempt < maxRetries) {
            try {
                response?.close()
                response = chain.proceed(request)

                if (response.code != 429 && response.code != 503) {
                    return response
                }

                // If rate limited, check Retry-After header
                val retryAfterHeader = response.header("Retry-After")
                val sleepDuration = retryAfterHeader?.toLongOrNull()?.let { it * 1000 } ?: delayMs

                Thread.sleep(sleepDuration)
                delayMs *= 2
                attempt++
            } catch (e: IOException) {
                // If network drop occurs on retryable GET requests
                if (request.method == "GET" && attempt < maxRetries - 1) {
                    Thread.sleep(delayMs)
                    delayMs *= 2
                    attempt++
                } else {
                    throw e
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Request retry was interrupted", e)
            }
        }

        return response ?: chain.proceed(request)
    }
}
