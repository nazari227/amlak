package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.PersianUtils
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AshianMelkTopBar(
    title: String,
    subtitle: String? = null,
    isOnline: Boolean = true,
    unreadNotificationsCount: Int = 0,
    onNotificationClick: () -> Unit = {},
    navigationIcon: @Composable (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Online/Offline pill
                            OnlineStatusBadge(isOnline = isOnline)
                        }
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = NeutralMedium
                            )
                        }
                    }
                },
                navigationIcon = {
                    navigationIcon?.invoke()
                },
                actions = {
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier.testTag("top_bar_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge(
                                        containerColor = StatusError,
                                        contentColor = Color.White
                                    ) {
                                        Text(PersianUtils.toPersianDigits(unreadNotificationsCount))
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (unreadNotificationsCount > 0) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = "اعلان‌ها",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            if (!isOnline) {
                OfflineNoticeBar()
            }
        }
    }
}

@Composable
fun OnlineStatusBadge(isOnline: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isOnline) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
        modifier = Modifier.height(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) StatusSuccess else StatusError)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isOnline) "آنلاین" else "آفلاین",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (isOnline) Color(0xFF166534) else Color(0xFF991B1B)
            )
        }
    }
}

@Composable
fun OfflineNoticeBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFEF3C7))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = Color(0xFF92400E),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "حالت آفلاین: اطلاعات از حافظه امن محلی نمایش داده می‌شود.",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF92400E)
        )
    }
}

@Composable
fun StatusBadge(status: String) {
    val (label, bg, fg) = when (status) {
        "active" -> Triple("فعال", Color(0xFFDCFCE7), Color(0xFF166534))
        "pending" -> Triple("در انتظار تایید", Color(0xFFFEF3C7), Color(0xFF92400E))
        "reserved" -> Triple("رزرو شده", Color(0xFFE0E7FF), Color(0xFF3730A3))
        "sold" -> Triple("واگذار شده", Color(0xFFF1F5F9), Color(0xFF475569))
        "new" -> Triple("جدید", Color(0xFFDCFCE7), Color(0xFF166534))
        "in_progress" -> Triple("در حال پیگیری", Color(0xFFE0F2FE), Color(0xFF0369A1))
        "matched" -> Triple("تطبیق موفق", Color(0xFFF3E8FF), Color(0xFF6B21A8))
        else -> Triple(status, Color(0xFFF1F5F9), NeutralMedium)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        modifier = Modifier.height(24.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = fg
            )
        }
    }
}

@Composable
fun PersianEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = NeutralMedium,
            textAlign = TextAlign.Center
        )
        if (actionButtonText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(actionButtonText)
            }
        }
    }
}
