package com.example.core.network

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(
        val message: String,
        val statusCode: Int? = null,
        val cause: Throwable? = null,
        val isRateLimited: Boolean = false,
        val isUnauthorized: Boolean = false
    ) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data
}
