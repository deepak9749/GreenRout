package com.example.presentation.household

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.KawadiwalaRequest
import com.example.data.model.WasteAvailability
import com.example.domain.model.AvailabilityStatus
import com.example.domain.model.RequestStatus
import com.example.presentation.components.AvailabilityStatusBadge
import com.example.presentation.components.EmptyStateView
import com.example.presentation.components.GreenRouteTopBar
import com.example.presentation.components.RequestStatusBadge
import com.example.utils.DateUtils

@Composable
fun HouseholdDashboardScreen(
    householdViewModel: HouseholdViewModel,
    onNavigateToMakeAvailable: () -> Unit,
    onNavigateToMarket: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onOpenLanguage: () -> Unit
) {
    val availabilities by householdViewModel.availabilities.collectAsState()
    val receivedRequests by householdViewModel.receivedRequests.collectAsState()
    val operationState by householdViewModel.operationState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(operationState) {
        when (operationState) {
            is OperationState.Success -> {
                snackbarHostState.showSnackbar((operationState as OperationState.Success).message)
                householdViewModel.clearOperationState()
            }
            is OperationState.Error -> {
                snackbarHostState.showSnackbar((operationState as OperationState.Error).message)
                householdViewModel.clearOperationState()
            }
            else -> {}
        }
    }

    val activeAvailability = availabilities.firstOrNull { it.status == AvailabilityStatus.AVAILABLE && !it.isExpired }
        ?: availabilities.firstOrNull { it.status == AvailabilityStatus.BOOKED }

    var availabilityPendingRemoval by remember { mutableStateOf<WasteAvailability?>(null) }

    if (availabilityPendingRemoval != null) {
        val target = availabilityPendingRemoval!!
        val isBooked = target.status == AvailabilityStatus.BOOKED

        AlertDialog(
            onDismissRequest = { availabilityPendingRemoval = null },
            title = {
                Text(
                    text = if (isBooked) stringResource(R.string.cancel_pickup)
                    else stringResource(R.string.confirm_remove_availability_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = if (isBooked) stringResource(R.string.confirm_cancel_booked_msg)
                    else stringResource(R.string.confirm_remove_availability_msg),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idToRemove = target.id
                        availabilityPendingRemoval = null
                        householdViewModel.removeAvailability(idToRemove)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_remove_button")
                ) {
                    Text(stringResource(R.string.remove_button))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { availabilityPendingRemoval = null },
                    modifier = Modifier.testTag("cancel_remove_dialog_button")
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            GreenRouteTopBar(
                title = stringResource(R.string.household_dashboard),
                subtitle = stringResource(R.string.tagline),
                onOpenLanguage = onOpenLanguage,
                onOpenNotifications = onNavigateToNotifications
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Schedule, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToMakeAvailable,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.make_waste_available), fontWeight = FontWeight.Bold)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                // Quick CTA Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = stringResource(R.string.core_message),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "List your recyclable scrap with 1 tap. Local Kawadiwalas will discover your location and submit competitive pickup offers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onNavigateToMakeAvailable,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.make_waste_available))
                        }
                    }
                }
            }

            // Active Availability Section
            item {
                Text(
                    text = stringResource(R.string.my_availability),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (activeAvailability == null) {
                item {
                    EmptyStateView(
                        title = "No Active Waste Listing",
                        message = stringResource(R.string.no_active_availability),
                        icon = Icons.Default.Schedule
                    )
                }
            } else {
                item {
                    ActiveAvailabilityCard(
                        availability = activeAvailability,
                        onRemove = { availabilityPendingRemoval = activeAvailability },
                        isLoading = operationState is OperationState.Loading
                    )
                }
            }

            // Received Requests Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.received_requests),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (receivedRequests.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${receivedRequests.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (activeAvailability == null || receivedRequests.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "No Requests Yet",
                        message = stringResource(R.string.no_requests_yet),
                        icon = Icons.Default.Storefront
                    )
                }
            } else {
                // Strict rule: DO NOT rank offers, DO NOT label any as "Best". Household compares transparently.
                items(receivedRequests, key = { it.id }) { request ->
                    HouseholdRequestCard(
                        request = request,
                        isBooked = (activeAvailability.status == AvailabilityStatus.BOOKED),
                        onAccept = {
                            householdViewModel.acceptRequest(
                                availabilityId = activeAvailability.id,
                                requestId = request.id,
                                kawadiwalaId = request.kawadiwalaId
                            )
                        },
                        isLoading = operationState is OperationState.Loading
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun ActiveAvailabilityCard(
    availability: WasteAvailability,
    onRemove: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvailabilityStatusBadge(status = availability.status)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = DateUtils.formatRemainingTime(availability.expiresAt),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Categories
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                availability.wasteTypes.forEach { type ->
                    val qty = availability.perWasteQuantity[type]
                    val label = if (qty != null && qty > 0) "$type (${qty.toInt()}kg)" else type
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (availability.locationType == "CUSTOM") Color(0xFF1E88E5) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val locLabel = if (availability.locationType == "CUSTOM") stringResource(R.string.custom_location) else stringResource(R.string.current_location)
                Text(
                    text = "$locLabel: ${String.format("%.4f", availability.lat)}, ${String.format("%.4f", availability.lng)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (availability.status == AvailabilityStatus.AVAILABLE || availability.status == AvailabilityStatus.BOOKED) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onRemove,
                    enabled = !isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("remove_availability_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (availability.status == AvailabilityStatus.BOOKED)
                            stringResource(R.string.cancel_pickup)
                        else
                            stringResource(R.string.remove_waste)
                    )
                }
            }
        }
    }
}

@Composable
private fun HouseholdRequestCard(
    request: KawadiwalaRequest,
    isBooked: Boolean,
    onAccept: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (request.status == RequestStatus.ACCEPTED) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (request.status == RequestStatus.ACCEPTED) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = request.kawadiwalaName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Estimated arrival: ${request.estimatedArrivalHours} Hour${if (request.estimatedArrivalHours > 1) "s" else ""}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RequestStatusBadge(status = request.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Breakdown of independent offers per waste type
            Text(
                text = "Offered Prices:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            request.offersPerWaste.forEach { (waste, amount) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "• $waste", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "₹${amount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (request.status == RequestStatus.PENDING && !isBooked) {
                Button(
                    onClick = onAccept,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.accept), fontWeight = FontWeight.Bold)
                }
            } else if (request.status == RequestStatus.ACCEPTED) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓ Accepted by you. Collector scheduled for arrival.",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            } else if (request.status == RequestStatus.COMPLETED) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓ Scrap Collected & Picked Up",
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
