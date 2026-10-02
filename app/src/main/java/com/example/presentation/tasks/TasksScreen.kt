package com.example.presentation.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.PersianUtils
import com.example.domain.model.TaskItem
import com.example.presentation.components.AshianMelkTopBar
import com.example.presentation.components.PersianEmptyState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    var reportText by remember { mutableStateOf("") }
    var hoursText by remember { mutableStateOf("1.5") }

    Scaffold(
        topBar = {
            AshianMelkTopBar(
                title = "وظایف و پیگیری‌ها",
                subtitle = "${PersianUtils.toPersianDigits(state.tasks.count { !it.isCompleted })} وظیفه در انتظار انجام"
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openWorkReportDialog(null) },
                modifier = Modifier.testTag("fab_add_work_report"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.PostAdd, contentDescription = null) },
                text = { Text("ثبت گزارش کار روزانه", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            PrimaryTabRow(
                selectedTabIndex = when (state.activeTab) {
                    "today" -> 0
                    "overdue" -> 1
                    else -> 2
                },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = state.activeTab == "today",
                    onClick = { viewModel.selectTab("today") },
                    text = { Text("وظایف امروز") }
                )
                Tab(
                    selected = state.activeTab == "overdue",
                    onClick = { viewModel.selectTab("overdue") },
                    text = { Text("معوقه / تاخیری") }
                )
                Tab(
                    selected = state.activeTab == "upcoming",
                    onClick = { viewModel.selectTab("upcoming") },
                    text = { Text("آینده / هفتگی") }
                )
            }

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.loadTasks() },
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                val filteredTasks = when (state.activeTab) {
                    "today" -> state.tasks.filter { !it.isOverdue }
                    "overdue" -> state.tasks.filter { it.isOverdue }
                    else -> state.tasks
                }

                if (filteredTasks.isEmpty() && !state.isLoading) {
                    PersianEmptyState(
                        icon = Icons.Outlined.CheckCircle,
                        title = "همه وظایف انجام شده است!",
                        description = "وظیفه معوقه‌ای در این بخش باقی نمانده است. می‌توانید گزارش روزانه خود را ثبت کنید."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredTasks, key = { it.id }) { task ->
                            EnterpriseTaskCard(
                                task = task,
                                onComplete = { viewModel.completeTask(task.id) },
                                onReport = { viewModel.openWorkReportDialog(task) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
        }

        // Work Report Dialog
        if (state.isReportDialogOpen) {
            AlertDialog(
                onDismissRequest = { viewModel.closeWorkReportDialog() },
                title = { Text("ثبت گزارش عملکرد کاری") },
                text = {
                    Column {
                        if (state.selectedTaskForReport != null) {
                            Text(
                                text = "مربوط به: ${state.selectedTaskForReport!!.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        OutlinedTextField(
                            value = hoursText,
                            onValueChange = { hoursText = it },
                            label = { Text("ساعات صرف شده (ساعت)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = reportText,
                            onValueChange = { reportText = it },
                            label = { Text("شرح اقدامات و نتیجه پیگیری") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.submitWorkReport(reportText, hoursText.toDoubleOrNull() ?: 1.0)
                            reportText = ""
                        }
                    ) {
                        Text("ثبت و ارسال گزارش")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeWorkReportDialog() }) {
                        Text("انصراف")
                    }
                }
            )
        }
    }
}

@Composable
fun EnterpriseTaskCard(
    task: TaskItem,
    onComplete: () -> Unit,
    onReport: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) NeutralLight else MaterialTheme.colorScheme.onSurface
                    )
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = NeutralMedium,
                        maxLines = 2
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (categoryLabel, categoryIcon) = when (task.category) {
                        "visit" -> "بازدید ملک" to Icons.Filled.DirectionsWalk
                        "call" -> "تماس تلفنی" to Icons.Filled.Phone
                        "contract" -> "قرارداد" to Icons.Filled.Gavel
                        else -> "کارشناسی" to Icons.Filled.Search
                    }
                    Icon(categoryIcon, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeutralMedium)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$categoryLabel • ${task.dueDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (task.isOverdue) StatusError else NeutralMedium
                    )
                }
            }

            IconButton(onClick = onReport) {
                Icon(
                    imageVector = Icons.Outlined.RateReview,
                    contentDescription = "ثبت گزارش این وظیفه",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
