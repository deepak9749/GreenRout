package com.example.presentation.household

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.KawadiwalaRequest
import com.example.data.model.WasteAvailability
import com.example.data.repository.RequestRepository
import com.example.data.repository.WasteRepository
import com.example.domain.model.AvailabilityStatus
import com.example.domain.model.WasteType
import com.example.utils.Constants
import com.example.utils.LocationHelper
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface OperationState {
    data object Idle : OperationState
    data class Loading(val message: String) : OperationState
    data class Success(val message: String) : OperationState
    data class Error(val message: String) : OperationState
}

class HouseholdViewModel(
    private val householdId: String,
    private val householdName: String,
    private val wasteRepository: WasteRepository,
    private val requestRepository: RequestRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    // Selection state for "Make Waste Available"
    val selectedWasteTypes = MutableStateFlow<Set<WasteType>>(emptySet())
    val perWasteQuantities = MutableStateFlow<Map<String, String>>(emptyMap())
    val selectedPhotoUri = MutableStateFlow<Uri?>(null)
    val isCustomLocation = MutableStateFlow<Boolean>(false)
    val customLocationCoordinates = MutableStateFlow<LatLng?>(null)

    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    // Household's own availabilities
    val availabilities: StateFlow<List<WasteAvailability>> =
        wasteRepository.getHouseholdAvailabilities(householdId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Received requests for currently active availability
    private val _receivedRequests = MutableStateFlow<List<KawadiwalaRequest>>(emptyList())
    val receivedRequests: StateFlow<List<KawadiwalaRequest>> = _receivedRequests.asStateFlow()

    private var activeRequestsJob: kotlinx.coroutines.Job? = null

    init {
        // Observe top active availability to watch received requests
        viewModelScope.launch {
            availabilities.collect { list ->
                val active = list.firstOrNull { it.status == AvailabilityStatus.AVAILABLE && !it.isExpired }
                    ?: list.firstOrNull { it.status == AvailabilityStatus.BOOKED }

                if (active != null) {
                    activeRequestsJob?.cancel()
                    activeRequestsJob = launch {
                        requestRepository.getRequestsForAvailability(active.id).collect { reqs ->
                            _receivedRequests.value = reqs
                        }
                    }
                } else {
                    _receivedRequests.value = emptyList()
                }
            }
        }
    }

    fun toggleWasteType(wasteType: WasteType) {
        val current = selectedWasteTypes.value.toMutableSet()
        val currentQuantities = perWasteQuantities.value.toMutableMap()

        if (current.contains(wasteType)) {
            current.remove(wasteType)
            currentQuantities.remove(wasteType.key)
        } else {
            current.add(wasteType)
        }
        selectedWasteTypes.value = current
        perWasteQuantities.value = currentQuantities
    }

    fun setWasteQuantity(wasteKey: String, quantity: String) {
        val current = perWasteQuantities.value.toMutableMap()
        if (quantity.isBlank()) {
            current.remove(wasteKey)
        } else {
            current[wasteKey] = quantity
        }
        perWasteQuantities.value = current
    }

    fun setPhotoUri(uri: Uri?) {
        selectedPhotoUri.value = uri
    }

    fun setLocationMode(custom: Boolean) {
        isCustomLocation.value = custom
        if (custom && customLocationCoordinates.value == null) {
            viewModelScope.launch {
                val loc = locationHelper.getCurrentLocation()
                val lat = loc?.latitude ?: Constants.DEMO_LATITUDE
                val lng = loc?.longitude ?: Constants.DEMO_LONGITUDE
                customLocationCoordinates.value = LatLng(lat, lng)
            }
        } else if (!custom) {
            customLocationCoordinates.value = null
        }
    }

    fun resetCustomLocationToCurrent() {
        viewModelScope.launch {
            val loc = locationHelper.getCurrentLocation()
            val lat = loc?.latitude ?: Constants.DEMO_LATITUDE
            val lng = loc?.longitude ?: Constants.DEMO_LONGITUDE
            customLocationCoordinates.value = LatLng(lat, lng)
        }
    }

    fun setCustomLocationCoordinates(latLng: LatLng?) {
        customLocationCoordinates.value = latLng
    }

    fun submitWasteAvailability() {
        val types = selectedWasteTypes.value
        if (types.isEmpty()) {
            _operationState.value = OperationState.Error("Please select at least one waste category")
            return
        }

        viewModelScope.launch {
            _operationState.value = OperationState.Loading("Capturing location & saving availability…")

            // 1. Determine Coordinates (Custom or Current GPS)
            val customCoord = customLocationCoordinates.value
            val isCustom = isCustomLocation.value && customCoord != null
            val lat: Double
            val lng: Double
            val locType: String

            if (isCustom) {
                lat = customCoord!!.latitude
                lng = customCoord.longitude
                locType = "CUSTOM"
            } else {
                val location = locationHelper.getCurrentLocation()
                lat = location?.latitude ?: Constants.DEMO_LATITUDE
                lng = location?.longitude ?: Constants.DEMO_LONGITUDE
                locType = "CURRENT"
            }

            // 2. Upload optional photo if chosen
            var photoUrl: String? = null
            val photoUri = selectedPhotoUri.value
            if (photoUri != null) {
                val uploadResult = wasteRepository.uploadPhoto(photoUri)
                photoUrl = uploadResult.getOrNull()
            }

            // 3. Map quantities
            val quantities = perWasteQuantities.value.mapNotNull { (k, v) ->
                val num = v.toDoubleOrNull()
                if (num != null && num > 0) k to num else null
            }.toMap()

            // 4. Save availability (24 hour validity)
            val result = wasteRepository.createAvailability(
                householdId = householdId,
                householdName = householdName,
                lat = lat,
                lng = lng,
                wasteTypes = types.map { it.key },
                perWasteQuantity = quantities,
                photoUrl = photoUrl,
                locationType = locType
            )

            result.fold(
                onSuccess = {
                    _operationState.value = OperationState.Success(
                        "Waste is now available on Kawadiwala scrap map for 24 hours!"
                    )
                    // Reset inputs
                    selectedWasteTypes.value = emptySet()
                    perWasteQuantities.value = emptyMap()
                    selectedPhotoUri.value = null
                    isCustomLocation.value = false
                    customLocationCoordinates.value = null
                },
                onFailure = {
                    _operationState.value = OperationState.Error(it.message ?: "Failed to save availability")
                }
            )
        }
    }

    fun removeAvailability(availabilityId: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading("Removing availability…")
            val res = wasteRepository.removeAvailability(availabilityId, householdId)
            res.fold(
                onSuccess = { removedItem ->
                    // Cancel all requests and notify any accepted Kawadiwala
                    requestRepository.cancelRequestsForAvailability(
                        availabilityId = availabilityId,
                        bookedKawadiwalaId = removedItem.acceptedKawadiwalaId
                    )
                    _operationState.value = OperationState.Success("Availability removed successfully")
                },
                onFailure = { _operationState.value = OperationState.Error(it.message ?: "Could not remove availability") }
            )
        }
    }

    fun acceptRequest(availabilityId: String, requestId: String, kawadiwalaId: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading("Accepting request atomically…")
            val res = requestRepository.acceptRequestAtomic(availabilityId, requestId, kawadiwalaId)
            res.fold(
                onSuccess = {
                    _operationState.value = OperationState.Success("Request accepted! Kawadiwala has been notified.")
                },
                onFailure = {
                    _operationState.value = OperationState.Error(it.message ?: "Unable to accept request.")
                }
            )
        }
    }

    fun clearOperationState() {
        _operationState.value = OperationState.Idle
    }
}
