package com.example.data.repository

import android.os.Build
import com.example.core.network.NetworkResult
import com.example.data.model.*
import com.example.domain.model.DeviceSession
import com.example.domain.model.UserProfile
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.LoginResult
import com.example.network.AshianMelkApiService
import com.example.security.EncryptedTokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val apiService: AshianMelkApiService,
    private val tokenStorage: EncryptedTokenStorage
) : AuthRepository {

    override suspend fun login(username: String, password: String): NetworkResult<LoginResult> =
        withContext(Dispatchers.IO) {
            try {
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                val response = apiService.login(
                    LoginRequest(
                        username = username.trim(),
                        password = password,
                        deviceId = tokenStorage.getDeviceId(),
                        deviceName = deviceName
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val data = body.data
                    if (data != null) {
                        if (data.status == "mfa_required" && !data.mfaToken.isNullOrEmpty()) {
                            return@withContext NetworkResult.Success(LoginResult.MfaRequired(data.mfaToken))
                        }

                        if (!data.accessToken.isNullOrEmpty() && !data.refreshToken.isNullOrEmpty()) {
                            tokenStorage.saveTokens(data.accessToken, data.refreshToken)
                            val userDto = data.user
                            val profile = if (userDto != null) {
                                tokenStorage.saveUserProfile(
                                    userId = userDto.id,
                                    fullName = userDto.fullName,
                                    phone = userDto.phone,
                                    branchId = userDto.branchId,
                                    branchName = userDto.branchName,
                                    role = userDto.role
                                )
                                userDto.toDomain()
                            } else {
                                getCurrentUser()
                            }
                            return@withContext NetworkResult.Success(LoginResult.Success(profile))
                        }
                    }
                    NetworkResult.Error(body.message ?: "خطا در احراز هویت", response.code())
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "نام کاربری یا رمز عبور اشتباه است."
                        403 -> "دسترسی حساب کاربری شما محدود شده است."
                        429 -> "تعداد درخواست‌های ورود بیش از حد مجاز است. لطفاً چند دقیقه دیگر تلاش کنید."
                        else -> "خطا در برقراری ارتباط با سرور (${response.code()})"
                    }
                    NetworkResult.Error(errorMsg, response.code(), isUnauthorized = response.code() == 401)
                }
            } catch (e: Exception) {
                NetworkResult.Error("خطای شبکه: لطفاً اتصال اینترنت خود را بررسی کنید.", cause = e)
            }
        }

    override suspend fun verifyMfa(mfaToken: String, code: String): NetworkResult<UserProfile> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.verifyMfa(
                    MfaVerifyRequest(
                        mfaToken = mfaToken,
                        code = code.trim(),
                        deviceId = tokenStorage.getDeviceId()
                    )
                )

                if (response.isSuccessful && response.body()?.data != null) {
                    val data = response.body()!!.data!!
                    if (!data.accessToken.isNullOrEmpty() && !data.refreshToken.isNullOrEmpty()) {
                        tokenStorage.saveTokens(data.accessToken, data.refreshToken)
                        val userDto = data.user
                        if (userDto != null) {
                            tokenStorage.saveUserProfile(
                                userId = userDto.id,
                                fullName = userDto.fullName,
                                phone = userDto.phone,
                                branchId = userDto.branchId,
                                branchName = userDto.branchName,
                                role = userDto.role
                            )
                            return@withContext NetworkResult.Success(userDto.toDomain())
                        }
                    }
                    NetworkResult.Success(getCurrentUser())
                } else {
                    NetworkResult.Error("کد تایید دو مرحله‌ای نادرست یا منقضی شده است.", response.code())
                }
            } catch (e: Exception) {
                NetworkResult.Error("خطا در تایید کد دو مرحله‌ای", cause = e)
            }
        }

    override suspend fun logoutDevice(): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.logoutDevice(LogoutDeviceRequest(deviceId = tokenStorage.getDeviceId()))
        } catch (_: Exception) {
            // Ignore network drop during logout
        } finally {
            tokenStorage.clearAuth()
        }
        NetworkResult.Success(Unit)
    }

    override suspend fun logoutAllDevices(): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.logoutAllDevices()
        } catch (_: Exception) {
            // Ignore network drop
        } finally {
            tokenStorage.clearAuth()
        }
        NetworkResult.Success(Unit)
    }

    override suspend fun getActiveSessions(): NetworkResult<List<DeviceSession>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getActiveSessions()
                if (response.isSuccessful && response.body()?.data != null) {
                    val sessions = response.body()!!.data!!.map {
                        DeviceSession(
                            sessionId = it.sessionId,
                            deviceName = it.deviceName,
                            lastActive = it.lastActive,
                            ipAddress = it.ipAddress,
                            isCurrentDevice = it.isCurrent
                        )
                    }
                    NetworkResult.Success(sessions)
                } else {
                    // Fallback to current device
                    NetworkResult.Success(
                        listOf(
                            DeviceSession(
                                sessionId = tokenStorage.getDeviceId(),
                                deviceName = "${Build.MANUFACTURER} ${Build.MODEL} (این دستگاه)",
                                lastActive = "اکنون",
                                ipAddress = "127.0.0.1",
                                isCurrentDevice = true
                            )
                        )
                    )
                }
            } catch (e: Exception) {
                NetworkResult.Success(
                    listOf(
                        DeviceSession(
                            sessionId = tokenStorage.getDeviceId(),
                            deviceName = "${Build.MANUFACTURER} ${Build.MODEL} (این دستگاه)",
                            lastActive = "اکنون",
                            ipAddress = "127.0.0.1",
                            isCurrentDevice = true
                        )
                    )
                )
            }
        }

    override fun isLoggedIn(): Boolean = tokenStorage.isLoggedIn()

    override fun getCurrentUser(): UserProfile {
        return UserProfile(
            id = tokenStorage.getUserId(),
            username = "consultant",
            fullName = tokenStorage.getUserName(),
            phone = tokenStorage.getUserPhone(),
            email = "agent@ashianmelk.ir",
            branchId = tokenStorage.getBranchId(),
            branchName = tokenStorage.getBranchName(),
            role = tokenStorage.getUserRole()
        )
    }

    private fun UserDto.toDomain(): UserProfile {
        return UserProfile(
            id = this.id,
            username = this.username,
            fullName = this.fullName,
            phone = this.phone,
            email = this.email ?: "",
            branchId = this.branchId,
            branchName = this.branchName,
            role = this.role,
            avatarUrl = this.avatarUrl
        )
    }
}
