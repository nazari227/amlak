package com.example.presentation.demands

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.domain.model.DemandFollowUpNote
import com.example.presentation.components.StatusBadge
import com.example.presentation.properties.PropertyCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemandDetailScreen(
    demandId: Long,
    viewModel: DemandsViewModel,
    onBackClick: () -> Unit,
    onPropertyClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteInput by remember { mutableStateOf("") }

    LaunchedEffect(demandId) {
        viewModel.selectDemand(demandId)
    }

    val demand = state.selectedDemand

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(demand?.clientName ?: "جزئیات متقاضی") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddNoteDialog = true }) {
                        Icon(Icons.Filled.PostAdd, contentDescription = "افزودن یادداشت پیگیری")
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
                            val phone = demand?.clientPhone ?: ""
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$phone")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("action_call_client"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تماس با متقاضی", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showAddNoteDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ثبت پیگیری", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { paddingValues ->
        if (demand != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
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
                                Text(
                                    text = demand.clientName,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                StatusBadge(status = demand.status)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "مشاور مسئول: ${demand.assignedConsultant} • ثبت شده در تاریخ: ${demand.createdAt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeutralMedium
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = NeutralBorder)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("بودجه درخواستی:", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "از ${PersianUtils.formatPrice(demand.minBudget)} تا ${PersianUtils.formatPrice(demand.maxBudget)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = RealEstateBlue)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("حداقل متراژ و خواب:", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${PersianUtils.formatArea(demand.minArea)} • حداقل ${PersianUtils.toPersianDigits(demand.rooms ?: 2)} خواب",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("مناطق ترجیحی:", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = demand.preferredNeighborhoods.joinToString("، "),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }

                // Matching Properties Section
                item {
                    Text(
                        text = "فایل‌های منطبق با بودجه و نیاز متقاضی",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (state.matchingProperties.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                        ) {
                            Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "فایل کاملاً منطبق در این لحظه یافت نشد. می‌توانید از بخش املاک فایل‌های مشابه را پیشنهاد دهید.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeutralMedium
                                )
                            }
                        }
                    }
                } else {
                    items(state.matchingProperties) { property ->
                        PropertyCard(
                            property = property,
                            onClick = { onPropertyClick(property.id) }
                        )
                    }
                }

                // Follow up notes timeline
                item {
                    Text(
                        text = "تاریخچه پیگیری‌ها و یادداشت‌های مشاور",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                val notes = demand.followUpNotes
                if (notes.isEmpty()) {
                    item {
                        Text(
                            text = "هنوز یادداشت پیگیری برای این متقاضی ثبت نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeutralMedium
                        )
                    }
                } else {
                    items(notes) { note ->
                        FollowUpNoteCard(note = note)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Add Follow-Up Note Dialog
        if (showAddNoteDialog) {
            AlertDialog(
                onDismissRequest = { showAddNoteDialog = false },
                title = { Text("ثبت یادداشت پیگیری جدید") },
                text = {
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("متن پیگیری یا نتیجه تماس با متقاضی") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.addFollowUpNote(demandId, noteInput)
                            noteInput = ""
                            showAddNoteDialog = false
                        }
                    ) {
                        Text("ثبت یادداشت")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddNoteDialog = false }) {
                        Text("انصراف")
                    }
                }
            )
        }
    }
}

@Composable
fun FollowUpNoteCard(note: DemandFollowUpNote) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = note.author,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = note.createdAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = NeutralMedium
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
