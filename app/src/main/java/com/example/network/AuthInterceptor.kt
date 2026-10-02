package com.example.network

import com.example.security.EncryptedTokenStorage
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenStorage: EncryptedTokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
            .header("X-Device-Id", tokenStorage.getDeviceId())
            .header("X-App-Version", "1.0")
            .header("Accept", "application/json")
            .header("Accept-Language", "fa-IR,fa;q=0.9,en;q=0.8")

        // Attach Authorization header if access token exists and not already provided
        val accessToken = tokenStorage.getAccessToken()
        if (!accessToken.isNullOrBlank() && originalRequest.header("Authorization") == null) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }

        return chain.proceed(requestBuilder.build())
    }
}
