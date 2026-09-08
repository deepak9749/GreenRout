package com.example.presentation.kawadiwala.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.RouteInfo
import com.example.data.model.WasteAvailability
import com.example.data.repository.RequestRepository
import com.example.data.repository.RouteRepository
import com.example.data.repository.WasteRepository
import com.example.utils.Constants
import com.example.utils.LocationHelper
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

sealed interface TrackingUiState {
    data object Loading : TrackingUiState
    data class Success(
        val availability: WasteAvailability,
        val currentPosition: LatLng,
        val destinationPosition: LatLng,
        val routeInfo: RouteInfo?,
        val isNavigating: Boolean = false,
        val isArrived: Boolean = false,
        val isCalculatingRoute: Boolean = false,
        val gpsEnabled: Boolean = true,
        val hasLocationPermission: Boolean = true,
        val routeError: String? = null
    ) : TrackingUiState

    data class Expired(val message: String) : TrackingUiState
    data class AccessDenied(val message: String) : TrackingUiState
    data class Error(val message: String) : TrackingUiState
}

class KawadiwalaTrackingViewModel(
    private val availabilityId: String,
    private val kawadiwalaId: String,
    private val wasteRepository: WasteRepository,
    private val routeRepository: RouteRepository,
    private val locationHelper: LocationHelper,
    private val requestRepository: RequestRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<TrackingUiState>(TrackingUiState.Loading)
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    private val pickupMutex = Mutex()
    private val _isCompletingPickup = MutableStateFlow(false)
    val isCompletingPickup: StateFlow<Boolean> = _isCompletingPickup.asStateFlow()

    private val _pickupCompletionSuccess = MutableStateFlow<String?>(null)
    val pickupCompletionSuccess: StateFlow<String?> = _pickupCompletionSuccess.asStateFlow()

    private val _pickupCompletionError = MutableStateFlow<String?>(null)
    val pickupCompletionError: StateFlow<String?> = _pickupCompletionError.asStateFlow()

    val isPickupCompleted = MutableStateFlow(false)

    private var navigationJob: Job? = null
    private var lastCalculatedOrigin: LatLng? = null

    init {
        loadBookedPickupAndInitialize()
        observePickupLifecycle()
    }

    private fun loadBookedPickupAndInitialize() {
        viewModelScope.launch {
            _uiState.value = TrackingUiState.Loading

            val result = wasteRepository.getBookedPickupForKawadiwala(availabilityId, kawadiwalaId)
            if (result.isFailure) {
                val exception = result.exceptionOrNull()
                when (exception) {
                    is SecurityException -> {
                        _uiState.value = TrackingUiState.AccessDenied(
                            exception.message ?: "Access Denied: You are not authorized to track this location."
                        )
                    }
                    is NoSuchElementException, is IllegalStateException -> {
                        _uiState.value = TrackingUiState.Expired(
                            exception.message ?: "This pickup location is no longer available."
                        )
                    }
                    else -> {
                        _uiState.value = TrackingUiState.Error(
                            exception?.message ?: "Unable to load pickup details."
                        )
                    }
                }
                return@launch
            }

            val availability = result.getOrThrow()
            val destLatLng = LatLng(availability.lat, availability.lng)

            // Get initial Kawadiwala GPS location or default nearby coordinates
            val currentLoc = locationHelper.getCurrentLocation()
            val initialLatLng = if (currentLoc != null) {
                LatLng(currentLoc.latitude, currentLoc.longitude)
            } else {
                Constants.DEFAULT_DEMO_LOCATION
            }

            val hasPermission = locationHelper.hasLocationPermission()
            val isGpsOn = locationHelper.isGpsEnabled()

            _uiState.value = TrackingUiState.Success(
                availability = availability,
                currentPosition = initialLatLng,
                destinationPosition = destLatLng,
                routeInfo = null,
                isNavigating = false,
                isArrived = false,
                isCalculatingRoute = true,
                gpsEnabled = isGpsOn,
                hasLocationPermission = hasPermission
            )

            // Compute initial road route
            computeRoute(initialLatLng, destLatLng, availability.householdName)
        }
    }

    private fun observePickupLifecycle() {
        viewModelScope.launch {
            wasteRepository.observeBookedPickup(availabilityId, kawadiwalaId).collect { availability ->
                if (isPickupCompleted.value) return@collect
                if (availability == null) {
                    // Availability was cancelled, removed, or expired
                    stopNavigation()
                    _uiState.value = TrackingUiState.Expired(
                        "This pickup location is no longer available."
                    )
                }
            }
        }
    }

    private fun computeRoute(origin: LatLng, destination: LatLng, destName: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                if (state is TrackingUiState.Success) {
                    state.copy(isCalculatingRoute = true, routeError = null)
                } else state
            }

            val routeResult = routeRepository.getRoute(origin, destination, destName)
            lastCalculatedOrigin = origin

            if (routeResult.isSuccess) {
                val route = routeResult.getOrThrow()
                _uiState.update { state ->
                    if (state is TrackingUiState.Success) {
                        state.copy(
                            routeInfo = route,
                            isCalculatingRoute = false,
                            routeError = null
                        )
                    } else state
                }
            } else {
                _uiState.update { state ->
                    if (state is TrackingUiState.Success) {
                        state.copy(
                            isCalculatingRoute = false,
                            routeError = routeResult.exceptionOrNull()?.message ?: "Unable to calculate route"
                        )
                    } else state
                }
            }
        }
    }

    fun startNavigation() {
        val currentState = _uiState.value as? TrackingUiState.Success ?: return

        val isGpsOn = locationHelper.isGpsEnabled()
        val hasPermission = locationHelper.hasLocationPermission()

        if (!hasPermission) {
            _uiState.update {
                if (it is TrackingUiState.Success) it.copy(hasLocationPermission = false) else it
            }
            return
        }

        if (!isGpsOn) {
            _uiState.update {
                if (it is TrackingUiState.Success) it.copy(gpsEnabled = false) else it
            }
            return
        }

        _uiState.update {
            if (it is TrackingUiState.Success) it.copy(isNavigating = true, gpsEnabled = true) else it
        }

        navigationJob?.cancel()
        navigationJob = viewModelScope.launch {
            locationHelper.getLocationUpdates(intervalMs = 3000L).collect { location ->
                val newPos = LatLng(location.latitude, location.longitude)
                val state = _uiState.value as? TrackingUiState.Success ?: return@collect

                val dest = state.destinationPosition
                val distanceToDest = calculateDistanceMeters(newPos, dest)

                // Check if arrived within 45m
                val isArrivedNow = distanceToDest <= 45.0

                _uiState.update { s ->
                    if (s is TrackingUiState.Success) {
                        s.copy(
                            currentPosition = newPos,
                            isArrived = if (isArrivedNow) true else s.isArrived
                        )
                    } else s
                }

                // If Kawadiwala moved more than 25 meters from last route calculation, update route
                val lastOrigin = lastCalculatedOrigin
                if (lastOrigin == null || calculateDistanceMeters(newPos, lastOrigin) > 25.0) {
                    computeRoute(newPos, dest, state.availability.householdName)
                }
            }
        }
    }

    fun stopNavigation() {
        navigationJob?.cancel()
        navigationJob = null
        _uiState.update { state ->
            if (state is TrackingUiState.Success) {
                state.copy(isNavigating = false)
            } else state
        }
    }

    fun markAsArrived() {
        _uiState.update { state ->
            if (state is TrackingUiState.Success) {
                state.copy(isArrived = true)
            } else state
        }
    }

    fun dismissArrival() {
        _uiState.update { state ->
            if (state is TrackingUiState.Success) {
                state.copy(isArrived = false)
            } else state
        }
    }

    fun confirmPickedUp() {
        if (_isCompletingPickup.value || isPickupCompleted.value) {
            return // Safe against duplicate taps
        }

        viewModelScope.launch {
            if (!pickupMutex.tryLock()) {
                return@launch // Another completion in flight
            }
            try {
                _isCompletingPickup.value = true
                _pickupCompletionError.value = null

                val result = wasteRepository.markPickupAsPickedUp(availabilityId, kawadiwalaId)
                if (result.isSuccess) {
                    // Stop navigation immediately for this destination
                    stopNavigation()
                    isPickupCompleted.value = true

                    // Mark request completed if requestRepository provided
                    try {
                        requestRepository?.markRequestCompleted(availabilityId, kawadiwalaId)
                    } catch (_: Exception) {}

                    _pickupCompletionSuccess.value = "Pickup completed successfully."
                } else {
                    _pickupCompletionError.value = "Unable to complete pickup. Please try again."
                }
            } catch (e: Exception) {
                _pickupCompletionError.value = "Unable to complete pickup. Please try again."
            } finally {
                _isCompletingPickup.value = false
                pickupMutex.unlock()
            }
        }
    }

    fun clearCompletionError() {
        _pickupCompletionError.value = null
    }

    fun retryRoute() {
        val state = _uiState.value as? TrackingUiState.Success ?: return
        computeRoute(state.currentPosition, state.destinationPosition, state.availability.householdName)
    }

    fun refreshLocationPermission() {
        val hasPermission = locationHelper.hasLocationPermission()
        val isGpsOn = locationHelper.isGpsEnabled()
        _uiState.update { state ->
            if (state is TrackingUiState.Success) {
                state.copy(hasLocationPermission = hasPermission, gpsEnabled = isGpsOn)
            } else state
        }
        if (hasPermission) {
            viewModelScope.launch {
                val loc = locationHelper.getCurrentLocation()
                if (loc != null) {
                    val newPos = LatLng(loc.latitude, loc.longitude)
                    _uiState.update { state ->
                        if (state is TrackingUiState.Success) state.copy(currentPosition = newPos) else state
                    }
                    val state = _uiState.value as? TrackingUiState.Success
                    if (state != null) {
                        computeRoute(newPos, state.destinationPosition, state.availability.householdName)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopNavigation()
    }

    private fun calculateDistanceMeters(p1: LatLng, p2: LatLng): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLng = Math.toRadians(p2.longitude - p1.longitude)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(p1.latitude)) * kotlin.math.cos(Math.toRadians(p2.latitude)) *
            kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }

    companion object {
        fun provideFactory(
            availabilityId: String,
            kawadiwalaId: String,
            wasteRepository: WasteRepository,
            routeRepository: RouteRepository,
            locationHelper: LocationHelper,
            requestRepository: RequestRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return KawadiwalaTrackingViewModel(
                    availabilityId = availabilityId,
                    kawadiwalaId = kawadiwalaId,
                    wasteRepository = wasteRepository,
                    routeRepository = routeRepository,
                    locationHelper = locationHelper,
                    requestRepository = requestRepository
                ) as T
            }
        }
    }
}
