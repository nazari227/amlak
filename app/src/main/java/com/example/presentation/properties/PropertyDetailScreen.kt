package com.example.presentation.properties

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.network.NetworkResult
import com.example.core.util.PersianUtils
import com.example.domain.model.Property
import com.example.domain.repository.PropertyRepository
import com.example.presentation.components.StatusBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    propertyId: Long,
    propertyRepository: PropertyRepository,
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit
) {
    val context = LocalContext.current
    var propertyState by remember { mutableStateOf<Property?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(propertyId) {
        isLoading = true
        val result = propertyRepository.getPropertyDetail(propertyId)
        if (result is NetworkResult.Success) {
            propertyState = result.data
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(propertyState?.code ?: "جزئیات ملک") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditClick(propertyId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "ویرایش ملک")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:02122000000")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("action_call_agent"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تماس با مشاور", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "فایل ملک ${propertyState?.title ?: ""} (کد: ${propertyState?.code ?: ""}) در سامانه آشیان ملک"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "اشتراک‌گذاری فایل ملک"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اشتراک‌گذاری", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val property = propertyState
            if (property != null) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // 1. Gallery Carousel
                    item {
                        val images = if (property.images.isNotEmpty()) property.images else listOfNotNull(property.thumbnail)
                        if (images.isNotEmpty()) {
                            val pagerState = rememberPagerState(pageCount = { images.size })
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .background(Color(0xFFE2E8F0))
                            ) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    AsyncImage(
                                        model = images[page],
                                        contentDescription = "تصویر ملک",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                if (images.size > 1) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 12.dp)
                                    ) {
                                        Text(
                                            text = "${PersianUtils.toPersianDigits(pagerState.currentPage + 1)} از ${PersianUtils.toPersianDigits(images.size)}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Title & Status
                    item {
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
                                StatusBadge(status = property.status)
                                Text(
                                    text = property.code,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = property.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Place, contentDescription = null, tint = NeutralMedium, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${property.city}، ${property.neighborhood} • ${property.branchName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NeutralMedium
                                )
                            }
                        }
                    }

                    // 3. Pricing Box
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (property.transactionType == "sale") {
                                    Text("قیمت کل اعلامی", style = MaterialTheme.typography.labelMedium, color = NeutralMedium)
                                    Text(
                                        text = PersianUtils.formatPrice(property.price),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 22.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    val pricePerMeter = if (property.area > 0) (property.price / property.area).toLong() else 0
                                    Text(
                                        text = "قیمت هر متر مربع: ${PersianUtils.formatPrice(pricePerMeter)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeutralDark
                                    )
                                } else {
                                    Text(
                                        text = "مبلغ ودیعه: ${PersianUtils.formatPrice(property.mortgagePrice)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "اجاره ماهانه: ${PersianUtils.formatPrice(property.price)}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 4. Specs Grid
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "مشخصات فنی ملک",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SpecBox(modifier = Modifier.weight(1f), title = "متراژ", value = PersianUtils.formatArea(property.area), icon = Icons.Outlined.SquareFoot)
                                SpecBox(modifier = Modifier.weight(1f), title = "تعداد خواب", value = PersianUtils.toPersianDigits(property.rooms), icon = Icons.Outlined.Bed)
                                SpecBox(modifier = Modifier.weight(1f), title = "طبقه", value = "${PersianUtils.toPersianDigits(property.floor ?: 1)} از ${PersianUtils.toPersianDigits(property.totalFloors ?: 5)}", icon = Icons.Outlined.Layers)
                                SpecBox(modifier = Modifier.weight(1f), title = "سال ساخت", value = PersianUtils.toPersianDigits(property.yearBuilt ?: 1400), icon = Icons.Outlined.CalendarToday)
                            }
                        }
                    }

                    // 5. Backend Coordinates Map Representation
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "موقعیت جغرافیایی و نقشه",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "مختصات ثبت شده در سرور: ${property.latitude ?: 35.6892} , ${property.longitude ?: 51.3890}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeutralMedium
                                        )
                                        Text(
                                            text = property.address ?: "${property.city}، ${property.neighborhood}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = NeutralDark
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SpecBox(modifier: Modifier = Modifier, title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            Text(title, style = MaterialTheme.typography.labelSmall, color = NeutralMedium)
        }
    }
}
