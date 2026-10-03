package com.example.presentation.properties

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.util.PersianUtils
import com.example.domain.model.Property
import com.example.domain.model.PropertyFilter
import com.example.presentation.components.AshianMelkTopBar
import com.example.presentation.components.PersianEmptyState
import com.example.presentation.components.StatusBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyListScreen(
    viewModel: PropertyListViewModel,
    canCreateProperty: Boolean,
    onPropertyClick: (Long) -> Unit,
    onCreatePropertyClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    // Detect when user scrolled to the bottom to trigger next page load
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 2 && !state.isLoadingMore && !state.hasReachedEnd
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadProperties(isRefresh = false)
        }
    }

    Scaffold(
        topBar = {
            AshianMelkTopBar(
                title = "فهرست املاک",
                subtitle = "${PersianUtils.toPersianDigits(state.properties.size)} فایل آماده",
                isOnline = state.isOnline
            )
        },
        floatingActionButton = {
            if (canCreateProperty) {
                ExtendedFloatingActionButton(
                    onClick = onCreatePropertyClick,
                    modifier = Modifier.testTag("fab_add_property"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("ثبت ملک جدید", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search & Filter Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("property_search_input"),
                    placeholder = { Text("جستجو با کد ملک، محله یا عنوان...") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "پاک کردن")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                val hasFilter = state.activeFilter.transactionType != null ||
                        state.activeFilter.propertyType != null ||
                        state.activeFilter.branchId != null

                FilledTonalIconButton(
                    onClick = { viewModel.toggleFilterSheet(true) },
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("property_filter_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (hasFilter) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "فیلترها",
                        tint = if (hasFilter) MaterialTheme.colorScheme.primary else NeutralMedium
                    )
                }
            }

            // Property List or Empty State
            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.loadProperties(isRefresh = true) },
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.properties.isEmpty() && !state.isLoading) {
                    PersianEmptyState(
                        icon = Icons.Outlined.HomeWork,
                        title = "ملکی یافت نشد",
                        description = "با تغییر فیلترها یا جستجوی عبارت دیگر مجدداً تلاش کنید.",
                        actionButtonText = "پاکسازی فیلترها",
                        onActionClick = { viewModel.resetFilter() }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(state.properties, key = { it.id }) { property ->
                            PropertyCard(
                                property = property,
                                onClick = { onPropertyClick(property.id) }
                            )
                        }

                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(72.dp)) // Space for FAB
                        }
                    }
                }
            }
        }

        // Filter Bottom Sheet
        if (state.isFilterSheetVisible) {
            PropertyFilterBottomSheet(
                currentFilter = state.activeFilter,
                onApply = { viewModel.applyFilter(it) },
                onReset = { viewModel.resetFilter() },
                onDismiss = { viewModel.toggleFilterSheet(false) }
            )
        }
    }
}

@Composable
fun PropertyCard(property: Property, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("property_card_${property.code}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!property.thumbnail.isNullOrEmpty()) {
                    AsyncImage(
                        model = property.thumbnail,
                        contentDescription = property.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Apartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Top badges overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(status = property.status)

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = property.code,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Info details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = property.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${property.city}، ${property.neighborhood} • ${property.branchName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Price display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (property.transactionType == "sale") {
                            Text(
                                text = "قیمت کل",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = PersianUtils.formatPrice(property.price),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "ودیعه: ${PersianUtils.formatPrice(property.mortgagePrice)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "اجاره ماهانه: ${PersianUtils.formatPrice(property.price)}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Key Specs tags
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SpecTag(
                            icon = Icons.Outlined.SquareFoot,
                            text = PersianUtils.formatArea(property.area)
                        )
                        SpecTag(
                            icon = Icons.Outlined.Bed,
                            text = "${PersianUtils.toPersianDigits(property.rooms)} خ"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpecTag(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.height(26.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyFilterBottomSheet(
    currentFilter: PropertyFilter,
    onApply: (PropertyFilter) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTxType by remember { mutableStateOf(currentFilter.transactionType) }
    var selectedPropType by remember { mutableStateOf(currentFilter.propertyType) }
    var neighborhoodInput by remember { mutableStateOf(currentFilter.neighborhood ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "فیلترهای جستجو",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onReset) {
                    Text("حذف فیلترها", color = StatusError)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transaction Type
            Text("نوع معامله", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedTxType == null,
                    onClick = { selectedTxType = null },
                    label = { Text("همه") }
                )
                FilterChip(
                    selected = selectedTxType == "sale",
                    onClick = { selectedTxType = "sale" },
                    label = { Text("خرید و فروش") }
                )
                FilterChip(
                    selected = selectedTxType == "rent",
                    onClick = { selectedTxType = "rent" },
                    label = { Text("رهن و اجاره") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Property Type
            Text("نوع کاربری ملک", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedPropType == null,
                    onClick = { selectedPropType = null },
                    label = { Text("همه") }
                )
                FilterChip(
                    selected = selectedPropType == "apartment",
                    onClick = { selectedPropType = "apartment" },
                    label = { Text("آپارتمان") }
                )
                FilterChip(
                    selected = selectedPropType == "villa",
                    onClick = { selectedPropType = "villa" },
                    label = { Text("ویلا") }
                )
                FilterChip(
                    selected = selectedPropType == "office",
                    onClick = { selectedPropType = "office" },
                    label = { Text("اداری") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = neighborhoodInput,
                onValueChange = { neighborhoodInput = it },
                label = { Text("نام محله (مثلاً: زعفرانیه، نیاوران)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onApply(
                        currentFilter.copy(
                            transactionType = selectedTxType,
                            propertyType = selectedPropType,
                            neighborhood = neighborhoodInput.ifBlank { null }
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("apply_filter_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("اعمال فیلترها", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
