package com.example.network

import com.example.BuildConfig
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
            .header("X-App-Version", BuildConfig.VERSION_NAME)
            .header("Accept", "application/json")
            .header("Accept-Language", "fa-IR,fa;q=0.9,en;q=0.8")

        val publicAuthEndpoint =
            originalRequest.url.encodedPath.endsWith("/auth/login") ||
            originalRequest.url.encodedPath.endsWith("/auth/refresh")

        // Never attach a stale bearer token to login/refresh. Those endpoints use
        // credentials or the refresh token in the request body.
        val accessToken = tokenStorage.getAccessToken()
        if (!publicAuthEndpoint &&
            !accessToken.isNullOrBlank() &&
            originalRequest.header("Authorization") == null
        ) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }

        return chain.proceed(requestBuilder.build())
    }
}
