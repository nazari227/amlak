package com.example.presentation.creation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.PersianUtils
import com.example.domain.model.PropertyDraft
import com.example.presentation.components.OsmMapPreview
import com.example.ui.theme.*
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyCreationWizardScreen(
    viewModel: PropertyCreationViewModel,
    onBackClick: () -> Unit,
    onSuccessFinish: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.submitSuccess, state.createdProperty) {
        if (state.submitSuccess && state.createdProperty != null) {
            onSuccessFinish(state.createdProperty!!.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ثبت ملک جدید", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = StatusSuccess,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (state.isSavingDraft) "در حال رمزنگاری و ذخیره..." else "پیش‌نویس ذخیره امن (Android Keystore)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = StatusSuccess
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
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
                    if (state.currentStep > 1) {
                        OutlinedButton(
                            onClick = { viewModel.previousStep() },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("گام قبلی", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (state.currentStep < 5) {
                        Button(
                            onClick = { viewModel.nextStep() },
                            modifier = Modifier
                                .weight(if (state.currentStep > 1) 1f else 2f)
                                .height(50.dp)
                                .testTag("wizard_next_step_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("مرحله بعدی", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.submitProperty() },
                            modifier = Modifier
                                .weight(if (state.currentStep > 1) 1.5f else 2f)
                                .height(50.dp)
                                .testTag("wizard_submit_property_button"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !state.isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (state.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("در حال ارسال و اعتبارسنجی...")
                            } else {
                                Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ثبت و انتشار ملک", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Restore unfinished draft notification
            AnimatedVisibility(visible = state.hasUnfinishedDraft) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "پیش‌نویس ناتمام یافت شد. مایل به بازیابی هستید؟",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Row {
                            TextButton(onClick = { viewModel.restoreUnfinishedDraft() }) {
                                Text("بازیابی", fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { viewModel.dismissUnfinishedDraft() }) {
                                Icon(Icons.Filled.Close, contentDescription = "بستن", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Stepper Header
            StepProgressHeader(
                currentStep = state.currentStep,
                onStepClick = { viewModel.goToStep(it) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Step Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                item {
                    when (state.currentStep) {
                        1 -> Step1BasicData(state.draft) { title, txType, propType, price, mort, area, rooms, yr, fl, totFl ->
                            viewModel.updateBasicData(title, txType, propType, price, mort, area, rooms, yr, fl, totFl)
                        }
                        2 -> Step2OwnerData(state.draft) { name, phone, notes ->
                            viewModel.updateOwnerData(name, phone, notes)
                        }
                        3 -> Step3LocationData(state.draft) { city, neighborhood, address, lat, lng ->
                            viewModel.updateLocationData(city, neighborhood, address, lat, lng)
                        }
                        4 -> Step4PhotosAndFeatures(state.draft) { feats, desc, imgs ->
                            viewModel.updateFeaturesAndPhotos(feats, desc, imgs)
                        }
                        5 -> Step5ReviewAndSubmit(state, viewModel)
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun StepProgressHeader(currentStep: Int, onStepClick: (Int) -> Unit) {
    val steps = listOf("پایه", "مالک", "موقعیت", "تصاویر", "تایید")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, title ->
            val stepNumber = index + 1
            val isCurrent = stepNumber == currentStep
            val isCompleted = stepNumber < currentStep

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onStepClick(stepNumber) }
                    .padding(4.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        isCurrent -> MaterialTheme.colorScheme.primary
                        isCompleted -> StatusSuccess
                        else -> Color(0xFFE2E8F0)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isCompleted) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text(
                                text = PersianUtils.toPersianDigits(stepNumber),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isCurrent) Color.White else NeutralMedium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else NeutralMedium
                )
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 1: Basic Property Data
// -------------------------------------------------------------
@Composable
fun Step1BasicData(
    draft: PropertyDraft,
    onUpdate: (String, String, String, Long, Long, Double, Int, Int, Int, Int) -> Unit
) {
    var title by remember { mutableStateOf(draft.title) }
    var txType by remember { mutableStateOf(draft.transactionType) }
    var propType by remember { mutableStateOf(draft.propertyType) }
    var priceStr by remember { mutableStateOf(if (draft.price > 0) draft.price.toString() else "") }
    var mortgageStr by remember { mutableStateOf(if (draft.mortgagePrice > 0) draft.mortgagePrice.toString() else "") }
    var areaStr by remember { mutableStateOf(if (draft.area > 0) draft.area.toInt().toString() else "") }
    var rooms by remember { mutableIntStateOf(draft.rooms) }
    var yearBuiltStr by remember { mutableStateOf(draft.yearBuilt.toString()) }
    var floorStr by remember { mutableStateOf(draft.floor.toString()) }
    var totalFloorsStr by remember { mutableStateOf(draft.totalFloors.toString()) }

    fun sync() {
        onUpdate(
            title,
            txType,
            propType,
            priceStr.toLongOrNull() ?: 0L,
            mortgageStr.toLongOrNull() ?: 0L,
            areaStr.toDoubleOrNull() ?: 0.0,
            rooms,
            yearBuiltStr.toIntOrNull() ?: 1400,
            floorStr.toIntOrNull() ?: 1,
            totalFloorsStr.toIntOrNull() ?: 5
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("گام ۱: اطلاعات پایه ملک", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it; sync() },
            label = { Text("عنوان ملک (مثال: آپارتمان ۱۸۰ متری نیاوران)") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_property_title"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text("نوع معامله", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = txType == "sale",
                onClick = { txType = "sale"; sync() },
                label = { Text("فروش") }
            )
            FilterChip(
                selected = txType == "rent",
                onClick = { txType = "rent"; sync() },
                label = { Text("رهن و اجاره") }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("نوع کاربری", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = propType == "apartment",
                onClick = { propType = "apartment"; sync() },
                label = { Text("آپارتمان") }
            )
            FilterChip(
                selected = propType == "villa",
                onClick = { propType = "villa"; sync() },
                label = { Text("ویلا") }
            )
            FilterChip(
                selected = propType == "office",
                onClick = { propType = "office"; sync() },
                label = { Text("اداری") }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (txType == "sale") {
            OutlinedTextField(
                value = priceStr,
                onValueChange = { priceStr = it.filter { ch -> ch.isDigit() }; sync() },
                label = { Text("قیمت کل به تومان") },
                supportingText = {
                    val p = priceStr.toLongOrNull() ?: 0L
                    if (p > 0) Text(PersianUtils.formatPrice(p), color = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        } else {
            OutlinedTextField(
                value = mortgageStr,
                onValueChange = { mortgageStr = it.filter { ch -> ch.isDigit() }; sync() },
                label = { Text("ودیعه (تومان)") },
                supportingText = {
                    val m = mortgageStr.toLongOrNull() ?: 0L
                    if (m > 0) Text(PersianUtils.formatPrice(m), color = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = priceStr,
                onValueChange = { priceStr = it.filter { ch -> ch.isDigit() }; sync() },
                label = { Text("اجاره ماهانه (تومان)") },
                supportingText = {
                    val p = priceStr.toLongOrNull() ?: 0L
                    if (p > 0) Text(PersianUtils.formatPrice(p), color = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = areaStr,
                onValueChange = { areaStr = it.filter { ch -> ch.isDigit() }; sync() },
                label = { Text("متراژ (متر)") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = rooms.toString(),
                onValueChange = { rooms = it.toIntOrNull() ?: 1; sync() },
                label = { Text("تعداد خواب") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = floorStr,
                onValueChange = { floorStr = it; sync() },
                label = { Text("طبقه") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = totalFloorsStr,
                onValueChange = { totalFloorsStr = it; sync() },
                label = { Text("تعداد کل طبقات") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = yearBuiltStr,
                onValueChange = { yearBuiltStr = it; sync() },
                label = { Text("سال ساخت") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 2: Owner & Contact Information (Encrypted Locally)
// -------------------------------------------------------------
@Composable
fun Step2OwnerData(
    draft: PropertyDraft,
    onUpdate: (String, String, String) -> Unit
) {
    var ownerName by remember { mutableStateOf(draft.ownerName) }
    var ownerPhone by remember { mutableStateOf(draft.ownerPhone) }
    var ownerNotes by remember { mutableStateOf(draft.ownerNotes) }

    fun sync() {
        onUpdate(ownerName, ownerPhone, ownerNotes)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("گام ۲: مشخصات مالک و تماس", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(8.dp))

        // Security Notice Box
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFEFF6FF),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "اطلاعات مالک و تماس محرمانه بوده و در پیش‌نویس با کلید سخت‌افزاری Android Keystore رمزنگاری می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primaryDark
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = ownerName,
            onValueChange = { ownerName = it; sync() },
            label = { Text("نام و نام خانوادگی مالک") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_owner_name"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = ownerPhone,
            onValueChange = { ownerPhone = it; sync() },
            label = { Text("شماره همراه مالک") },
            placeholder = { Text("۰۹۱۲۳۴۵۶۷۸۹") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_owner_phone"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = ownerNotes,
            onValueChange = { ownerNotes = it; sync() },
            label = { Text("یادداشت‌های محرمانه مشاور در مورد مالک و شرایط معامله") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

// -------------------------------------------------------------
// STEP 3: Location
// -------------------------------------------------------------
@Composable
fun Step3LocationData(
    draft: PropertyDraft,
    onUpdate: (String, String, String, Double, Double) -> Unit
) {
    var city by remember { mutableStateOf(draft.city) }
    var neighborhood by remember { mutableStateOf(draft.neighborhood) }
    var address by remember { mutableStateOf(draft.address) }
    var latText by remember(draft.idempotencyKey) {
        mutableStateOf(draft.latitude.takeIf { it != 0.0 }?.toString().orEmpty())
    }
    var lngText by remember(draft.idempotencyKey) {
        mutableStateOf(draft.longitude.takeIf { it != 0.0 }?.toString().orEmpty())
    }

    fun latitude(): Double? = latText.trim().replace('،', '.').replace(',', '.').toDoubleOrNull()
    fun longitude(): Double? = lngText.trim().replace('،', '.').replace(',', '.').toDoubleOrNull()

    fun sync() {
        onUpdate(city, neighborhood, address, latitude() ?: 0.0, longitude() ?: 0.0)
    }

    val lat = latitude()
    val lng = longitude()
    val validCoordinates = lat != null && lng != null &&
        lat in -90.0..90.0 && lng in -180.0..180.0

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("گام ۳: موقعیت مکانی ملک", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = city,
                onValueChange = { city = it; sync() },
                label = { Text("شهر") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = neighborhood,
                onValueChange = { neighborhood = it; sync() },
                label = { Text("محله / منطقه") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it; sync() },
            label = { Text("آدرس دقیق (خیابان، کوچه، پلاک، زنگ)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = latText,
                onValueChange = { latText = it; sync() },
                label = { Text("عرض جغرافیایی") },
                placeholder = { Text("مثال: 35.6892") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = lngText,
                onValueChange = { lngText = it; sync() },
                label = { Text("طول جغرافیایی") },
                placeholder = { Text("مثال: 51.3890") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (validCoordinates) {
                OsmMapPreview(
                    latitude = lat!!,
                    longitude = lng!!,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "برای نمایش نقشه، مختصات معتبر را وارد کنید.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "نقشه OpenStreetMap داخل خود اپ نمایش داده می‌شود.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 4: Photos & Features
// -------------------------------------------------------------
@Composable
fun Step4PhotosAndFeatures(
    draft: PropertyDraft,
    onUpdate: (List<String>, String, List<String>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allFeatures = listOf(
        "پارکینگ سندی", "آسانسور باربر", "انباری اختصاصی",
        "بالکن رو به آفتاب", "استخر و سونا", "لابی مجلل با لابی‌من",
        "روف‌گاردن", "دوربین مداربسته", "سیستم هوشمند BMS"
    )
    val selectedFeatures = remember(draft.idempotencyKey) {
        mutableStateListOf<String>().apply { addAll(draft.features) }
    }
    val imagePaths = remember(draft.idempotencyKey) {
        mutableStateListOf<String>().apply { addAll(draft.localImagePaths) }
    }
    var description by remember(draft.idempotencyKey) { mutableStateOf(draft.description) }
    var cameraTargetPath by remember { mutableStateOf<String?>(null) }
    var imageMessage by remember { mutableStateOf<String?>(null) }

    fun sync() {
        onUpdate(selectedFeatures.toList(), description, imagePaths.toList())
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val room = (10 - imagePaths.size).coerceAtLeast(0)
            val copied = withContext(Dispatchers.IO) {
                uris.take(room).mapNotNull { copyImageToDraftCache(context, it) }
            }
            if (copied.isNotEmpty()) {
                imagePaths.addAll(copied)
                sync()
                imageMessage = "${copied.size} تصویر از گالری اضافه شد."
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val path = cameraTargetPath
        if (success && path != null && File(path).isFile) {
            if (imagePaths.size < 10) {
                imagePaths.add(path)
                sync()
                imageMessage = "عکس دوربین اضافه شد."
            }
        } else if (path != null) {
            File(path).delete()
        }
        cameraTargetPath = null
    }

    fun launchCamera() {
        if (imagePaths.size >= 10) {
            imageMessage = "حداکثر ۱۰ تصویر برای هر پرونده قابل انتخاب است."
            return
        }
        val file = createDraftCameraFile(context)
        cameraTargetPath = file.absolutePath
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else imageMessage = "برای گرفتن عکس جدید، اجازه دوربین لازم است."
    }

    fun requestCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("گام ۴: امکانات و توضیحات ملک", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(14.dp))

        Text("امکانات اختصاصی", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(8.dp))

        allFeatures.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { feature ->
                    val isChecked = selectedFeatures.contains(feature)
                    FilterChip(
                        selected = isChecked,
                        onClick = {
                            if (isChecked) selectedFeatures.remove(feature) else selectedFeatures.add(feature)
                            sync()
                        },
                        label = { Text(feature) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تصاویر ملک", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "گالری با Photo Picker امن اندروید باز می‌شود و دسترسی کامل به عکس‌های گوشی نمی‌گیرد. دوربین فقط هنگام گرفتن عکس مجوز می‌خواهد. تصاویر قبل از ارسال فشرده می‌شوند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = imagePaths.size < 10,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("انتخاب از گالری")
                    }
                    Button(
                        onClick = { requestCamera() },
                        enabled = imagePaths.size < 10,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("دوربین")
                    }
                }

                if (imageMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        imageMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (imagePaths.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${PersianUtils.toPersianDigits(imagePaths.size)} تصویر انتخاب شده",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(imagePaths.toList()) { path ->
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = "تصویر انتخاب‌شده",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = {
                                        imagePaths.remove(path)
                                        File(path).delete()
                                        sync()
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Cancel,
                                        contentDescription = "حذف تصویر",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it; sync() },
            label = { Text("توضیحات تکمیلی فایل برای همکاران و متقاضیان") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

// -------------------------------------------------------------
// STEP 5: Review & Submit (Idempotency & Optimistic Concurrency)
// -------------------------------------------------------------
@Composable
fun Step5ReviewAndSubmit(
    state: PropertyCreationUiState,
    viewModel: PropertyCreationViewModel
) {
    val draft = state.draft

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("گام ۵: بررسی و ثبت نهایی", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(12.dp))

        if (state.errorMessage != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Upload progress bar during upload
        if (state.isSubmitting) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("در حال فشرده‌سازی و ارسال تکه‌ای به سرور...", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.uploadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "پیشرفت: ${PersianUtils.toPersianDigits((state.uploadProgress * 100).toInt())}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Summary Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = draft.title.ifBlank { "عنوان ملک وارد نشده است" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${draft.city}، ${draft.neighborhood.ifBlank { "نامشخص" }} • ${if (draft.transactionType == "sale") "فروش" else "رهن و اجاره"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("قیمت اعلامی:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = PersianUtils.formatPrice(draft.price),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("متراژ و خواب:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${PersianUtils.formatArea(draft.area)} • ${PersianUtils.toPersianDigits(draft.rooms)} خواب",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("مالک:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${draft.ownerName} (${draft.ownerPhone})",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Idempotency & Optimistic Concurrency metadata badge
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "شناسه یکتایی (Idempotency Key): ${draft.idempotencyKey.take(12)}...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "نسخه همزمانی (base_version): ${draft.baseVersion} • اعتبارسنجی SHA-256 فعال است",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


private fun copyImageToDraftCache(context: Context, uri: Uri): String? {
    return try {
        val dir = File(context.cacheDir, "draft_photos").apply { mkdirs() }
        val mime = context.contentResolver.getType(uri).orEmpty().lowercase()
        val ext = when {
            "png" in mime -> "png"
            "webp" in mime -> "webp"
            else -> "jpg"
        }
        val file = File(dir, "gallery_${System.nanoTime()}.${ext}")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        file.takeIf { it.isFile && it.length() > 0 }?.absolutePath
    } catch (_: Exception) {
        null
    }
}

private fun createDraftCameraFile(context: Context): File {
    val dir = File(context.cacheDir, "draft_photos").apply { mkdirs() }
    return File(dir, "camera_${System.nanoTime()}.jpg")
}
