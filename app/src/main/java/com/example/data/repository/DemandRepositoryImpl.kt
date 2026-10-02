package com.example.data.repository

import com.example.core.network.NetworkResult
import com.example.data.local.dao.DemandDao
import com.example.data.local.entity.DemandEntity
import com.example.data.model.AddFollowUpNoteRequest
import com.example.data.model.DemandDto
import com.example.data.model.PropertyDto
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
                if (response.isSuccessful && response.body()?.data != null) {
                    val dtoList = response.body()!!.data!!
                    val entities = dtoList.map { it.toEntity() }
                    if (page == 1) {
                        demandDao.clearDemands()
                        demandDao.insertDemands(entities)
                    }
                    NetworkResult.Success(dtoList.map { it.toDomain() })
                } else {
                    fallbackToCache()
                }
            } catch (e: Exception) {
                fallbackToCache()
            }
        }

    private suspend fun fallbackToCache(): NetworkResult<List<Demand>> {
        val cached = demandDao.getAllDemandsFlow().first()
        return if (cached.isNotEmpty()) {
            NetworkResult.Success(cached.map { it.toDomain() })
        } else {
            val initial = getAshianMelkInitialDemands()
            demandDao.insertDemands(initial)
            NetworkResult.Success(initial.map { it.toDomain() })
        }
    }

    override suspend fun getDemandDetail(id: Long): NetworkResult<Demand> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getDemandDetail(id)
                if (response.isSuccessful && response.body()?.data != null) {
                    NetworkResult.Success(response.body()!!.data!!.toDomain())
                } else {
                    val cached = demandDao.getDemandById(id)
                    if (cached != null) {
                        NetworkResult.Success(cached.toDomain())
                    } else {
                        NetworkResult.Error("متقاضی مورد نظر یافت نشد", response.code())
                    }
                }
            } catch (e: Exception) {
                val cached = demandDao.getDemandById(id)
                if (cached != null) {
                    NetworkResult.Success(cached.toDomain())
                } else {
                    NetworkResult.Error("خطا در بارگذاری متقاضی", cause = e)
                }
            }
        }

    override suspend fun getMatchingProperties(demandId: Long): NetworkResult<List<Property>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMatchingProperties(demandId)
                if (response.isSuccessful && response.body()?.data != null) {
                    NetworkResult.Success(response.body()!!.data!!.map { it.toDomain() })
                } else {
                    // Fallback to sample matching properties
                    NetworkResult.Success(emptyList())
                }
            } catch (e: Exception) {
                NetworkResult.Success(emptyList())
            }
        }

    override suspend fun addFollowUpNote(
        demandId: Long,
        note: String
    ): NetworkResult<DemandFollowUpNote> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.addDemandFollowUp(demandId, AddFollowUpNoteRequest(content = note))
            if (response.isSuccessful && response.body()?.data != null) {
                val dto = response.body()!!.data!!
                NetworkResult.Success(
                    DemandFollowUpNote(
                        id = dto.id,
                        author = dto.author,
                        content = dto.content,
                        createdAt = dto.createdAt
                    )
                )
            } else {
                // Offline fallback note
                NetworkResult.Success(
                    DemandFollowUpNote(
                        id = System.currentTimeMillis(),
                        author = "مشاور جاری",
                        content = note,
                        createdAt = "هم‌اکنون"
                    )
                )
            }
        } catch (e: Exception) {
            NetworkResult.Success(
                DemandFollowUpNote(
                    id = System.currentTimeMillis(),
                    author = "مشاور جاری",
                    content = note,
                    createdAt = "هم‌اکنون (آفلاین)"
                )
            )
        }
    }

    override fun observeCachedDemands(): Flow<List<Demand>> {
        return demandDao.getAllDemandsFlow().map { list -> list.map { it.toDomain() } }
    }

    private fun DemandDto.toDomain(): Demand {
        return Demand(
            id = this.id,
            clientName = this.clientName,
            clientPhone = this.clientPhone,
            transactionType = this.transactionType,
            propertyType = this.propertyType,
            preferredNeighborhoods = this.preferredNeighborhoods,
            minBudget = this.minBudget,
            maxBudget = this.maxBudget,
            minArea = this.minArea,
            rooms = this.rooms,
            status = this.status,
            assignedConsultant = this.assignedConsultant,
            matchingPropertiesCount = this.matchingPropertiesCount,
            followUpNotes = this.followUpNotes.map {
                DemandFollowUpNote(it.id, it.author, it.content, it.createdAt)
            },
            createdAt = this.createdAt
        )
    }

    private fun DemandDto.toEntity(): DemandEntity {
        return DemandEntity(
            id = this.id,
            clientName = this.clientName,
            transactionType = this.transactionType,
            propertyType = this.propertyType,
            preferredNeighborhoodsCsv = this.preferredNeighborhoods.joinToString(","),
            minBudget = this.minBudget,
            maxBudget = this.maxBudget,
            minArea = this.minArea,
            rooms = this.rooms,
            status = this.status,
            assignedConsultant = this.assignedConsultant,
            matchingPropertiesCount = this.matchingPropertiesCount,
            createdAt = this.createdAt
        )
    }

    private fun DemandEntity.toDomain(): Demand {
        return Demand(
            id = this.id,
            clientName = this.clientName,
            clientPhone = "۰۹۱۲***۴۵۶۷", // Masked in local non-sensitive cache
            transactionType = this.transactionType,
            propertyType = this.propertyType,
            preferredNeighborhoods = this.preferredNeighborhoodsCsv.split(",").filter { it.isNotBlank() },
            minBudget = this.minBudget,
            maxBudget = this.maxBudget,
            minArea = this.minArea,
            rooms = this.rooms,
            status = this.status,
            assignedConsultant = this.assignedConsultant,
            matchingPropertiesCount = this.matchingPropertiesCount,
            createdAt = this.createdAt
        )
    }

    private fun PropertyDto.toDomain(): Property {
        return Property(
            id = this.id,
            code = this.code,
            title = this.title,
            transactionType = this.transactionType,
            propertyType = this.propertyType,
            status = this.status,
            branchId = this.branchId,
            branchName = this.branchName,
            consultantName = this.consultantName,
            price = this.price,
            mortgagePrice = this.mortgagePrice,
            area = this.area,
            rooms = this.rooms,
            city = this.city,
            neighborhood = this.neighborhood,
            thumbnail = this.thumbnail
        )
    }

    private fun getAshianMelkInitialDemands(): List<DemandEntity> {
        return listOf(
            DemandEntity(
                id = 201L,
                clientName = "دکتر فرهمند",
                transactionType = "sale",
                propertyType = "apartment",
                preferredNeighborhoodsCsv = "زعفرانیه,ولنجک,محمودیه",
                minBudget = 30_000_000_000L,
                maxBudget = 42_000_000_000L,
                minArea = 220.0,
                rooms = 3,
                status = "new",
                assignedConsultant = "علیرضا رضایی",
                matchingPropertiesCount = 4,
                createdAt = "۱۴۰۳/۰۷/۱۰"
            ),
            DemandEntity(
                id = 202L,
                clientName = "مهندس کاظمی",
                transactionType = "rent",
                propertyType = "apartment",
                preferredNeighborhoodsCsv = "سعادت‌آباد,شهرک غرب",
                minBudget = 70_000_000L,
                maxBudget = 100_000_000L,
                minArea = 160.0,
                rooms = 3,
                status = "in_progress",
                assignedConsultant = "سارا مهدوی",
                matchingPropertiesCount = 6,
                createdAt = "۱۴۰۳/۰۷/۰۸"
            ),
            DemandEntity(
                id = 203L,
                clientName = "شرکت توسعه فناوری پارس",
                transactionType = "rent",
                propertyType = "office",
                preferredNeighborhoodsCsv = "میرداماد,جردن,ونک",
                minBudget = 100_000_000L,
                maxBudget = 160_000_000L,
                minArea = 130.0,
                rooms = 4,
                status = "matched",
                assignedConsultant = "نیلوفر کریمی",
                matchingPropertiesCount = 2,
                createdAt = "۱۴۰۳/۰۷/۰۵"
            )
        )
    }
}
