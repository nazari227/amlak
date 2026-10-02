package com.example.data.repository

import com.example.core.network.NetworkResult
import com.example.data.local.dao.DemandDao
import com.example.data.local.entity.DemandEntity
import com.example.data.model.DemandDto
import com.example.data.model.DemandMatchDto
import com.example.domain.model.Demand
import com.example.domain.model.DemandFollowUpNote
import com.example.domain.model.Property
import com.example.domain.repository.DemandRepository
import com.example.network.AshianMelkApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DemandRepositoryImpl(
    private val apiService: AshianMelkApiService,
    private val demandDao: DemandDao
) : DemandRepository {

    override suspend fun getDemands(page: Int, status: String?): NetworkResult<List<Demand>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getDemands(page = page, status = status)
                val data = response.body()?.data
                if (response.isSuccessful && data != null) {
                    val mapped = data.items.map { it.toDomain() }
                    if (page == 1) {
                        demandDao.clearDemands()
                        demandDao.insertDemands(mapped.map { it.toEntity() })
                    }
                    NetworkResult.Success(mapped)
                } else {
                    fallbackToCache(response.code())
                }
            } catch (e: Exception) {
                val cached = demandDao.getAllDemandsFlow().first()
                if (cached.isNotEmpty()) NetworkResult.Success(cached.map { it.toDomain() })
                else NetworkResult.Error("تقاضاها در حالت آفلاین قبلاً روی این دستگاه ذخیره نشده‌اند.", cause = e)
            }
        }

    private suspend fun fallbackToCache(code: Int): NetworkResult<List<Demand>> {
        val cached = demandDao.getAllDemandsFlow().first()
        return if (cached.isNotEmpty()) NetworkResult.Success(cached.map { it.toDomain() })
        else NetworkResult.Error("دریافت تقاضاها از سرور انجام نشد.", code)
    }

    override suspend fun getDemandDetail(id: Long): NetworkResult<Demand> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getDemandDetail(id)
                val data = response.body()?.data
                if (response.isSuccessful && data != null) {
                    val demand = data.demand.toDomain()
                    demandDao.insertDemands(listOf(demand.toEntity()))
                    NetworkResult.Success(demand)
                } else {
                    val cached = demandDao.getDemandById(id)
                    if (cached != null) NetworkResult.Success(cached.toDomain())
                    else NetworkResult.Error("تقاضای موردنظر در محدوده دسترسی شما یافت نشد.", response.code())
                }
            } catch (e: Exception) {
                val cached = demandDao.getDemandById(id)
                if (cached != null) NetworkResult.Success(cached.toDomain())
                else NetworkResult.Error("جزئیات تقاضا در حالت آفلاین در دسترس نیست.", cause = e)
            }
        }

    override suspend fun getMatchingProperties(demandId: Long): NetworkResult<List<Property>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getDemandDetail(demandId)
                val data = response.body()?.data
                if (response.isSuccessful && data != null) {
                    NetworkResult.Success(data.matches.map { it.toProperty() })
                } else {
                    NetworkResult.Error("فایل‌های منطبق دریافت نشدند.", response.code())
                }
            } catch (e: Exception) {
                NetworkResult.Error("برای مشاهده فایل‌های منطبق اتصال اینترنت لازم است.", cause = e)
            }
        }

    override suspend fun addFollowUpNote(
        demandId: Long,
        note: String
    ): NetworkResult<DemandFollowUpNote> =
        NetworkResult.Error("ثبت پیگیری تقاضا هنوز در API موبایل Core 1.1.00 ارائه نشده است؛ هیچ داده ساختگی ثبت نشد.")

    override fun observeCachedDemands(): Flow<List<Demand>> =
        demandDao.getAllDemandsFlow().map { list -> list.map { it.toDomain() } }

    private fun DemandDto.toDomain(): Demand {
        val places = listOf(location.neighborhood, location.district, location.city)
            .filter { it.isNotBlank() }
            .distinct()
        return Demand(
            id = id,
            clientName = "تقاضا #$id",
            clientPhone = "",
            transactionType = transactionType,
            propertyType = propertyType,
            preferredNeighborhoods = places,
            minBudget = budget.min.toLong(),
            maxBudget = budget.max.toLong(),
            minArea = requirements.areaMin,
            rooms = requirements.bedroomsMin.takeIf { it > 0 },
            status = status,
            assignedConsultant = assignedAgentUserId.takeIf { it > 0 }?.let { "کاربر #$it" }.orEmpty(),
            matchingPropertiesCount = matchCount,
            followUpNotes = emptyList(),
            createdAt = createdAt
        )
    }

    private fun DemandMatchDto.toProperty(): Property = Property(
        id = if (listingId > 0) listingId else propertyId,
        code = listingPublicId,
        title = title,
        transactionType = "",
        propertyType = propertyType,
        status = status,
        branchId = 0,
        branchName = "",
        consultantName = "",
        price = amount?.toLong() ?: 0,
        area = area,
        rooms = bedrooms,
        neighborhood = neighborhood.ifBlank { district }
    )

    private fun Demand.toEntity(): DemandEntity = DemandEntity(
        id = id,
        clientName = clientName,
        transactionType = transactionType,
        propertyType = propertyType,
        preferredNeighborhoodsCsv = preferredNeighborhoods.joinToString(","),
        minBudget = minBudget,
        maxBudget = maxBudget,
        minArea = minArea,
        rooms = rooms,
        status = status,
        assignedConsultant = assignedConsultant,
        matchingPropertiesCount = matchingPropertiesCount,
        createdAt = createdAt
    )

    private fun DemandEntity.toDomain(): Demand = Demand(
        id = id,
        clientName = clientName,
        clientPhone = "",
        transactionType = transactionType,
        propertyType = propertyType,
        preferredNeighborhoods = preferredNeighborhoodsCsv.split(",").filter { it.isNotBlank() },
        minBudget = minBudget,
        maxBudget = maxBudget,
        minArea = minArea,
        rooms = rooms,
        status = status,
        assignedConsultant = assignedConsultant,
        matchingPropertiesCount = matchingPropertiesCount,
        createdAt = createdAt
    )
}
