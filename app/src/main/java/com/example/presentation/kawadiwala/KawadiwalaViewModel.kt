package com.example.presentation.kawadiwala

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.KawadiwalaRequest
import com.example.data.model.WasteAvailability
import com.example.data.repository.RequestRepository
import com.example.data.repository.WasteRepository
import com.example.utils.Constants
import com.example.utils.LocationHelper
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface KawadiwalaUiState {
    data object Idle : KawadiwalaUiState
    data class Loading(val message: String) : KawadiwalaUiState
    data class Success(val message: String) : KawadiwalaUiState
    data class Error(val message: String) : KawadiwalaUiState
}

class KawadiwalaViewModel(
    private val kawadiwalaId: String,
    private val kawadiwalaName: String,
    private val wasteRepository: WasteRepository,
    private val requestRepository: RequestRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    // Initial default demo map position
    val defaultMapPosition = CameraPosition.fromLatLngZoom(Constants.DEFAULT_DEMO_LOCATION, Constants.DEFAULT_MAP_ZOOM)
    val userCurrentLocation = MutableStateFlow<LatLng?>(null)

    // Filtered visible waste availabilities for this Kawadiwala
    val activeAvailabilities: StateFlow<List<WasteAvailability>> =
        wasteRepository.getActiveAvailabilitiesForKawadiwala(kawadiwalaId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sent requests by this Kawadiwala
    val sentRequests: StateFlow<List<KawadiwalaRequest>> =
        requestRepository.getRequestsForKawadiwala(kawadiwalaId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected availability on map for offer submission dialog
    val selectedAvailability = MutableStateFlow<WasteAvailability?>(null)

    // Form inputs for offer dialog
    val offerInputs = MutableStateFlow<Map<String, String>>(emptyMap())
    val selectedArrivalHours = MutableStateFlow(1) // 1, 2, or 3

    private val _uiState = MutableStateFlow<KawadiwalaUiState>(KawadiwalaUiState.Idle)
    val uiState: StateFlow<KawadiwalaUiState> = _uiState.asStateFlow()

    init {
        updateUserLocation()
    }

    fun updateUserLocation() {
        viewModelScope.launch {
            val loc = locationHelper.getCurrentLocation()
            if (loc != null) {
                userCurrentLocation.value = LatLng(loc.latitude, loc.longitude)
            }
        }
    }

    fun selectMarker(availability: WasteAvailability) {
        selectedAvailability.value = availability
        // Initialize offer input map with empty strings
        val initOffers = availability.wasteTypes.associateWith { "" }
        offerInputs.value = initOffers
        selectedArrivalHours.value = 1
        _uiState.value = KawadiwalaUiState.Idle
    }

    fun dismissMarkerDialog() {
        selectedAvailability.value = null
        offerInputs.value = emptyMap()
    }

    fun updateOffer(wasteType: String, offerAmount: String) {
        val current = offerInputs.value.toMutableMap()
        current[wasteType] = offerAmount
        offerInputs.value = current
    }

    fun setArrivalHours(hours: Int) {
        if (hours in 1..3) {
            selectedArrivalHours.value = hours
        }
    }

    fun submitRequest() {
        val availability = selectedAvailability.value ?: return
        val rawOffers = offerInputs.value

        // Validate each waste type has a valid positive numeric offer
        val numericOffers = mutableMapOf<String, Double>()
        for (waste in availability.wasteTypes) {
            val valStr = rawOffers[waste]
            val parsed = valStr?.toDoubleOrNull()
            if (parsed == null || parsed <= 0) {
                _uiState.value = KawadiwalaUiState.Error("Please enter valid offer amounts for all waste items")
                return
            }
            numericOffers[waste] = parsed
        }

        viewModelScope.launch {
            _uiState.value = KawadiwalaUiState.Loading("Submitting request to household…")
            val result = requestRepository.submitRequest(
                wasteAvailabilityId = availability.id,
                kawadiwalaId = kawadiwalaId,
                kawadiwalaName = kawadiwalaName,
                offersPerWaste = numericOffers,
                estimatedArrivalHours = selectedArrivalHours.value
            )

            result.fold(
                onSuccess = {
                    _uiState.value = KawadiwalaUiState.Success("Request sent to household successfully!")
                    dismissMarkerDialog()
                },
                onFailure = {
                    _uiState.value = KawadiwalaUiState.Error(it.message ?: "Failed to submit request")
                }
            )
        }
    }

    fun clearUiState() {
        _uiState.value = KawadiwalaUiState.Idle
    }
}
