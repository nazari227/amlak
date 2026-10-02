package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.core.network.NetworkResult
import com.example.data.local.dao.PropertyDao
import com.example.data.local.entity.PropertyEntity
import com.example.data.model.BeginUploadRequest
import com.example.data.model.CaseDto
import com.example.data.model.CasePayloadRequest
import com.example.data.model.CreateCaseRequest
import com.example.data.model.UpdateCaseRequest
import com.example.domain.model.Property
import com.example.domain.model.PropertyDraft
import com.example.domain.model.PropertyFilter
import com.example.domain.repository.PropertyRepository
import com.example.network.AshianMelkApiService
import com.example.security.EncryptedDraftStorage
import com.example.security.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

class PropertyRepositoryImpl(
    private val context: Context,
    private val apiService: AshianMelkApiService,
    private val propertyDao: PropertyDao,
    private val encryptedDraftStorage: EncryptedDraftStorage
) : PropertyRepository {

    override suspend fun getProperties(
        page: Int,
        perPage: Int,
        filter: PropertyFilter?
    ): NetworkResult<List<Property>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCases(
                page = page,
                perPage = perPage,
                search = filter?.searchQuery ?: filter?.code ?: filter?.neighborhood,
                status = filter?.status,
                transactionType = normalizeTransaction(filter?.transactionType),
                lifecycle = "active"
            )
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                val properties = data.items.map { it.toDomain() }
                if (page == 1 && filter == null) {
                    propertyDao.clearProperties()
                    propertyDao.insertProperties(properties.map { it.toEntity() })
                }
                NetworkResult.Success(properties)
            } else {
                loadFromCacheOrError(response.code(), response.message())
            }
        } catch (e: Exception) {
            val cached = propertyDao.getAllPropertiesFlow().first()
            if (cached.isNotEmpty()) NetworkResult.Success(cached.map { it.toDomain() })
            else NetworkResult.Error("اتصال به سرور املاک برقرار نشد و داده ذخیره‌شده‌ای روی دستگاه وجود ندارد.", cause = e)
        }
    }

    private suspend fun loadFromCacheOrError(code: Int, message: String): NetworkResult<List<Property>> {
        val cached = propertyDao.getAllPropertiesFlow().first()
        return if (cached.isNotEmpty()) {
            NetworkResult.Success(cached.map { it.toDomain() })
        } else {
            NetworkResult.Error("دریافت پرونده‌های ملکی انجام نشد: $message", code)
        }
    }

    override suspend fun getPropertyDetail(id: Long): NetworkResult<Property> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCaseDetail(id)
            val detail = response.body()?.data
            if (response.isSuccessful && detail != null) {
                val property = detail.caseItem.copy(version = detail.serverVersion.ifBlank { detail.caseItem.version }).toDomain()
                propertyDao.insertProperties(listOf(property.toEntity()))
                NetworkResult.Success(property)
            } else {
                val cached = propertyDao.getPropertyById(id)
                if (cached != null) NetworkResult.Success(cached.toDomain())
                else NetworkResult.Error("پرونده موردنظر در محدوده دسترسی شما یافت نشد.", response.code())
            }
        } catch (e: Exception) {
            val cached = propertyDao.getPropertyById(id)
            if (cached != null) NetworkResult.Success(cached.toDomain())
            else NetworkResult.Error("جزئیات پرونده در حالت آفلاین در دسترس نیست.", cause = e)
        }
    }

    override suspend fun createProperty(
        draft: PropertyDraft,
        onProgress: (Float) -> Unit
    ): NetworkResult<Property> = withContext(Dispatchers.IO) {
        encryptedDraftStorage.saveDraft(draft)
        val compressed = mutableListOf<File>()
        try {
            onProgress(0.05f)

            for ((index, imagePath) in draft.localImagePaths.withIndex()) {
                val source = File(imagePath)
                if (!source.isFile) {
                    return@withContext NetworkResult.Error("یکی از تصاویر انتخاب‌شده دیگر روی دستگاه موجود نیست.")
                }
                compressed += compressImageFile(source)
                onProgress(0.05f + (0.15f * (index + 1) / draft.localImagePaths.size.coerceAtLeast(1)))
            }

            for ((index, image) in compressed.withIndex()) {
                val uploadResult = uploadDraftImage(draft.idempotencyKey, image) { progress ->
                    val imageBase = 0.20f + (0.55f * index / compressed.size.coerceAtLeast(1))
                    val imageShare = 0.55f / compressed.size.coerceAtLeast(1)
                    onProgress(imageBase + imageShare * progress)
                }
                if (uploadResult is NetworkResult.Error) return@withContext uploadResult
            }

            onProgress(0.80f)
            val response = apiService.createCase(
                idempotencyKey = draft.idempotencyKey,
                request = CreateCaseRequest(
                    payload = draft.toCasePayload(),
                    draftClientKey = draft.idempotencyKey
                )
            )
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                val created = data.caseItem.toDomain()
                propertyDao.insertProperties(listOf(created.toEntity()))
                encryptedDraftStorage.deleteDraft(draft.idempotencyKey)
                onProgress(1.0f)
                NetworkResult.Success(created)
            } else {
                NetworkResult.Error(
                    message = when (response.code()) {
                        400 -> "اطلاعات پرونده کامل یا معتبر نیست. فیلدهای الزامی را بررسی کنید."
                        403 -> "نقش کاربری شما اجازه ثبت این پرونده یا موقعیت دقیق را ندارد."
                        409 -> "این ثبت با تغییر دیگری تداخل دارد. اطلاعات را تازه‌سازی کنید."
                        else -> "ثبت پرونده روی سرور انجام نشد (${response.code()})."
                    },
                    statusCode = response.code()
                )
            }
        } catch (e: Exception) {
            NetworkResult.Error("ارسال پرونده کامل نشد؛ پیش‌نویس رمزگذاری‌شده روی دستگاه حفظ شد.", cause = e)
        } finally {
            compressed.forEach { file ->
                if (file.parentFile?.name == "compressed_images") file.delete()
            }
        }
    }

    private suspend fun uploadDraftImage(
        draftKey: String,
        file: File,
        onProgress: (Float) -> Unit
    ): NetworkResult<Unit> {
        val sha256 = SecurityUtils.calculateSha256(file)
        val begin = apiService.beginUpload(
            BeginUploadRequest(
                fileName = file.name,
                mimeType = "image/jpeg",
                totalBytes = file.length(),
                draftClientKey = draftKey,
                sha256 = sha256
            )
        )
        val upload = begin.body()?.data?.upload
        if (!begin.isSuccessful || upload == null) {
            return NetworkResult.Error("شروع بارگذاری تصویر انجام نشد.", begin.code())
        }

        val maxChunk = (upload.chunkMaxBytes ?: 5_242_880L).coerceIn(64 * 1024L, 5_242_880L)
        var offset = upload.offset.coerceAtLeast(0)
        RandomAccessFile(file, "r").use { input ->
            val buffer = ByteArray(maxChunk.toInt())
            while (offset < file.length()) {
                input.seek(offset)
                val requested = minOf(buffer.size.toLong(), file.length() - offset).toInt()
                val read = input.read(buffer, 0, requested)
                if (read <= 0) return NetworkResult.Error("خواندن تصویر برای ادامه بارگذاری ممکن نشد.")
                val body = buffer.copyOf(read).toRequestBody("application/octet-stream".toMediaType())
                val part = apiService.uploadChunk(upload.uploadId, offset, bytes = body)
                val state = part.body()?.data?.upload
                if (!part.isSuccessful || state == null) {
                    if (part.code() == 409) {
                        val status = apiService.getUploadStatus(upload.uploadId).body()?.data?.upload
                        if (status != null && status.offset >= 0) {
                            offset = status.offset
                            continue
                        }
                    }
                    return NetworkResult.Error("ادامه بارگذاری تصویر انجام نشد.", part.code())
                }
                offset = state.offset
                onProgress((offset.toFloat() / file.length().coerceAtLeast(1L)).coerceIn(0f, 1f))
            }
        }

        val complete = apiService.completeUpload(upload.uploadId)
        if (!complete.isSuccessful || complete.body()?.data?.upload == null) {
            return NetworkResult.Error("نهایی‌سازی تصویر روی سرور انجام نشد.", complete.code())
        }
        onProgress(1f)
        return NetworkResult.Success(Unit)
    }

    override suspend fun updateProperty(
        id: Long,
        draft: PropertyDraft,
        baseVersion: String
    ): NetworkResult<Property> = withContext(Dispatchers.IO) {
        if (baseVersion.isBlank()) {
            return@withContext NetworkResult.Error("نسخه سرور پرونده مشخص نیست؛ ابتدا پرونده را تازه‌سازی کنید.", 428)
        }
        try {
            val response = apiService.updateCase(
                id,
                UpdateCaseRequest(baseVersion = baseVersion, payload = draft.toCasePayload())
            )
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                val updated = data.caseItem.copy(
                    version = data.serverVersion?.takeIf { it.isNotBlank() } ?: data.caseItem.version
                ).toDomain()
                propertyDao.insertProperties(listOf(updated.toEntity()))
                NetworkResult.Success(updated)
            } else if (response.code() == 409) {
                NetworkResult.Error("پرونده توسط همکار دیگری تغییر کرده است. نسخه جدید را دریافت و دوباره ویرایش کنید.", 409)
            } else {
                NetworkResult.Error("ویرایش پرونده روی سرور انجام نشد.", response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("ویرایش پرونده نیاز به اتصال آنلاین دارد.", cause = e)
        }
    }

    override fun observeCachedProperties(): Flow<List<Property>> =
        propertyDao.getAllPropertiesFlow().map { list -> list.map { it.toDomain() } }

    override fun searchCachedProperties(query: String): Flow<List<Property>> =
        propertyDao.searchProperties(query).map { list -> list.map { it.toDomain() } }

    override suspend fun saveDraft(draft: PropertyDraft) = withContext(Dispatchers.IO) {
        encryptedDraftStorage.saveDraft(draft)
    }

    override suspend fun getDraft(idempotencyKey: String): PropertyDraft? = withContext(Dispatchers.IO) {
        encryptedDraftStorage.getDraft(idempotencyKey)
    }

    override suspend fun getLatestDraft(): PropertyDraft? = withContext(Dispatchers.IO) {
        encryptedDraftStorage.getLatestDraft()
    }

    override suspend fun getAllDrafts(): List<PropertyDraft> = withContext(Dispatchers.IO) {
        encryptedDraftStorage.getAllDrafts()
    }

    override suspend fun deleteDraft(idempotencyKey: String) = withContext(Dispatchers.IO) {
        encryptedDraftStorage.deleteDraft(idempotencyKey)
    }

    private fun PropertyDraft.toCasePayload(): CasePayloadRequest {
        val tx = normalizeTransaction(transactionType) ?: "sale"
        val type = normalizePropertyType(propertyType)
        val isRental = tx == "rent" || tx == "mortgage_rent"
        return CasePayloadRequest(
            title = title.trim(),
            transactionType = tx,
            propertyType = type,
            priceDisplayMode = "numeric",
            price = if (!isRental) price.takeIf { it > 0 } else null,
            depositAmount = if (isRental) mortgagePrice.takeIf { it > 0 } else null,
            rentAmount = if (isRental) price.takeIf { it > 0 } else null,
            area = area.takeIf { it > 0 },
            bedrooms = rooms.takeIf { it > 0 },
            buildYear = yearBuilt.takeIf { it > 0 },
            floorNo = floor,
            totalFloors = totalFloors.takeIf { it > 0 },
            ownerName = ownerName.trim(),
            ownerMobile = ownerPhone.trim(),
            cityName = city.trim(),
            neighborhoodName = neighborhood.trim(),
            exactAddress = address.trim().takeIf { it.isNotEmpty() },
            exactLat = latitude.takeIf { it != 0.0 },
            exactLng = longitude.takeIf { it != 0.0 },
            publicDescription = description.trim().takeIf { it.isNotEmpty() },
            internalSummary = ownerNotes.trim().takeIf { it.isNotEmpty() }
        )
    }

    private fun normalizeTransaction(value: String?): String? = when (value) {
        "mortgage" -> "mortgage_rent"
        null, "" -> null
        else -> value
    }

    private fun normalizePropertyType(value: String): String = when (value) {
        "store" -> "commercial"
        else -> value
    }

    private fun CaseDto.toDomain(): Property = Property(
        id = id,
        code = caseCode,
        title = property.title,
        transactionType = transactionType,
        propertyType = property.type,
        status = status,
        branchId = branchId,
        branchName = branchName,
        consultantName = if (assignedAgentUserId > 0) "کاربر #$assignedAgentUserId" else "",
        price = 0,
        mortgagePrice = 0,
        area = property.area,
        rooms = property.bedrooms,
        city = "",
        neighborhood = location.neighborhood.ifBlank { location.district },
        baseVersion = version,
        updatedAt = updatedAt
    )

    private fun Property.toEntity(): PropertyEntity = PropertyEntity(
        id = id,
        code = code,
        title = title,
        transactionType = transactionType,
        propertyType = propertyType,
        status = status,
        branchId = branchId,
        branchName = branchName,
        consultantName = consultantName,
        price = price,
        mortgagePrice = mortgagePrice,
        area = area,
        rooms = rooms,
        floor = floor,
        totalFloors = totalFloors,
        yearBuilt = yearBuilt,
        city = city,
        neighborhood = neighborhood,
        thumbnail = thumbnail,
        baseVersion = baseVersion
    )

    private fun PropertyEntity.toDomain(): Property = Property(
        id = id,
        code = code,
        title = title,
        transactionType = transactionType,
        propertyType = propertyType,
        status = status,
        branchId = branchId,
        branchName = branchName,
        consultantName = consultantName,
        price = price,
        mortgagePrice = mortgagePrice,
        area = area,
        rooms = rooms,
        floor = floor,
        totalFloors = totalFloors,
        yearBuilt = yearBuilt,
        city = city,
        neighborhood = neighborhood,
        thumbnail = thumbnail,
        baseVersion = baseVersion
    )

    private fun compressImageFile(file: File): File {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: throw IllegalArgumentException("تصویر قابل خواندن نیست.")
        val maxDimension = 1600
        val largest = maxOf(bitmap.width, bitmap.height)
        val scaled = if (largest > maxDimension) {
            val ratio = maxDimension.toFloat() / largest.toFloat()
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt().coerceAtLeast(1),
                (bitmap.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else bitmap

        val outDir = File(context.cacheDir, "compressed_images").apply { mkdirs() }
        val output = File(outDir, "mobile_${System.nanoTime()}.jpg")
        FileOutputStream(output).use {
            if (!scaled.compress(Bitmap.CompressFormat.JPEG, 82, it)) {
                throw IllegalStateException("فشرده‌سازی تصویر انجام نشد.")
            }
        }
        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()
        return output
    }
}
