package com.example.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.PersianUtils
import com.example.domain.model.Appointment
import com.example.domain.model.Demand
import com.example.domain.model.TaskItem
import com.example.presentation.components.AshianMelkTopBar
import com.example.presentation.components.StatusBadge
import com.example.security.AppAccess
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    access: AppAccess,
    onNavigateToCreateProperty: () -> Unit,
    onNavigateToProperties: (String?) -> Unit,
    onNavigateToDemands: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPropertyDetail: (Long) -> Unit,
    onNavigateToDemandDetail: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            AshianMelkTopBar(
                title = "سامانه املاک آشیان",
                subtitle = state.userProfile?.branchName ?: "شعبه مرکزی",
                isOnline = state.isOnline,
                unreadNotificationsCount = state.summary?.unreadNotificationsCount ?: 0,
                onNotificationClick = onNavigateToNotifications
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.loadDashboard() },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Welcome Greeting Banner
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    ConsultantWelcomeCard(
                        consultantName = state.userProfile?.fullName ?: "همکار گرامی",
                        branchName = (state.userProfile?.branchName ?: "شعبه مرکزی")
                            .ifBlank { access.roleLabel }
                    )
                }

                // 2. Quick Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (access.canCreateProperty) {
                            Button(
                                onClick = onNavigateToCreateProperty,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("quick_add_property_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Filled.AddHome, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ثبت ملک جدید", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (access.showProperties) {
                            OutlinedButton(
                                onClick = { onNavigateToProperties(null) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("quick_search_properties_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جستجوی فایل‌ها", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // 3. Metric KPI Cards Grid
                item {
                    Text(
                        text = "خلاصه فعالیت‌های امروز",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DashboardMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "وظایف امروز",
                            count = state.summary?.todayTasksCount ?: 0,
                            icon = Icons.Filled.Checklist,
                            tint = Color(0xFF0F52BA),
                            bg = Color(0xFFEFF6FF),
                            onClick = onNavigateToTasks
                        )
                        DashboardMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "قرارهای بازدید",
                            count = state.summary?.todayAppointmentsCount ?: 0,
                            icon = Icons.Filled.Event,
                            tint = Color(0xFF0D9488),
                            bg = Color(0xFFF0FDFA),
                            onClick = onNavigateToTasks
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DashboardMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "متقاضیان جدید",
                            count = state.summary?.newDemandsCount ?: 0,
                            icon = Icons.Filled.GroupAdd,
                            tint = Color(0xFFD97706),
                            bg = Color(0xFFFFFBEB),
                            onClick = onNavigateToDemands
                        )
                        DashboardMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "املاک فعال شعبه",
                            count = state.summary?.activePropertiesCount ?: 0,
                            icon = Icons.Filled.Apartment,
                            tint = Color(0xFF7C3AED),
                            bg = Color(0xFFF5F3FF),
                            onClick = { onNavigateToProperties(null) }
                        )
                    }
                }

                // 4. Today's Appointments Section
                val appointments = state.summary?.todayAppointments ?: emptyList()
                if (appointments.isNotEmpty()) {
                    item {
                        SectionHeader(title = "قرارهای بازدید امروز", onMoreClick = onNavigateToTasks)
                    }
                    items(appointments) { appointment ->
                        AppointmentItemCard(
                            appointment = appointment,
                            onClick = { /* Open appointment */ }
                        )
                    }
                }

                // 5. Today's Tasks & Follow-ups
                val tasks = state.summary?.todayTasks ?: emptyList()
                if (tasks.isNotEmpty()) {
                    item {
                        SectionHeader(title = "وظایف و پیگیری‌های امروز", onMoreClick = onNavigateToTasks)
                    }
                    items(tasks) { task ->
                        TodayTaskItemCard(
                            task = task,
                            onComplete = { /* Complete action */ },
                            onItemClick = onNavigateToTasks
                        )
                    }
                }

                // 6. New Assigned Demands
                val demands = state.summary?.recentDemands ?: emptyList()
                if (demands.isNotEmpty()) {
                    item {
                        SectionHeader(title = "متقاضیان جدید ارجاع شده", onMoreClick = onNavigateToDemands)
                    }
                    items(demands) { demand ->
                        RecentDemandCard(
                            demand = demand,
                            onClick = { onNavigateToDemandDetail(demand.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun ConsultantWelcomeCard(
    consultantName: String,
    branchName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "سلام، $consultantName",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$branchName • روز کاری پرباری داشته باشید",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralMedium
                )
            }
        }
    }
}

@Composable
fun DashboardMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("metric_${title}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = bg,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                    }
                }
                Text(
                    text = PersianUtils.toPersianDigits(count),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = NeutralMedium
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, onMoreClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        TextButton(onClick = onMoreClick) {
            Text(
                text = "مشاهده همه",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AppointmentItemCard(appointment: Appointment, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF0FDFA),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color(0xFF0D9488), modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appointment.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${appointment.clientName} • ${appointment.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralMedium
                )
                Text(
                    text = appointment.location,
                    style = MaterialTheme.typography.labelSmall,
                    color = RealEstateBlue
                )
            }
        }
    }
}

@Composable
fun TodayTaskItemCard(task: TaskItem, onComplete: () -> Unit, onItemClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onComplete() },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.isCompleted) NeutralLight else MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = task.dueDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (task.isOverdue) StatusError else NeutralMedium
                )
            }
        }
    }
}

@Composable
fun RecentDemandCard(demand: Demand, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = demand.clientName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                StatusBadge(status = demand.status)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "بودجه: تا ${PersianUtils.formatPrice(demand.maxBudget)} • مناطق: ${demand.preferredNeighborhoods.joinToString("، ")}",
                style = MaterialTheme.typography.bodySmall,
                color = NeutralMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircleOutline, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${PersianUtils.toPersianDigits(demand.matchingPropertiesCount)} فایل پیشنهادی منطبق در سامانه",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = StatusSuccess
                )
            }
        }
    }
}
