package com.example.data.repository

import android.os.Build
import com.example.core.network.NetworkResult
import com.example.diagnostics.CrashDiagnostics
import com.example.data.model.LoginRequest
import com.example.data.model.UserDto
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
    private val tokenStorage: EncryptedTokenStorage,
    private val crashDiagnostics: CrashDiagnostics
) : AuthRepository {

    override suspend fun login(login: String, password: String): NetworkResult<LoginResult> =
        authenticate(login, password, "")

    override suspend fun verifyMfa(login: String, password: String, code: String): NetworkResult<UserProfile> {
        return when (val result = authenticate(login, password, code)) {
            is NetworkResult.Success -> when (val value = result.data) {
                is LoginResult.Success -> NetworkResult.Success(value.profile)
                LoginResult.MfaRequired -> NetworkResult.Error("کد احراز هویت دومرحله‌ای لازم است.", 401, isUnauthorized = true)
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    private suspend fun authenticate(login: String, password: String, mfaCode: String): NetworkResult<LoginResult> =
        withContext(Dispatchers.IO) {
            crashDiagnostics.markAuthStage(if (mfaCode.isBlank()) "sending_password_login" else "sending_mfa_login")
            try {
                val response = apiService.login(
                    LoginRequest(
                        login = login.trim(),
                        password = password,
                        mfaCode = mfaCode.trim(),
                        deviceId = tokenStorage.getDeviceId(),
                        deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".take(120),
                        platform = "android"
                    )
                )

                crashDiagnostics.markAuthStage("login_http_${response.code()}")
                if (response.isSuccessful) {
                    val tokens = response.body()?.data
                        ?: return@withContext NetworkResult.Error("پاسخ ورود از سرور ناقص است.", response.code())
                    crashDiagnostics.markAuthStage("saving_tokens")
                    tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken)
                    crashDiagnostics.markAuthStage("fetching_profile")
                    when (val profile = fetchAndStoreProfile()) {
                        is NetworkResult.Success -> {
                            crashDiagnostics.finishAuthStage("login_success_profile_loaded")
                            NetworkResult.Success(LoginResult.Success(profile.data))
                        }
                        is NetworkResult.Error -> {
                            tokenStorage.clearAuth()
                            profile
                        }
                        is NetworkResult.Loading -> NetworkResult.Loading
                    }
                } else {
                    val raw = response.errorBody()?.string().orEmpty()
                    val mfaRequired = response.code() == 401 &&
                        raw.contains("api_mfa_required")
                    val mfaInvalid = response.code() == 401 &&
                        raw.contains("api_mfa_invalid")

                    if (mfaRequired) {
                        crashDiagnostics.finishAuthStage("mfa_required_ui")
                        NetworkResult.Success(LoginResult.MfaRequired)
                    } else if (mfaInvalid) {
                        crashDiagnostics.finishAuthStage("mfa_invalid")
                        NetworkResult.Error(
                            "کد احراز هویت دومرحله‌ای معتبر نیست.",
                            401,
                            isUnauthorized = false
                        )
                    } else {
                        val message = when (response.code()) {
                            400 -> "اطلاعات ورود یا شناسه دستگاه معتبر نیست."
                            401 -> if (mfaCode.isNotBlank()) "کد احراز هویت یا اطلاعات ورود معتبر نیست." else "نام کاربری یا رمز عبور صحیح نیست."
                            403 -> "دسترسی این حساب به سامانه داخلی فعال نیست."
                            429 -> "تعداد تلاش‌های ورود بیش از حد مجاز است. کمی بعد دوباره تلاش کنید."
                            else -> "ورود به سامانه انجام نشد (${response.code()})."
                        }
                        crashDiagnostics.finishAuthStage("login_error_${response.code()}")
                        NetworkResult.Error(message, response.code(), isUnauthorized = response.code() == 401)
                    }
                }
            } catch (e: Exception) {
                crashDiagnostics.finishAuthStage("login_exception_${e.javaClass.simpleName}")
                NetworkResult.Error("ارتباط امن با سرور برقرار نشد.", cause = e)
            }
        }

    private suspend fun fetchAndStoreProfile(): NetworkResult<UserProfile> {
        return try {
            val response = apiService.getUserProfile()
            val user = response.body()?.data?.user
            if (response.isSuccessful && user != null) {
                val profile = user.toDomain()
                tokenStorage.saveUserProfile(
                    userId = profile.id,
                    fullName = profile.fullName,
                    branchId = profile.branchId,
                    role = profile.role,
                    capabilities = profile.capabilities
                )
                NetworkResult.Success(profile)
            } else {
                NetworkResult.Error("اطلاعات حساب کاربری از سرور دریافت نشد.", response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("دریافت پروفایل کاربر انجام نشد.", cause = e)
        }
    }

    override suspend fun logoutDevice(): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.logoutDevice()
        } catch (_: Exception) {
        } finally {
            tokenStorage.clearAuth()
        }
        NetworkResult.Success(Unit)
    }

    override suspend fun logoutAllDevices(): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.logoutAllDevices()
        } catch (_: Exception) {
        } finally {
            tokenStorage.clearAuth()
        }
        NetworkResult.Success(Unit)
    }

    override suspend fun getActiveSessions(): NetworkResult<List<DeviceSession>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getActiveSessions()
                val sessions = response.body()?.data?.sessions
                if (response.isSuccessful && sessions != null) {
                    NetworkResult.Success(
                        sessions.map {
                            DeviceSession(
                                sessionId = it.sessionId,
                                deviceName = it.deviceName.ifBlank { it.platform },
                                lastActive = it.lastUsedAt.ifBlank { it.issuedAt },
                                ipAddress = "",
                                isCurrentDevice = false
                            )
                        }
                    )
                } else {
                    NetworkResult.Error("نشست‌های فعال دریافت نشد.", response.code())
                }
            } catch (e: Exception) {
                NetworkResult.Error("ارتباط با سرور برای دریافت نشست‌ها برقرار نشد.", cause = e)
            }
        }

    override fun isLoggedIn(): Boolean = tokenStorage.isLoggedIn()

    override fun getCurrentUser(): UserProfile = UserProfile(
        id = tokenStorage.getUserId(),
        username = "",
        fullName = tokenStorage.getUserName(),
        phone = "",
        email = "",
        branchId = tokenStorage.getBranchId(),
        branchName = "",
        role = tokenStorage.getUserRole(),
        capabilities = tokenStorage.getUserCapabilities()
    )

    private fun UserDto.toDomain(): UserProfile = UserProfile(
        id = id,
        username = "",
        fullName = displayName,
        phone = "",
        email = "",
        branchId = branchId,
        branchName = "",
        role = staffType,
        avatarUrl = null,
        capabilities = capabilities
    )
}
