package com.example.data.repository

import android.net.Uri
import com.example.data.model.WasteAvailability
import com.example.data.remote.FirebaseManager
import com.example.domain.model.AvailabilityStatus
import com.example.domain.model.RequestStatus
import com.example.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class WasteRepository {
    private val firestore: FirebaseFirestore? get() = FirebaseManager.getFirestore()
    private val storage: FirebaseStorage? get() = FirebaseManager.getStorage()

    // Local in-memory state flow ensuring reactive updates & offline resilience
    private val availabilitiesMap = mutableMapOf<String, WasteAvailability>()
    private val availabilitiesFlow = MutableStateFlow<List<WasteAvailability>>(emptyList())

    init {
        seedInitialDemoLocations()
    }

    private fun seedInitialDemoLocations() {
        val now = System.currentTimeMillis()
        val dayMs = Constants.AVAILABILITY_DURATION_MS

        val demoItems = listOf(
            WasteAvailability(
                id = "demo_loc_1",
                householdId = "demo_household_1",
                householdName = "Ramesh Sharma (Household 1)",
                lat = 22.445120,
                lng = 88.416210,
                wasteTypes = listOf("Plastic", "Iron"),
                perWasteQuantity = mapOf("Plastic" to 8.0, "Iron" to 12.0),
                availableAt = now - (2 * 3600 * 1000L), // 2 hours ago
                expiresAt = now + dayMs - (2 * 3600 * 1000L),
                status = AvailabilityStatus.AVAILABLE
            ),
            WasteAvailability(
                id = "demo_loc_2",
                householdId = "demo_household_2",
                householdName = "Ananya Sen (Household 2)",
                lat = 22.441840,
                lng = 88.413920,
                wasteTypes = listOf("Paper", "Glass"),
                perWasteQuantity = mapOf("Paper" to 15.0, "Glass" to 6.0),
                availableAt = now - (1 * 3600 * 1000L),
                expiresAt = now + dayMs - (1 * 3600 * 1000L),
                status = AvailabilityStatus.AVAILABLE
            ),
            WasteAvailability(
                id = "demo_loc_3",
                householdId = "demo_household_3",
                householdName = "Bikash Mukherjee (Household 3)",
                lat = 22.446250,
                lng = 88.418530,
                wasteTypes = listOf("Electronics", "TV"),
                perWasteQuantity = mapOf("Electronics" to 2.0, "TV" to 1.0),
                availableAt = now - (4 * 3600 * 1000L),
                expiresAt = now + dayMs - (4 * 3600 * 1000L),
                status = AvailabilityStatus.AVAILABLE
            ),
            WasteAvailability(
                id = "demo_loc_4",
                householdId = "demo_household_4",
                householdName = "Pooja Banerjee (Household 4)",
                lat = 22.440210,
                lng = 88.417150,
                wasteTypes = listOf("Plastic", "Metal"),
                perWasteQuantity = mapOf("Plastic" to 5.0, "Metal" to 7.0),
                availableAt = now - (30 * 60 * 1000L),
                expiresAt = now + dayMs - (30 * 60 * 1000L),
                status = AvailabilityStatus.AVAILABLE
            ),
            WasteAvailability(
                id = "demo_booked_mandal",
                householdId = "demo_mandal_bhavan",
                householdName = "Mandal Bhavan",
                lat = 22.443500,
                lng = 88.416800,
                wasteTypes = listOf("Plastic", "Iron", "Paper"),
                perWasteQuantity = mapOf("Plastic" to 12.0, "Iron" to 18.0, "Paper" to 25.0),
                availableAt = now - (1 * 3600 * 1000L),
                expiresAt = now + dayMs - (1 * 3600 * 1000L),
                status = AvailabilityStatus.BOOKED,
                acceptedKawadiwalaId = "demo_imran"
            ),
            WasteAvailability(
                id = "demo_booked_santi",
                householdId = "demo_santi_kunja",
                householdName = "Santi Kunja",
                lat = 22.441200,
                lng = 88.413900,
                wasteTypes = listOf("Glass", "Metal", "Electronics"),
                perWasteQuantity = mapOf("Glass" to 8.0, "Metal" to 10.0, "Electronics" to 2.0),
                availableAt = now - (45 * 60 * 1000L),
                expiresAt = now + dayMs - (45 * 60 * 1000L),
                status = AvailabilityStatus.BOOKED,
                acceptedKawadiwalaId = "demo_imran"
            )
        )
        demoItems.forEach { availabilitiesMap[it.id] = it }
        availabilitiesFlow.value = availabilitiesMap.values.toList()
    }

    suspend fun createAvailability(
        householdId: String,
        householdName: String,
        lat: Double,
        lng: Double,
        wasteTypes: List<String>,
        perWasteQuantity: Map<String, Double>,
        photoUrl: String?,
        locationType: String = "CURRENT"
    ): Result<WasteAvailability> {
        if (wasteTypes.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please select at least one waste category"))
        }

        val now = System.currentTimeMillis()
        val expiresAt = now + Constants.AVAILABILITY_DURATION_MS // Strictly 24 hours
        val id = "avail_${UUID.randomUUID().toString().take(8)}"

        val availability = WasteAvailability(
            id = id,
            householdId = householdId,
            householdName = householdName,
            lat = lat,
            lng = lng,
            wasteTypes = wasteTypes,
            perWasteQuantity = perWasteQuantity,
            photoUrl = photoUrl,
            locationType = locationType,
            availableAt = now,
            expiresAt = expiresAt,
            status = AvailabilityStatus.AVAILABLE,
            createdAt = now,
            updatedAt = now
        )

        // Save locally first for instant UI response
        availabilitiesMap[id] = availability
        availabilitiesFlow.value = availabilitiesMap.values.toList()

        val firestoreInstance = firestore
        if (firestoreInstance != null) {
            try {
                firestoreInstance.collection(Constants.COLLECTION_AVAILABILITY)
                    .document(id)
                    .set(availability.toMap())
                    .await()
            } catch (_: Exception) {
                // Keep local on offline/network exception
            }
        }

        return Result.success(availability)
    }

    suspend fun removeAvailability(availabilityId: String, householdId: String): Result<WasteAvailability> {
        val current = availabilitiesMap[availabilityId]
        if (current != null) {
            if (current.householdId != householdId) {
                return Result.failure(SecurityException("Unauthorized: You can only remove your own waste availability"))
            }
            val updated = current.copy(
                status = AvailabilityStatus.REMOVED,
                updatedAt = System.currentTimeMillis()
            )
            availabilitiesMap[availabilityId] = updated
            availabilitiesFlow.value = availabilitiesMap.values.toList()

            val firestoreInstance = firestore
            if (firestoreInstance != null) {
                try {
                    firestoreInstance.collection(Constants.COLLECTION_AVAILABILITY)
                        .document(availabilityId)
                        .update("status", AvailabilityStatus.REMOVED.name, "updatedAt", System.currentTimeMillis())
                        .await()
                } catch (_: Exception) {}
            }
            return Result.success(updated)
        }

        val firestoreInstance = firestore
        if (firestoreInstance != null) {
            try {
                val doc = firestoreInstance.collection(Constants.COLLECTION_AVAILABILITY)
                    .document(availabilityId)
                    .get()
                    .await()
                if (!doc.exists()) {
                    return Result.failure(NoSuchElementException("Waste availability not found"))
                }
                val docHouseholdId = doc.getString("householdId") ?: ""
                if (docHouseholdId != householdId) {
                    return Result.failure(SecurityException("Unauthorized: You can only remove your own waste availability"))
                }
                val item = WasteAvailability.fromMap(doc.id, doc.data ?: emptyMap()).copy(
                    status = AvailabilityStatus.REMOVED,
                    updatedAt = System.currentTimeMillis()
                )
                firestoreInstance.collection(Constants.COLLECTION_AVAILABILITY)
                    .document(availabilityId)
                    .update("status", AvailabilityStatus.REMOVED.name, "updatedAt", System.currentTimeMillis())
                    .await()
                availabilitiesMap[availabilityId] = item
                availabilitiesFlow.value = availabilitiesMap.values.toList()
                return Result.success(item)
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }

        return Result.failure(NoSuchElementException("Waste availability not found"))
    }

    suspend fun updateAvailabilityStatus(
        availabilityId: String,
        status: AvailabilityStatus,
        acceptedKawadiwalaId: String? = null
    ): Result<Unit> {
        val current = availabilitiesMap[availabilityId]
        if (current != null) {
            val updated = current.copy(
                status = status,
                acceptedKawadiwalaId = acceptedKawadiwalaId ?: current.acceptedKawadiwalaId,
                updatedAt = System.currentTimeMillis()
            )
            availabilitiesMap[availabilityId] = updated
            availabilitiesFlow.value = availabilitiesMap.values.toList()
        }
        return Result.success(Unit)
    }

    fun getHouseholdAvailabilities(householdId: String): Flow<List<WasteAvailability>> = callbackFlow {
        val firestoreInstance = firestore
        var registration: ListenerRegistration? = null

        if (firestoreInstance != null) {
            registration = firestoreInstance.collection(Constants.COLLECTION_AVAILABILITY)
                .whereEqualTo("householdId", householdId)
                .addSnapshotListener { snapshots, _ ->
                    if (snapshots != null) {
                        val list = snapshots.documents.map { doc ->
                            WasteAvailability.fromMap(doc.id, doc.data ?: emptyMap())
                        }.sortedByDescending { it.createdAt }

                        list.forEach { availabilitiesMap[it.id] = it }
                        availabilitiesFlow.value = availabilitiesMap.values.toList()
                        trySend(list)
                    } else {
                        val filtered = availabilitiesMap.values.filter { it.householdId == householdId }
                            .sortedByDescending { it.createdAt }
                        trySend(filtered)
                    }
                }
        } else {
            val job = launch {
                availabilitiesFlow.collect { all ->
                    val filtered = all.filter { it.householdId == householdId }
                        .sortedByDescending { it.createdAt }
                    trySend(filtered)
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        awaitClose { registration?.remove() }
    }

    fun getActiveAvailabilitiesForKawadiwala(kawadiwalaId: String): Flow<List<WasteAvailability>> = callbackFlow {
        val firestoreInstance = firestore
        var registration: ListenerRegistration? = null

        if (firestoreInstance != null) {
            registration = firestoreInstance.collection(Constants.COLLECTION_AVAILABILITY)
                .addSnapshotListener { snapshots, _ ->
                    val now = System.currentTimeMillis()
                    if (snapshots != null) {
                        val all = snapshots.documents.map { doc ->
                            WasteAvailability.fromMap(doc.id, doc.data ?: emptyMap())
                        }
                        all.forEach { availabilitiesMap[it.id] = it }
                        availabilitiesFlow.value = availabilitiesMap.values.toList()

                        val visible = filterVisibleForKawadiwala(all, kawadiwalaId, now)
                        trySend(visible)
                    } else {
                        val visible = filterVisibleForKawadiwala(availabilitiesMap.values.toList(), kawadiwalaId, now)
                        trySend(visible)
                    }
                }
        } else {
            val job = launch {
                availabilitiesFlow.collect { all ->
                    val now = System.currentTimeMillis()
                    val visible = filterVisibleForKawadiwala(all, kawadiwalaId, now)
                    trySend(visible)
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        awaitClose { registration?.remove() }
    }

    private fun filterVisibleForKawadiwala(
        list: List<WasteAvailability>,
        kawadiwalaId: String,
        now: Long
    ): List<WasteAvailability> {
        return list.filter { item ->
            val isExpired = now > item.expiresAt || item.status == AvailabilityStatus.EXPIRED
            val isRemoved = item.status == AvailabilityStatus.REMOVED
            val isPickedUp = item.status == AvailabilityStatus.PICKED_UP

            if (isExpired || isRemoved || isPickedUp) {
                false
            } else when (item.status) {
                AvailabilityStatus.AVAILABLE -> true
                AvailabilityStatus.BOOKED -> item.acceptedKawadiwalaId == kawadiwalaId // Booked by THIS Kawadiwala
                else -> false
            }
        }
    }

    suspend fun uploadPhoto(imageUri: Uri): Result<String> {
        val storageInstance = storage
        if (storageInstance != null) {
            return try {
                val ref = storageInstance.reference.child("waste_photos/${UUID.randomUUID()}.jpg")
                ref.putFile(imageUri).await()
                val downloadUrl = ref.downloadUrl.await().toString()
                Result.success(downloadUrl)
            } catch (e: Exception) {
                Result.success(imageUri.toString()) // Fallback to local URI
            }
        }
        return Result.success(imageUri.toString())
    }

    suspend fun getBookedPickupForKawadiwala(
        availabilityId: String,
        kawadiwalaId: String
    ): Result<WasteAvailability> {
        val now = System.currentTimeMillis()
        var item: WasteAvailability? = availabilitiesMap[availabilityId]

        val firestoreInstance = firestore
        if (item == null && firestoreInstance != null) {
            try {
                val doc = firestoreInstance.collection(Constants.COLLECTION_AVAILABILITY)
                    .document(availabilityId)
                    .get()
                    .await()
                if (doc.exists()) {
                    item = WasteAvailability.fromMap(doc.id, doc.data ?: emptyMap())
                    availabilitiesMap[item.id] = item
                }
            } catch (_: Exception) {}
        }

        if (item == null) {
            return Result.failure(NoSuchElementException("Pickup location not found"))
        }

        if (item.status != AvailabilityStatus.BOOKED) {
            return Result.failure(IllegalStateException("Pickup location is not in booked status"))
        }

        if (now > item.expiresAt || item.status == AvailabilityStatus.EXPIRED || item.status == AvailabilityStatus.REMOVED || item.status == AvailabilityStatus.PICKED_UP) {
            return Result.failure(IllegalStateException("This pickup location is no longer available"))
        }

        if (item.acceptedKawadiwalaId != kawadiwalaId) {
            return Result.failure(SecurityException("Access Denied: You are not the accepted Kawadiwala for this booked pickup"))
        }

        return Result.success(item)
    }

    fun observeBookedPickup(
        availabilityId: String,
        kawadiwalaId: String
    ): Flow<WasteAvailability?> = callbackFlow {
        val firestoreInstance = firestore
        var registration: ListenerRegistration? = null

        if (firestoreInstance != null) {
            registration = firestoreInstance.collection(Constants.COLLECTION_AVAILABILITY)
                .document(availabilityId)
                .addSnapshotListener { snapshot, _ ->
                    val now = System.currentTimeMillis()
                    if (snapshot != null && snapshot.exists()) {
                        val item = WasteAvailability.fromMap(snapshot.id, snapshot.data ?: emptyMap())
                        availabilitiesMap[item.id] = item
                        if (item.status == AvailabilityStatus.BOOKED &&
                            item.acceptedKawadiwalaId == kawadiwalaId &&
                            now <= item.expiresAt
                        ) {
                            trySend(item)
                        } else {
                            trySend(null)
                        }
                    } else {
                        val local = availabilitiesMap[availabilityId]
                        if (local != null && local.status == AvailabilityStatus.BOOKED &&
                            local.acceptedKawadiwalaId == kawadiwalaId &&
                            now <= local.expiresAt
                        ) {
                            trySend(local)
                        } else {
                            trySend(null)
                        }
                    }
                }
        } else {
            val job = launch {
                availabilitiesFlow.collect { list ->
                    val now = System.currentTimeMillis()
                    val item = list.find { it.id == availabilityId }
                    if (item != null && item.status == AvailabilityStatus.BOOKED &&
                        item.acceptedKawadiwalaId == kawadiwalaId &&
                        now <= item.expiresAt
                    ) {
                        trySend(item)
                    } else {
                        trySend(null)
                    }
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        awaitClose { registration?.remove() }
    }

    suspend fun markPickupAsPickedUp(
        availabilityId: String,
        kawadiwalaId: String
    ): Result<Unit> {
        val now = System.currentTimeMillis()
        val firestoreInstance = firestore

        if (firestoreInstance != null) {
            try {
                firestoreInstance.runTransaction { transaction ->
                    val docRef = firestoreInstance.collection(Constants.COLLECTION_AVAILABILITY).document(availabilityId)
                    val snapshot = transaction.get(docRef)
                    if (!snapshot.exists()) {
                        throw NoSuchElementException("Pickup location not found")
                    }
                    val currentStatus = snapshot.getString("status") ?: AvailabilityStatus.AVAILABLE.name
                    val acceptedKawadiwalaId = snapshot.getString("acceptedKawadiwalaId")

                    if (acceptedKawadiwalaId != kawadiwalaId) {
                        throw SecurityException("Access Denied: You are not the accepted Kawadiwala for this booked pickup")
                    }

                    if (currentStatus == AvailabilityStatus.PICKED_UP.name) {
                        // Already completed, idempotent safe
                        return@runTransaction
                    }

                    if (currentStatus != AvailabilityStatus.BOOKED.name) {
                        throw IllegalStateException("Pickup location is not in booked status")
                    }

                    transaction.update(
                        docRef,
                        mapOf(
                            "status" to AvailabilityStatus.PICKED_UP.name,
                            "pickedUpAt" to now,
                            "updatedAt" to now
                        )
                    )
                }.await()

                // Also update corresponding accepted request in requests collection if any
                try {
                    val requestsSnapshot = firestoreInstance.collection(Constants.COLLECTION_REQUESTS)
                        .whereEqualTo("wasteAvailabilityId", availabilityId)
                        .whereEqualTo("kawadiwalaId", kawadiwalaId)
                        .whereEqualTo("status", RequestStatus.ACCEPTED.name)
                        .get().await()

                    for (doc in requestsSnapshot.documents) {
                        doc.reference.update(
                            mapOf(
                                "status" to RequestStatus.COMPLETED.name,
                                "updatedAt" to now
                            )
                        )
                    }
                } catch (_: Exception) {}

            } catch (e: Exception) {
                // If firestore fails (e.g. offline/no network), update in local memory safely
                val localResult = executeLocalPickupCompletion(availabilityId, kawadiwalaId, now)
                if (localResult.isFailure) return localResult
            }
        }

        // Local state update (for offline / instant reactive consistency)
        val localResult = executeLocalPickupCompletion(availabilityId, kawadiwalaId, now)
        if (localResult.isFailure) return localResult

        return Result.success(Unit)
    }

    private fun executeLocalPickupCompletion(
        availabilityId: String,
        kawadiwalaId: String,
        now: Long
    ): Result<Unit> {
        val current = availabilitiesMap[availabilityId]
            ?: return Result.failure(NoSuchElementException("Pickup location not found"))

        if (current.acceptedKawadiwalaId != kawadiwalaId) {
            return Result.failure(SecurityException("Access Denied: You are not the accepted Kawadiwala for this booked pickup"))
        }

        if (current.status == AvailabilityStatus.PICKED_UP) {
            return Result.success(Unit) // Idempotent duplicate call safe
        }

        if (current.status != AvailabilityStatus.BOOKED) {
            return Result.failure(IllegalStateException("Pickup location is not in booked status"))
        }

        val updated = current.copy(
            status = AvailabilityStatus.PICKED_UP,
            pickedUpAt = now,
            updatedAt = now
        )
        availabilitiesMap[availabilityId] = updated
        availabilitiesFlow.value = availabilitiesMap.values.toList()

        return Result.success(Unit)
    }
}

