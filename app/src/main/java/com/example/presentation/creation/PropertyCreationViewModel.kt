package com.example.presentation.creation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.NetworkResult
import com.example.domain.model.Property
import com.example.domain.model.PropertyDraft
import com.example.domain.repository.PropertyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class PropertyCreationUiState(
    val currentStep: Int = 1, // 1 to 5
    val draft: PropertyDraft = PropertyDraft(idempotencyKey = UUID.randomUUID().toString()),
    val isSavingDraft: Boolean = false,
    val draftSavedTimestamp: Long? = null,
    val hasUnfinishedDraft: Boolean = false,
    val isSubmitting: Boolean = false,
    val uploadProgress: Float = 0f,
    val submitSuccess: Boolean = false,
    val createdProperty: Property? = null,
    val errorMessage: String? = null,
    val conflictOccurred: Boolean = false
)

class PropertyCreationViewModel(
    private val propertyRepository: PropertyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PropertyCreationUiState())
    val uiState: StateFlow<PropertyCreationUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null

    init {
        checkForExistingDraft()
    }

    private fun checkForExistingDraft() {
        viewModelScope.launch {
            val latestDraft = propertyRepository.getLatestDraft()
            if (latestDraft != null && latestDraft.title.isNotBlank()) {
                _uiState.value = _uiState.value.copy(
                    hasUnfinishedDraft = true
                )
            }
        }
    }

    fun restoreUnfinishedDraft() {
        viewModelScope.launch {
            val latestDraft = propertyRepository.getLatestDraft()
            if (latestDraft != null) {
                _uiState.value = _uiState.value.copy(
                    draft = latestDraft,
                    hasUnfinishedDraft = false
                )
            }
        }
    }

    fun dismissUnfinishedDraft() {
        _uiState.value = _uiState.value.copy(hasUnfinishedDraft = false)
    }

    fun nextStep() {
        if (_uiState.value.currentStep < 5) {
            _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep + 1)
            triggerAutoSave()
        }
    }

    fun previousStep() {
        if (_uiState.value.currentStep > 1) {
            _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep - 1)
        }
    }

    fun goToStep(step: Int) {
        if (step in 1..5) {
            _uiState.value = _uiState.value.copy(currentStep = step)
        }
    }

    // Step 1: Basic
    fun updateBasicData(
        title: String,
        transactionType: String,
        propertyType: String,
        price: Long,
        mortgagePrice: Long,
        area: Double,
        rooms: Int,
        yearBuilt: Int,
        floor: Int,
        totalFloors: Int
    ) {
        val updated = _uiState.value.draft.copy(
            title = title,
            transactionType = transactionType,
            propertyType = propertyType,
            price = price,
            mortgagePrice = mortgagePrice,
            area = area,
            rooms = rooms,
            yearBuilt = yearBuilt,
            floor = floor,
            totalFloors = totalFloors,
            updatedAt = System.currentTimeMillis()
        )
        _uiState.value = _uiState.value.copy(draft = updated)
        triggerAutoSave()
    }

    // Step 2: Owner / Contact (Sensitive)
    fun updateOwnerData(ownerName: String, ownerPhone: String, ownerNotes: String) {
        val updated = _uiState.value.draft.copy(
            ownerName = ownerName,
            ownerPhone = ownerPhone,
            ownerNotes = ownerNotes,
            updatedAt = System.currentTimeMillis()
        )
        _uiState.value = _uiState.value.copy(draft = updated)
        triggerAutoSave()
    }

    // Step 3: Location
    fun updateLocationData(city: String, neighborhood: String, address: String, lat: Double, lng: Double) {
        val updated = _uiState.value.draft.copy(
            city = city,
            neighborhood = neighborhood,
            address = address,
            latitude = lat,
            longitude = lng,
            updatedAt = System.currentTimeMillis()
        )
        _uiState.value = _uiState.value.copy(draft = updated)
        triggerAutoSave()
    }

    // Step 4: Photos & Features
    fun updateFeaturesAndPhotos(features: List<String>, description: String, localImages: List<String>) {
        val updated = _uiState.value.draft.copy(
            features = features,
            description = description,
            localImagePaths = localImages,
            updatedAt = System.currentTimeMillis()
        )
        _uiState.value = _uiState.value.copy(draft = updated)
        triggerAutoSave()
    }

    private fun triggerAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1000) // debounce autosave
            _uiState.value = _uiState.value.copy(isSavingDraft = true)
            propertyRepository.saveDraft(_uiState.value.draft)
            _uiState.value = _uiState.value.copy(
                isSavingDraft = false,
                draftSavedTimestamp = System.currentTimeMillis()
            )
        }
    }

    fun submitProperty() {
        val draft = _uiState.value.draft
        if (draft.title.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "لطفاً عنوان ملک را وارد کنید.")
            return
        }

        _uiState.value = _uiState.value.copy(
            isSubmitting = true,
            uploadProgress = 0.05f,
            errorMessage = null,
            conflictOccurred = false
        )

        viewModelScope.launch {
            val result = propertyRepository.createProperty(draft) { progress ->
                _uiState.value = _uiState.value.copy(uploadProgress = progress)
            }

            when (result) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submitSuccess = true,
                        createdProperty = result.data,
                        uploadProgress = 1.0f
                    )
                }
                is NetworkResult.Error -> {
                    val isConflict = result.statusCode == 409
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = result.message,
                        conflictOccurred = isConflict
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }
}
