package com.example.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.PersianUtils
import com.example.domain.model.AppNotification
import com.example.presentation.components.AshianMelkTopBar
import com.example.presentation.components.PersianEmptyState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onDeepLink: (type: String, targetId: Long?) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            AshianMelkTopBar(
                title = "صندوق اعلان‌ها",
                subtitle = "${PersianUtils.toPersianDigits(state.unreadCount)} اعلان خوانده‌نشده"
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Action header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "پیام‌ها و هشدارهای سیستمی",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = NeutralMedium
                )
                if (state.unreadCount > 0) {
                    TextButton(onClick = { viewModel.markAllAsRead() }) {
                        Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("خوانده شدن همه", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.loadNotifications() },
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.notifications.isEmpty() && !state.isLoading) {
                    PersianEmptyState(
                        icon = Icons.Outlined.NotificationsNone,
                        title = "اعلانی وجود ندارد",
                        description = "صندوق اعلان‌های شما خالی است."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.notifications, key = { it.id }) { item ->
                            NotificationCard(
                                notification = item,
                                onClick = {
                                    viewModel.markAsRead(item.id)
                                    onDeepLink(item.type, item.targetId)
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("notification_item_${notification.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead) Color(0xFFF0F7FF) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            val (icon, tint) = when (notification.type) {
                "property" -> Icons.Filled.Apartment to RealEstateBlue
                "demand" -> Icons.Filled.PersonSearch to Color(0xFFD97706)
                "task" -> Icons.Filled.Assignment to Color(0xFF0D9488)
                "appointment" -> Icons.Filled.Event to Color(0xFF7C3AED)
                else -> Icons.Filled.Info to NeutralMedium
            }

            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
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
                        text = notification.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralMedium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = notification.createdAt,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = NeutralLight
                )
            }
        }
    }
}
