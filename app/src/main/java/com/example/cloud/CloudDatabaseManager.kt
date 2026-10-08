package com.example.cloud

import android.content.Context
import android.util.Log
import com.example.data.AttendanceEntity
import com.example.data.CarePlanEntity
import com.example.data.ClinicalNoteEntity
import com.example.data.DocumentEntity
import com.example.data.HospitalRepository
import com.example.data.NotificationEntity
import com.example.data.PatientEntity
import com.example.data.UserEntity
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
import kotlinx.coroutines.launch
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

    private val _isCloudConnected = MutableStateFlow(false)
    val isCloudConnected: StateFlow<Boolean> = _isCloudConnected.asStateFlow()

    private val _cloudSyncStatus = MutableStateFlow("Initializing Cloud Database...")
    val cloudSyncStatus: StateFlow<String> = _cloudSyncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime.asStateFlow()

    private val _latestBroadcast = MutableStateFlow<com.example.model.HospitalBroadcast?>(
        com.example.model.HospitalBroadcast(
            id = "init_broadcast",
            title = "🚀 Real-Time Multi-Device Hospital Sync Active",
            body = "Automatic instant push notifications and shared database active across all doctors, staff, and devices.",
            priority = "APP_UPDATE",
            senderName = "Rita Sharma (Admin)",
            senderRole = "Administrator",
            time = "Today",
            audience = "all"
        )
    )
    val latestBroadcast: StateFlow<com.example.model.HospitalBroadcast?> = _latestBroadcast.asStateFlow()

    private val seenNotificationIds = mutableSetOf<String>()

    fun clearLatestBroadcast() {
        _latestBroadcast.value = null
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

    private var firestoreInstance: FirebaseFirestore? = null

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
    fun startRealtimeCloudSync(context: Context, repository: HospitalRepository) {
        val db = getFirestore(context) ?: return
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Seed cloud from local database if Firestore is freshly provisioned and empty
        seedCloudFromLocalIfEmpty(context, repository)

        // 1. Listen for Shared Cloud Notifications & Push Alerts
        try {
            notificationListener?.remove()
            notificationListener = db.collection(COLL_NOTIFICATIONS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Notification listener notice: ${error.message}")
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
                            val senderDeviceId = data["senderDeviceId"] as? String ?: data["senderDevice"] as? String
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

                            // Trigger instant notification if from another device
                            if (!seenNotificationIds.contains(id)) {
                                seenNotificationIds.add(id)
                                val isFromDifferentDevice = senderDeviceId != FcmManager.DEVICE_INSTANCE_ID
                                if (isFromDifferentDevice || forceAlert) {
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
                        Log.w(TAG, "Patient snapshot listener notice: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val id = doc.id
                            val name = doc.getString("name") ?: continue
                            val age = doc.getLong("age")?.toInt() ?: 0
                            val gender = doc.getString("gender") ?: "Other"
                            val ward = doc.getString("ward") ?: "General Ward"
                            val bed = doc.getString("bed") ?: "G-101"
                            val doctorId = doc.getString("doctorId") ?: "DOC-0001"
                            val admittedOn = doc.getString("admittedOn") ?: "Today"
                            val condition = doc.getString("condition") ?: "Stable"
                            val phone = doc.getString("phone") ?: ""
                            val refDoc = doc.getString("referralDoctorId")
                            val refReason = doc.getString("referralReason")
                            val refBy = doc.getString("referralBy")
                            val refDate = doc.getString("referralDate")
                            val senderDeviceId = doc.getString("senderDeviceId") ?: ""

                            if (change.type == DocumentChange.Type.REMOVED) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    repository.deletePatient(id)
                                }
                                if (!isInitialPatientLoad && senderDeviceId != FcmManager.DEVICE_INSTANCE_ID) {
                                    FcmManager.showSystemNotification(
                                        context = context,
                                        title = "🏥 Patient Discharged",
                                        body = "$name discharged from $ward Bed $bed",
                                        kind = "task"
                                    )
                                }
                                continue
                            }

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
                                referralDate = refDate
                            )
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.insertPatient(patient)
                            }

                            // Trigger background notification when new patient is admitted or updated on another device
                            if (!isInitialPatientLoad && senderDeviceId != FcmManager.DEVICE_INSTANCE_ID) {
                                val isNew = change.type == DocumentChange.Type.ADDED
                                val title = if (isNew) "🏥 New Patient Admitted: $name" else "🔄 Patient Updated: $name"
                                val body = "Ward: $ward · Bed: $bed · Condition: $condition · Doctor: $doctorId"
                                val isCritical = condition.equals("Critical", ignoreCase = true) || condition.equals("Emergency", ignoreCase = true)
                                FcmManager.showSystemNotification(
                                    context = context,
                                    title = (if (isCritical) "🚨 " else "") + title,
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
                        Log.w(TAG, "User snapshot listener notice: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val id = doc.id
                            val name = doc.getString("name") ?: continue
                            val role = doc.getString("role") ?: "NURSE"
                            val dept = doc.getString("dept") ?: doc.getString("department") ?: "General"
                            val spec = doc.getString("specialty") ?: "General"
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
                                if (!isInitialUserLoad && senderDeviceId != FcmManager.DEVICE_INSTANCE_ID) {
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

                            if (!isInitialUserLoad && senderDeviceId != FcmManager.DEVICE_INSTANCE_ID) {
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
                        Log.w(TAG, "Document snapshot listener notice: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val id = doc.id
                            val patientId = doc.getString("patientId") ?: continue
                            val title = doc.getString("title") ?: "Document"
                            val category = doc.getString("category") ?: "Medical Report"
                            val date = doc.getString("date") ?: doc.getString("timestamp") ?: timeFormat.format(Date())
                            val docTypeOrUri = doc.getString("docTypeOrUri") ?: doc.getString("previewBase64") ?: "SEED_LAB"
                            val filterApplied = doc.getString("filterApplied") ?: "ORIGINAL"
                            val remarks = doc.getString("remarks") ?: doc.getString("notes") ?: ""
                            val addedBy = doc.getString("addedBy") ?: doc.getString("doctorName") ?: "Staff"
                            val senderDeviceId = doc.getString("senderDeviceId") ?: ""

                            if (change.type == DocumentChange.Type.REMOVED) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    repository.deleteDocument(id)
                                }
                                continue
                            }

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

                            // Alert if a new medical report or scan was uploaded from another phone
                            if (!isInitialDocLoad && change.type == DocumentChange.Type.ADDED &&
                                senderDeviceId != FcmManager.DEVICE_INSTANCE_ID &&
                                !seenNotificationIds.contains(id)
                            ) {
                                seenNotificationIds.add(id)
                                FcmManager.showSystemNotification(
                                    context = context,
                                    title = "📄 New $category Available",
                                    body = "$title for Patient $patientId uploaded by $addedBy",
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
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val id = doc.id
                            val patientId = doc.getString("patientId") ?: continue
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

                            if (!isInitialNotesLoad && change.type == DocumentChange.Type.ADDED &&
                                senderDeviceId != FcmManager.DEVICE_INSTANCE_ID
                            ) {
                                FcmManager.showSystemNotification(
                                    context = context,
                                    title = "📝 Clinical Note: Patient $patientId",
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
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val id = doc.id
                            val patientId = doc.getString("patientId") ?: continue
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

                            if (!isInitialCarePlanLoad && senderDeviceId != FcmManager.DEVICE_INSTANCE_ID) {
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
                    if (error != null) return@addSnapshotListener
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
                                senderDeviceId != FcmManager.DEVICE_INSTANCE_ID
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
                                val localUsers = repository.allUsers.firstOrNull() ?: emptyList()
                                localUsers.forEach { user ->
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
                                val localPatients = repository.allPatients.firstOrNull() ?: emptyList()
                                localPatients.forEach { pt ->
                                    syncPatientToCloud(context, pt)
                                }
                                val localCarePlans = repository.allCarePlans.firstOrNull() ?: emptyList()
                                localCarePlans.forEach { cp ->
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

            if (forceAlertOnThisDevice) {
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
                "senderDeviceId" to FcmManager.DEVICE_INSTANCE_ID,
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
                "pass" to user.pass,
                "role" to user.role,
                "dept" to user.dept,
                "department" to user.dept,
                "specialty" to user.specialty,
                "phone" to user.phone,
                "email" to user.email,
                "photoUri" to (user.photoUri ?: ""),
                "joinedDate" to user.joinedDate,
                "senderDeviceId" to FcmManager.DEVICE_INSTANCE_ID,
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

    fun stopListeners() {
        notificationListener?.remove()
        patientListener?.remove()
        userListener?.remove()
        documentListener?.remove()
        clinicalNoteListener?.remove()
        carePlanListener?.remove()
        attendanceListener?.remove()
    }
}
