package com.example.presentation.demands

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.PersianUtils
import com.example.domain.model.Demand
import com.example.presentation.components.AshianMelkTopBar
import com.example.presentation.components.PersianEmptyState
import com.example.presentation.components.StatusBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemandsScreen(
    viewModel: DemandsViewModel,
    onDemandClick: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            AshianMelkTopBar(
                title = "متقاضیان ملک",
                subtitle = "${PersianUtils.toPersianDigits(state.demands.size)} متقاضی ارجاع شده"
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status Tabs
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedStatus == null,
                        onClick = { viewModel.loadDemands(null) },
                        label = { Text("همه") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedStatus == "new",
                        onClick = { viewModel.loadDemands("new") },
                        label = { Text("جدید") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedStatus == "in_progress",
                        onClick = { viewModel.loadDemands("in_progress") },
                        label = { Text("در حال پیگیری") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedStatus == "matched",
                        onClick = { viewModel.loadDemands("matched") },
                        label = { Text("تطبیق موفق") }
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.loadDemands() },
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.demands.isEmpty() && !state.isLoading) {
                    PersianEmptyState(
                        icon = Icons.Outlined.People,
                        title = "متقاضی‌ای ثبت نشده است",
                        description = "در حال حاضر تقاضای جدیدی با این فیلتر به شما اختصاص داده نشده است."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.demands, key = { it.id }) { demand ->
                            DemandListItemCard(
                                demand = demand,
                                onClick = { onDemandClick(demand.id) }
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
fun DemandListItemCard(demand: Demand, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("demand_card_${demand.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = demand.clientName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = demand.clientPhone,
                            style = MaterialTheme.typography.bodySmall,
                            color = NeutralMedium
                        )
                    }
                }

                StatusBadge(status = demand.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = NeutralBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Budget & Area specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("محدوده بودجه:", style = MaterialTheme.typography.labelSmall, color = NeutralMedium)
                    Text(
                        text = "تا ${PersianUtils.formatPrice(demand.maxBudget)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = RealEstateBlue)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("حداقل متراژ:", style = MaterialTheme.typography.labelSmall, color = NeutralMedium)
                    Text(
                        text = PersianUtils.formatArea(demand.minArea),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Preferred Neighborhoods
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Place, contentDescription = null, tint = NeutralMedium, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "مناطق ترجیحی: ${demand.preferredNeighborhoods.joinToString("، ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralMedium,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Matching Properties highlight
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF0FDF4),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${PersianUtils.toPersianDigits(demand.matchingPropertiesCount)} فایل پیشنهادی در سیستم",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = StatusSuccess
                        )
                    }
                    Text(
                        text = "مشاهده تطابق >",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = StatusSuccess
                    )
                }
            }
        }
    }
}
