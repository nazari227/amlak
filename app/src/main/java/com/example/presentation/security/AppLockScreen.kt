package com.example.presentation.security

import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.security.BiometricLockManager

@Composable
fun AppLockScreen(
    lockManager: BiometricLockManager,
    setupMode: Boolean,
    onUnlocked: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var message by remember { mutableStateOf<String?>(null) }
    var prompting by remember { mutableStateOf(false) }

    fun authenticate() {
        if (activity == null || prompting) return
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val manager = BiometricManager.from(activity)
        when (manager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                prompting = true
                val prompt = BiometricPrompt(
                    activity,
                    ContextCompat.getMainExecutor(activity),
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            prompting = false
                            message = null
                            if (setupMode) lockManager.enableForCurrentUser() else lockManager.markUnlocked()
                            onUnlocked()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            prompting = false
                            message = errString.toString()
                        }

                        override fun onAuthenticationFailed() {
                            message = "اثر انگشت یا قفل دستگاه تأیید نشد."
                        }
                    }
                )
                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(if (setupMode) "فعال‌سازی قفل امن آشیان ملک" else "باز کردن آشیان ملک")
                    .setSubtitle(
                        if (setupMode)
                            "برای حفاظت از پرونده‌ها، با اثر انگشت یا قفل امن گوشی تأیید کنید."
                        else "برای دسترسی به اطلاعات پرونده‌ها هویت خود را تأیید کنید."
                    )
                    .setAllowedAuthenticators(authenticators)
                    .build()
                prompt.authenticate(info)
            }
            else -> {
                message = "برای این حساب باید ابتدا اثر انگشت، PIN یا قفل امن دستگاه را در تنظیمات گوشی فعال کنید."
            }
        }
    }

    LaunchedEffect(setupMode) {
        if (!setupMode) authenticate()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (setupMode) Icons.Filled.Security else Icons.Filled.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (setupMode) "قفل امن برای این حساب" else "آشیان ملک قفل است",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (setupMode)
                            "برای مشاوران و مسئولان، پس از اولین ورود قفل بیومتریک/قفل دستگاه فعال می‌شود. رمز سایت روی گوشی ذخیره نمی‌شود."
                        else "توکن ورود رمزگذاری‌شده است؛ برای نمایش دوباره پرونده‌ها باید هویت دستگاه تأیید شود.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (message != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(message!!, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { authenticate() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (setupMode) "فعال‌سازی با اثر انگشت / قفل گوشی" else "باز کردن")
                    }
                    if (setupMode) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تنظیم قفل امن گوشی")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onLogout) {
                        Text("خروج از حساب")
                    }
                }
            }
        }
    }
}
