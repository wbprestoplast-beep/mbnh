package com.example.cloud

import android.content.Context
import android.util.Log
import com.example.data.AttendanceEntity
import com.example.data.CarePlanEntity
import com.example.data.HospitalRepository
import com.example.data.NotificationEntity
import com.example.data.PatientEntity
import com.example.fcm.FcmManager
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CloudDatabaseManager {
    private const val TAG = "CloudDatabaseManager"

    // Shared Cloud Firestore Collection Names
    const val COLL_PATIENTS = "hospital_patients"
    const val COLL_NOTIFICATIONS = "hospital_notifications"
    const val COLL_CARE_PLANS = "hospital_care_plans"
    const val COLL_ATTENDANCE = "hospital_attendance"
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
            title = "🚀 System Update: Real-Time Multi-Phone Cloud Sync Active",
            body = "Instant push alerts and shared cloud database operational across all Android phones and Web clients.",
            priority = "APP_UPDATE",
            senderName = "Rita Sharma (Admin)",
            senderRole = "Administrator",
            time = "Today",
            audience = "all"
        )
    )
    val latestBroadcast: StateFlow<com.example.model.HospitalBroadcast?> = _latestBroadcast.asStateFlow()

    private val seenNotificationIds = mutableSetOf<String>()
    private var notificationListener: ListenerRegistration? = null
    private var patientListener: ListenerRegistration? = null
    private var carePlanListener: ListenerRegistration? = null

    private var firestoreInstance: FirebaseFirestore? = null

    private fun getFirestore(context: Context): FirebaseFirestore? {
        if (firestoreInstance != null) return firestoreInstance
        return try {
            FcmManager.initialize(context)
            val db = FirebaseFirestore.getInstance()
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
     * Starts listening to shared Cloud Firestore collections:
     * 1. hospital_notifications: Detects new cloud broadcasts/alerts and immediately fires
     *    system push notifications via FcmManager while syncing into local Room DB.
     * 2. hospital_patients: Syncs admitted patients bidirectionally.
     * 3. hospital_care_plans: Syncs task updates across devices and web portal.
     */
    fun startRealtimeCloudSync(context: Context, repository: HospitalRepository) {
        val db = getFirestore(context) ?: return
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        // 1. Listen for Shared Cloud Notifications & Push Alerts
        try {
            notificationListener?.remove()
            notificationListener = db.collection(COLL_NOTIFICATIONS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Notification listener error: ${error.message}")
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

                            // If this is a broadcast or system announcement, update latest broadcast banner
                            if (kind in listOf("admin", "broadcast", "emergency", "app_update")) {
                                _latestBroadcast.value = com.example.model.HospitalBroadcast(
                                    id = id,
                                    title = title,
                                    body = body,
                                    priority = priority,
                                    senderName = senderName,
                                    senderRole = senderRole,
                                    time = time,
                                    audience = audience,
                                    timestamp = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
                                )
                            }

                            // If this is a newly arrived cloud document that we haven't alerted for:
                            if (!seenNotificationIds.contains(id)) {
                                seenNotificationIds.add(id)

                                // Trigger instant push notification across phones:
                                // If from another phone/web OR if forceAlert is true
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
            Log.w(TAG, "Failed attaching notification snapshot listener: ${e.message}")
        }

        // 2. Listen for Shared Cloud Patients
        try {
            patientListener?.remove()
            patientListener = db.collection(COLL_PATIENTS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Patient listener error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        for (doc in snapshot.documents) {
                            val pId = doc.id
                            val name = doc.getString("name") ?: continue
                            val age = doc.getLong("age")?.toInt() ?: 0
                            val gender = doc.getString("gender") ?: "M"
                            val ward = doc.getString("ward") ?: "General Ward"
                            val bed = doc.getString("bed") ?: "G-101"
                            val doctorId = doc.getString("doctorId") ?: "DOC-2001"
                            val admittedOn = doc.getString("admittedOn") ?: ""
                            val condition = doc.getString("condition") ?: "Observation"
                            val phone = doc.getString("phone") ?: ""
                            val refDoc = doc.getString("referralDoctorId")
                            val refReason = doc.getString("referralReason")
                            val refBy = doc.getString("referralBy")
                            val refDate = doc.getString("referralDate")

                            val patient = PatientEntity(
                                id = pId,
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
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed attaching patient snapshot listener: ${e.message}")
        }

        // 3. Listen for Shared Care Plans
        try {
            carePlanListener?.remove()
            carePlanListener = db.collection(COLL_CARE_PLANS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        for (doc in snapshot.documents) {
                            val id = doc.id
                            val patientId = doc.getString("patientId") ?: continue
                            val title = doc.getString("title") ?: ""
                            val due = doc.getString("due") ?: ""
                            val status = doc.getString("status") ?: "pending"

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
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed attaching care plan snapshot listener: ${e.message}")
        }
    }

    /**
     * Publishes a push notification & broadcast into the Shared Cloud Database.
     * All subscribed Android devices and Web Apps will receive this push notification in real-time.
     */
    fun publishNotificationToCloud(context: Context, notification: NotificationEntity) {
        seenNotificationIds.add(notification.id)
        val db = getFirestore(context) ?: return
        try {
            val payload = hashMapOf(
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
                    Log.d(TAG, "Notification published to Cloud Database: ${notification.id}")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed publishing notification to cloud: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud publish error: ${e.message}")
        }
    }

    /**
     * Dispatches an instant hospital-wide announcement, emergency code, or app update
     * to ALL Android phones and Web clients simultaneously through Cloud Firestore & FCM.
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
            val payload = hashMapOf(
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
                "createdAt" to broadcast.timestamp
            )
            db.collection(COLL_NOTIFICATIONS).document(broadcast.id)
                .set(payload, SetOptions.merge())
                .addOnSuccessListener {
                    _lastSyncTime.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    Log.d(TAG, "Broadcast dispatched to all devices: ${broadcast.id}")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed dispatching broadcast to cloud: ${e.message}")
                }

            // Also mirror to dedicated broadcasts collection for persistent history
            db.collection(COLL_BROADCASTS).document(broadcast.id)
                .set(payload, SetOptions.merge())

            // Trigger instant alert on this phone
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
                "lastUpdated" to System.currentTimeMillis()
            )
            db.collection(COLL_PATIENTS).document(patient.id)
                .set(payload, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Sync patient to cloud error: ${e.message}")
        }
    }

    fun syncCarePlanToCloud(context: Context, plan: CarePlanEntity) {
        val db = getFirestore(context) ?: return
        try {
            val payload = hashMapOf(
                "patientId" to plan.patientId,
                "title" to plan.title,
                "due" to plan.due,
                "status" to plan.status,
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
                "timestamp" to System.currentTimeMillis()
            )
            val docId = "${attendance.userId}_${attendance.date}"
            db.collection(COLL_ATTENDANCE).document(docId)
                .set(payload, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Sync attendance error: ${e.message}")
        }
    }

    fun stopListeners() {
        notificationListener?.remove()
        patientListener?.remove()
        carePlanListener?.remove()
    }
}
