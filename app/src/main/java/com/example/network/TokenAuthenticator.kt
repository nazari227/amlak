package com.example.network

import com.example.data.model.BaseApiResponse
import com.example.data.model.RefreshTokenRequest
import com.example.data.model.TokenResponse
import com.example.security.EncryptedTokenStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class TokenAuthenticator(
    private val tokenStorage: EncryptedTokenStorage,
    private val onSessionRevoked: () -> Unit
) : Authenticator {

    private val lock = Any()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        synchronized(lock) {
            val currentToken = tokenStorage.getAccessToken()
            val requestToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
                ?.trim()

            if (!currentToken.isNullOrBlank() && currentToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = tokenStorage.getRefreshToken() ?: return revoke()
            val newTokens = performRefreshTokenCall(refreshToken) ?: return revoke()

            tokenStorage.saveTokens(newTokens.accessToken, newTokens.refreshToken)
            return response.request.newBuilder()
                .header("Authorization", "Bearer ${newTokens.accessToken}")
                .build()
        }
    }

    private fun performRefreshTokenCall(refreshToken: String): TokenResponse? {
        return try {
            val adapter = moshi.adapter(RefreshTokenRequest::class.java)
            val body = adapter.toJson(
                RefreshTokenRequest(refreshToken, tokenStorage.getDeviceId())
            )
            val request = Request.Builder()
                .url("${AshianMelkApiService.BASE_URL}auth/refresh")
                .header("X-Device-Id", tokenStorage.getDeviceId())
                .header("Accept", "application/json")
                .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
                .newCall(request)
                .execute()
                .use { refreshResponse ->
                    if (!refreshResponse.isSuccessful) return null
                    val json = refreshResponse.body?.string() ?: return null
                    val type = Types.newParameterizedType(BaseApiResponse::class.java, TokenResponse::class.java)
                    moshi.adapter<BaseApiResponse<TokenResponse>>(type).fromJson(json)?.data
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun revoke(): Request? {
        tokenStorage.clearAuth()
        onSessionRevoked()
        return null
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
