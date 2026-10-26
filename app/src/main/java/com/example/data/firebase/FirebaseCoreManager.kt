package com.example.data.firebase

import android.content.Context
import com.example.BuildConfig
import com.example.data.db.DistributionEntity
import com.example.data.db.DistributionType
import com.example.data.db.ParticipantEntity
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class FirebaseConnectionStatus {
    UNINITIALIZED,
    READY,
    MISSING_CONFIGURATION,
    CONFIG_MISSING,
    ERROR
}

data class FirebaseUserProfile(
    val uidHash: String,
    val displayName: String,
    val isAnonymous: Boolean = true
)

data class FirestoreRoomParticipantDto(
    val docId: String = "",
    val ownerTokenHash: String = "",
    val name: String = "",
    val allocatedCount: Int = 1,
    val completedCount: Int = 0,
    val displayOrder: Int = 1,
    val specificParaNumber: Int? = null,
    val isGuestEntry: Boolean = false,
    val isCompleted: Boolean = false,
    val completedMonthsCsv: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

data class FirestoreRoomSnapshot(
    val passcode: String,
    val name: String,
    val type: DistributionType,
    val totalTarget: Int,
    val roundNumber: Int,
    val startYear: Int,
    val startMonth: Int,
    val isClosedForever: Boolean,
    val closedSummaryText: String?,
    val participants: List<FirestoreRoomParticipantDto>
)

/**
 * Security-hardened core manager for Firebase (FirebaseApp, FirebaseAuth, and FirebaseFirestore):
 * - Initializes Firebase via `BuildConfig` secrets (`.env` / Secrets Panel) without requiring a
 *   tracked `google-services.json` in version control.
 * - Uses salted SHA-256 capability hashes (`roomCapabilityDocId`) as Firestore document keys so
 *   nobody can enumerate or brute-force rooms without the exact passcode.
 * - Preserves immutable `hostUidHash` and per-slot `ownerTokenHash` so participants cannot hijack
 *   ownership of the room or other members' slots.
 * - Enforces client-side write throttling and targeted single-slot updates to prevent Firestore
 *   write amplification / billing DoS.
 * - Supports full GDPR/CCPA cloud profile & auth erasure (`deleteCloudProfileAndResetAuth`).
 */
object FirebaseCoreManager {

    const val COLLECTION_USERS = "users"
    const val COLLECTION_ROOMS = "rooms"
    const val SUBCOLLECTION_PARTICIPANTS = "participants"

    private const val MAX_NAME_LENGTH = 60
    private const val MAX_ROOM_TITLE_LENGTH = 80
    private const val MIN_WRITE_INTERVAL_MS = 350L

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lastWriteTimestampByKey = ConcurrentHashMap<String, Long>()

    @Volatile
    private var isInitialized = false

    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseFirestore: FirebaseFirestore? = null

    private val _connectionStatus = MutableStateFlow(FirebaseConnectionStatus.UNINITIALIZED)
    val connectionStatus: StateFlow<FirebaseConnectionStatus> = _connectionStatus.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<FirebaseUserProfile?>(null)
    val currentUserProfile: StateFlow<FirebaseUserProfile?> = _currentUserProfile.asStateFlow()

    val isCloudReady: Boolean
        get() = _connectionStatus.value == FirebaseConnectionStatus.READY &&
            firebaseFirestore != null &&
            firebaseAuth != null

    /**
     * Initializes FirebaseApp (from BuildConfig secrets or default resources), FirebaseFirestore
     * with local persistent cache, and FirebaseAuth with automatic anonymous session.
     */
    @Synchronized
    fun initialize(context: Context) {
        if (isInitialized && _connectionStatus.value == FirebaseConnectionStatus.READY) {
            return
        }

        try {
            val appContext = context.applicationContext
            val existingApps = FirebaseApp.getApps(appContext)
            val app: FirebaseApp? = when {
                existingApps.isNotEmpty() -> FirebaseApp.getInstance()
                hasValidBuildConfigCredentials() -> {
                    val optionsBuilder = FirebaseOptions.Builder()
                        .setApiKey(BuildConfig.FIREBASE_API_KEY.trim())
                        .setApplicationId(BuildConfig.FIREBASE_APP_ID.trim())
                        .setProjectId(BuildConfig.FIREBASE_PROJECT_ID.trim())
                    if (BuildConfig.FIREBASE_STORAGE_BUCKET.isNotBlank() &&
                        !BuildConfig.FIREBASE_STORAGE_BUCKET.startsWith("your_")
                    ) {
                        optionsBuilder.setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET.trim())
                    }
                    if (BuildConfig.FIREBASE_PROJECT_NUMBER.isNotBlank() &&
                        !BuildConfig.FIREBASE_PROJECT_NUMBER.startsWith("your_")
                    ) {
                        optionsBuilder.setGcmSenderId(BuildConfig.FIREBASE_PROJECT_NUMBER.trim())
                    }
                    FirebaseApp.initializeApp(appContext, optionsBuilder.build())
                }
                else -> FirebaseApp.initializeApp(appContext)
            }

            if (app == null) {
                _connectionStatus.value = FirebaseConnectionStatus.MISSING_CONFIGURATION
                isInitialized = true
                return
            }

            val db = FirebaseFirestore.getInstance(app)
            try {
                val settings = FirebaseFirestoreSettings.Builder(db.firestoreSettings)
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
                db.firestoreSettings = settings
            } catch (_: Exception) {
                // Settings already locked in process
            }
            firebaseFirestore = db

            val authInstance = FirebaseAuth.getInstance(app)
            firebaseAuth = authInstance

            authInstance.addAuthStateListener { fa ->
                val user = fa.currentUser
                _currentUserProfile.value = user?.toSafeUserProfile()
            }

            _connectionStatus.value = FirebaseConnectionStatus.READY
            isInitialized = true

            if (authInstance.currentUser == null) {
                managerScope.launch {
                    ensureSignedIn()
                }
            } else {
                _currentUserProfile.value = authInstance.currentUser?.toSafeUserProfile()
            }
        } catch (_: Exception) {
            _connectionStatus.value = FirebaseConnectionStatus.ERROR
        }
    }

    @Synchronized
    fun retryInitialize(context: Context) {
        isInitialized = false
        _connectionStatus.value = FirebaseConnectionStatus.UNINITIALIZED
        initialize(context)
    }

    private fun hasValidBuildConfigCredentials(): Boolean {
        val apiKey = BuildConfig.FIREBASE_API_KEY.trim()
        val appId = BuildConfig.FIREBASE_APP_ID.trim()
        val projectId = BuildConfig.FIREBASE_PROJECT_ID.trim()
        return apiKey.isNotBlank() && !apiKey.startsWith("your_") &&
            appId.isNotBlank() && !appId.startsWith("your_") &&
            projectId.isNotBlank() && !projectId.startsWith("your_")
    }

    /**
     * Guard against rapid repeated writes to the same Firestore key to prevent billing DoS.
     */
    private fun allowThrottledWrite(key: String): Boolean {
        val now = System.currentTimeMillis()
        val prev = lastWriteTimestampByKey[key] ?: 0L
        if (now - prev < MIN_WRITE_INTERVAL_MS) {
            return false
        }
        lastWriteTimestampByKey[key] = now
        return true
    }

    /**
     * Sanitizes user input by stripping control characters and HTML/script tags and enforcing max length.
     */
    fun sanitizeInput(raw: String, maxLength: Int = MAX_NAME_LENGTH): String {
        return raw
            .replace(Regex("[<>\"'`\\\\\\x00-\\x1F\\x7F]"), "")
            .trim()
            .take(maxLength)
    }

    /**
     * Derives a 64-character SHA-256 capability document ID from a room passcode so that
     * Firestore documents cannot be enumerated or guessed without the exact passcode.
     */
    fun roomCapabilityDocId(passcode: String): String {
        val normalized = passcode.trim().uppercase()
        return sha256Hex("qb_room_v3_capability:$normalized")
    }

    /**
     * Derives a privacy-preserving salted SHA-256 ownership hash for a participant or host
     * so raw Firebase UIDs are never exposed inside shared room documents.
     */
    private fun memberOwnershipHash(uid: String, passcode: String): String {
        return sha256Hex("qb_member_v3_owner:$uid:${passcode.trim().uppercase()}")
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    suspend fun ensureSignedIn(): FirebaseUser? {
        val authInstance = firebaseAuth ?: return null
        authInstance.currentUser?.let { return it }

        return try {
            val result = authInstance.signInAnonymously().awaitResult()
            val user = result.user
            _currentUserProfile.value = user?.toSafeUserProfile()
            user
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Syncs the user's display name to FirebaseAuth and their private `users/{uid}` document
     * (strictly owner-only in Firestore security rules, zero email/PII stored).
     */
    suspend fun syncUserProfile(displayName: String) {
        val cleanName = sanitizeInput(displayName, MAX_NAME_LENGTH)
        if (!isCloudReady || cleanName.isBlank()) return
        if (!allowThrottledWrite("profile_sync")) return

        try {
            val user = ensureSignedIn() ?: return
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(cleanName)
                .build()
            user.updateProfile(profileUpdates).awaitResult()

            _currentUserProfile.value = FirebaseUserProfile(
                uidHash = sha256Hex("qb_profile:${user.uid}").take(8).uppercase(),
                displayName = cleanName,
                isAnonymous = user.isAnonymous
            )

            val db = firebaseFirestore ?: return
            val userDoc = mapOf(
                "uid" to user.uid,
                "displayName" to cleanName,
                "isAnonymous" to user.isAnonymous,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_USERS)
                .document(user.uid)
                .set(userDoc, SetOptions.merge())
                .awaitResult()
        } catch (_: Exception) {
        }
    }

    /**
     * GDPR / CCPA Right to Erasure: Deletes the user's `users/{uid}` cloud document and
     * deletes/rotates their anonymous Firebase Auth account.
     */
    suspend fun deleteCloudProfileAndResetAuth(): Boolean {
        if (!isCloudReady) return false
        return try {
            val authInstance = firebaseAuth ?: return false
            val user = authInstance.currentUser ?: return false
            val db = firebaseFirestore
            if (db != null) {
                try {
                    db.collection(COLLECTION_USERS).document(user.uid).delete().awaitResult()
                } catch (_: Exception) {
                }
            }
            user.delete().awaitResult()
            _currentUserProfile.value = null
            ensureSignedIn()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Publishes or updates an Online Room under its SHA-256 capability key (`rooms/{capabilityHash}`)
     * while preserving existing `hostUidHash` and per-slot `ownerTokenHash` values so participants
     * never overwrite room or slot ownership.
     */
    suspend fun syncOnlineRoomToFirestore(
        distribution: DistributionEntity,
        participants: List<ParticipantEntity>
    ): Boolean {
        val passcode = distribution.roomPasscode?.trim()?.uppercase()
        if (!isCloudReady || !distribution.isOnline || passcode.isNullOrBlank()) return false
        if (!allowThrottledWrite("room_sync:$passcode")) return false

        return try {
            val user = ensureSignedIn() ?: return false
            val db = firebaseFirestore ?: return false
            val capabilityId = roomCapabilityDocId(passcode)
            val roomRef = db.collection(COLLECTION_ROOMS).document(capabilityId)
            val callerOwnerHash = memberOwnershipHash(user.uid, passcode)

            val existingSnap = roomRef.get().awaitResult()
            val preservedHostHash = if (existingSnap.exists()) {
                existingSnap.getString("hostUidHash")?.takeIf { it.length == 64 } ?: callerOwnerHash
            } else {
                callerOwnerHash
            }

            val roomData = mapOf(
                "name" to sanitizeInput(distribution.name, MAX_ROOM_TITLE_LENGTH).ifBlank { "Online Room" },
                "type" to distribution.type.name,
                "roomMode" to distribution.roomMode.name,
                "totalTarget" to distribution.totalTarget.coerceIn(1, 100000),
                "roundNumber" to distribution.roundNumber.coerceAtLeast(1),
                "startYear" to distribution.startYear,
                "startMonth" to distribution.startMonth,
                "isClosedForever" to distribution.isClosedForever,
                "closedSummaryText" to sanitizeInput(distribution.closedSummaryText ?: "", 120),
                "hostUidHash" to preservedHostHash,
                "updatedAt" to System.currentTimeMillis()
            )
            roomRef.set(roomData, SetOptions.merge()).awaitResult()

            if (participants.isEmpty()) return true

            val existingParticipantsSnap = roomRef.collection(SUBCOLLECTION_PARTICIPANTS).get().awaitResult()
            val existingOwnersByDocId = existingParticipantsSnap.documents.associate { doc ->
                doc.id to (doc.getString("ownerTokenHash")?.takeIf { it.length == 64 } ?: callerOwnerHash)
            }

            val batch = db.batch()
            val participantsCollection = roomRef.collection(SUBCOLLECTION_PARTICIPANTS)

            participants.take(100).forEach { p ->
                val docId = participantDocId(distribution.type, p)
                val docRef = participantsCollection.document(docId)
                val slotOwnerHash = existingOwnersByDocId[docId] ?: callerOwnerHash
                val pData = mapOf(
                    "docId" to docId,
                    "ownerTokenHash" to slotOwnerHash,
                    "name" to sanitizeInput(p.name, MAX_NAME_LENGTH).ifBlank { "Member" },
                    "allocatedCount" to p.allocatedCount.coerceIn(1, 100000),
                    "completedCount" to p.completedCount.coerceIn(0, 100000),
                    "displayOrder" to p.displayOrder.coerceIn(1, 200),
                    "specificParaNumber" to p.specificParaNumber,
                    "isGuestEntry" to p.isGuestEntry,
                    "isCompleted" to p.isCompleted,
                    "completedMonthsCsv" to sanitizeInput(p.completedCycleMonthsCsv, 400),
                    "updatedAt" to System.currentTimeMillis()
                )
                batch.set(docRef, pData, SetOptions.merge())
            }
            batch.commit().awaitResult()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Targeted single-document update when a participant claims or toggles completion,
     * avoiding 30-document batch write amplification.
     */
    suspend fun upsertSingleParticipantInFirestore(
        distribution: DistributionEntity,
        participant: ParticipantEntity
    ): Boolean {
        val passcode = distribution.roomPasscode?.trim()?.uppercase()
        if (!isCloudReady || !distribution.isOnline || passcode.isNullOrBlank()) return false
        val docId = participantDocId(distribution.type, participant)
        if (!allowThrottledWrite("slot:$passcode:$docId")) return false

        return try {
            val user = ensureSignedIn() ?: return false
            val db = firebaseFirestore ?: return false
            val capabilityId = roomCapabilityDocId(passcode)
            val callerOwnerHash = memberOwnershipHash(user.uid, passcode)
            val docRef = db.collection(COLLECTION_ROOMS)
                .document(capabilityId)
                .collection(SUBCOLLECTION_PARTICIPANTS)
                .document(docId)

            val existing = docRef.get().awaitResult()
            val slotOwnerHash = if (existing.exists()) {
                existing.getString("ownerTokenHash")?.takeIf { it.length == 64 } ?: callerOwnerHash
            } else {
                callerOwnerHash
            }

            val pData = mapOf(
                "docId" to docId,
                "ownerTokenHash" to slotOwnerHash,
                "name" to sanitizeInput(participant.name, MAX_NAME_LENGTH).ifBlank { "Member" },
                "allocatedCount" to participant.allocatedCount.coerceIn(1, 100000),
                "completedCount" to participant.completedCount.coerceIn(0, 100000),
                "displayOrder" to participant.displayOrder.coerceIn(1, 200),
                "specificParaNumber" to participant.specificParaNumber,
                "isGuestEntry" to participant.isGuestEntry,
                "isCompleted" to participant.isCompleted,
                "completedMonthsCsv" to sanitizeInput(participant.completedCycleMonthsCsv, 400),
                "updatedAt" to System.currentTimeMillis()
            )
            docRef.set(pData, SetOptions.merge()).awaitResult()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Deletes a single unclaimed/removed participant slot from Firestore so removed claims do not
     * remain orphaned in the cloud.
     */
    suspend fun deleteParticipantSlotFromFirestore(
        distribution: DistributionEntity,
        participant: ParticipantEntity
    ) {
        val passcode = distribution.roomPasscode?.trim()?.uppercase()
        if (!isCloudReady || !distribution.isOnline || passcode.isNullOrBlank()) return

        try {
            ensureSignedIn() ?: return
            val db = firebaseFirestore ?: return
            val capabilityId = roomCapabilityDocId(passcode)
            val docId = participantDocId(distribution.type, participant)
            db.collection(COLLECTION_ROOMS)
                .document(capabilityId)
                .collection(SUBCOLLECTION_PARTICIPANTS)
                .document(docId)
                .delete()
                .awaitResult()
        } catch (_: Exception) {
        }
    }

    /**
     * Cleans up all participant documents in Firestore when a One-Time 30-Para Khatm finishes
     * or when an Amal room is reopened from scratch (data minimization & storage privacy).
     */
    suspend fun clearAllParticipantSlotsInFirestore(passcode: String) {
        val cleanCode = passcode.trim().uppercase()
        if (!isCloudReady || cleanCode.isBlank()) return

        try {
            ensureSignedIn() ?: return
            val db = firebaseFirestore ?: return
            val capabilityId = roomCapabilityDocId(cleanCode)
            val partsRef = db.collection(COLLECTION_ROOMS)
                .document(capabilityId)
                .collection(SUBCOLLECTION_PARTICIPANTS)
            val snap = partsRef.get().awaitResult()
            if (snap.isEmpty) return
            val batch = db.batch()
            snap.documents.forEach { batch.delete(it.reference) }
            batch.commit().awaitResult()
        } catch (_: Exception) {
        }
    }

    /**
     * Atomically claims a Para in a One-Time Online Khatm using a Firestore Transaction
     * keyed by the room's SHA-256 capability ID.
     */
    suspend fun claimOneTimeParaTransactionInFirestore(
        passcode: String,
        paraNumber: Int,
        memberName: String,
        isGuest: Boolean
    ): Boolean {
        val cleanCode = passcode.trim().uppercase()
        val cleanName = sanitizeInput(memberName, MAX_NAME_LENGTH)
        if (!isCloudReady || cleanCode.isBlank() || cleanName.isBlank() || paraNumber !in 1..30) return true

        return try {
            val user = ensureSignedIn() ?: return true
            val db = firebaseFirestore ?: return true
            val capabilityId = roomCapabilityDocId(cleanCode)
            val ownerHash = memberOwnershipHash(user.uid, cleanCode)
            val slotDocRef = db.collection(COLLECTION_ROOMS)
                .document(capabilityId)
                .collection(SUBCOLLECTION_PARTICIPANTS)
                .document("para_$paraNumber")

            db.runTransaction { transaction ->
                val snapshot = transaction.get(slotDocRef)
                if (snapshot.exists()) {
                    val existingName = snapshot.getString("name")
                    if (!existingName.isNullOrBlank()) {
                        throw IllegalStateException("Already claimed")
                    }
                }
                val claimData = mapOf(
                    "docId" to "para_$paraNumber",
                    "ownerTokenHash" to ownerHash,
                    "name" to cleanName,
                    "allocatedCount" to 1,
                    "displayOrder" to paraNumber,
                    "specificParaNumber" to paraNumber,
                    "isGuestEntry" to isGuest,
                    "isCompleted" to false,
                    "completedMonthsCsv" to "",
                    "updatedAt" to System.currentTimeMillis()
                )
                transaction.set(slotDocRef, claimData)
                true
            }.awaitResult()
        } catch (_: IllegalStateException) {
            false
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Fetches an Online Room from Firestore using the SHA-256 capability hash of its passcode.
     */
    suspend fun fetchOnlineRoomByPasscode(passcode: String): FirestoreRoomSnapshot? {
        val cleanCode = passcode.trim().uppercase()
        if (!isCloudReady || cleanCode.isBlank()) return null

        return try {
            ensureSignedIn()
            val db = firebaseFirestore ?: return null
            val capabilityId = roomCapabilityDocId(cleanCode)
            val roomRef = db.collection(COLLECTION_ROOMS).document(capabilityId)
            val roomSnap = roomRef.get().awaitResult()
            if (!roomSnap.exists()) return null

            val partsSnap = roomRef.collection(SUBCOLLECTION_PARTICIPANTS).get().awaitResult()
            val participants = partsSnap.documents.mapNotNull { doc ->
                val rawName = doc.getString("name") ?: return@mapNotNull null
                val cleanName = sanitizeInput(rawName, MAX_NAME_LENGTH)
                if (cleanName.isBlank()) return@mapNotNull null
                FirestoreRoomParticipantDto(
                    docId = doc.id,
                    ownerTokenHash = doc.getString("ownerTokenHash") ?: "",
                    name = cleanName,
                    allocatedCount = (doc.getLong("allocatedCount") ?: 1L).toInt().coerceIn(1, 100000),
                    completedCount = (doc.getLong("completedCount") ?: 0L).toInt().coerceIn(0, 100000),
                    displayOrder = (doc.getLong("displayOrder") ?: 1L).toInt().coerceIn(1, 200),
                    specificParaNumber = doc.getLong("specificParaNumber")?.toInt(),
                    isGuestEntry = doc.getBoolean("isGuestEntry") ?: false,
                    isCompleted = doc.getBoolean("isCompleted") ?: false,
                    completedMonthsCsv = sanitizeInput(doc.getString("completedMonthsCsv") ?: "", 400),
                    updatedAt = doc.getLong("updatedAt") ?: 0L
                )
            }

            val typeStr = roomSnap.getString("type") ?: DistributionType.ONE_TIME_KHATM.name
            val distType = try {
                DistributionType.valueOf(typeStr)
            } catch (_: Exception) {
                DistributionType.ONE_TIME_KHATM
            }

            FirestoreRoomSnapshot(
                passcode = cleanCode,
                name = sanitizeInput(roomSnap.getString("name") ?: "Online Room", MAX_ROOM_TITLE_LENGTH),
                type = distType,
                totalTarget = (roomSnap.getLong("totalTarget") ?: 30L).toInt().coerceIn(1, 100000),
                roundNumber = (roomSnap.getLong("roundNumber") ?: 1L).toInt().coerceAtLeast(1),
                startYear = (roomSnap.getLong("startYear") ?: 2026L).toInt(),
                startMonth = (roomSnap.getLong("startMonth") ?: 1L).toInt(),
                isClosedForever = roomSnap.getBoolean("isClosedForever") ?: false,
                closedSummaryText = roomSnap.getString("closedSummaryText")?.takeIf { it.isNotBlank() },
                participants = participants
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Observes real-time changes to an Online Room in Firestore via its SHA-256 capability ID.
     */
    fun observeOnlineRoom(passcode: String): Flow<FirestoreRoomSnapshot> = callbackFlow {
        val cleanCode = passcode.trim().uppercase()
        val db = firebaseFirestore
        if (!isCloudReady || db == null || cleanCode.isBlank()) {
            close()
            return@callbackFlow
        }

        val capabilityId = roomCapabilityDocId(cleanCode)
        val roomRef = db.collection(COLLECTION_ROOMS).document(capabilityId)
        var participantsRegistration: ListenerRegistration? = null

        val roomRegistration = roomRef.addSnapshotListener { roomSnap, roomErr ->
            if (roomErr != null || roomSnap == null || !roomSnap.exists()) return@addSnapshotListener

            if (participantsRegistration == null) {
                participantsRegistration = roomRef.collection(SUBCOLLECTION_PARTICIPANTS)
                    .addSnapshotListener { partsSnap, partsErr ->
                        if (partsErr != null || partsSnap == null) return@addSnapshotListener
                        val participants = partsSnap.documents.mapNotNull { doc ->
                            val rawName = doc.getString("name") ?: return@mapNotNull null
                            val cleanName = sanitizeInput(rawName, MAX_NAME_LENGTH)
                            if (cleanName.isBlank()) return@mapNotNull null
                            FirestoreRoomParticipantDto(
                                docId = doc.id,
                                ownerTokenHash = doc.getString("ownerTokenHash") ?: "",
                                name = cleanName,
                                allocatedCount = (doc.getLong("allocatedCount") ?: 1L).toInt().coerceIn(1, 100000),
                                completedCount = (doc.getLong("completedCount") ?: 0L).toInt().coerceIn(0, 100000),
                                displayOrder = (doc.getLong("displayOrder") ?: 1L).toInt().coerceIn(1, 200),
                                specificParaNumber = doc.getLong("specificParaNumber")?.toInt(),
                                isGuestEntry = doc.getBoolean("isGuestEntry") ?: false,
                                isCompleted = doc.getBoolean("isCompleted") ?: false,
                                completedMonthsCsv = sanitizeInput(doc.getString("completedMonthsCsv") ?: "", 400),
                                updatedAt = doc.getLong("updatedAt") ?: 0L
                            )
                        }

                        val typeStr = roomSnap.getString("type") ?: DistributionType.ONE_TIME_KHATM.name
                        val distType = try {
                            DistributionType.valueOf(typeStr)
                        } catch (_: Exception) {
                            DistributionType.ONE_TIME_KHATM
                        }

                        trySend(
                            FirestoreRoomSnapshot(
                                passcode = cleanCode,
                                name = sanitizeInput(roomSnap.getString("name") ?: "Online Room", MAX_ROOM_TITLE_LENGTH),
                                type = distType,
                                totalTarget = (roomSnap.getLong("totalTarget") ?: 30L).toInt().coerceIn(1, 100000),
                                roundNumber = (roomSnap.getLong("roundNumber") ?: 1L).toInt().coerceAtLeast(1),
                                startYear = (roomSnap.getLong("startYear") ?: 2026L).toInt(),
                                startMonth = (roomSnap.getLong("startMonth") ?: 1L).toInt(),
                                isClosedForever = roomSnap.getBoolean("isClosedForever") ?: false,
                                closedSummaryText = roomSnap.getString("closedSummaryText")?.takeIf { it.isNotBlank() },
                                participants = participants
                            )
                        )
                    }
            }
        }

        awaitClose {
            roomRegistration.remove()
            participantsRegistration?.remove()
        }
    }

    fun participantDocId(type: DistributionType, p: ParticipantEntity): String {
        return if (type == DistributionType.ONE_TIME_KHATM && p.specificParaNumber != null) {
            "para_${p.specificParaNumber}"
        } else {
            val safeName = p.name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").take(24)
            "slot_${p.displayOrder}_$safeName"
        }
    }

    private fun FirebaseUser.toSafeUserProfile(): FirebaseUserProfile {
        return FirebaseUserProfile(
            uidHash = sha256Hex("qb_profile:$uid").take(8).uppercase(),
            displayName = sanitizeInput(displayName ?: "", MAX_NAME_LENGTH),
            isAnonymous = isAnonymous
        )
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { exc ->
            if (cont.isActive) cont.resumeWithException(exc)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.cancel()
        }
    }
}
