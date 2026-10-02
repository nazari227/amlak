package com.example.network

import com.example.data.model.BaseApiResponse
import com.example.data.model.RefreshTokenRequest
import com.example.data.model.RefreshTokenResponse
import com.example.security.EncryptedTokenStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class TokenAuthenticator(
    private val tokenStorage: EncryptedTokenStorage,
    private val onSessionRevoked: () -> Unit
) : Authenticator {

    private val lock = Any()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite loops if authentication repeatedly fails
        if (responseCount(response) >= 3) {
            return null
        }

        synchronized(lock) {
            val currentToken = tokenStorage.getAccessToken()
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")?.trim()

            // If token was already refreshed by another thread while waiting for lock, retry with existing new token
            if (currentToken != null && currentToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = tokenStorage.getRefreshToken() ?: run {
                tokenStorage.clearAuth()
                onSessionRevoked()
                return null
            }

            // Perform synchronous refresh call
            val newTokens = performRefreshTokenCall(refreshToken)
            if (newTokens != null) {
                tokenStorage.saveTokens(newTokens.accessToken, newTokens.refreshToken)
                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${newTokens.accessToken}")
                    .build()
            } else {
                // Refresh failed or revoked
                tokenStorage.clearAuth()
                onSessionRevoked()
                return null
            }
        }
    }

    private fun performRefreshTokenCall(refreshToken: String): RefreshTokenResponse? {
        return try {
            val client = OkHttpClient.Builder().build()
            val jsonAdapter = moshi.adapter(RefreshTokenRequest::class.java)
            val requestBodyString = jsonAdapter.toJson(
                RefreshTokenRequest(
                    refreshToken = refreshToken,
                    deviceId = tokenStorage.getDeviceId()
                )
            )

            val request = Request.Builder()
                .url("${AshianMelkApiService.BASE_URL}auth/refresh-token")
                .header("X-Device-Id", tokenStorage.getDeviceId())
                .post(requestBodyString.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val callResponse = client.newCall(request).execute()
            if (!callResponse.isSuccessful) return null

            val responseBodyString = callResponse.body?.string() ?: return null
            val type = Types.newParameterizedType(BaseApiResponse::class.java, RefreshTokenResponse::class.java)
            val adapter = moshi.adapter<BaseApiResponse<RefreshTokenResponse>>(type)
            val result = adapter.fromJson(responseBodyString)
            result?.data
        } catch (e: Exception) {
            null
        }
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
