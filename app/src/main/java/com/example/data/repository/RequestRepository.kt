package com.example.data.repository

import com.example.data.model.KawadiwalaRequest
import com.example.data.remote.FirebaseManager
import com.example.domain.model.AvailabilityStatus
import com.example.domain.model.RequestStatus
import com.example.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RequestRepository(
    private val wasteRepository: WasteRepository,
    private val notificationRepository: NotificationRepository
) {
    private val firestore: FirebaseFirestore? get() = FirebaseManager.getFirestore()
    private val localRequestsMap = mutableMapOf<String, KawadiwalaRequest>()
    private val requestsFlow = MutableStateFlow<List<KawadiwalaRequest>>(emptyList())
    private val acceptanceMutex = Mutex()

    init {
        seedInitialDemoRequests()
    }

    private fun seedInitialDemoRequests() {
        val r1 = KawadiwalaRequest(
            id = "req_demo_1",
            wasteAvailabilityId = "demo_loc_1",
            kawadiwalaId = "demo_kawadiwala_1",
            kawadiwalaName = "Subhash Mondal (Collector)",
            offersPerWaste = mapOf("Plastic" to 300.0, "Iron" to 500.0),
            estimatedArrivalHours = 1,
            status = RequestStatus.PENDING,
            createdAt = System.currentTimeMillis() - 45 * 60 * 1000L
        )
        val r2 = KawadiwalaRequest(
            id = "req_demo_2",
            wasteAvailabilityId = "demo_loc_1",
            kawadiwalaId = "demo_kawadiwala_2",
            kawadiwalaName = "Arun Roy (Collector)",
            offersPerWaste = mapOf("Plastic" to 340.0, "Iron" to 480.0),
            estimatedArrivalHours = 2,
            status = RequestStatus.PENDING,
            createdAt = System.currentTimeMillis() - 30 * 60 * 1000L
        )
        val r3 = KawadiwalaRequest(
            id = "req_demo_3",
            wasteAvailabilityId = "demo_loc_1",
            kawadiwalaId = "demo_kawadiwala_3",
            kawadiwalaName = "Debashis Pal (Collector)",
            offersPerWaste = mapOf("Plastic" to 320.0, "Iron" to 520.0),
            estimatedArrivalHours = 3,
            status = RequestStatus.PENDING,
            createdAt = System.currentTimeMillis() - 15 * 60 * 1000L
        )
        val rImranMandal = KawadiwalaRequest(
            id = "req_imran_mandal",
            wasteAvailabilityId = "demo_booked_mandal",
            kawadiwalaId = "demo_imran",
            kawadiwalaName = "Imran",
            offersPerWaste = mapOf("Plastic" to 420.0, "Iron" to 750.0, "Paper" to 280.0),
            estimatedArrivalHours = 1,
            status = RequestStatus.ACCEPTED,
            createdAt = System.currentTimeMillis() - 60 * 60 * 1000L,
            updatedAt = System.currentTimeMillis() - 40 * 60 * 1000L
        )
        val rImranSanti = KawadiwalaRequest(
            id = "req_imran_santi",
            wasteAvailabilityId = "demo_booked_santi",
            kawadiwalaId = "demo_imran",
            kawadiwalaName = "Imran",
            offersPerWaste = mapOf("Glass" to 150.0, "Metal" to 400.0, "Electronics" to 600.0),
            estimatedArrivalHours = 2,
            status = RequestStatus.ACCEPTED,
            createdAt = System.currentTimeMillis() - 45 * 60 * 1000L,
            updatedAt = System.currentTimeMillis() - 30 * 60 * 1000L
        )

        listOf(r1, r2, r3, rImranMandal, rImranSanti).forEach { localRequestsMap[it.id] = it }
        requestsFlow.value = localRequestsMap.values.toList()
    }

    suspend fun submitRequest(
        wasteAvailabilityId: String,
        kawadiwalaId: String,
        kawadiwalaName: String,
        offersPerWaste: Map<String, Double>,
        estimatedArrivalHours: Int
    ): Result<KawadiwalaRequest> {
        if (offersPerWaste.isEmpty() || offersPerWaste.values.any { it <= 0 }) {
            return Result.failure(IllegalArgumentException("Please enter valid offer amounts"))
        }
        if (estimatedArrivalHours !in 1..3) {
            return Result.failure(IllegalArgumentException("Estimated arrival must be 1, 2, or 3 hours"))
        }

        // Prevent duplicate pending request from same Kawadiwala
        val existing = localRequestsMap.values.firstOrNull {
            it.wasteAvailabilityId == wasteAvailabilityId &&
            it.kawadiwalaId == kawadiwalaId &&
            it.status == RequestStatus.PENDING
        }
        if (existing != null) {
            return Result.failure(IllegalStateException("You have already submitted an active request for this pickup"))
        }

        val id = "req_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val request = KawadiwalaRequest(
            id = id,
            wasteAvailabilityId = wasteAvailabilityId,
            kawadiwalaId = kawadiwalaId,
            kawadiwalaName = kawadiwalaName,
            offersPerWaste = offersPerWaste,
            estimatedArrivalHours = estimatedArrivalHours,
            status = RequestStatus.PENDING,
            createdAt = now,
            updatedAt = now
        )

        localRequestsMap[id] = request
        requestsFlow.value = localRequestsMap.values.toList()

        val firestoreInstance = firestore
        if (firestoreInstance != null) {
            try {
                firestoreInstance.collection(Constants.COLLECTION_REQUESTS)
                    .document(id)
                    .set(request.toMap())
                    .await()
            } catch (_: Exception) {}
        }

        // Notify household of new request
        notificationRepository.sendNotification(
            userId = "household_target", // routed to availability household
            title = "New Kawadiwala Request Received",
            message = "$kawadiwalaName submitted a pickup offer for your waste (${estimatedArrivalHours}h arrival).",
            type = Constants.TYPE_REQUEST_RECEIVED
        )

        return Result.success(request)
    }

    fun getRequestsForAvailability(availabilityId: String): Flow<List<KawadiwalaRequest>> = callbackFlow {
        val firestoreInstance = firestore
        var registration: ListenerRegistration? = null

        if (firestoreInstance != null) {
            registration = firestoreInstance.collection(Constants.COLLECTION_REQUESTS)
                .whereEqualTo("wasteAvailabilityId", availabilityId)
                .addSnapshotListener { snapshots, _ ->
                    if (snapshots != null) {
                        val list = snapshots.documents.map {
                            KawadiwalaRequest.fromMap(it.id, it.data ?: emptyMap())
                        }
                        list.forEach { localRequestsMap[it.id] = it }
                        requestsFlow.value = localRequestsMap.values.toList()
                        trySend(list)
                    } else {
                        val filtered = localRequestsMap.values.filter { it.wasteAvailabilityId == availabilityId }
                        trySend(filtered)
                    }
                }
        } else {
            val job = launch {
                requestsFlow.collect { all ->
                    val filtered = all.filter { it.wasteAvailabilityId == availabilityId }
                    trySend(filtered)
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        awaitClose { registration?.remove() }
    }

    fun getRequestsForKawadiwala(kawadiwalaId: String): Flow<List<KawadiwalaRequest>> = callbackFlow {
        val firestoreInstance = firestore
        var registration: ListenerRegistration? = null

        if (firestoreInstance != null) {
            registration = firestoreInstance.collection(Constants.COLLECTION_REQUESTS)
                .whereEqualTo("kawadiwalaId", kawadiwalaId)
                .addSnapshotListener { snapshots, _ ->
                    if (snapshots != null) {
                        val list = snapshots.documents.map {
                            KawadiwalaRequest.fromMap(it.id, it.data ?: emptyMap())
                        }.sortedByDescending { it.createdAt }

                        list.forEach { localRequestsMap[it.id] = it }
                        requestsFlow.value = localRequestsMap.values.toList()
                        trySend(list)
                    } else {
                        val filtered = localRequestsMap.values.filter { it.kawadiwalaId == kawadiwalaId }
                            .sortedByDescending { it.createdAt }
                        trySend(filtered)
                    }
                }
        } else {
            val job = launch {
                requestsFlow.collect { all ->
                    val filtered = all.filter { it.kawadiwalaId == kawadiwalaId }
                        .sortedByDescending { it.createdAt }
                    trySend(filtered)
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        awaitClose { registration?.remove() }
    }

    suspend fun acceptRequestAtomic(
        availabilityId: String,
        selectedRequestId: String,
        selectedKawadiwalaId: String
    ): Result<Unit> = acceptanceMutex.withLock {
        val now = System.currentTimeMillis()
        val firestoreInstance = firestore

        if (firestoreInstance != null) {
            try {
                firestoreInstance.runTransaction { transaction ->
                    val availRef = firestoreInstance.collection(Constants.COLLECTION_AVAILABILITY).document(availabilityId)
                    val availDoc = transaction.get(availRef)
                    if (!availDoc.exists()) {
                        throw IllegalStateException("Waste availability no longer exists")
                    }

                    val currentStatus = availDoc.getString("status") ?: AvailabilityStatus.AVAILABLE.name
                    val expiresAt = availDoc.getLong("expiresAt") ?: 0L
                    if (currentStatus != AvailabilityStatus.AVAILABLE.name) {
                        throw IllegalStateException("Pickup has already been booked or removed")
                    }
                    if (now > expiresAt) {
                        throw IllegalStateException("Availability has expired")
                    }

                    val selectedReqRef = firestoreInstance.collection(Constants.COLLECTION_REQUESTS).document(selectedRequestId)
                    val selectedReqDoc = transaction.get(selectedReqRef)
                    if (!selectedReqDoc.exists()) {
                        throw IllegalStateException("Request not found")
                    }

                    val reqStatus = selectedReqDoc.getString("status") ?: RequestStatus.PENDING.name
                    if (reqStatus != RequestStatus.PENDING.name) {
                        throw IllegalStateException("Request is no longer pending")
                    }

                    // 1. Mark availability BOOKED
                    transaction.update(availRef, mapOf(
                        "status" to AvailabilityStatus.BOOKED.name,
                        "acceptedKawadiwalaId" to selectedKawadiwalaId,
                        "updatedAt" to now
                    ))

                    // 2. Mark selected request ACCEPTED
                    transaction.update(selectedReqRef, mapOf(
                        "status" to RequestStatus.ACCEPTED.name,
                        "updatedAt" to now
                    ))
                }.await()

                // Close other pending requests in background/batch
                try {
                    val allRequests = firestoreInstance.collection(Constants.COLLECTION_REQUESTS)
                        .whereEqualTo("wasteAvailabilityId", availabilityId)
                        .get().await()

                    for (doc in allRequests.documents) {
                        if (doc.id != selectedRequestId && doc.getString("status") == RequestStatus.PENDING.name) {
                            doc.reference.update("status", RequestStatus.REJECTED.name, "updatedAt", now)
                        }
                    }
                } catch (_: Exception) {}
            } catch (e: Exception) {
                // If firestore fails or isn't reachable, perform atomic local transaction
                val localResult = executeLocalAtomicAcceptance(availabilityId, selectedRequestId, selectedKawadiwalaId, now)
                if (localResult.isFailure) return localResult
            }
        } else {
            val localResult = executeLocalAtomicAcceptance(availabilityId, selectedRequestId, selectedKawadiwalaId, now)
            if (localResult.isFailure) return localResult
        }

        // Send confirmation notification to selected Kawadiwala
        notificationRepository.sendNotification(
            userId = selectedKawadiwalaId,
            title = "Pickup Request Accepted!",
            message = "Your offer was accepted by the household. Location coordinates are confirmed for pickup.",
            type = Constants.TYPE_REQUEST_ACCEPTED
        )

        return Result.success(Unit)
    }

    private suspend fun executeLocalAtomicAcceptance(
        availabilityId: String,
        selectedRequestId: String,
        selectedKawadiwalaId: String,
        now: Long
    ): Result<Unit> {
        val targetRequest = localRequestsMap[selectedRequestId]
            ?: return Result.failure(IllegalStateException("Request not found"))

        if (targetRequest.status != RequestStatus.PENDING) {
            return Result.failure(IllegalStateException("Request is no longer pending"))
        }

        // Update availability in waste repo
        wasteRepository.updateAvailabilityStatus(
            availabilityId = availabilityId,
            status = AvailabilityStatus.BOOKED,
            acceptedKawadiwalaId = selectedKawadiwalaId
        )

        // Atomically update local requests
        localRequestsMap.keys.toList().forEach { reqId ->
            val req = localRequestsMap[reqId]
            if (req != null && req.wasteAvailabilityId == availabilityId) {
                val newStatus = if (reqId == selectedRequestId) RequestStatus.ACCEPTED else RequestStatus.REJECTED
                localRequestsMap[reqId] = req.copy(status = newStatus, updatedAt = now)
            }
        }
        requestsFlow.value = localRequestsMap.values.toList()

        return Result.success(Unit)
    }

    suspend fun markRequestCompleted(
        availabilityId: String,
        kawadiwalaId: String
    ): Result<Unit> {
        val now = System.currentTimeMillis()
        val firestoreInstance = firestore

        if (firestoreInstance != null) {
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
        }

        // Update in-memory state
        val acceptedRequests = localRequestsMap.values.filter {
            it.wasteAvailabilityId == availabilityId &&
            it.kawadiwalaId == kawadiwalaId &&
            it.status == RequestStatus.ACCEPTED
        }
        acceptedRequests.forEach { req ->
            val updated = req.copy(status = RequestStatus.COMPLETED, updatedAt = now)
            localRequestsMap[req.id] = updated
        }
        requestsFlow.value = localRequestsMap.values.toList()

        // Send celebratory notification
        notificationRepository.sendNotification(
            userId = "household_target",
            title = "Scrap Collected Successfully",
            message = "Your recyclable waste has been picked up. Thank you for contributing to clean recycling with Green Route!",
            type = Constants.TYPE_PICKUP_COMPLETED
        )

        return Result.success(Unit)
    }

    suspend fun cancelRequestsForAvailability(
        availabilityId: String,
        bookedKawadiwalaId: String? = null
    ): Result<Unit> {
        val now = System.currentTimeMillis()
        val firestoreInstance = firestore

        if (firestoreInstance != null) {
            try {
                val requestsSnapshot = firestoreInstance.collection(Constants.COLLECTION_REQUESTS)
                    .whereEqualTo("wasteAvailabilityId", availabilityId)
                    .get().await()

                val batch = firestoreInstance.batch()
                for (doc in requestsSnapshot.documents) {
                    val currentStatus = doc.getString("status")
                    if (currentStatus != RequestStatus.COMPLETED.name) {
                        batch.update(
                            doc.reference,
                            mapOf(
                                "status" to RequestStatus.CANCELLED.name,
                                "updatedAt" to now
                            )
                        )
                    }
                }
                batch.commit().await()
            } catch (_: Exception) {}
        }

        // Update in-memory state
        var notifiedKawadiwala = bookedKawadiwalaId
        localRequestsMap.values.filter { it.wasteAvailabilityId == availabilityId }.forEach { req ->
            if (req.status == RequestStatus.ACCEPTED && notifiedKawadiwala == null) {
                notifiedKawadiwala = req.kawadiwalaId
            }
            if (req.status != RequestStatus.COMPLETED) {
                localRequestsMap[req.id] = req.copy(
                    status = RequestStatus.CANCELLED,
                    updatedAt = now
                )
            }
        }
        requestsFlow.value = localRequestsMap.values.toList()

        // Notify accepted Kawadiwala if this availability was booked
        if (!notifiedKawadiwala.isNullOrEmpty()) {
            notificationRepository.sendNotification(
                userId = notifiedKawadiwala!!,
                title = "Pickup Cancelled",
                message = "The household has removed the waste availability. This pickup and navigation route have been cancelled.",
                type = Constants.TYPE_PICKUP_CANCELLED
            )
        }

        return Result.success(Unit)
    }
}
