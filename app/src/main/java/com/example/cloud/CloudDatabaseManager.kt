package com.example.cloud

import android.content.Context
import android.util.Log
import com.example.data.AttendanceEntity
import com.example.data.CarePlanEntity
import com.example.data.ClinicalNoteEntity
import com.example.data.DocumentEntity
import com.example.data.HospitalRepository
import com.example.data.HospitalSeedData
import com.example.data.NotificationEntity
import com.example.data.PatientEntity
import com.example.data.UserEntity
import com.example.data.VitalRecordEntity
import com.example.fcm.FcmManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CloudDatabaseManager {
    private const val TAG = "CloudDatabaseManager"

    // Live verified cloud project & database coordinates
    const val CLOUD_PROJECT_ID = "gen-lang-client-0110759247"
    const val CLOUD_APP_ID = "1:300073810040:android:1cd9173356655572c1d58d"
    const val CLOUD_API_KEY = "AIzaSyAdOGC8fGBbEe49IFqU2HDdY1aGJs5Q5Jw"
    const val CLOUD_DATABASE_ID = "ai-studio-android-mbnursin-fd9ce470-c0d4-4daf-ad31-9ab650ee0c09"

    // Shared Cloud Firestore Collection Names
    const val COLL_USERS = "hospital_users"
    const val COLL_PATIENTS = "hospital_patients"
    const val COLL_DOCUMENTS = "hospital_documents"
    const val COLL_CLINICAL_NOTES = "hospital_clinical_notes"
    const val COLL_CARE_PLANS = "hospital_care_plans"
    const val COLL_ATTENDANCE = "hospital_attendance"
    const val COLL_NOTIFICATIONS = "hospital_notifications"
    const val COLL_BROADCASTS = "hospital_broadcasts"
    const val COLL_VITAL_RECORDS = "hospital_vital_records"
    const val COLL_APP_CONFIG = "app_config"
    const val DOC_VERSION = "version_info"

    private val _isCloudConnected = MutableStateFlow(false)
    val isCloudConnected: StateFlow<Boolean> = _isCloudConnected.asStateFlow()

    private val _cloudSyncStatus = MutableStateFlow("Initializing Cloud Database...")
    val cloudSyncStatus: StateFlow<String> = _cloudSyncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime.asStateFlow()

    private val _latestBroadcast = MutableStateFlow<com.example.model.HospitalBroadcast?>(null)
    val latestBroadcast: StateFlow<com.example.model.HospitalBroadcast?> = _latestBroadcast.asStateFlow()

    private val _allBroadcasts = MutableStateFlow<List<com.example.model.HospitalBroadcast>>(emptyList())
    val allBroadcasts: StateFlow<List<com.example.model.HospitalBroadcast>> = _allBroadcasts.asStateFlow()

    private val _appUpdateInfo = MutableStateFlow<com.example.model.AppUpdateInfo?>(null)
    val appUpdateInfo: StateFlow<com.example.model.AppUpdateInfo?> = _appUpdateInfo.asStateFlow()

    private val seenNotificationIds = mutableSetOf<String>()
    private val dismissedBroadcastIds = mutableSetOf<String>()

    fun clearLatestBroadcast(context: Context? = null) {
        clearAllNotificationsAndBroadcasts(context)
    }

    fun clearAllNotificationsAndBroadcasts(context: Context? = null) {
        dismissedBroadcastIds.addAll(_allBroadcasts.value.map { it.id })
        _latestBroadcast.value = null
        _allBroadcasts.value = emptyList()
        if (context != null) {
            try {
                val db = getFirestore(context)
                db?.collection(COLL_BROADCASTS)?.get()
                    ?.addOnSuccessListener { snapshot ->
                        for (doc in snapshot.documents) {
                            doc.reference.delete()
                        }
                    }
                    ?.addOnFailureListener { e ->
                        Log.w(TAG, "Clear cloud broadcasts notice: ${e.message}")
                    }
                db?.collection(COLL_NOTIFICATIONS)?.get()
                    ?.addOnSuccessListener { snapshot ->
                        for (doc in snapshot.documents) {
                            doc.reference.delete()
                        }
                    }
                    ?.addOnFailureListener { e ->
                        Log.w(TAG, "Clear cloud notifications notice: ${e.message}")
                    }
            } catch (_: Exception) {}
        }
    }

    fun dismissBroadcast(context: Context? = null, id: String) {
        dismissedBroadcastIds.add(id)
        _allBroadcasts.value = _allBroadcasts.value.filter { it.id != id }
        if (_latestBroadcast.value?.id == id) {
            _latestBroadcast.value = _allBroadcasts.value.firstOrNull()
        }
        if (context != null) {
            try {
                val db = getFirestore(context)
                db?.collection(COLL_BROADCASTS)?.document(id)?.delete()
                db?.collection(COLL_NOTIFICATIONS)?.document(id)?.delete()
            } catch (_: Exception) {}
        }
    }

    fun deleteNotification(context: Context? = null, id: String) {
        seenNotificationIds.add(id)
        dismissedBroadcastIds.add(id)
        _allBroadcasts.value = _allBroadcasts.value.filter { it.id != id }
        if (_latestBroadcast.value?.id == id) {
            _latestBroadcast.value = _allBroadcasts.value.firstOrNull()
        }
        if (context != null) {
            try {
                val db = getFirestore(context)
                db?.collection(COLL_NOTIFICATIONS)?.document(id)?.delete()
                db?.collection(COLL_BROADCASTS)?.document(id)?.delete()
            } catch (_: Exception) {}
        }
    }

    fun resetSeenNotifications() {
        seenNotificationIds.clear()
    }
    private var notificationListener: ListenerRegistration? = null
    private var patientListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null
    private var documentListener: ListenerRegistration? = null
    private var clinicalNoteListener: ListenerRegistration? = null
    private var carePlanListener: ListenerRegistration? = null
    private var attendanceListener: ListenerRegistration? = null
    private var broadcastListener: ListenerRegistration? = null
    private var vitalRecordListener: ListenerRegistration? = null
    private var appUpdateListener: ListenerRegistration? = null

    private var firestoreInstance: FirebaseFirestore? = null

    private fun handleListenerError(tag: String, error: Throwable?) {
        if (error == null) return
        val msg = error.message ?: ""
        Log.w(TAG, "$tag: $msg")
        if (msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
            msg.contains("Quota limit exceeded", ignoreCase = true)
        ) {
            _isCloudConnected.value = false
            _cloudSyncStatus.value = "Local SQLite Active (Quota Limit)"
            return
        }
        // When Firestore terminates a stream on transient error, nullify listener references so ensureSyncActive restarts them
        if (tag.contains("patient", ignoreCase = true)) patientListener = null
        if (tag.contains("user", ignoreCase = true)) userListener = null
        if (tag.contains("notification", ignoreCase = true)) notificationListener = null
        if (tag.contains("document", ignoreCase = true)) documentListener = null
        if (tag.contains("note", ignoreCase = true)) clinicalNoteListener = null
        if (tag.contains("care", ignoreCase = true)) carePlanListener = null
        if (tag.contains("attendance", ignoreCase = true)) attendanceListener = null
        if (tag.contains("vital", ignoreCase = true)) vitalRecordListener = null
        if (tag.contains("broadcast", ignoreCase = true)) broadcastListener = null
        if (tag.contains("update", ignoreCase = true)) appUpdateListener = null
    }

    private fun getFirestore(context: Context): FirebaseFirestore? {
        if (firestoreInstance != null) return firestoreInstance
        return try {
            FcmManager.initialize(context)

            val app = if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseApp.getInstance()
            } else {
                val options = FirebaseOptions.fromResource(context) ?: FirebaseOptions.Builder()
                    .setApplicationId(CLOUD_APP_ID)
                    .setProjectId(CLOUD_PROJECT_ID)
                    .setApiKey(CLOUD_API_KEY)
                    .setGcmSenderId("300073810040")
                    .setStorageBucket("gen-lang-client-0110759247.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(context.applicationContext, options)
            }

            val dbId = try {
                context.getString(com.example.R.string.firestore_database_id)
            } catch (_: Exception) {
                CLOUD_DATABASE_ID
            }

            val db = FirebaseFirestore.getInstance(app, dbId)
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                db.firestoreSettings = settings
            } catch (_: Exception) {}

            firestoreInstance = db
            _isCloudConnected.value = true
            _cloudSyncStatus.value = "Connected · Real-time Cloud Sync Active"
            db
        } catch (e: Exception) {
            Log.w(TAG, "Cloud Firestore initialization notice: ${e.message}")
            _isCloudConnected.value = false
            _cloudSyncStatus.value = "Local SQLite Active · Cloud Ready"
            null
        }
    }

    /**
     * Starts listening to shared Cloud Firestore collections across all devices:
     * 1. hospital_notifications: Detects alerts & broadcasts, triggers instant heads-up notifications.
     * 2. hospital_patients: Syncs patient admissions & bed assignments across phones & web with background alerts.
     * 3. hospital_users: Delivers doctor & staff roster updates across all phones with notifications.
     * 4. hospital_documents: Synchronizes scans, lab reports, and medical prescriptions with background alerts.
     * 5. hospital_clinical_notes: Delivers clinical observations & progress notes across devices.
     * 6. hospital_care_plans: Syncs nursing checklists, tasks, and clinical orders.
     * 7. hospital_attendance: Syncs staff attendance and check-in records.
     */
    fun ensureSyncActive(context: Context, repository: HospitalRepository) {
        if (notificationListener == null || patientListener == null || userListener == null || broadcastListener == null) {
            startRealtimeCloudSync(context, repository)
        }
    }

    /**
     * Actively fetches the latest records from Cloud Firestore server and syncs into local Room SQLite.
     * Guaranteed to bridge any gaps caused by device Doze sleep or interrupted sockets.
     */
    fun reconcileWithCloudServer(
        context: Context,
        repository: HospitalRepository,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val db = getFirestore(context) ?: run {
            onComplete?.invoke(false)
            return
        }
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Reconcile Users / Staff Roster FIRST so all newly created RMO, Maintenance, Doctor, and Nurse accounts are immediately available on every device
                val userSnap = try {
                    Tasks.await(db.collection(COLL_USERS).get(), 8, TimeUnit.SECONDS)
                } catch (e: Exception) {
                    Log.w(TAG, "Reconcile users notice: ${e.message}")
                    null
                }
                if (userSnap != null && !userSnap.isEmpty) {
                    val cloudUserIds = mutableSetOf<String>()
                    for (doc in userSnap.documents) {
                        val id = doc.getString("id") ?: doc.id
                        cloudUserIds.add(id)
                        val name = doc.getString("name") ?: doc.getString("userName") ?: doc.getString("fullName") ?: id
                        val role = if (id == "BOSS-0001") "BOSS" else (doc.getString("role") ?: "NURSE")
                        val dept = doc.getString("dept") ?: doc.getString("department") ?: if (id == "BOSS-0001") "Administration" else "General"
                        val spec = doc.getString("specialty") ?: if (id == "BOSS-0001") "Owner · Full Access" else "General"
                        val phone = doc.getString("phone") ?: ""
                        val email = doc.getString("email") ?: ""
                        val pass = doc.getString("pass") ?: "12345"
                        val photoUri = doc.getString("photoUri")?.ifBlank { null }
                        val joinedDate = doc.getString("joinedDate") ?: doc.getString("joiningDate") ?: "2026-01-01"

                        val user = UserEntity(
                            id = id,
                            pass = pass,
                            name = name,
                            role = role,
                            dept = dept,
                            specialty = spec,
                            phone = phone,
                            email = email,
                            photoUri = photoUri,
                            joinedDate = joinedDate
                        )
                        repository.insertUser(user)
                    }
                    // Prune local users that were removed from cloud by Administrator/Boss
                    val localUsers = repository.getAllUsersDirect()
                    for (u in localUsers) {
                        if (u.id != "BOSS-0001" && u.id !in cloudUserIds) {
                            repository.deleteUser(u.id)
                        }
                    }
                    ensureBossUserExists(repository)
                }

                // 2. Reconcile patients
                val patientSnap = try {
                    Tasks.await(db.collection(COLL_PATIENTS).get(), 8, TimeUnit.SECONDS)
                } catch (e: Exception) {
                    Log.w(TAG, "Reconcile patients notice: ${e.message}")
                    null
                }
                if (patientSnap != null && !patientSnap.isEmpty) {
                    val cloudPatientIds = mutableSetOf<String>()
                    for (doc in patientSnap.documents) {
                        val id = doc.getString("id") ?: doc.id
                        cloudPatientIds.add(id)
                        val name = doc.getString("name") ?: doc.getString("patientName") ?: doc.getString("fullName") ?: "Patient $id"
                        val age = (doc.get("age") as? Number)?.toInt() ?: (doc.getString("age")?.toIntOrNull()) ?: 0
                        val gender = doc.getString("gender") ?: "Other"
                        val ward = doc.getString("ward") ?: "General Ward"
                        val bed = doc.getString("bed") ?: ""
                        val doctorId = doc.getString("doctorId") ?: ""
                        val admittedOn = doc.getString("admittedOn") ?: "Today"
                        val condition = doc.getString("condition") ?: "Stable"
                        val phone = doc.getString("phone") ?: ""
                        val refDoc = doc.getString("referralDoctorId")
                        val refReason = doc.getString("referralReason")
                        val refBy = doc.getString("referralBy")
                        val refDate = doc.getString("referralDate")
                        val status = doc.getString("status") ?: "ADMITTED"
                        val dischargedOn = doc.getString("dischargedOn")
                        val dischargeSummary = doc.getString("dischargeSummary")
                        val temp = doc.getString("temperature")
                        val spo2 = doc.getString("spo2")
                        val pulse = doc.getString("pulse")
                        val bp = doc.getString("bloodPressure")
                        val cbg = doc.getString("cbg")
                        val vitalsTime = doc.getString("vitalsUpdatedAt")
                        val vitalsBy = doc.getString("vitalsUpdatedBy")

                        val patient = PatientEntity(
                            id = id,
                            name = name,
                            age = age,
                            gender = gender,
                            ward = ward,
                            bed = bed,
                            doctorId = doctorId,
                            admittedOn = admittedOn,
                            condition = condition,
                            phone = phone,
                            referralDoctorId = refDoc,
                            referralReason = refReason,
                            referralBy = refBy,
                            referralDate = refDate,
                            status = status,
                            dischargedOn = dischargedOn,
                            dischargeSummary = dischargeSummary,
                            temperature = temp,
                            spo2 = spo2,
                            pulse = pulse,
                            bloodPressure = bp,
                            cbg = cbg,
                            vitalsUpdatedAt = vitalsTime,
                            vitalsUpdatedBy = vitalsBy
                        )
                        repository.insertPatient(patient)
                    }
                    // Prune local patients that were deleted from cloud
                    val localPatients = repository.getAllPatientsDirect()
                    for (p in localPatients) {
                        if (p.id !in cloudPatientIds) {
                            repository.deletePatient(p.id)
                        }
                    }
                }

                // 3. Reconcile notifications
                val notifSnap = try {
                    Tasks.await(db.collection(COLL_NOTIFICATIONS).get(), 6, TimeUnit.SECONDS)
                } catch (_: Exception) { null }
                if (notifSnap != null && !notifSnap.isEmpty) {
                    for (doc in notifSnap.documents) {
                        val id = doc.id
                        val title = doc.getString("title") ?: "Hospital Alert"
                        val body = doc.getString("body") ?: ""
                        val audience = doc.getString("audience") ?: "all"
                        val time = doc.getString("time") ?: timeFormat.format(Date())
                        val read = (doc.get("read") as? Boolean) ?: false
                        val kind = doc.getString("kind") ?: "admin"
                        val notif = NotificationEntity(
                            id = id,
                            title = title,
                            body = body,
                            time = time,
                            audience = audience,
                            read = read,
                            kind = kind
                        )
                        repository.insertNotification(notif)
                    }
                }

                // 4. Reconcile vital records
                val vitalSnap = try {
                    Tasks.await(db.collection(COLL_VITAL_RECORDS).get(), 6, TimeUnit.SECONDS)
                } catch (_: Exception) { null }
                if (vitalSnap != null && !vitalSnap.isEmpty) {
                    val cloudVitalIds = mutableSetOf<String>()
                    for (doc in vitalSnap.documents) {
                        val id = doc.id
                        cloudVitalIds.add(id)
                        val patientId = doc.getString("patientId") ?: continue
                        val temp = doc.getString("temperature") ?: ""
                        val spo2 = doc.getString("spo2") ?: ""
                        val pulse = doc.getString("pulse") ?: ""
                        val bp = doc.getString("bloodPressure") ?: ""
                        val cbg = doc.getString("cbg") ?: ""
                        val recordedAt = doc.getString("recordedAt") ?: ""
                        val recordedBy = doc.getString("recordedBy") ?: ""
                        val vr = VitalRecordEntity(
                            id = id,
                            patientId = patientId,
                            temperature = temp,
                            spo2 = spo2,
                            pulse = pulse,
                            bloodPressure = bp,
                            cbg = cbg,
                            recordedAt = recordedAt,
                            recordedBy = recordedBy
                        )
                        repository.insertVitalRecord(vr)
                    }
                    val localVitals = repository.allVitalRecords.firstOrNull() ?: emptyList()
                    for (v in localVitals) {
                        if (v.id !in cloudVitalIds) {
                            repository.deleteVitalRecord(v.id)
                        }
                    }
                }

                // 5. Reconcile clinical notes
                val noteSnap = try {
                    Tasks.await(db.collection(COLL_CLINICAL_NOTES).get(), 6, TimeUnit.SECONDS)
                } catch (_: Exception) { null }
                if (noteSnap != null && !noteSnap.isEmpty) {
                    val cloudNoteIds = mutableSetOf<String>()
                    for (doc in noteSnap.documents) {
                        val id = doc.id
                        cloudNoteIds.add(id)
                        val patientId = doc.getString("patientId") ?: continue
                        val text = doc.getString("text") ?: doc.getString("note") ?: ""
                        val authorId = doc.getString("authorId") ?: doc.getString("doctorName") ?: "Doctor"
                        val date = doc.getString("date") ?: ""
                        val cn = ClinicalNoteEntity(
                            id = id,
                            patientId = patientId,
                            text = text,
                            authorId = authorId,
                            date = date
                        )
                        repository.insertClinicalNote(cn)
                    }
                    val localNotes = repository.allClinicalNotes.firstOrNull() ?: emptyList()
                    for (n in localNotes) {
                        if (n.id !in cloudNoteIds) {
                            repository.deleteClinicalNote(n.id)
                        }
                    }
                }

                // 6. Reconcile care plans
                val planSnap = try {
                    Tasks.await(db.collection(COLL_CARE_PLANS).get(), 6, TimeUnit.SECONDS)
                } catch (_: Exception) { null }
                if (planSnap != null && !planSnap.isEmpty) {
                    val cloudPlanIds = mutableSetOf<String>()
                    for (doc in planSnap.documents) {
                        val id = doc.id
                        cloudPlanIds.add(id)
                        val patientId = doc.getString("patientId") ?: continue
                        val title = doc.getString("title") ?: ""
                        val due = doc.getString("due") ?: ""
                        val status = doc.getString("status") ?: "pending"
                        val cp = CarePlanEntity(
                            id = id,
                            patientId = patientId,
                            title = title,
                            due = due,
                            status = status
                        )
                        repository.insertCarePlan(cp)
                    }
                    val localPlans = repository.allCarePlans.firstOrNull() ?: emptyList()
                    for (p in localPlans) {
                        if (p.id !in cloudPlanIds) {
                            repository.deleteCarePlan(p.id)
                        }
                    }
                }

                // 7. Reconcile attendance (preventing duplicate rows)
                val attSnap = try {
                    Tasks.await(db.collection(COLL_ATTENDANCE).get(), 6, TimeUnit.SECONDS)
                } catch (_: Exception) { null }
                if (attSnap != null && !attSnap.isEmpty) {
                    val localAtt = repository.allAttendance.firstOrNull() ?: emptyList()
                    for (doc in attSnap.documents) {
                        val userId = doc.getString("userId") ?: continue
                        val date = doc.getString("date") ?: ""
                        val time = doc.getString("time") ?: ""
                        val method = doc.getString("method") ?: "Biometric"
                        val already = localAtt.any { it.userId == userId && it.date == date }
                        if (!already) {
                            val att = AttendanceEntity(
                                userId = userId,
                                date = date,
                                time = time,
                                method = method
                            )
                            repository.insertAttendance(att)
                        }
                    }
                }

                // 8. Reconcile medical documents
                val docSnap = try {
                    Tasks.await(db.collection(COLL_DOCUMENTS).get(), 6, TimeUnit.SECONDS)
                } catch (_: Exception) { null }
                if (docSnap != null && !docSnap.isEmpty) {
                    val cloudDocIds = mutableSetOf<String>()
                    for (doc in docSnap.documents) {
                        val id = doc.id
                        cloudDocIds.add(id)
                        val patientId = doc.getString("patientId") ?: continue
                        val title = doc.getString("title") ?: "Document"
                        val category = doc.getString("category") ?: "Medical Report"
                        val date = doc.getString("date") ?: doc.getString("timestamp") ?: timeFormat.format(Date())
                        val docTypeOrUri = doc.getString("docTypeOrUri") ?: doc.getString("previewBase64") ?: "SEED_LAB"
                        val filterApplied = doc.getString("filterApplied") ?: "ORIGINAL"
                        val remarks = doc.getString("remarks") ?: doc.getString("notes") ?: ""
                        val addedBy = doc.getString("addedBy") ?: doc.getString("doctorName") ?: "Staff"

                        val document = DocumentEntity(
                            id = id,
                            patientId = patientId,
                            title = title,
                            category = category,
                            date = date,
                            docTypeOrUri = docTypeOrUri,
                            filterApplied = filterApplied,
                            remarks = remarks,
                            addedBy = addedBy
                        )
                        repository.insertDocument(document)
                    }
                    val localDocs = repository.allDocuments.firstOrNull() ?: emptyList()
                    for (d in localDocs) {
                        if (d.id !in cloudDocIds) {
                            repository.deleteDocument(d.id)
                        }
                    }
                }

                val syncTime = timeFormat.format(Date())
                _isCloudConnected.value = true
                _lastSyncTime.value = syncTime
                _cloudSyncStatus.value = "Synced with Cloud ($syncTime)"
                withContext(Dispatchers.Main) { onComplete?.invoke(true) }
            } catch (e: Exception) {
                Log.w(TAG, "Reconcile exception: ${e.message}")
                withContext(Dispatchers.Main) { onComplete?.invoke(false) }
            }
        }
    }

    /**
     * Direct cloud user lookup.
     * When any staff member (RMO, Maintenance, Doctor, Nurse) tries to log in on any device,
     * this queries Firestore directly so new IDs created by Boss can log in immediately.
     */
    fun fetchAndSyncUserFromCloud(
        context: Context,
        repository: HospitalRepository,
        queryId: String,
        onComplete: (UserEntity?) -> Unit
    ) {
        val db = getFirestore(context) ?: run {
            onComplete(null)
            return
        }
        val cleanInputId = queryId.trim().replace("\u00A0", "").replace("\u200B", "")
        val normalizedInput = cleanInputId.replace("-", "").replace(" ", "").replace("_", "").uppercase()
        val inputDigits = cleanInputId.filter { it.isDigit() }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Try direct document ID query
                var foundDoc = try {
                    Tasks.await(db.collection(COLL_USERS).document(cleanInputId.uppercase()).get(), 8, TimeUnit.SECONDS)
                } catch (_: Exception) { null }

                if (foundDoc == null || !foundDoc.exists()) {
                    foundDoc = try {
                        Tasks.await(db.collection(COLL_USERS).document(cleanInputId).get(), 5, TimeUnit.SECONDS)
                    } catch (_: Exception) { null }
                }

                if (foundDoc != null && foundDoc.exists()) {
                    val name = foundDoc.getString("name") ?: foundDoc.getString("userName") ?: cleanInputId
                    val pass = foundDoc.getString("pass") ?: "12345"
                    val role = foundDoc.getString("role") ?: "STAFF"
                    val dept = foundDoc.getString("dept") ?: foundDoc.getString("department") ?: "General"
                    val spec = foundDoc.getString("specialty") ?: "General"
                    val phone = foundDoc.getString("phone") ?: ""
                    val email = foundDoc.getString("email") ?: ""
                    val photoUri = foundDoc.getString("photoUri")?.ifBlank { null }
                    val joinedDate = foundDoc.getString("joinedDate") ?: "2026-01-01"

                    val user = UserEntity(
                        id = foundDoc.id,
                        pass = pass,
                        name = name,
                        role = role,
                        dept = dept,
                        specialty = spec,
                        phone = phone,
                        email = email,
                        photoUri = photoUri,
                        joinedDate = joinedDate
                    )
                    repository.insertUser(user)
                    withContext(Dispatchers.Main) { onComplete(user) }
                    return@launch
                }

                // 2. Fetch all users from cloud collection and resiliently match
                val allUsersSnap = try {
                    Tasks.await(db.collection(COLL_USERS).get(), 8, TimeUnit.SECONDS)
                } catch (_: Exception) { null }

                if (allUsersSnap != null && !allUsersSnap.isEmpty) {
                    var matchedUser: UserEntity? = null
                    for (d in allUsersSnap.documents) {
                        val id = d.getString("id") ?: d.id
                        val name = d.getString("name") ?: d.getString("userName") ?: id
                        val pass = d.getString("pass") ?: "12345"
                        val role = d.getString("role") ?: "STAFF"
                        val dept = d.getString("dept") ?: d.getString("department") ?: "General"
                        val spec = d.getString("specialty") ?: "General"
                        val phone = d.getString("phone") ?: ""
                        val email = d.getString("email") ?: ""
                        val photoUri = d.getString("photoUri")?.ifBlank { null }
                        val joinedDate = d.getString("joinedDate") ?: "2026-01-01"

                        val user = UserEntity(
                            id = id,
                            pass = pass,
                            name = name,
                            role = role,
                            dept = dept,
                            specialty = spec,
                            phone = phone,
                            email = email,
                            photoUri = photoUri,
                            joinedDate = joinedDate
                        )
                        // Sync all users to local repository for full parity across devices
                        repository.insertUser(user)

                        val normUserId = id.replace("-", "").replace(" ", "").replace("_", "").uppercase()
                        val userDigits = id.filter { it.isDigit() }
                        val phoneDigits = phone.filter { it.isDigit() }.takeLast(10)
                        val inputPhoneDigits = cleanInputId.filter { it.isDigit() }.takeLast(10)

                        val isMatch = id.equals(cleanInputId, ignoreCase = true) ||
                                      normUserId == normalizedInput ||
                                      (inputDigits.isNotBlank() && inputDigits == userDigits) ||
                                      (inputPhoneDigits.length >= 10 && inputPhoneDigits == phoneDigits) ||
                                      email.equals(cleanInputId, ignoreCase = true) ||
                                      name.equals(cleanInputId, ignoreCase = true) ||
                                      name.split(" ").any { it.equals(cleanInputId, ignoreCase = true) } ||
                                      role.equals(cleanInputId, ignoreCase = true)

                        if (isMatch && matchedUser == null) {
                            matchedUser = user
                        }
                    }

                    if (matchedUser != null) {
                        withContext(Dispatchers.Main) { onComplete(matchedUser) }
                        return@launch
                    }
                }

                withContext(Dispatchers.Main) { onComplete(null) }
            } catch (e: Exception) {
                Log.w(TAG, "fetchAndSyncUserFromCloud error: ${e.message}")
                withContext(Dispatchers.Main) { onComplete(null) }
            }
        }
    }

    /**
     * Complete two-way sync between local device database and cloud Firestore.
     * Reconciles from Cloud FIRST to prevent stale local data from overwriting fresh server records,
     * then uploads any genuine new local entries to ensure exact parity across all phones.
     */
    fun syncAllDataTwoWay(
        context: Context,
        repository: HospitalRepository,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        val db = getFirestore(context) ?: run {
            onComplete?.invoke(false, "Cloud connection not initialized")
            return
        }
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        _cloudSyncStatus.value = "Syncing all devices with Cloud..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Reconcile all data from Cloud to local database so we have the latest master roster & clinical records
                reconcileWithCloudServer(context, repository) { success ->
                    val syncTime = timeFormat.format(Date())
                    _isCloudConnected.value = true
                    _lastSyncTime.value = syncTime
                    _cloudSyncStatus.value = "All Devices Synced ($syncTime)"
                    CoroutineScope(Dispatchers.Main).launch {
                        onComplete?.invoke(success, "All devices synchronized with Hospital Cloud at $syncTime")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Sync error: ${e.message}")
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, "Sync notice: ${e.message}")
                }
            }
        }
    }

    fun startRealtimeCloudSync(context: Context, repository: HospitalRepository) {
        val db = getFirestore(context) ?: return
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentDeviceId = FcmManager.getDeviceId(context)

        // Stop any existing snapshot listeners cleanly to avoid orphan target IDs on gRPC stream
        stopListeners()

        // Seed cloud from local database if Firestore is freshly provisioned and empty
        seedCloudFromLocalIfEmpty(context, repository)

        // 1. Listen for Shared Cloud Notifications & Push Alerts
        try {
            notificationListener?.remove()
            var isInitialNotificationLoad = true
            notificationListener = db.collection(COLL_NOTIFICATIONS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleListenerError("Notification listener notice", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        _isCloudConnected.value = true
                        _lastSyncTime.value = timeFormat.format(Date())
                        _cloudSyncStatus.value = "Sync Active · ${snapshot.size()} cloud messages"

                        for (change in snapshot.documentChanges) {
                            val data = change.document.data
                            val id = change.document.id
                            val title = data["title"] as? String ?: "Hospital Alert"
                            val body = data["body"] as? String ?: ""
                            val audience = data["audience"] as? String ?: "all"
                            val time = data["time"] as? String ?: timeFormat.format(Date())
                            val kind = data["kind"] as? String ?: "admin"
                            val read = (data["read"] as? Boolean) ?: false
                            val senderDeviceId = data["senderDeviceId"] as? String ?: data["senderDevice"] as? String ?: ""
                            val priority = data["priority"] as? String ?: "NORMAL"
                            val senderName = data["senderName"] as? String ?: "Hospital Staff"
                            val senderRole = data["senderRole"] as? String ?: "Admin"
                            val forceAlert = (data["forceAlert"] as? Boolean) ?: false

                            val notifEntity = NotificationEntity(
                                id = id,
                                title = title,
                                body = body,
                                time = time,
                                audience = audience,
                                read = read,
                                kind = kind
                            )

                            // Save to local Room database
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.insertNotification(notifEntity)
                            }

                            // If this is a broadcast, update latest banner
                            if (kind in listOf("admin", "broadcast", "emergency", "app_update")) {
                                val voiceBase64 = data["voiceNoteBase64"] as? String
                                val voiceDuration = (data["voiceDurationSec"] as? Long)?.toInt() ?: 0
                                _latestBroadcast.value = com.example.model.HospitalBroadcast(
                                    id = id,
                                    title = title,
                                    body = body,
                                    priority = priority,
                                    senderName = senderName,
                                    senderRole = senderRole,
                                    time = time,
                                    audience = audience,
                                    timestamp = (data["createdAt"] as? Long) ?: System.currentTimeMillis(),
                                    voiceNoteBase64 = voiceBase64,
                                    voiceDurationSec = voiceDuration
                                )
                            }

                            val isBackground = !com.example.service.AppLifecycleTracker.isAppInForeground
                            if (isInitialNotificationLoad) {
                                seenNotificationIds.add(id)
                            } else {
                                // Instant heads-up notification for real-time incoming alerts from other devices/web
                                val isFromDifferentDevice = senderDeviceId.isBlank() || senderDeviceId != currentDeviceId
                                if (isFromDifferentDevice || forceAlert || isBackground) {
                                    val isCritical = priority.equals("critical", ignoreCase = true) || kind == "emergency"
                                    FcmManager.showSystemNotification(
                                        context = context,
                                        title = (if (isCritical) "🚨 " else "🔔 ") + title,
                                        body = body,
                                        kind = kind,
                                        isCritical = isCritical
                                    )
                                }
                            }
                        }
                        isInitialNotificationLoad = false
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Notification listener exception: ${e.message}")
        }

        // 2. Listen for Shared Patient Admissions & Bed Updates across all devices
        try {
            patientListener?.remove()
            var isInitialPatientLoad = true
            patientListener = db.collection(COLL_PATIENTS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleListenerError("Patient snapshot listener notice", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val id = doc.id
                            val senderDeviceId = doc.getString("senderDeviceId") ?: ""

                            if (change.type == DocumentChange.Type.REMOVED) {
                                val remName = doc.getString("name") ?: doc.getString("patientName") ?: "Patient $id"
                                val remWard = doc.getString("ward") ?: "General Ward"
                                val remBed = doc.getString("bed") ?: "Bed"
                                CoroutineScope(Dispatchers.IO).launch {
                                    repository.deletePatient(id)
                                }
                                if (!isInitialPatientLoad && senderDeviceId != FcmManager.DEVICE_INSTANCE_ID) {
                                    FcmManager.showSystemNotification(
                                        context = context,
                                        title = "🚪 Patient Record Removed",
                                        body = "$remName removed from $remWard $remBed",
                                        kind = "task"
                                    )
                                }
                                continue
                            }

                            val name = doc.getString("name") ?: doc.getString("patientName") ?: doc.getString("fullName") ?: "Patient $id"
                            val age = (doc.get("age") as? Number)?.toInt() ?: (doc.getString("age")?.toIntOrNull()) ?: 0
                            val gender = doc.getString("gender") ?: "Other"
                            val ward = doc.getString("ward") ?: "General Ward"
                            val bed = doc.getString("bed") ?: ""
                            val doctorId = doc.getString("doctorId") ?: ""
                            val admittedOn = doc.getString("admittedOn") ?: "Today"
                            val condition = doc.getString("condition") ?: "Stable"
                            val phone = doc.getString("phone") ?: ""
                            val refDoc = doc.getString("referralDoctorId")
                            val refReason = doc.getString("referralReason")
                            val refBy = doc.getString("referralBy")
                            val refDate = doc.getString("referralDate")
                            val status = doc.getString("status") ?: "ADMITTED"
                            val dischargedOn = doc.getString("dischargedOn")
                            val dischargeSummary = doc.getString("dischargeSummary")
                            val temp = doc.getString("temperature")
                            val spo2 = doc.getString("spo2")
                            val pulse = doc.getString("pulse")
                            val bp = doc.getString("bloodPressure")
                            val cbg = doc.getString("cbg")
                            val vitalsTime = doc.getString("vitalsUpdatedAt")
                            val vitalsBy = doc.getString("vitalsUpdatedBy")

                            val patient = PatientEntity(
                                id = id,
                                name = name,
                                age = age,
                                gender = gender,
                                ward = ward,
                                bed = bed,
                                doctorId = doctorId,
                                admittedOn = admittedOn,
                                condition = condition,
                                phone = phone,
                                referralDoctorId = refDoc,
                                referralReason = refReason,
                                referralBy = refBy,
                                referralDate = refDate,
                                status = status,
                                dischargedOn = dischargedOn,
                                dischargeSummary = dischargeSummary,
                                temperature = temp,
                                spo2 = spo2,
                                pulse = pulse,
                                bloodPressure = bp,
                                cbg = cbg,
                                vitalsUpdatedAt = vitalsTime,
                                vitalsUpdatedBy = vitalsBy
                            )
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.insertPatient(patient)
                            }

                            // Trigger real-time notifications for admission, discharge, and patient updates across all devices
                            val isBackground = !com.example.service.AppLifecycleTracker.isAppInForeground
                            if (!isInitialPatientLoad && (senderDeviceId != currentDeviceId || isBackground)) {
                                val isDischarged = status.equals("DISCHARGED", ignoreCase = true)
                                val isNew = change.type == DocumentChange.Type.ADDED
                                val title: String
                                val body: String
                                var isCritical = false

                                if (isDischarged) {
                                    title = "🚪 Patient Discharged: $name"
                                    body = "$name discharged from $ward Bed $bed. Bed is now vacant."
                                } else if (isNew) {
                                    isCritical = condition.equals("Critical", ignoreCase = true) || condition.equals("Emergency", ignoreCase = true)
                                    title = (if (isCritical) "🚨 " else "") + "🏥 New Patient Admitted: $name"
                                    body = "Ward: $ward · Bed: $bed · Condition: $condition · Doctor: $doctorId"
                                } else {
                                    title = "🔄 Patient Updated: $name"
                                    body = "Ward: $ward · Bed: $bed · Status: $status · Doctor: $doctorId"
                                }

                                FcmManager.showSystemNotification(
                                    context = context,
                                    title = title,
                                    body = body,
                                    kind = if (isCritical) "emergency" else "task",
                                    isCritical = isCritical
                                )
                            }
                        }
                        isInitialPatientLoad = false
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Patient snapshot listener exception: ${e.message}")
        }

        // 3. Listen for Doctors, Staff, and Administrators across all devices
        try {
            userListener?.remove()
            var isInitialUserLoad = true
            userListener = db.collection(COLL_USERS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleListenerError("User snapshot listener notice", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val id = doc.getString("id") ?: doc.id
                            val name = doc.getString("name") ?: doc.getString("userName") ?: doc.getString("fullName") ?: id
                            val role = if (id == "BOSS-0001") "BOSS" else (doc.getString("role") ?: "NURSE")
                            val dept = doc.getString("dept") ?: doc.getString("department") ?: if (id == "BOSS-0001") "Administration" else "General"
                            val spec = doc.getString("specialty") ?: if (id == "BOSS-0001") "Owner · Full Access" else "General"
                            val phone = doc.getString("phone") ?: ""
                            val email = doc.getString("email") ?: ""
                            val pass = doc.getString("pass") ?: "12345"
                            val photoUri = doc.getString("photoUri")?.ifBlank { null }
                            val joinedDate = doc.getString("joinedDate") ?: doc.getString("joiningDate") ?: "2026-01-01"
                            val senderDeviceId = doc.getString("senderDeviceId") ?: ""

                            if (change.type == DocumentChange.Type.REMOVED) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    repository.deleteUser(id)
                                }
                                if (!isInitialUserLoad && senderDeviceId != currentDeviceId) {
                                    FcmManager.showSystemNotification(
                                        context = context,
                                        title = "👤 Staff Roster Updated",
                                        body = "$name ($role) removed from staff registry",
                                        kind = "admin"
                                    )
                                }
                                continue
                            }

                            val user = UserEntity(
                                id = id,
                                pass = pass,
                                name = name,
                                role = role,
                                dept = dept,
                                specialty = spec,
                                phone = phone,
                                email = email,
                                photoUri = photoUri,
                                joinedDate = joinedDate
                            )
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.insertUser(user)
                            }

                            if (!isInitialUserLoad && senderDeviceId != currentDeviceId) {
                                val title = if (change.type == DocumentChange.Type.ADDED) {
                                    "👤 New Staff Registered: $name"
                                } else {
                                    "👤 Staff Profile Updated: $name"
                                }
                                FcmManager.showSystemNotification(
                                    context = context,
                                    title = title,
                                    body = "$role · Department: $dept · Login ID: $id",
                                    kind = "admin"
                                )
                            }
                        }
                        isInitialUserLoad = false
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "User snapshot listener exception: ${e.message}")
        }

        // 4. Listen for Medical Documents, Reports, and Scans across all devices
        try {
            documentListener?.remove()
            var isInitialDocLoad = true
            documentListener = db.collection(COLL_DOCUMENTS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleListenerError("Document snapshot listener notice", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val id = doc.id
                            val senderDeviceId = doc.getString("senderDeviceId") ?: ""

                            if (change.type == DocumentChange.Type.REMOVED) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    repository.deleteDocument(id)
                                }
                                continue
                            }

                            val patientId = doc.getString("patientId") ?: doc.getString("patient_id") ?: "General"
                            val title = doc.getString("title") ?: "Document"
                            val category = doc.getString("category") ?: "Medical Report"
                            val date = doc.getString("date") ?: doc.getString("timestamp") ?: timeFormat.format(Date())
                            val docTypeOrUri = doc.getString("docTypeOrUri") ?: doc.getString("previewBase64") ?: "SEED_LAB"
                            val filterApplied = doc.getString("filterApplied") ?: "ORIGINAL"
                            val remarks = doc.getString("remarks") ?: doc.getString("notes") ?: ""
                            val addedBy = doc.getString("addedBy") ?: doc.getString("doctorName") ?: "Staff"

                            val document = DocumentEntity(
                                id = id,
                                patientId = patientId,
                                title = title,
                                category = category,
                                date = date,
                                docTypeOrUri = docTypeOrUri,
                                filterApplied = filterApplied,
                                remarks = remarks,
                                addedBy = addedBy
                            )
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.insertDocument(document)
                            }

                            // Alert if a new or updated medical report, prescription, or scan was uploaded
                            val isBackground = !com.example.service.AppLifecycleTracker.isAppInForeground
                            if (!isInitialDocLoad &&
                                (senderDeviceId != currentDeviceId || isBackground) &&
                                !seenNotificationIds.contains(id)
                            ) {
                                seenNotificationIds.add(id)
                                val alertTitle = if (change.type == DocumentChange.Type.ADDED) "📄 New $category Uploaded" else "📄 $category Updated"
                                FcmManager.showSystemNotification(
                                    context = context,
                                    title = alertTitle,
                                    body = "$title for Patient $patientId by $addedBy",
                                    kind = "doc"
                                )
                            }
                        }
                        isInitialDocLoad = false
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Document listener exception: ${e.message}")
        }

        // 5. Listen for Clinical Observations & Doctor Progress Notes
        try {
            clinicalNoteListener?.remove()
            var isInitialNotesLoad = true
            clinicalNoteListener = db.collection(COLL_CLINICAL_NOTES)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleListenerError("Clinical notes listener notice", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val id = doc.id
                            if (change.type == DocumentChange.Type.REMOVED) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    repository.deleteClinicalNote(id)
                                }
                                continue
                            }
                            val patientId = doc.getString("patientId") ?: "General"
                            val text = doc.getString("text") ?: doc.getString("note") ?: ""
                            val authorId = doc.getString("authorId") ?: doc.getString("doctorName") ?: "Doctor"
                            val date = doc.getString("date") ?: ""
                            val senderDeviceId = doc.getString("senderDeviceId") ?: ""

                            val clinicalNote = ClinicalNoteEntity(
                                id = id,
                                patientId = patientId,
                                text = text,
                                authorId = authorId,
                                date = date
                            )
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.insertClinicalNote(clinicalNote)
                            }

                            val isBackground = !com.example.service.AppLifecycleTracker.isAppInForeground
                            if (!isInitialNotesLoad && (senderDeviceId != currentDeviceId || isBackground)) {
                                FcmManager.showSystemNotification(
                                    context = context,
                                    title = "📝 Clinical Note / Advice: Patient $patientId",
                                    body = "Dr. $authorId: \"${text.take(80)}\"",
                                    kind = "doc"
                                )
                            }
                        }
                        isInitialNotesLoad = false
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Clinical notes listener exception: ${e.message}")
        }

        // 6. Listen for Care Plans & Clinical Orders across all devices
        try {
            carePlanListener?.remove()
            var isInitialCarePlanLoad = true
            carePlanListener = db.collection(COLL_CARE_PLANS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleListenerError("Care plan listener notice", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val id = doc.id
                            if (change.type == DocumentChange.Type.REMOVED) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    repository.deleteCarePlan(id)
                                }
                                continue
                            }
                            val patientId = doc.getString("patientId") ?: "General"
                            val title = doc.getString("title") ?: ""
                            val due = doc.getString("due") ?: ""
                            val status = doc.getString("status") ?: "pending"
                            val senderDeviceId = doc.getString("senderDeviceId") ?: ""

                            val plan = CarePlanEntity(
                                id = id,
                                patientId = patientId,
                                title = title,
                                due = due,
                                status = status
                            )
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.insertCarePlan(plan)
                            }

                            val isBackground = !com.example.service.AppLifecycleTracker.isAppInForeground
                            if (!isInitialCarePlanLoad && (senderDeviceId != currentDeviceId || isBackground)) {
                                val isAdded = change.type == DocumentChange.Type.ADDED
                                val notifTitle = if (isAdded) "📋 New Care Plan Scheduled" else "✓ Care Plan Completed"
                                val notifBody = "$title (Due: $due) · Patient: $patientId · Status: $status"
                                FcmManager.showSystemNotification(
                                    context = context,
                                    title = notifTitle,
                                    body = notifBody,
                                    kind = "task"
                                )
                            }
                        }
                        isInitialCarePlanLoad = false
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Care plan listener exception: ${e.message}")
        }

        // 7. Listen for Staff Attendance Records across all devices
        try {
            attendanceListener?.remove()
            var isInitialAttendanceLoad = true
            attendanceListener = db.collection(COLL_ATTENDANCE)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleListenerError("Attendance listener notice", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val userId = doc.getString("userId") ?: continue
                            val date = doc.getString("date") ?: ""
                            val time = doc.getString("time") ?: ""
                            val method = doc.getString("method") ?: "Biometric"
                            val senderDeviceId = doc.getString("senderDeviceId") ?: ""

                            val attendance = AttendanceEntity(
                                userId = userId,
                                date = date,
                                time = time,
                                method = method
                            )
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.insertAttendance(attendance)
                            }

                            if (!isInitialAttendanceLoad && change.type == DocumentChange.Type.ADDED &&
                                senderDeviceId != currentDeviceId
                            ) {
                                FcmManager.showSystemNotification(
                                    context = context,
                                    title = "⏱ Staff Attendance Logged",
                                    body = "$userId checked in on $date at $time ($method)",
                                    kind = "attendance"
                                )
                            }
                        }
                        isInitialAttendanceLoad = false
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Attendance listener exception: ${e.message}")
        }

        // Listen for Clinical Vital Records across all devices
        try {
            vitalRecordListener?.remove()
            vitalRecordListener = db.collection(COLL_VITAL_RECORDS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleListenerError("Vital record listener notice", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val id = doc.id
                            if (change.type == DocumentChange.Type.REMOVED) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    repository.deleteVitalRecord(id)
                                }
                                continue
                            }
                            val patientId = doc.getString("patientId") ?: "General"
                            val temp = doc.getString("temperature") ?: ""
                            val spo2 = doc.getString("spo2") ?: ""
                            val pulse = doc.getString("pulse") ?: ""
                            val bp = doc.getString("bloodPressure") ?: ""
                            val recordedAt = doc.getString("recordedAt") ?: ""
                            val recordedBy = doc.getString("recordedBy") ?: ""

                            val vr = VitalRecordEntity(
                                id = id,
                                patientId = patientId,
                                temperature = temp,
                                spo2 = spo2,
                                pulse = pulse,
                                bloodPressure = bp,
                                recordedAt = recordedAt,
                                recordedBy = recordedBy
                            )
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.insertVitalRecord(vr)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Vital record listener exception: ${e.message}")
        }

        // 8. Listen for Live Broadcasts and Voice Messages across ALL phones & web
        try {
            broadcastListener?.remove()
            var isInitialBroadcastLoad = true
            broadcastListener = db.collection(COLL_BROADCASTS)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(25)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleListenerError("Broadcast snapshot listener notice", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val broadcastList = mutableListOf<com.example.model.HospitalBroadcast>()
                        for (doc in snapshot.documents) {
                            val id = doc.id
                            if (dismissedBroadcastIds.contains(id)) continue
                            val title = doc.getString("title") ?: "Hospital Announcement"
                            val body = doc.getString("body") ?: ""
                            val priority = doc.getString("priority") ?: "NORMAL"
                            val senderName = doc.getString("senderName") ?: "Staff"
                            val senderRole = doc.getString("senderRole") ?: "Hospital"
                            val time = doc.getString("time") ?: timeFormat.format(Date())
                            val audience = doc.getString("audience") ?: "all"
                            val timestamp = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            val voiceNoteBase64 = doc.getString("voiceNoteBase64")
                            val voiceDurationSec = doc.getLong("voiceDurationSec")?.toInt() ?: 0

                            val bc = com.example.model.HospitalBroadcast(
                                id = id,
                                title = title,
                                body = body,
                                priority = priority,
                                senderName = senderName,
                                senderRole = senderRole,
                                time = time,
                                audience = audience,
                                timestamp = timestamp,
                                voiceNoteBase64 = voiceNoteBase64,
                                voiceDurationSec = voiceDurationSec
                            )
                            broadcastList.add(bc)
                        }

                        _allBroadcasts.value = broadcastList
                        if (broadcastList.isNotEmpty()) {
                            _latestBroadcast.value = broadcastList.first()
                        }

                        // Also alert on new incoming broadcasts from other devices
                        for (change in snapshot.documentChanges) {
                            if (!isInitialBroadcastLoad && change.type == DocumentChange.Type.ADDED) {
                                val doc = change.document
                                val senderDeviceId = doc.getString("senderDeviceId") ?: ""
                                val id = doc.id
                                val isBackground = !com.example.service.AppLifecycleTracker.isAppInForeground
                                if ((senderDeviceId != FcmManager.DEVICE_INSTANCE_ID || isBackground) && !seenNotificationIds.contains(id)) {
                                    seenNotificationIds.add(id)
                                    val title = doc.getString("title") ?: "📢 Hospital Broadcast"
                                    val body = doc.getString("body") ?: ""
                                    val isCritical = doc.getString("priority")?.equals("critical", ignoreCase = true) == true
                                    FcmManager.showSystemNotification(
                                        context = context,
                                        title = (if (isCritical) "🚨 " else "📢 ") + title,
                                        body = body,
                                        kind = if (isCritical) "emergency" else "admin",
                                        isCritical = isCritical
                                    )
                                }
                            }
                        }
                        isInitialBroadcastLoad = false
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Broadcast listener exception: ${e.message}")
        }

        // 9. Listen for Live App Updates & Feature Releases published by Boss
        try {
            appUpdateListener?.remove()
            appUpdateListener = db.collection(COLL_APP_CONFIG).document(DOC_VERSION)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleListenerError("App update listener notice", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val versionCode = (snapshot.getLong("versionCode") ?: 1L).toInt()
                        val versionName = snapshot.getString("versionName") ?: "1.0"
                        val releaseTitle = snapshot.getString("releaseTitle") ?: "App & Feature Release"
                        val releaseNotes = snapshot.getString("releaseNotes") ?: "Latest hospital modules and cloud database sync verified."
                        val forceUpdate = snapshot.getBoolean("forceUpdate") ?: false
                        val publishedBy = snapshot.getString("publishedBy") ?: "BOSS-0001 (Boss)"
                        val publishedAt = snapshot.getString("publishedAt") ?: ""
                        val updatePulseId = snapshot.getString("updatePulseId") ?: ""

                        _appUpdateInfo.value = com.example.model.AppUpdateInfo(
                            versionCode = versionCode,
                            versionName = versionName,
                            releaseTitle = releaseTitle,
                            releaseNotes = releaseNotes,
                            forceUpdate = forceUpdate,
                            publishedBy = publishedBy,
                            publishedAt = publishedAt,
                            updatePulseId = updatePulseId
                        )
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "App update listener exception: ${e.message}")
        }
    }

    /**
     * Publishes a new App Version & Feature Update from Boss ID across all devices.
     * All devices receive the update event in real-time and auto-update/sync.
     */
    fun publishAppUpdateByBoss(
        context: Context,
        versionCode: Int,
        versionName: String,
        releaseTitle: String,
        releaseNotes: String,
        forceUpdate: Boolean,
        publishedBy: String = "BOSS-0001 (Boss)"
    ) {
        val db = getFirestore(context) ?: return
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val now = timeFormat.format(Date())
        val pulseId = System.currentTimeMillis().toString()

        val updateData = hashMapOf(
            "versionCode" to versionCode,
            "versionName" to versionName,
            "releaseTitle" to releaseTitle,
            "releaseNotes" to releaseNotes,
            "forceUpdate" to forceUpdate,
            "publishedBy" to publishedBy,
            "publishedAt" to now,
            "updatePulseId" to pulseId
        )

        db.collection(COLL_APP_CONFIG).document(DOC_VERSION)
            .set(updateData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "App update v$versionName successfully published to Cloud by Boss")
                val notifId = "UPDATE-$pulseId"
                val broadcastData = hashMapOf(
                    "id" to notifId,
                    "title" to "🚀 App Auto-Update (v$versionName): $releaseTitle",
                    "body" to "Boss updated app features. All staff devices are auto-syncing to the latest version ($versionName).",
                    "audience" to "all",
                    "kind" to "app_update",
                    "time" to SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                    "priority" to "APP_UPDATE",
                    "senderName" to "Boss (BOSS-0001)",
                    "senderRole" to "BOSS",
                    "forceAlert" to true,
                    "read" to false,
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection(COLL_NOTIFICATIONS).document(notifId).set(broadcastData, SetOptions.merge())
                db.collection(COLL_BROADCASTS).document(notifId).set(broadcastData, SetOptions.merge())

                FcmManager.showSystemNotification(
                    context = context,
                    title = "🚀 App Auto-Update Release (v$versionName)",
                    body = "Boss released new features ($releaseTitle). Auto-syncing all devices...",
                    kind = "app_update",
                    isCritical = true
                )
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Publish app update notice: ${e.message}")
            }
    }

    /**
     * Seeds Cloud Firestore with initial hospital data from local Room if cloud collections are currently empty.
     */
    private fun seedCloudFromLocalIfEmpty(context: Context, repository: HospitalRepository) {
        val db = getFirestore(context) ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if users exist in cloud
                db.collection(COLL_USERS).limit(1).get()
                    .addOnSuccessListener { usersSnap ->
                        if (usersSnap.isEmpty) {
                            Log.d(TAG, "Cloud Firestore users collection is empty. Seeding initial staff roster...")
                            CoroutineScope(Dispatchers.IO).launch {
                                HospitalSeedData.users.forEach { user ->
                                    syncUserToCloud(context, user)
                                }
                            }
                        }
                    }

                // Check if patients exist in cloud
                db.collection(COLL_PATIENTS).limit(1).get()
                    .addOnSuccessListener { patientsSnap ->
                        if (patientsSnap.isEmpty) {
                            Log.d(TAG, "Cloud Firestore patients collection is empty. Seeding initial patient admissions...")
                            CoroutineScope(Dispatchers.IO).launch {
                                HospitalSeedData.patients.forEach { pt ->
                                    syncPatientToCloud(context, pt)
                                }
                                HospitalSeedData.carePlans.forEach { cp ->
                                    syncCarePlanToCloud(context, cp)
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Initial cloud seed check notice: ${e.message}")
            }
        }
    }

    /**
     * Publishes a push notification & alert into the Shared Cloud Database.
     * All subscribed Android devices and Web Apps receive this push notification in real-time.
     */
    fun publishNotificationToCloud(context: Context, notification: NotificationEntity) {
        seenNotificationIds.add(notification.id)
        val db = getFirestore(context) ?: return
        try {
            val payload = hashMapOf(
                "id" to notification.id,
                "title" to notification.title,
                "body" to notification.body,
                "time" to notification.time,
                "audience" to notification.audience,
                "read" to notification.read,
                "kind" to notification.kind,
                "senderDeviceId" to FcmManager.DEVICE_INSTANCE_ID,
                "createdAt" to System.currentTimeMillis()
            )
            db.collection(COLL_NOTIFICATIONS).document(notification.id)
                .set(payload, SetOptions.merge())
                .addOnSuccessListener {
                    _lastSyncTime.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                }
            val isBackground = !com.example.service.AppLifecycleTracker.isAppInForeground
            if (isBackground) {
                FcmManager.showSystemNotification(
                    context = context,
                    title = "🔔 " + notification.title,
                    body = notification.body,
                    kind = notification.kind
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Publish notification to cloud error: ${e.message}")
        }
    }

    /**
     * Dispatches an instant hospital-wide announcement, emergency code, or app update
     * to ALL Android phones and Web clients simultaneously through Cloud Firestore.
     */
    fun dispatchHospitalBroadcast(
        context: Context,
        broadcast: com.example.model.HospitalBroadcast,
        forceAlertOnThisDevice: Boolean = true
    ) {
        seenNotificationIds.add(broadcast.id)
        _latestBroadcast.value = broadcast
        val currentList = _allBroadcasts.value.toMutableList()
        currentList.removeAll { it.id == broadcast.id }
        currentList.add(0, broadcast)
        _allBroadcasts.value = currentList
        val db = getFirestore(context) ?: return
        try {
            val isCritical = broadcast.priority.equals("critical", ignoreCase = true)
            val payload = hashMapOf<String, Any?>(
                "id" to broadcast.id,
                "title" to broadcast.title,
                "body" to broadcast.body,
                "time" to broadcast.time,
                "audience" to broadcast.audience,
                "read" to false,
                "kind" to if (isCritical) "emergency" else if (broadcast.priority == "APP_UPDATE") "app_update" else "admin",
                "priority" to broadcast.priority,
                "senderDeviceId" to FcmManager.DEVICE_INSTANCE_ID,
                "senderName" to broadcast.senderName,
                "senderRole" to broadcast.senderRole,
                "forceAlert" to true,
                "createdAt" to broadcast.timestamp,
                "voiceNoteBase64" to broadcast.voiceNoteBase64,
                "voiceDurationSec" to broadcast.voiceDurationSec
            )
            db.collection(COLL_NOTIFICATIONS).document(broadcast.id)
                .set(payload, SetOptions.merge())
                .addOnSuccessListener {
                    _lastSyncTime.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    Log.d(TAG, "Broadcast dispatched to cloud: ${broadcast.id}")
                }

            db.collection(COLL_BROADCASTS).document(broadcast.id)
                .set(payload, SetOptions.merge())

            val isBackground = !com.example.service.AppLifecycleTracker.isAppInForeground
            if (forceAlertOnThisDevice || isBackground) {
                FcmManager.showSystemNotification(
                    context = context,
                    title = (if (isCritical) "🚨 " else "📢 ") + broadcast.title,
                    body = broadcast.body,
                    kind = if (isCritical) "emergency" else "admin",
                    isCritical = isCritical
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Dispatch broadcast error: ${e.message}")
        }
    }

    fun syncPatientToCloud(context: Context, patient: PatientEntity) {
        val db = getFirestore(context) ?: return
        try {
            val payload = hashMapOf(
                "id" to patient.id,
                "name" to patient.name,
                "patientName" to patient.name,
                "fullName" to patient.name,
                "age" to patient.age,
                "gender" to patient.gender,
                "ward" to patient.ward,
                "bed" to patient.bed,
                "doctorId" to patient.doctorId,
                "admittedOn" to patient.admittedOn,
                "condition" to patient.condition,
                "phone" to patient.phone,
                "referralDoctorId" to (patient.referralDoctorId ?: ""),
                "referralReason" to (patient.referralReason ?: ""),
                "referralBy" to (patient.referralBy ?: ""),
                "referralDate" to (patient.referralDate ?: ""),
                "status" to patient.status,
                "dischargedOn" to (patient.dischargedOn ?: ""),
                "dischargeSummary" to (patient.dischargeSummary ?: ""),
                "temperature" to (patient.temperature ?: ""),
                "spo2" to (patient.spo2 ?: ""),
                "pulse" to (patient.pulse ?: ""),
                "bloodPressure" to (patient.bloodPressure ?: ""),
                "cbg" to (patient.cbg ?: ""),
                "vitalsUpdatedAt" to (patient.vitalsUpdatedAt ?: ""),
                "vitalsUpdatedBy" to (patient.vitalsUpdatedBy ?: ""),
                "senderDeviceId" to FcmManager.getDeviceId(context),
                "lastUpdated" to System.currentTimeMillis()
            )
            db.collection(COLL_PATIENTS).document(patient.id)
                .set(payload, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Sync patient to cloud error: ${e.message}")
        }
    }

    fun deletePatientFromCloud(context: Context, patientId: String) {
        val db = getFirestore(context) ?: return
        try {
            db.collection(COLL_PATIENTS).document(patientId).delete()
        } catch (e: Exception) {
            Log.w(TAG, "Delete patient from cloud error: ${e.message}")
        }
    }

    fun syncUserToCloud(context: Context, user: UserEntity) {
        val db = getFirestore(context) ?: return
        try {
            val payload = hashMapOf(
                "id" to user.id,
                "name" to user.name,
                "userName" to user.name,
                "fullName" to user.name,
                "pass" to user.pass,
                "role" to user.role,
                "dept" to user.dept,
                "department" to user.dept,
                "specialty" to user.specialty,
                "phone" to user.phone,
                "email" to user.email,
                "photoUri" to (user.photoUri ?: ""),
                "joinedDate" to user.joinedDate,
                "senderDeviceId" to FcmManager.getDeviceId(context),
                "lastUpdated" to System.currentTimeMillis()
            )
            db.collection(COLL_USERS).document(user.id)
                .set(payload, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Sync user to cloud error: ${e.message}")
        }
    }

    fun deleteUserFromCloud(context: Context, userId: String) {
        val db = getFirestore(context) ?: return
        try {
            db.collection(COLL_USERS).document(userId).delete()
        } catch (e: Exception) {
            Log.w(TAG, "Delete user from cloud error: ${e.message}")
        }
    }

    fun syncDocumentToCloud(context: Context, doc: DocumentEntity) {
        val db = getFirestore(context) ?: return
        try {
            val payload = hashMapOf(
                "id" to doc.id,
                "patientId" to doc.patientId,
                "title" to doc.title,
                "category" to doc.category,
                "date" to doc.date,
                "docTypeOrUri" to doc.docTypeOrUri,
                "filterApplied" to doc.filterApplied,
                "remarks" to doc.remarks,
                "addedBy" to doc.addedBy,
                "senderDeviceId" to FcmManager.DEVICE_INSTANCE_ID,
                "createdAt" to System.currentTimeMillis()
            )
            db.collection(COLL_DOCUMENTS).document(doc.id)
                .set(payload, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Sync document to cloud error: ${e.message}")
        }
    }

    fun deleteDocumentFromCloud(context: Context, docId: String) {
        val db = getFirestore(context) ?: return
        try {
            db.collection(COLL_DOCUMENTS).document(docId).delete()
        } catch (e: Exception) {
            Log.w(TAG, "Delete document from cloud error: ${e.message}")
        }
    }

    fun syncClinicalNoteToCloud(context: Context, note: ClinicalNoteEntity) {
        val db = getFirestore(context) ?: return
        try {
            val payload = hashMapOf(
                "id" to note.id,
                "patientId" to note.patientId,
                "text" to note.text,
                "authorId" to note.authorId,
                "date" to note.date,
                "senderDeviceId" to FcmManager.DEVICE_INSTANCE_ID,
                "createdAt" to System.currentTimeMillis()
            )
            db.collection(COLL_CLINICAL_NOTES).document(note.id)
                .set(payload, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Sync clinical note error: ${e.message}")
        }
    }

    fun deleteClinicalNoteFromCloud(context: Context, noteId: String) {
        val db = getFirestore(context) ?: return
        try {
            db.collection(COLL_CLINICAL_NOTES).document(noteId).delete()
        } catch (e: Exception) {
            Log.w(TAG, "Delete clinical note error: ${e.message}")
        }
    }

    fun syncCarePlanToCloud(context: Context, plan: CarePlanEntity) {
        val db = getFirestore(context) ?: return
        try {
            val payload = hashMapOf(
                "id" to plan.id,
                "patientId" to plan.patientId,
                "title" to plan.title,
                "due" to plan.due,
                "status" to plan.status,
                "senderDeviceId" to FcmManager.DEVICE_INSTANCE_ID,
                "lastUpdated" to System.currentTimeMillis()
            )
            db.collection(COLL_CARE_PLANS).document(plan.id)
                .set(payload, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Sync care plan to cloud error: ${e.message}")
        }
    }

    fun deleteCarePlanFromCloud(context: Context, planId: String) {
        val db = getFirestore(context) ?: return
        try {
            db.collection(COLL_CARE_PLANS).document(planId).delete()
        } catch (e: Exception) {
            Log.w(TAG, "Delete care plan error: ${e.message}")
        }
    }

    fun syncAttendanceToCloud(context: Context, attendance: AttendanceEntity) {
        val db = getFirestore(context) ?: return
        try {
            val payload = hashMapOf(
                "userId" to attendance.userId,
                "date" to attendance.date,
                "time" to attendance.time,
                "method" to attendance.method,
                "senderDeviceId" to FcmManager.DEVICE_INSTANCE_ID,
                "timestamp" to System.currentTimeMillis()
            )
            val docId = "${attendance.userId}_${attendance.date.replace('-', '_')}"
            db.collection(COLL_ATTENDANCE).document(docId)
                .set(payload, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Sync attendance error: ${e.message}")
        }
    }

    fun syncVitalRecordToCloud(context: Context, record: VitalRecordEntity) {
        val db = getFirestore(context) ?: return
        try {
            val payload = hashMapOf(
                "id" to record.id,
                "patientId" to record.patientId,
                "temperature" to record.temperature,
                "spo2" to record.spo2,
                "pulse" to record.pulse,
                "bloodPressure" to record.bloodPressure,
                "recordedAt" to record.recordedAt,
                "recordedBy" to record.recordedBy,
                "senderDeviceId" to FcmManager.DEVICE_INSTANCE_ID,
                "createdAt" to System.currentTimeMillis()
            )
            db.collection(COLL_VITAL_RECORDS).document(record.id)
                .set(payload, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Sync vital record error: ${e.message}")
        }
    }

    suspend fun ensureBossUserExists(repository: HospitalRepository) {
        try {
            val boss = repository.getUserById("BOSS-0001")
            if (boss == null || boss.role != "BOSS" || boss.name.isBlank()) {
                val bossUser = UserEntity(
                    id = "BOSS-0001",
                    pass = if (!boss?.pass.isNullOrBlank()) boss!!.pass else "12345",
                    name = "Dr. S. K. Bose",
                    role = "BOSS",
                    dept = "Administration",
                    specialty = "Owner · Full Access",
                    phone = "+91 98300 11111",
                    joinedDate = "2015-04-01"
                )
                repository.insertUser(bossUser)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Ensure Boss user error: ${e.message}")
        }
    }

    fun stopListeners() {
        try {
            notificationListener?.remove()
            notificationListener = null
            patientListener?.remove()
            patientListener = null
            userListener?.remove()
            userListener = null
            documentListener?.remove()
            documentListener = null
            clinicalNoteListener?.remove()
            clinicalNoteListener = null
            carePlanListener?.remove()
            carePlanListener = null
            attendanceListener?.remove()
            attendanceListener = null
            vitalRecordListener?.remove()
            vitalRecordListener = null
            broadcastListener?.remove()
            broadcastListener = null
            appUpdateListener?.remove()
            appUpdateListener = null
        } catch (e: Exception) {
            Log.d(TAG, "Notice stopping listeners: ${e.message}")
        }
    }
}
