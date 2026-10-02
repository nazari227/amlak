package com.example.network

import com.example.security.EncryptedTokenStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    fun createService(
        tokenStorage: EncryptedTokenStorage,
        onSessionRevoked: () -> Unit
    ): AshianMelkApiService {

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenStorage))
            .addInterceptor(RateLimitRetryInterceptor())
            .addInterceptor(SafeLoggingInterceptor())
            .authenticator(TokenAuthenticator(tokenStorage, onSessionRevoked))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(AshianMelkApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(AshianMelkApiService::class.java)
    }
}
