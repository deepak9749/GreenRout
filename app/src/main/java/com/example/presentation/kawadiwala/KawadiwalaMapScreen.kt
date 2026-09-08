package com.example.presentation.kawadiwala

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.AppThemeMode
import com.example.data.local.LocalThemePreferences
import com.example.domain.model.AvailabilityStatus
import com.example.presentation.components.GreenRouteTopBar
import com.example.utils.Constants
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@Composable
fun KawadiwalaMapScreen(
    kawadiwalaViewModel: KawadiwalaViewModel,
    onNavigateToRequests: () -> Unit,
    onNavigateToMarket: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onOpenLanguage: () -> Unit,
    onNavigateToTracking: (String) -> Unit = {}
) {
    val activeAvailabilities by kawadiwalaViewModel.activeAvailabilities.collectAsState()
    val selectedAvailability by kawadiwalaViewModel.selectedAvailability.collectAsState()
    val uiState by kawadiwalaViewModel.uiState.collectAsState()
    val userLocation by kawadiwalaViewModel.userCurrentLocation.collectAsState()

    val bookedPickups = remember(activeAvailabilities) {
        activeAvailabilities.filter { it.status == AvailabilityStatus.BOOKED }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = kawadiwalaViewModel.defaultMapPosition
    }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is KawadiwalaUiState.Success) {
            snackbarHostState.showSnackbar((uiState as KawadiwalaUiState.Success).message)
            kawadiwalaViewModel.clearUiState()
        }
    }

    Scaffold(
        topBar = {
            GreenRouteTopBar(
                title = stringResource(R.string.green_route_scrap_map),
                subtitle = stringResource(R.string.demo_locality),
                onOpenLanguage = onOpenLanguage,
                onOpenNotifications = onNavigateToNotifications
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                    label = { Text(stringResource(R.string.nav_map)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToRequests,
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = "Requests") },
                    label = { Text(stringResource(R.string.nav_requests)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToMarket,
                    icon = { Icon(Icons.Default.Storefront, contentDescription = "Market") },
                    label = { Text(stringResource(R.string.nav_market)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToNotifications,
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Notifications") },
                    label = { Text(stringResource(R.string.notifications)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToProfile,
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text(stringResource(R.string.profile)) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // REAL GOOGLE MAP
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = true,
                    myLocationButtonEnabled = false
                ),
                properties = MapProperties(
                    isMyLocationEnabled = false
                )
            ) {
                // Render markers for all available household pickups
                activeAvailabilities.forEach { availability ->
                    val isBookedByMe = (availability.status == AvailabilityStatus.BOOKED)
                    val markerColor = if (isBookedByMe) {
                        BitmapDescriptorFactory.HUE_ORANGE
                    } else {
                        BitmapDescriptorFactory.HUE_GREEN
                    }

                    val titleText = if (isBookedByMe) {
                        "⭐ Booked by You - ${availability.householdName}"
                    } else {
                        availability.householdName
                    }

                    Marker(
                        state = MarkerState(position = LatLng(availability.lat, availability.lng)),
                        title = titleText,
                        snippet = availability.wasteTypes.joinToString(", "),
                        icon = BitmapDescriptorFactory.defaultMarker(markerColor),
                        onClick = {
                            kawadiwalaViewModel.selectMarker(availability)
                            true
                        }
                    )
                }
            }

            // Top Status & Booked Locations Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Floating Status Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.pickups_available_count, activeAvailabilities.size),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Tap any green marker to inspect waste & send offer",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val themePrefs = runCatching { LocalThemePreferences.current }.getOrNull()
                            val themeMode by themePrefs?.themeMode?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(AppThemeMode.LIGHT) }
                            val isDark = themeMode == AppThemeMode.DARK

                            IconButton(
                                onClick = { themePrefs?.toggleTheme() },
                                modifier = Modifier.testTag("map_theme_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    tint = if (isDark) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(
                                                Constants.DEFAULT_DEMO_LOCATION,
                                                Constants.DEFAULT_MAP_ZOOM
                                            )
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationSearching,
                                    contentDescription = "Reset View",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Dedicated Booked Locations List (ORANGE) with "TRACK LOCATION"
                if (bookedPickups.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(14.dp))
                            .testTag("booked_locations_container"),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF8E1).copy(alpha = 0.96f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE65100))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.active_booked_pickups) + " (${bookedPickups.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFE65100)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            bookedPickups.forEach { booked ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(10.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "🟠 ${booked.householdName}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = booked.wasteTypes.joinToString(", "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { onNavigateToTracking(booked.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("track_location_btn_${booked.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Navigation,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = stringResource(R.string.track_location),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating My Location Button
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        val target = userLocation ?: Constants.DEFAULT_DEMO_LOCATION
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(target, 16f)
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 20.dp)
                    .size(52.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Current Location")
            }

            // Bottom Sheet / Dialog for Selected Marker
            if (selectedAvailability != null) {
                RequestOfferDialog(
                    availability = selectedAvailability!!,
                    kawadiwalaViewModel = kawadiwalaViewModel,
                    onDismiss = { kawadiwalaViewModel.dismissMarkerDialog() },
                    onNavigateToTracking = onNavigateToTracking
                )
            }
        }
    }
}
