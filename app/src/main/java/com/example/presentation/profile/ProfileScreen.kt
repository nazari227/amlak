package com.example.presentation.profile

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.PersianUtils
import com.example.domain.model.DeviceSession
import com.example.presentation.components.AshianMelkTopBar
import com.example.security.SecurityUtils
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogoutDone: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showLogoutAllDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) {
            onLogoutDone()
        }
    }

    // Apply screenshot protection to current window if enabled
    LaunchedEffect(state.isScreenshotProtected) {
        val window = (context as? Activity)?.window
        SecurityUtils.setWindowSecure(window, state.isScreenshotProtected)
    }

    Scaffold(
        topBar = {
            AshianMelkTopBar(
                title = "پروفایل و تنظیمات امنیتی",
                subtitle = state.userProfile?.branchName ?: "شعبه مرکزی"
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = state.userProfile?.fullName ?: "مشاور املاک",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "${state.userProfile?.branchName} • مشاور رسمی",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeutralMedium
                        )

                        if (!state.userProfile?.phone.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.userProfile!!.phone,
                                style = MaterialTheme.typography.labelMedium,
                                color = RealEstateBlue
                            )
                        }
                    }
                }
            }

            // Security & Privacy Settings
            item {
                Text(
                    text = "امنیت و حریم خصوصی",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "محافظت از صفحه و جلوگیری از اسکرین‌شات",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "جلوگیری از ضبط صفحه و تصویربرداری از اطلاعات محرمانه مشتریان (FLAG_SECURE)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeutralMedium
                                )
                            }
                            Switch(
                                checked = state.isScreenshotProtected,
                                onCheckedChange = { viewModel.toggleScreenshotProtection(it) },
                                modifier = Modifier.testTag("switch_screenshot_protection")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = NeutralBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "رمزنگاری محلی سخت‌افزاری Android Keystore فعال است.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeutralMedium
                            )
                        }
                    }
                }
            }

            // Device Sessions List
            item {
                Text(
                    text = "نشست‌ها و دستگاه‌های متصل",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(state.sessions) { session ->
                DeviceSessionCard(session = session)
            }

            // Logout Actions
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.logoutCurrentDevice() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("button_logout_device"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("خروج از این دستگاه", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showLogoutAllDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("button_logout_all_devices"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)
                ) {
                    Icon(Icons.Filled.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("خروج از تمام دستگاه‌ها و ابطال نشست‌ها", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Confirmation Dialog for Logout All Devices
        if (showLogoutAllDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutAllDialog = false },
                title = { Text("خروج از تمام دستگاه‌ها") },
                text = {
                    Text("آیا مطمئن هستید؟ با این کار تمام نشست‌های فعال شما در سایر دستگاه‌ها ابطال خواهد شد.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.logoutAllDevices()
                            showLogoutAllDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                    ) {
                        Text("خروج از همه")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutAllDialog = false }) {
                        Text("انصراف")
                    }
                }
            )
        }
    }
}

@Composable
fun DeviceSessionCard(session: DeviceSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (session.isCurrentDevice) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (session.isCurrentDevice) Icons.Filled.Smartphone else Icons.Filled.Devices,
                        contentDescription = null,
                        tint = if (session.isCurrentDevice) StatusSuccess else NeutralMedium,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.deviceName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (session.isCurrentDevice) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = "این دستگاه",
                                color = Color(0xFF166534),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "آخرین فعالیت: ${session.lastActive} • شناسه: ${session.sessionId.take(8)}...",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeutralMedium
                )
            }
        }
    }
}
