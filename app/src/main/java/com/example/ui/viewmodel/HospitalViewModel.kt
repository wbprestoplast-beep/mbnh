package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AttendanceEntity
import com.example.data.CarePlanEntity
import com.example.data.ClinicalNoteEntity
import com.example.data.DocumentEntity
import com.example.data.HospitalRepository
import com.example.data.NotificationEntity
import com.example.data.PatientEntity
import com.example.data.UserEntity
import com.example.model.AppScreen
import com.example.model.BroadcastPriority
import com.example.model.DocCategory
import com.example.model.DocumentFilter
import com.example.model.HospitalBroadcast
import com.example.model.UserRole
import com.example.ui.theme.AppThemeSetting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class HospitalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HospitalRepository
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val todayDate: String get() = dateFormat.format(Date())
    val currentTime: String get() = timeFormat.format(Date())

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = HospitalRepository(db.hospitalDao())
        viewModelScope.launch {
            repository.ensureSeeded()
            // Start real-time Firestore sync & cloud push notification listener
            com.example.cloud.CloudDatabaseManager.startRealtimeCloudSync(application, repository)
        }
    }

    // Database Streams
    val users: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val patients: StateFlow<List<PatientEntity>> = repository.allPatients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<DocumentEntity>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clinicalNotes: StateFlow<List<ClinicalNoteEntity>> = repository.allClinicalNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val carePlans: StateFlow<List<CarePlanEntity>> = repository.allCarePlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendance: StateFlow<List<AttendanceEntity>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // FCM Push Messaging Streams
    val fcmToken: StateFlow<String?> = com.example.fcm.FcmManager.fcmToken
    val fcmSubscribedTopics: StateFlow<Set<String>> = com.example.fcm.FcmManager.subscribedTopics

    // Cloud Database Shared Streams
    val isCloudConnected: StateFlow<Boolean> = com.example.cloud.CloudDatabaseManager.isCloudConnected
    val cloudSyncStatus: StateFlow<String> = com.example.cloud.CloudDatabaseManager.cloudSyncStatus
    val lastCloudSyncTime: StateFlow<String?> = com.example.cloud.CloudDatabaseManager.lastSyncTime
    val latestBroadcast: StateFlow<HospitalBroadcast?> = com.example.cloud.CloudDatabaseManager.latestBroadcast

    // Active User & Navigation
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _screenStack = MutableStateFlow<List<AppScreen>>(listOf(AppScreen.HOME))
    val screenStack: StateFlow<List<AppScreen>> = _screenStack.asStateFlow()

    val currentScreen: AppScreen
        get() = _screenStack.value.lastOrNull() ?: AppScreen.HOME

    private val _selectedPatientId = MutableStateFlow<String?>(null)
    val selectedPatientId: StateFlow<String?> = _selectedPatientId.asStateFlow()

    private val _selectedDocument = MutableStateFlow<DocumentEntity?>(null)
    val selectedDocument: StateFlow<DocumentEntity?> = _selectedDocument.asStateFlow()

    private val _scannerOpen = MutableStateFlow(false)
    val scannerOpen: StateFlow<Boolean> = _scannerOpen.asStateFlow()

    private val _scannerTargetPatientId = MutableStateFlow<String?>(null)
    val scannerTargetPatientId: StateFlow<String?> = _scannerTargetPatientId.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _themeSetting = MutableStateFlow(AppThemeSetting.AUTO)
    val themeSetting: StateFlow<AppThemeSetting> = _themeSetting.asStateFlow()

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun setThemeSetting(setting: AppThemeSetting) {
        _themeSetting.value = setting
    }

    // Auth
    fun login(id: String, pass: String): Boolean {
        val user = users.value.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }
        if (user != null && user.pass == pass.trim()) {
            _currentUser.value = user
            _screenStack.value = listOf(AppScreen.HOME)
            showToast("Welcome back, ${user.name.split(" ").firstOrNull() ?: user.name} 👋")
            return true
        }
        return false
    }

    fun resetPassword(id: String, newPass: String): Boolean {
        val user = users.value.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }
        if (user != null) {
            val passToSet = newPass.trim().ifBlank { "12345" }
            viewModelScope.launch {
                repository.updateUserPassword(user.id, passToSet)
                repository.insertNotification(
                    NotificationEntity(
                        id = "nt_" + UUID.randomUUID().toString().take(8),
                        title = "Password reset completed",
                        body = "Password for ${user.name} (${user.id}) was successfully updated.",
                        time = currentTime,
                        audience = user.id,
                        kind = "admin"
                    )
                )
            }
            showToast("✓ Password updated for ${user.name}")
            return true
        }
        return false
    }

    fun logout() {
        _currentUser.value = null
        _screenStack.value = listOf(AppScreen.HOME)
        showToast("Logged out")
    }

    // Navigation
    fun navigateTo(screen: AppScreen) {
        _screenStack.value = listOf(screen)
    }

    fun pushScreen(screen: AppScreen) {
        _screenStack.value = _screenStack.value + screen
    }

    fun navigateBack(): Boolean {
        if (_screenStack.value.size > 1) {
            _screenStack.value = _screenStack.value.dropLast(1)
            return true
        }
        return false
    }

    fun openPatientDetail(patientId: String) {
        _selectedPatientId.value = patientId
        pushScreen(AppScreen.PATIENT_DETAIL)
    }

    fun openScanner(patientId: String? = null) {
        _scannerTargetPatientId.value = patientId ?: patients.value.firstOrNull()?.id
        _scannerOpen.value = true
    }

    fun closeScanner() {
        _scannerOpen.value = false
        _scannerTargetPatientId.value = null
    }

    fun openDocumentViewer(doc: DocumentEntity) {
        _selectedDocument.value = doc
    }

    fun closeDocumentViewer() {
        _selectedDocument.value = null
    }

    // Patient Actions
    fun admitPatient(
        name: String,
        age: Int,
        gender: String,
        phone: String,
        ward: String,
        bed: String,
        doctorId: String,
        condition: String
    ) {
        val newIdNum = 5000 + patients.value.size + 1
        val newPatient = PatientEntity(
            id = "PT-$newIdNum",
            name = name,
            age = age,
            gender = gender,
            ward = ward,
            bed = bed,
            doctorId = doctorId,
            admittedOn = todayDate,
            condition = condition,
            phone = phone
        )
        viewModelScope.launch {
            repository.insertPatient(newPatient)
            com.example.cloud.CloudDatabaseManager.syncPatientToCloud(getApplication(), newPatient)
            showToast("🛏 Patient $name admitted to Bed $bed")
            navigateBack()
        }
    }

    fun referPatient(patientId: String, toDoctorId: String, reason: String) {
        val currentP = patients.value.firstOrNull { it.id == patientId } ?: return
        val updated = currentP.copy(
            referralDoctorId = toDoctorId,
            referralReason = reason,
            referralBy = currentUser.value?.id ?: "DOC",
            referralDate = todayDate
        )
        val toDoc = users.value.firstOrNull { it.id == toDoctorId }
        val refNotif = NotificationEntity(
            id = "nt_" + UUID.randomUUID().toString().take(8),
            title = "New patient referral",
            body = "${currentUser.value?.name ?: "Doctor"} referred ${currentP.name} to you. Reason: $reason",
            time = currentTime,
            audience = toDoctorId,
            kind = "refer"
        )
        viewModelScope.launch {
            repository.updatePatient(updated)
            repository.insertNotification(refNotif)
            com.example.cloud.CloudDatabaseManager.syncPatientToCloud(getApplication(), updated)
            com.example.cloud.CloudDatabaseManager.publishNotificationToCloud(getApplication(), refNotif)
            com.example.fcm.FcmManager.showSystemNotification(
                getApplication(),
                "New Referral: ${currentP.name}",
                "Referred to you by ${currentUser.value?.name ?: "Doctor"}. Reason: $reason",
                "refer"
            )
            showToast("🔁 Referred to ${toDoc?.name ?: "Doctor"} — push alert sent")
        }
    }

    fun saveDocument(
        patientId: String,
        title: String,
        category: String,
        docTypeOrUri: String,
        filter: DocumentFilter,
        remarks: String
    ) {
        val docId = "doc_" + UUID.randomUUID().toString().take(8)
        val doc = DocumentEntity(
            id = docId,
            patientId = patientId,
            title = title.ifBlank { "$category — $patientId" },
            category = category,
            date = todayDate,
            docTypeOrUri = docTypeOrUri,
            filterApplied = filter.name,
            remarks = remarks,
            addedBy = currentUser.value?.id ?: "STAFF"
        )
        val pt = patients.value.firstOrNull { it.id == patientId }
        viewModelScope.launch {
            repository.insertDocument(doc)
            repository.insertNotification(
                NotificationEntity(
                    id = "nt_" + UUID.randomUUID().toString().take(8),
                    title = "Document uploaded",
                    body = "$category added to ${pt?.name ?: patientId}'s record by ${currentUser.value?.name ?: "Staff"}.",
                    time = currentTime,
                    audience = pt?.doctorId ?: "all",
                    kind = "doc"
                )
            )
            showToast("📄 Document saved to patient record")
            closeScanner()
        }
    }

    fun addClinicalNote(patientId: String, text: String) {
        if (text.isBlank()) return
        val note = ClinicalNoteEntity(
            id = "note_" + UUID.randomUUID().toString().take(8),
            patientId = patientId,
            text = text.trim(),
            authorId = currentUser.value?.id ?: "DOC",
            date = todayDate
        )
        viewModelScope.launch {
            repository.insertClinicalNote(note)
            showToast("Clinical note saved")
        }
    }

    fun addCarePlan(patientId: String, title: String, due: String = todayDate) {
        if (title.isBlank()) return
        val plan = CarePlanEntity(
            id = "plan_" + UUID.randomUUID().toString().take(8),
            patientId = patientId,
            title = title.trim(),
            due = due,
            status = "pending"
        )
        val pt = patients.value.firstOrNull { it.id == patientId }
        val taskNotif = NotificationEntity(
            id = "nt_" + UUID.randomUUID().toString().take(8),
            title = "Care plan task added",
            body = "${title.trim()} — ${pt?.name ?: patientId} (${pt?.bed ?: ""}). Due today.",
            time = currentTime,
            audience = pt?.doctorId ?: "all",
            kind = "task"
        )
        viewModelScope.launch {
            repository.insertCarePlan(plan)
            repository.insertNotification(taskNotif)
            com.example.cloud.CloudDatabaseManager.syncCarePlanToCloud(getApplication(), plan)
            com.example.cloud.CloudDatabaseManager.publishNotificationToCloud(getApplication(), taskNotif)
            showToast("🔔 Task added · push alert dispatched")
        }
    }

    fun toggleCarePlan(planId: String, currentStatus: String) {
        val newStatus = if (currentStatus == "done") "pending" else "done"
        viewModelScope.launch {
            repository.updateCarePlanStatus(planId, newStatus)
            val current = carePlans.value.firstOrNull { it.id == planId }
            if (current != null) {
                com.example.cloud.CloudDatabaseManager.syncCarePlanToCloud(
                    getApplication(),
                    current.copy(status = newStatus)
                )
            }
            showToast(if (newStatus == "done") "✓ Task completed" else "Task marked pending")
        }
    }

    fun markAttendance(method: String) {
        val user = currentUser.value ?: return
        val already = attendance.value.any { it.userId == user.id && it.date == todayDate }
        if (already) {
            showToast("Already checked in today")
            return
        }
        val record = AttendanceEntity(
            userId = user.id,
            date = todayDate,
            time = currentTime,
            method = method
        )
        val notif = NotificationEntity(
            id = "nt_" + UUID.randomUUID().toString().take(8),
            title = "Attendance recorded",
            body = "${user.name} (${user.id}) checked in at $currentTime via $method.",
            time = currentTime,
            audience = "BOSS-0001",
            kind = "attendance"
        )
        viewModelScope.launch {
            repository.insertAttendance(record)
            repository.insertNotification(notif)
            com.example.cloud.CloudDatabaseManager.syncAttendanceToCloud(getApplication(), record)
            com.example.cloud.CloudDatabaseManager.publishNotificationToCloud(getApplication(), notif)
            showToast("✅ Attendance marked via $method")
        }
    }

    fun broadcastAnnouncement(title: String, body: String) {
        if (title.isBlank() || body.isBlank()) return
        val cleanTitle = title.trim()
        val cleanBody = body.trim()
        val notif = NotificationEntity(
            id = "nt_" + UUID.randomUUID().toString().take(8),
            title = cleanTitle,
            body = cleanBody,
            time = currentTime,
            audience = "all",
            kind = "admin"
        )
        viewModelScope.launch {
            repository.insertNotification(notif)
            // 1. Publish into shared Cloud Database so all Android & Web instances receive it
            com.example.cloud.CloudDatabaseManager.publishNotificationToCloud(getApplication(), notif)

            // 2. Trigger local system push notification via FCM manager
            com.example.fcm.FcmManager.showSystemNotification(
                getApplication(),
                cleanTitle,
                cleanBody,
                "admin"
            )
            showToast("📢 Broadcast announcement dispatched via Cloud DB & FCM")
        }
    }

    /**
     * Dispatches high-priority alerts, emergency codes, app updates, and clinical notices
     * to ALL Android phones and Web clients simultaneously.
     */
    fun dispatchInstantBroadcast(
        title: String,
        body: String,
        priority: BroadcastPriority = BroadcastPriority.NORMAL,
        targetAudience: String = "all"
    ) {
        if (title.isBlank() || body.isBlank()) return
        val user = currentUser.value
        val cleanTitle = title.trim()
        val cleanBody = body.trim()
        val broadcast = HospitalBroadcast(
            id = "bc_" + UUID.randomUUID().toString().take(8),
            title = cleanTitle,
            body = cleanBody,
            priority = priority.name,
            senderName = user?.name ?: "Hospital Administration",
            senderRole = user?.role ?: "Admin",
            time = currentTime,
            audience = targetAudience,
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            val notif = NotificationEntity(
                id = broadcast.id,
                title = cleanTitle,
                body = cleanBody,
                time = broadcast.time,
                audience = targetAudience,
                kind = if (priority == BroadcastPriority.CRITICAL) "emergency" else if (priority == BroadcastPriority.APP_UPDATE) "app_update" else "admin"
            )
            repository.insertNotification(notif)
            com.example.cloud.CloudDatabaseManager.dispatchHospitalBroadcast(
                context = getApplication(),
                broadcast = broadcast,
                forceAlertOnThisDevice = true
            )
            showToast("🚀 Dispatched instantly to all Android phones & Web clients")
        }
    }

    /**
     * Simulates an incoming FCM message (e.g. from Cloud Function, console, or external system)
     * Demonstrates live push notification handling and automatic sync into the room database
     */
    fun simulateIncomingFcmPush(
        title: String = "🚨 Critical Update — Ward ICU",
        body: String = "Emergency team required in ICU Bed 201 for vital stabilization.",
        kind: String = "admin"
    ) {
        com.example.fcm.FcmManager.processIncomingMessage(
            context = getApplication(),
            title = title,
            body = body,
            audience = "all",
            kind = kind
        )
        showToast("⚡ Incoming FCM push notification received")
    }

    fun syncAllLocalDataToCloud() {
        val app = getApplication<android.app.Application>()
        viewModelScope.launch {
            patients.value.forEach { pt ->
                com.example.cloud.CloudDatabaseManager.syncPatientToCloud(app, pt)
            }
            carePlans.value.forEach { plan ->
                com.example.cloud.CloudDatabaseManager.syncCarePlanToCloud(app, plan)
            }
            notifications.value.forEach { notif ->
                com.example.cloud.CloudDatabaseManager.publishNotificationToCloud(app, notif)
            }
            showToast("☁️ Local database synchronized with Cloud Firestore")
        }
    }

    fun toggleTopicSubscription(topic: String, shouldSubscribe: Boolean) {
        if (shouldSubscribe) {
            com.example.fcm.FcmManager.subscribeToTopic(topic)
            showToast("Subscribed to topic: $topic")
        } else {
            com.example.fcm.FcmManager.unsubscribeFromTopic(topic)
            showToast("Unsubscribed from topic: $topic")
        }
    }

    fun addStaff(
        name: String,
        role: UserRole,
        dept: String,
        specialty: String,
        phone: String,
        email: String,
        photoUri: String?,
        pass: String
    ) {
        val prefix = role.prefix
        val existingNums = users.value
            .filter { it.id.startsWith("$prefix-") }
            .mapNotNull { it.id.substringAfter("-").toIntOrNull() }
        val nextNum = if (existingNums.isNotEmpty()) existingNums.maxOrNull()!! + 1
        else when (role) {
            UserRole.DOCTOR -> 2001
            UserRole.ADMINISTRATOR -> 1001
            UserRole.NURSE -> 3001
            UserRole.TECHNICIAN -> 3501
            else -> 4001
        }
        val newId = "$prefix-${String.format(Locale.getDefault(), "%04d", nextNum)}"
        val newUser = UserEntity(
            id = newId,
            pass = pass,
            name = name.trim(),
            role = role.name,
            dept = dept,
            specialty = specialty,
            phone = phone,
            email = email,
            photoUri = photoUri,
            joinedDate = todayDate
        )
        viewModelScope.launch {
            repository.insertUser(newUser)
            repository.insertNotification(
                NotificationEntity(
                    id = "nt_" + UUID.randomUUID().toString().take(8),
                    title = "New member invited",
                    body = "$name joined as ${role.label} · Login ID: $newId",
                    time = currentTime,
                    audience = "all",
                    kind = "admin"
                )
            )
            showToast("✉️ $name registered with ID $newId")
        }
    }

    fun deleteStaff(userId: String) {
        if (userId == "BOSS-0001") {
            showToast("⚠️ Boss account cannot be deleted")
            return
        }
        viewModelScope.launch {
            repository.deleteUser(userId)
            showToast("Staff member removed")
        }
    }

    fun updateUserPhoto(userId: String, photoUri: String?) {
        viewModelScope.launch {
            repository.updateUserPhoto(userId, photoUri)
            if (_currentUser.value?.id == userId) {
                _currentUser.value = _currentUser.value?.copy(photoUri = photoUri)
            }
            showToast("📷 Profile photo updated")
        }
    }

    fun deleteCurrentAccount() {
        val user = _currentUser.value ?: return
        if (user.id == "BOSS-0001") {
            showToast("⚠️ Boss account cannot be deleted")
            return
        }
        viewModelScope.launch {
            repository.deleteUser(user.id)
            _currentUser.value = null
            _screenStack.value = listOf(AppScreen.HOME)
            showToast("Account deleted permanently")
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }
}
