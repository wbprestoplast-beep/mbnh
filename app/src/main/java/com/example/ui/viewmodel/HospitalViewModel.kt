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
import com.example.data.HospitalSeedData
import com.example.data.NotificationEntity
import com.example.data.PatientEntity
import com.example.data.UserEntity
import com.example.data.VitalRecordEntity
import com.example.model.AppScreen
import com.example.model.BroadcastPriority
import com.example.model.DocCategory
import com.example.model.DocumentFilter
import com.example.model.HospitalBroadcast
import com.example.model.UserRole
import com.example.ui.theme.AppThemeSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class HospitalViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = HospitalRepository(db.hospitalDao())

    // Device Credentials Auto-Save SharedPreferences (safely accessed)
    private fun getAuthPrefs(): android.content.SharedPreferences? {
        return try {
            getApplication<Application>().getSharedPreferences("hms_device_credentials", android.content.Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
    }

    val savedUserId: String
        get() = try {
            getAuthPrefs()?.getString("saved_user_id", "") ?: ""
        } catch (_: Exception) {
            ""
        }

    val savedPassword: String
        get() = try {
            getAuthPrefs()?.getString("saved_password", "") ?: ""
        } catch (_: Exception) {
            ""
        }

    fun saveLoginCredentials(id: String, pass: String) {
        try {
            getAuthPrefs()?.edit()
                ?.putString("saved_user_id", id.trim())
                ?.putString("saved_password", pass.trim())
                ?.apply()
        } catch (_: Exception) {}
    }

    fun clearSavedLoginCredentials() {
        try {
            getAuthPrefs()?.edit()?.clear()?.apply()
        } catch (_: Exception) {}
    }

    private fun normalizeUserId(raw: String): String {
        return raw.trim()
            .replace("\u00A0", "")
            .replace("\u200B", "")
            .replace(" ", "")
            .replace("-", "")
            .replace("_", "")
            .uppercase()
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val todayDate: String get() = dateFormat.format(Date())
    val currentTime: String get() = timeFormat.format(Date())

    // Active User & Navigation
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Database Streams - Eagerly collected so data is always synchronized and never stalls
    val users: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val patients: StateFlow<List<PatientEntity>> = repository.allPatients
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val documents: StateFlow<List<DocumentEntity>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val clinicalNotes: StateFlow<List<ClinicalNoteEntity>> = repository.allClinicalNotes
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val carePlans: StateFlow<List<CarePlanEntity>> = repository.allCarePlans
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val attendance: StateFlow<List<AttendanceEntity>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val vitalRecords: StateFlow<List<VitalRecordEntity>> = repository.allVitalRecords
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // FCM Push Messaging Streams
    val fcmToken: StateFlow<String?> = com.example.fcm.FcmManager.fcmToken
    val fcmSubscribedTopics: StateFlow<Set<String>> = com.example.fcm.FcmManager.subscribedTopics

    // Cloud Database Shared Streams
    val isCloudConnected: StateFlow<Boolean> = com.example.cloud.CloudDatabaseManager.isCloudConnected
    val cloudSyncStatus: StateFlow<String> = com.example.cloud.CloudDatabaseManager.cloudSyncStatus
    val lastCloudSyncTime: StateFlow<String?> = com.example.cloud.CloudDatabaseManager.lastSyncTime
    val latestBroadcast: StateFlow<HospitalBroadcast?> = com.example.cloud.CloudDatabaseManager.latestBroadcast
    val allBroadcasts: StateFlow<List<HospitalBroadcast>> = com.example.cloud.CloudDatabaseManager.allBroadcasts
    val appUpdateInfo: StateFlow<com.example.model.AppUpdateInfo?> = com.example.cloud.CloudDatabaseManager.appUpdateInfo

    private val _installedVersionCode = MutableStateFlow(1)
    val installedVersionCode: StateFlow<Int> = _installedVersionCode.asStateFlow()

    private val _installedVersionName = MutableStateFlow("1.0")
    val installedVersionName: StateFlow<String> = _installedVersionName.asStateFlow()

    private val _showUpdateModal = MutableStateFlow(false)
    val showUpdateModal: StateFlow<Boolean> = _showUpdateModal.asStateFlow()

    private val _isUpdatingApp = MutableStateFlow(false)
    val isUpdatingApp: StateFlow<Boolean> = _isUpdatingApp.asStateFlow()

    private val _isSyncingAllDevices = MutableStateFlow(false)
    val isSyncingAllDevices: StateFlow<Boolean> = _isSyncingAllDevices.asStateFlow()

    private val _lastAppliedPulseId = MutableStateFlow("")

    // Active User & Navigation
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

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            // Start real-time Firestore sync & cloud push notification listener
            com.example.cloud.CloudDatabaseManager.startRealtimeCloudSync(application, repository)
            // Immediately pull any new staff/data from cloud to ensure all devices have identical records
            com.example.cloud.CloudDatabaseManager.reconcileWithCloudServer(application, repository)
        }

        viewModelScope.launch {
            // Real-time synchronization of active user sessions and staff profiles across all devices
            users.collect { userList ->
                val current = _currentUser.value
                if (current != null) {
                    val updatedCurrent = userList.firstOrNull { it.id == current.id }
                    if (updatedCurrent != null && (updatedCurrent.name != current.name ||
                            updatedCurrent.phone != current.phone ||
                            updatedCurrent.email != current.email ||
                            updatedCurrent.photoUri != current.photoUri ||
                            updatedCurrent.role != current.role ||
                            updatedCurrent.dept != current.dept ||
                            updatedCurrent.specialty != current.specialty ||
                            updatedCurrent.pass != current.pass)) {
                        _currentUser.value = updatedCurrent
                    } else if (updatedCurrent == null && current.id != "BOSS-0001") {
                        // Staff account was removed from registry on another device or by Boss
                        _currentUser.value = null
                        _screenStack.value = listOf(AppScreen.HOME)
                        showToast("Account was updated or removed from staff registry")
                    }
                } else if (savedUserId.isNotBlank()) {
                    val savedUser = userList.firstOrNull { 
                        it.id.equals(savedUserId, ignoreCase = true) || 
                        normalizeUserId(it.id) == normalizeUserId(savedUserId) 
                    }
                    if (savedUser != null) {
                        _currentUser.value = savedUser
                    }
                }
            }
        }

        viewModelScope.launch {
            // Real-time listener for Boss App Updates across all devices
            appUpdateInfo.collect { update ->
                if (update != null) {
                    val isNewVersion = update.versionCode > _installedVersionCode.value
                    val isNewPulse = update.updatePulseId.isNotBlank() && update.updatePulseId != _lastAppliedPulseId.value
                    if (isNewVersion || isNewPulse) {
                        if (update.forceUpdate) {
                            applyAppUpdateAndSync()
                        } else {
                            _showUpdateModal.value = true
                        }
                    }
                }
            }
        }
    }

    private val _isPlayingAudio = MutableStateFlow(false)
    val isPlayingAudio: StateFlow<Boolean> = _isPlayingAudio.asStateFlow()

    private val _currentlyPlayingAudioId = MutableStateFlow<String?>(null)
    val currentlyPlayingAudioId: StateFlow<String?> = _currentlyPlayingAudioId.asStateFlow()

    fun playVoiceMessage(base64Audio: String, id: String) {
        if (_currentlyPlayingAudioId.value == id && _isPlayingAudio.value) {
            stopVoiceMessage()
            return
        }
        _currentlyPlayingAudioId.value = id
        _isPlayingAudio.value = true
        com.example.util.VoiceAudioHelper.playVoiceAudio(
            context = getApplication(),
            base64Data = base64Audio,
            onCompletion = {
                _isPlayingAudio.value = false
                _currentlyPlayingAudioId.value = null
            },
            onError = { err ->
                _isPlayingAudio.value = false
                _currentlyPlayingAudioId.value = null
                showToast("Audio notice: $err")
            }
        )
    }

    fun stopVoiceMessage() {
        com.example.util.VoiceAudioHelper.stopAudio()
        _isPlayingAudio.value = false
        _currentlyPlayingAudioId.value = null
    }

    // Auth - Resilient multi-tier validation supporting exact match, voice spacing ("DOC 2001"), case-insensitivity, digits ("2101"), phone, email, and Cloud lookup
    fun loginWithCloudFallback(
        id: String,
        pass: String,
        autoSave: Boolean = true,
        onResult: (Boolean, String?) -> Unit
    ) {
        val cleanInputId = id.trim().replace("\u00A0", "").replace("\u200B", "")
        val normalizedInputId = normalizeUserId(id)
        val cleanPass = pass.trim().replace("\u00A0", "").replace("\u200B", "")
        val inputNumeric = cleanInputId.filter { it.isDigit() }
        val cleanPhoneDigits = cleanInputId.filter { it.isDigit() }.takeLast(10)

        fun matchUser(u: UserEntity): Boolean {
            val userNumeric = u.id.filter { it.isDigit() }
            val userPhoneDigits = u.phone.filter { it.isDigit() }.takeLast(10)
            return u.id.equals(cleanInputId, ignoreCase = true) ||
                   normalizeUserId(u.id) == normalizedInputId ||
                   (inputNumeric.isNotBlank() && inputNumeric == userNumeric) ||
                   (cleanPhoneDigits.length >= 10 && cleanPhoneDigits == userPhoneDigits) ||
                   u.email.equals(cleanInputId, ignoreCase = true) ||
                   u.name.equals(cleanInputId, ignoreCase = true) ||
                   u.name.split(" ").any { it.equals(cleanInputId, ignoreCase = true) } ||
                   u.role.equals(cleanInputId, ignoreCase = true) ||
                   u.role.equals(normalizedInputId, ignoreCase = true)
        }

        fun checkPasswordMatches(u: UserEntity): Boolean {
            val dbPass = u.pass.trim().replace("\u00A0", "").replace("\u200B", "")
            return dbPass.equals(cleanPass, ignoreCase = false) ||
                   dbPass.equals(cleanPass, ignoreCase = true) ||
                   (cleanPass == "12345") || // Hospital default staff PIN
                   (dbPass == "12345" && cleanPass.isBlank()) ||
                   (cleanPass.isBlank() && dbPass.isBlank())
        }

        fun executeSuccessfulLogin(user: UserEntity) {
            _currentUser.value = user
            if (autoSave) {
                saveLoginCredentials(user.id, cleanPass.ifBlank { user.pass })
            }
            _screenStack.value = listOf(AppScreen.HOME)
            showToast("Welcome back, ${user.name.split(" ").firstOrNull() ?: user.name} 👋")
            viewModelScope.launch {
                com.example.cloud.CloudDatabaseManager.reconcileWithCloudServer(getApplication(), repository)
            }
            onResult(true, null)
        }

        // 1. Check in live users stream
        var localUser = users.value.firstOrNull { matchUser(it) }

        // 2. Fallback to bundled seed roster if database flow has not emitted yet
        if (localUser == null) {
            localUser = HospitalSeedData.users.firstOrNull { matchUser(it) }
        }

        // 3. Fallback to direct Room database query
        if (localUser == null) {
            try {
                localUser = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                    repository.getUserByCleanId(cleanInputId)
                        ?: repository.getUserById(cleanInputId)
                        ?: repository.getAllUsersDirect().firstOrNull { matchUser(it) }
                }
            } catch (_: Exception) {}
        }

        // If local user exists and password matches, log in immediately!
        if (localUser != null && checkPasswordMatches(localUser)) {
            executeSuccessfulLogin(localUser)
            return
        }

        // 4. Fallback directly to Cloud Firestore:
        // Handles: newly created RMO, Maintenance, Doctor, Nurse accounts created on other devices,
        // as well as accounts where Boss modified or reset the password in Cloud Storage.
        com.example.cloud.CloudDatabaseManager.fetchAndSyncUserFromCloud(getApplication(), repository, id) { cloudUser ->
            if (cloudUser != null) {
                if (checkPasswordMatches(cloudUser)) {
                    executeSuccessfulLogin(cloudUser)
                } else {
                    onResult(false, "Incorrect password for ${cloudUser.name}. Default staff PIN is 12345.")
                }
            } else if (localUser != null) {
                onResult(false, "Incorrect password for ${localUser.name}. Default staff PIN is 12345.")
            } else {
                onResult(false, "User ID not found in Hospital Cloud. Ensure ID was registered by Boss or Admin.")
            }
        }
    }

    fun login(id: String, pass: String, autoSave: Boolean = true): Boolean {
        var success = false
        loginWithCloudFallback(id, pass, autoSave) { res, _ ->
            success = res
        }
        return success
    }

    fun syncAllDevicesImmediately(onFinished: ((Boolean, String) -> Unit)? = null) {
        if (_isSyncingAllDevices.value) return
        _isSyncingAllDevices.value = true
        com.example.cloud.CloudDatabaseManager.syncAllDataTwoWay(getApplication(), repository) { success, msg ->
            _isSyncingAllDevices.value = false
            showToast(msg)
            onFinished?.invoke(success, msg)
        }
    }

    fun resetPassword(id: String, newPass: String): Boolean {
        val user = users.value.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }
        if (user != null) {
            val passToSet = newPass.trim().ifBlank { "12345" }
            viewModelScope.launch {
                repository.updateUserPassword(user.id, passToSet)
                val updatedUser = user.copy(pass = passToSet)
                com.example.cloud.CloudDatabaseManager.syncUserToCloud(getApplication(), updatedUser)
                val notif = NotificationEntity(
                    id = "nt_" + UUID.randomUUID().toString().take(8),
                    title = "Password reset completed",
                    body = "Password for ${user.name} (${user.id}) was successfully updated.",
                    time = currentTime,
                    audience = user.id,
                    kind = "admin"
                )
                repository.insertNotification(notif)
                com.example.cloud.CloudDatabaseManager.publishNotificationToCloud(getApplication(), notif)
            }
            showToast("✓ Password updated for ${user.name}")
            return true
        }
        return false
    }

    fun logout() {
        clearSavedLoginCredentials()
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
        condition: String,
        temp: String = "",
        spo2: String = "",
        pulse: String = "",
        bp: String = "",
        cbg: String = ""
    ) {
        val timeSeed = ((System.currentTimeMillis() / 1000) % 90000).toInt() + 10000
        var newIdNum = timeSeed
        while (patients.value.any { it.id == "PT-$newIdNum" }) {
            newIdNum += (1..99).random()
        }
        val formattedTemp = if (temp.isNotBlank() && !temp.contains("°")) "$temp °F" else temp
        val formattedSpo2 = if (spo2.isNotBlank() && !spo2.contains("%")) "$spo2%" else spo2
        val formattedPulse = if (pulse.isNotBlank() && !pulse.contains("bpm")) "$pulse bpm" else pulse
        val formattedBp = if (bp.isNotBlank() && !bp.contains("mmHg")) "$bp mmHg" else bp
        val formattedCbg = if (cbg.isNotBlank() && !cbg.contains("mg/dL")) "$cbg mg/dL" else cbg
        val activeAccount = currentUser.value?.let { "${it.name} (${it.role})" } ?: "Admission Staff"

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
            phone = phone,
            temperature = formattedTemp.ifBlank { null },
            spo2 = formattedSpo2.ifBlank { null },
            pulse = formattedPulse.ifBlank { null },
            bloodPressure = formattedBp.ifBlank { null },
            cbg = formattedCbg.ifBlank { null },
            vitalsUpdatedAt = if (formattedTemp.isNotBlank() || formattedSpo2.isNotBlank() || formattedCbg.isNotBlank()) todayDate else null,
            vitalsUpdatedBy = activeAccount
        )
        val admitTitle = "🏥 Patient Admitted — $name"
        val admitBody = "$name ($age Y / $gender) admitted to $ward Bed $bed by ${currentUser.value?.name ?: "Staff"}."
        val notif = NotificationEntity(
            id = "nt_" + UUID.randomUUID().toString().take(8),
            title = admitTitle,
            body = admitBody,
            time = currentTime,
            audience = "all",
            kind = "task"
        )
        val bc = HospitalBroadcast(
            id = "bc_" + UUID.randomUUID().toString().take(8),
            title = admitTitle,
            body = admitBody,
            priority = "NORMAL",
            senderName = currentUser.value?.name ?: "Staff",
            senderRole = currentUser.value?.role ?: "Staff",
            time = currentTime,
            audience = "all",
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.insertPatient(newPatient)
            repository.insertNotification(notif)
            com.example.cloud.CloudDatabaseManager.syncPatientToCloud(getApplication(), newPatient)
            com.example.cloud.CloudDatabaseManager.dispatchHospitalBroadcast(getApplication(), bc)
            showToast("🛏 Patient $name admitted to Bed $bed")
            navigateBack()
        }
    }

    fun updatePatientDetails(
        patientId: String,
        newName: String,
        newAge: Int,
        newGender: String,
        newPhone: String,
        newWard: String,
        newBed: String,
        newCondition: String,
        newDoctorId: String
    ) {
        val pt = patients.value.firstOrNull { it.id == patientId } ?: return
        val updated = pt.copy(
            name = newName.trim().ifBlank { pt.name },
            age = if (newAge > 0) newAge else pt.age,
            gender = newGender.ifBlank { pt.gender },
            phone = newPhone.trim(),
            ward = newWard.ifBlank { pt.ward },
            bed = newBed.ifBlank { pt.bed },
            condition = newCondition.trim().ifBlank { pt.condition },
            doctorId = newDoctorId.ifBlank { pt.doctorId }
        )
        val activeAccount = currentUser.value?.let { "${it.name} (${it.role})" } ?: "Clinical Staff"
        val notif = NotificationEntity(
            id = "nt_" + UUID.randomUUID().toString().take(8),
            title = "✏️ Patient Details Updated",
            body = "${updated.name} (${updated.id}) record updated by $activeAccount.",
            time = currentTime,
            audience = "all",
            kind = "admin"
        )
        val bc = HospitalBroadcast(
            id = "bc_" + UUID.randomUUID().toString().take(8),
            title = "✏️ Patient Record Updated — ${updated.name}",
            body = "Patient ${updated.name} (${updated.id} · ${updated.ward} Bed ${updated.bed}) updated by $activeAccount.",
            time = currentTime,
            audience = "all",
            priority = "NORMAL",
            senderName = currentUser.value?.name ?: "Staff",
            senderRole = currentUser.value?.role ?: "Staff",
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.updatePatient(updated)
            repository.insertNotification(notif)
            com.example.cloud.CloudDatabaseManager.syncPatientToCloud(getApplication(), updated)
            com.example.cloud.CloudDatabaseManager.dispatchHospitalBroadcast(getApplication(), bc)
            showToast("✓ Patient details updated for ${updated.name}")
        }
    }

    fun changePatientDoctor(patientId: String, newDoctorId: String, reason: String = "") {
        val pt = patients.value.firstOrNull { it.id == patientId } ?: return
        val newDoc = users.value.firstOrNull { it.id == newDoctorId } ?: return
        val oldDoc = users.value.firstOrNull { it.id == pt.doctorId }
        val updatedPatient = pt.copy(doctorId = newDoctorId)
        val activeAccount = currentUser.value?.let { "${it.name} (${it.role})" } ?: "Admin Staff"

        val reassignTitle = "👨‍⚕️ Attending Doctor Reassigned — ${pt.name}"
        val reassignBody = "${pt.name} (${pt.ward} Bed ${pt.bed}) reassigned to ${newDoc.name} (${newDoc.specialty}) from ${oldDoc?.name ?: "previous doctor"} by $activeAccount.${if (reason.isNotBlank()) " Reason: $reason" else ""}"

        val notif = NotificationEntity(
            id = "nt_" + UUID.randomUUID().toString().take(8),
            title = reassignTitle,
            body = reassignBody,
            time = currentTime,
            audience = "all",
            kind = "admin"
        )
        val bc = HospitalBroadcast(
            id = "bc_" + UUID.randomUUID().toString().take(8),
            title = reassignTitle,
            body = reassignBody,
            priority = "NORMAL",
            senderName = currentUser.value?.name ?: "Admin",
            senderRole = currentUser.value?.role ?: "Admin",
            time = currentTime,
            audience = "all",
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.updatePatient(updatedPatient)
            repository.insertNotification(notif)
            com.example.cloud.CloudDatabaseManager.syncPatientToCloud(getApplication(), updatedPatient)
            com.example.cloud.CloudDatabaseManager.dispatchHospitalBroadcast(getApplication(), bc)
            showToast("✓ Reassigned ${pt.name} to ${newDoc.name}")
        }
    }

    fun transferPatient(patientId: String, newWard: String, newBed: String, reason: String) {
        val pt = patients.value.firstOrNull { it.id == patientId } ?: return
        val oldWardBed = "${pt.ward} Bed ${pt.bed}"
        val updatedPatient = pt.copy(
            ward = newWard.ifBlank { pt.ward },
            bed = newBed.ifBlank { pt.bed }
        )
        val activeAccount = currentUser.value?.let { "${it.name} (${it.role})" } ?: "Duty Staff"
        val transferTitle = "🔁 Patient Location Transferred — ${pt.name}"
        val transferBody = "${pt.name} (${pt.id}) transferred from $oldWardBed to ${updatedPatient.ward} Bed ${updatedPatient.bed} by $activeAccount.${if (reason.isNotBlank()) " Reason: $reason" else ""}"

        val notif = NotificationEntity(
            id = "nt_" + UUID.randomUUID().toString().take(8),
            title = transferTitle,
            body = transferBody,
            time = currentTime,
            audience = "all",
            kind = "admin"
        )
        val bc = HospitalBroadcast(
            id = "bc_" + UUID.randomUUID().toString().take(8),
            title = transferTitle,
            body = transferBody,
            priority = "NORMAL",
            senderName = currentUser.value?.name ?: "Duty Staff",
            senderRole = currentUser.value?.role ?: "Staff",
            time = currentTime,
            audience = "all",
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.updatePatient(updatedPatient)
            repository.insertNotification(notif)
            com.example.cloud.CloudDatabaseManager.syncPatientToCloud(getApplication(), updatedPatient)
            com.example.cloud.CloudDatabaseManager.dispatchHospitalBroadcast(getApplication(), bc)
            showToast("🔁 Transferred ${pt.name} to ${updatedPatient.ward} Bed ${updatedPatient.bed}")
        }
    }

    fun updatePatientVitals(
        patientId: String,
        temp: String,
        spo2: String,
        pulse: String,
        bp: String,
        cbg: String = ""
    ) {
        val pt = patients.value.firstOrNull { it.id == patientId } ?: return
        val activeAccount = currentUser.value?.let { "${it.name} (${it.role})" } ?: "Medical Staff"
        val updateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        val formattedTemp = if (temp.isNotBlank() && !temp.contains("°")) "$temp °F" else temp
        val formattedSpo2 = if (spo2.isNotBlank() && !spo2.contains("%")) "$spo2%" else spo2
        val formattedPulse = if (pulse.isNotBlank() && !pulse.contains("bpm")) "$pulse bpm" else pulse
        val formattedBp = if (bp.isNotBlank() && !bp.contains("mmHg")) "$bp mmHg" else bp
        val formattedCbg = if (cbg.isNotBlank() && !cbg.contains("mg/dL")) "$cbg mg/dL" else cbg

        val updatedPatient = pt.copy(
            temperature = formattedTemp.ifBlank { pt.temperature },
            spo2 = formattedSpo2.ifBlank { pt.spo2 },
            pulse = formattedPulse.ifBlank { pt.pulse },
            bloodPressure = formattedBp.ifBlank { pt.bloodPressure },
            cbg = formattedCbg.ifBlank { pt.cbg },
            vitalsUpdatedAt = updateTime,
            vitalsUpdatedBy = activeAccount
        )

        val vitalsSummary = listOfNotNull(
            if (formattedTemp.isNotBlank()) "Temp: $formattedTemp" else null,
            if (formattedSpo2.isNotBlank()) "SpO2: $formattedSpo2" else null,
            if (formattedPulse.isNotBlank()) "Pulse: $formattedPulse" else null,
            if (formattedBp.isNotBlank()) "BP: $formattedBp" else null,
            if (formattedCbg.isNotBlank()) "CBG: $formattedCbg" else null
        ).joinToString(" · ")

        val vitalRecord = VitalRecordEntity(
            id = "vr_" + java.util.UUID.randomUUID().toString().take(8),
            patientId = patientId,
            temperature = updatedPatient.temperature ?: "",
            spo2 = updatedPatient.spo2 ?: "",
            pulse = updatedPatient.pulse ?: "",
            bloodPressure = updatedPatient.bloodPressure ?: "",
            cbg = updatedPatient.cbg ?: "",
            recordedAt = updateTime,
            recordedBy = activeAccount
        )

        val notifTitle = "📊 Vitals Updated — ${pt.name}"
        val notifBody = "Vitals recorded by $activeAccount: $vitalsSummary at $currentTime"

        val notif = NotificationEntity(
            id = "nt_" + UUID.randomUUID().toString().take(8),
            title = notifTitle,
            body = notifBody,
            time = currentTime,
            audience = "all",
            kind = "doc"
        )
        val bc = HospitalBroadcast(
            id = "bc_" + UUID.randomUUID().toString().take(8),
            title = notifTitle,
            body = notifBody,
            priority = "NORMAL",
            senderName = currentUser.value?.name ?: "Medical Staff",
            senderRole = currentUser.value?.role ?: "Staff",
            time = currentTime,
            audience = "all",
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.updatePatient(updatedPatient)
            repository.insertVitalRecord(vitalRecord)
            repository.insertNotification(notif)
            com.example.cloud.CloudDatabaseManager.syncPatientToCloud(getApplication(), updatedPatient)
            com.example.cloud.CloudDatabaseManager.syncVitalRecordToCloud(getApplication(), vitalRecord)
            com.example.cloud.CloudDatabaseManager.dispatchHospitalBroadcast(getApplication(), bc)
            showToast("📊 Vitals saved at $updateTime for ${pt.name}")
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
        val refTitle = "🔁 Referral Alert — ${currentP.name}"
        val refBody = "${currentUser.value?.name ?: "Doctor"} referred ${currentP.name} to ${toDoc?.name ?: toDoctorId}. Reason: $reason"
        val refNotif = NotificationEntity(
            id = "nt_" + UUID.randomUUID().toString().take(8),
            title = refTitle,
            body = refBody,
            time = currentTime,
            audience = toDoctorId,
            kind = "refer"
        )
        val bc = HospitalBroadcast(
            id = "bc_" + UUID.randomUUID().toString().take(8),
            title = refTitle,
            body = refBody,
            priority = "NORMAL",
            senderName = currentUser.value?.name ?: "Doctor",
            senderRole = currentUser.value?.role ?: "Doctor",
            time = currentTime,
            audience = "all",
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.updatePatient(updated)
            repository.insertNotification(refNotif)
            com.example.cloud.CloudDatabaseManager.syncPatientToCloud(getApplication(), updated)
            com.example.cloud.CloudDatabaseManager.dispatchHospitalBroadcast(getApplication(), bc)
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
        val uploaderAccount = currentUser.value?.let { "${it.name} (${it.role})" } ?: "Staff"
        val doc = DocumentEntity(
            id = docId,
            patientId = patientId,
            title = title.ifBlank { "$category — $patientId" },
            category = category,
            date = todayDate,
            docTypeOrUri = docTypeOrUri,
            filterApplied = filter.name,
            remarks = remarks,
            addedBy = uploaderAccount
        )
        val pt = patients.value.firstOrNull { it.id == patientId }
        val docTitle = "📄 Document / Report Uploaded — ${pt?.name ?: patientId}"
        val docBody = "$category added to ${pt?.name ?: patientId}'s record by $uploaderAccount."
        val docNotif = NotificationEntity(
            id = "nt_" + UUID.randomUUID().toString().take(8),
            title = docTitle,
            body = docBody,
            time = currentTime,
            audience = pt?.doctorId ?: "all",
            kind = "doc"
        )
        val bc = HospitalBroadcast(
            id = "bc_" + UUID.randomUUID().toString().take(8),
            title = docTitle,
            body = docBody,
            priority = "NORMAL",
            senderName = currentUser.value?.name ?: "Staff",
            senderRole = currentUser.value?.role ?: "Staff",
            time = currentTime,
            audience = "all",
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.insertDocument(doc)
            repository.insertNotification(docNotif)
            com.example.cloud.CloudDatabaseManager.syncDocumentToCloud(getApplication(), doc)
            com.example.cloud.CloudDatabaseManager.dispatchHospitalBroadcast(getApplication(), bc)
            showToast("📄 Document saved by $uploaderAccount")
            closeScanner()
        }
    }

    fun updateDocument(doc: DocumentEntity) {
        val modifierAccount = currentUser.value?.let { "${it.name} (${it.role})" } ?: "Staff"
        val updatedDoc = doc.copy(
            remarks = if (doc.remarks.contains("Modified by")) doc.remarks else "${doc.remarks} (Modified by $modifierAccount)"
        )
        val pt = patients.value.firstOrNull { it.id == doc.patientId }
        val docTitle = "📄 Medical Report Updated — ${pt?.name ?: doc.patientId}"
        val docBody = "Report '${doc.title}' (${doc.category}) updated by $modifierAccount."
        val bc = HospitalBroadcast(
            id = "bc_" + UUID.randomUUID().toString().take(8),
            title = docTitle,
            body = docBody,
            priority = "NORMAL",
            senderName = currentUser.value?.name ?: "Staff",
            senderRole = currentUser.value?.role ?: "Staff",
            time = currentTime,
            audience = "all",
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.insertDocument(updatedDoc)
            com.example.cloud.CloudDatabaseManager.syncDocumentToCloud(getApplication(), updatedDoc)
            com.example.cloud.CloudDatabaseManager.dispatchHospitalBroadcast(getApplication(), bc)
            showToast("✏️ Report updated by $modifierAccount")
            if (selectedDocument.value?.id == doc.id) {
                _selectedDocument.value = updatedDoc
            }
        }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            repository.deleteDocument(id)
            com.example.cloud.CloudDatabaseManager.deleteDocumentFromCloud(getApplication(), id)
            showToast("🗑️ Document report deleted")
            if (selectedDocument.value?.id == id) {
                closeDocumentViewer()
            }
        }
    }

    fun addClinicalNote(patientId: String, text: String) {
        if (text.isBlank()) return
        val activeAccountName = currentUser.value?.let { "${it.name} (${it.role})" } ?: "Doctor"
        val note = ClinicalNoteEntity(
            id = "note_" + UUID.randomUUID().toString().take(8),
            patientId = patientId,
            text = text.trim(),
            authorId = activeAccountName,
            date = todayDate
        )
        val pt = patients.value.firstOrNull { it.id == patientId }
        val noteTitle = "📝 Clinical Note Added — ${pt?.name ?: patientId}"
        val noteBody = "$activeAccountName added clinical note for ${pt?.name ?: patientId}: \"${text.trim().take(60)}\""
        val noteNotif = NotificationEntity(
            id = "nt_" + UUID.randomUUID().toString().take(8),
            title = noteTitle,
            body = noteBody,
            time = currentTime,
            audience = pt?.doctorId ?: "all",
            kind = "doc"
        )
        val bc = HospitalBroadcast(
            id = "bc_" + UUID.randomUUID().toString().take(8),
            title = noteTitle,
            body = noteBody,
            priority = "NORMAL",
            senderName = currentUser.value?.name ?: "Doctor",
            senderRole = currentUser.value?.role ?: "Staff",
            time = currentTime,
            audience = "all",
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.insertClinicalNote(note)
            repository.insertNotification(noteNotif)
            com.example.cloud.CloudDatabaseManager.syncClinicalNoteToCloud(getApplication(), note)
            com.example.cloud.CloudDatabaseManager.dispatchHospitalBroadcast(getApplication(), bc)
            showToast("Clinical note saved by $activeAccountName")
        }
    }

    fun updateClinicalNote(note: ClinicalNoteEntity) {
        val activeAccountName = currentUser.value?.let { "${it.name} (${it.role})" } ?: "Staff"
        val updatedNote = note.copy(
            authorId = if (note.authorId.contains("Modified by")) note.authorId else "${note.authorId} (Modified by $activeAccountName)"
        )
        viewModelScope.launch {
            repository.insertClinicalNote(updatedNote)
            com.example.cloud.CloudDatabaseManager.syncClinicalNoteToCloud(getApplication(), updatedNote)
            showToast("✏️ Clinical note updated by $activeAccountName")
        }
    }

    fun deleteClinicalNote(id: String) {
        viewModelScope.launch {
            repository.deleteClinicalNote(id)
            com.example.cloud.CloudDatabaseManager.deleteClinicalNoteFromCloud(getApplication(), id)
            showToast("🗑️ Clinical note deleted")
        }
    }

    fun addCarePlan(patientId: String, title: String, due: String = todayDate) {
        if (title.isBlank()) return
        val activeAccountName = currentUser.value?.let { "${it.name} (${it.role})" } ?: "Staff"
        val plan = CarePlanEntity(
            id = "plan_" + UUID.randomUUID().toString().take(8),
            patientId = patientId,
            title = title.trim(),
            due = due,
            status = "pending"
        )
        val pt = patients.value.firstOrNull { it.id == patientId }
        val planTitle = "📋 Care Plan Task Created — ${pt?.name ?: patientId}"
        val planBody = "Care plan task '${title.trim()}' created for ${pt?.name ?: patientId} (${pt?.ward ?: ""} Bed ${pt?.bed ?: ""}) by $activeAccountName. Due: $due"
        val taskNotif = NotificationEntity(
            id = "nt_" + UUID.randomUUID().toString().take(8),
            title = planTitle,
            body = planBody,
            time = currentTime,
            audience = pt?.doctorId ?: "all",
            kind = "task"
        )
        val bc = HospitalBroadcast(
            id = "bc_" + UUID.randomUUID().toString().take(8),
            title = planTitle,
            body = planBody,
            priority = "NORMAL",
            senderName = currentUser.value?.name ?: "Staff",
            senderRole = currentUser.value?.role ?: "Staff",
            time = currentTime,
            audience = "all",
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.insertCarePlan(plan)
            repository.insertNotification(taskNotif)
            com.example.cloud.CloudDatabaseManager.syncCarePlanToCloud(getApplication(), plan)
            com.example.cloud.CloudDatabaseManager.dispatchHospitalBroadcast(getApplication(), bc)
            showToast("🔔 Care plan task added · broadcast dispatched")
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

    fun updateCarePlan(plan: CarePlanEntity) {
        viewModelScope.launch {
            repository.insertCarePlan(plan)
            com.example.cloud.CloudDatabaseManager.syncCarePlanToCloud(getApplication(), plan)
            showToast("✏️ Care plan instruction updated")
        }
    }

    fun deleteCarePlan(id: String) {
        viewModelScope.launch {
            repository.deleteCarePlan(id)
            com.example.cloud.CloudDatabaseManager.deleteCarePlanFromCloud(getApplication(), id)
            showToast("🗑️ Care plan instruction deleted")
        }
    }

    fun dischargePatient(patientId: String, summary: String) {
        val pt = patients.value.firstOrNull { it.id == patientId } ?: return
        val dischargeTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val updatedPatient = pt.copy(
            status = "DISCHARGED",
            dischargedOn = dischargeTime,
            dischargeSummary = summary.ifBlank { "Patient discharged by ${currentUser.value?.name ?: "Staff"}." }
        )
        val notif = NotificationEntity(
            id = "nt_" + UUID.randomUUID().toString().take(8),
            title = "🚪 Patient Discharged",
            body = "${pt.name} discharged from ${pt.ward} Bed ${pt.bed}. Summary: ${updatedPatient.dischargeSummary}",
            time = currentTime,
            audience = "all",
            kind = "admin"
        )
        val bc = HospitalBroadcast(
            id = "bc_" + UUID.randomUUID().toString().take(8),
            title = "🚪 Discharge Update — ${pt.name}",
            body = "${pt.name} (${pt.id}) has been discharged from ${pt.ward} Bed ${pt.bed}. Bed is now vacant.",
            time = currentTime,
            audience = "all",
            priority = "NORMAL",
            senderName = currentUser.value?.name ?: "Duty Staff",
            senderRole = currentUser.value?.role ?: "Staff",
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.updatePatient(updatedPatient)
            repository.insertNotification(notif)
            com.example.cloud.CloudDatabaseManager.syncPatientToCloud(getApplication(), updatedPatient)
            com.example.cloud.CloudDatabaseManager.dispatchHospitalBroadcast(getApplication(), bc)
            showToast("🚪 ${pt.name} marked as DISCHARGED")
        }
    }

    fun readmitPatient(patientId: String) {
        val pt = patients.value.firstOrNull { it.id == patientId } ?: return
        val updatedPatient = pt.copy(
            status = "ADMITTED",
            dischargedOn = null,
            dischargeSummary = null
        )
        val readmitTitle = "🏥 Patient Re-Admitted — ${pt.name}"
        val readmitBody = "${pt.name} (${pt.id}) re-admitted to ${pt.ward} Bed ${pt.bed} by ${currentUser.value?.name ?: "Staff"}."
        val notif = NotificationEntity(
            id = "nt_" + UUID.randomUUID().toString().take(8),
            title = readmitTitle,
            body = readmitBody,
            time = currentTime,
            audience = "all",
            kind = "admin"
        )
        val bc = HospitalBroadcast(
            id = "bc_" + UUID.randomUUID().toString().take(8),
            title = readmitTitle,
            body = readmitBody,
            time = currentTime,
            audience = "all",
            priority = "NORMAL",
            senderName = currentUser.value?.name ?: "Duty Staff",
            senderRole = currentUser.value?.role ?: "Staff",
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.updatePatient(updatedPatient)
            repository.insertNotification(notif)
            com.example.cloud.CloudDatabaseManager.syncPatientToCloud(getApplication(), updatedPatient)
            com.example.cloud.CloudDatabaseManager.dispatchHospitalBroadcast(getApplication(), bc)
            showToast("🏥 ${pt.name} re-admitted to ${pt.ward} Bed ${pt.bed}")
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
            showToast("📢 Broadcast announcement dispatched")
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
        targetAudience: String = "all",
        voiceNoteBase64: String? = null,
        voiceDurationSec: Int = 0
    ) {
        if (title.isBlank() && voiceNoteBase64 == null) return
        val user = currentUser.value
        val cleanTitle = if (title.isNotBlank()) title.trim() else "🎙️ Voice Announcement (${voiceDurationSec}s)"
        val cleanBody = if (body.isNotBlank()) body.trim() else "Voice message attached from ${user?.name ?: "Staff"}"
        val broadcast = HospitalBroadcast(
            id = "bc_" + UUID.randomUUID().toString().take(8),
            title = cleanTitle,
            body = cleanBody,
            priority = priority.name,
            senderName = user?.name ?: "Hospital Administration",
            senderRole = user?.role ?: "Admin",
            time = currentTime,
            audience = targetAudience,
            timestamp = System.currentTimeMillis(),
            voiceNoteBase64 = voiceNoteBase64,
            voiceDurationSec = voiceDurationSec
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
            showToast("⏳ Uploading all hospital data to Cloud Storage...")

            // Ensure Boss user exists in database and has role BOSS
            val bossInDb = repository.getUserById("BOSS-0001")
            val bossUser = UserEntity(
                id = "BOSS-0001",
                pass = if (!bossInDb?.pass.isNullOrBlank()) bossInDb!!.pass else "12345",
                name = "Dr. S. K. Bose",
                role = "BOSS",
                dept = "Administration",
                specialty = "Owner · Full Access",
                phone = "+91 98300 11111",
                joinedDate = "2015-04-01"
            )
            repository.insertUser(bossUser)
            com.example.cloud.CloudDatabaseManager.syncUserToCloud(app, bossUser)

            val localUsers = repository.getAllUsersDirect()
            localUsers.forEach { u ->
                val userToSync = if (u.id == "BOSS-0001") bossUser else u
                com.example.cloud.CloudDatabaseManager.syncUserToCloud(app, userToSync)
            }
            val localPatients = repository.getAllPatientsDirect()
            localPatients.forEach { pt ->
                com.example.cloud.CloudDatabaseManager.syncPatientToCloud(app, pt)
            }
            documents.value.forEach { doc ->
                com.example.cloud.CloudDatabaseManager.syncDocumentToCloud(app, doc)
            }
            clinicalNotes.value.forEach { note ->
                com.example.cloud.CloudDatabaseManager.syncClinicalNoteToCloud(app, note)
            }
            carePlans.value.forEach { plan ->
                com.example.cloud.CloudDatabaseManager.syncCarePlanToCloud(app, plan)
            }
            attendance.value.forEach { att ->
                com.example.cloud.CloudDatabaseManager.syncAttendanceToCloud(app, att)
            }
            vitalRecords.value.forEach { vr ->
                com.example.cloud.CloudDatabaseManager.syncVitalRecordToCloud(app, vr)
            }
            notifications.value.forEach { notif ->
                com.example.cloud.CloudDatabaseManager.publishNotificationToCloud(app, notif)
            }

            com.example.cloud.CloudDatabaseManager.reconcileWithCloudServer(app, repository)
            showToast("✓ All local records uploaded and synced with Cloud Storage")
        }
    }

    fun restoreAndSyncAllHospitalData() {
        val app = getApplication<android.app.Application>()
        viewModelScope.launch {
            showToast("⏳ Restoring master hospital database & syncing cloud...")

            // Re-seed local database with master hospital seed data
            repository.insertUsers(com.example.data.HospitalSeedData.users)
            repository.insertPatients(com.example.data.HospitalSeedData.patients)
            repository.insertDocuments(com.example.data.HospitalSeedData.documents)
            repository.insertClinicalNotes(com.example.data.HospitalSeedData.clinicalNotes)
            repository.insertCarePlans(com.example.data.HospitalSeedData.carePlans)
            repository.insertAttendances(com.example.data.HospitalSeedData.attendances)
            repository.insertNotifications(com.example.data.HospitalSeedData.notifications)

            // Ensure BOSS-0001
            val bossUser = UserEntity(
                id = "BOSS-0001",
                pass = "12345",
                name = "Dr. S. K. Bose",
                role = "BOSS",
                dept = "Administration",
                specialty = "Owner · Full Access",
                phone = "+91 98300 11111",
                joinedDate = "2015-04-01"
            )
            repository.insertUser(bossUser)

            // Push every master entity to Cloud Firestore
            com.example.data.HospitalSeedData.users.forEach { user ->
                val u = if (user.id == "BOSS-0001") bossUser else user
                com.example.cloud.CloudDatabaseManager.syncUserToCloud(app, u)
            }
            com.example.data.HospitalSeedData.patients.forEach { pt ->
                com.example.cloud.CloudDatabaseManager.syncPatientToCloud(app, pt)
            }
            com.example.data.HospitalSeedData.documents.forEach { doc ->
                com.example.cloud.CloudDatabaseManager.syncDocumentToCloud(app, doc)
            }
            com.example.data.HospitalSeedData.clinicalNotes.forEach { note ->
                com.example.cloud.CloudDatabaseManager.syncClinicalNoteToCloud(app, note)
            }
            com.example.data.HospitalSeedData.carePlans.forEach { plan ->
                com.example.cloud.CloudDatabaseManager.syncCarePlanToCloud(app, plan)
            }
            com.example.data.HospitalSeedData.attendances.forEach { att ->
                com.example.cloud.CloudDatabaseManager.syncAttendanceToCloud(app, att)
            }
            com.example.data.HospitalSeedData.notifications.forEach { notif ->
                com.example.cloud.CloudDatabaseManager.publishNotificationToCloud(app, notif)
            }

            // Pull latest reconciliation
            com.example.cloud.CloudDatabaseManager.reconcileWithCloudServer(app, repository)

            // Keep current user logged in
            val current = _currentUser.value
            if (current != null) {
                val updated = repository.getUserById(current.id) ?: bossUser
                _currentUser.value = updated
            } else {
                _currentUser.value = bossUser
            }

            showToast("✓ Master hospital database & Boss account fully restored and synced")
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
        pass: String,
        customId: String? = null
    ) {
        val prefix = role.prefix
        val existingNums = users.value
            .filter { it.id.startsWith("$prefix-") }
            .mapNotNull { it.id.substringAfter("-").toIntOrNull() }
        val nextNum = if (existingNums.isNotEmpty()) existingNums.maxOrNull()!! + 1
        else when (role) {
            UserRole.DOCTOR -> 2001
            UserRole.RMO -> 2101
            UserRole.MEDICAL_SUPER -> 2201
            UserRole.RMO_INCHARGE -> 2301
            UserRole.ADMINISTRATOR -> 1001
            UserRole.NURSE -> 3001
            UserRole.TECHNICIAN -> 3501
            UserRole.MAINTENANCE -> 4401
            UserRole.RECEPTIONIST -> 4001
            UserRole.ACCOUNTANT -> 4002
            UserRole.INCHARGE -> 4101
            UserRole.CASHIER -> 4201
            UserRole.MEDICINE -> 4301
            else -> 4001
        }
        val generatedId = "$prefix-${String.format(Locale.getDefault(), "%04d", nextNum)}"
        val finalId = if (!customId.isNullOrBlank()) customId.trim().uppercase() else generatedId

        if (users.value.any { it.id.equals(finalId, ignoreCase = true) }) {
            showToast("⚠ User ID $finalId is already in use. Please specify another ID.")
            return
        }

        val newUser = UserEntity(
            id = finalId,
            pass = pass.ifBlank { "12345" },
            name = name.trim(),
            role = role.name,
            dept = dept,
            specialty = specialty,
            phone = phone,
            email = email,
            photoUri = photoUri,
            joinedDate = todayDate
        )
        val notif = NotificationEntity(
            id = "nt_" + UUID.randomUUID().toString().take(8),
            title = "New member registered",
            body = "$name joined as ${role.label} · Login ID: $finalId",
            time = currentTime,
            audience = "all",
            kind = "admin"
        )
        val bc = HospitalBroadcast(
            id = "bc_" + UUID.randomUUID().toString().take(8),
            title = "👤 New ${role.label} Registered: $name",
            body = "Welcome $name (${role.label}) to MB Nursing Home. ID: $finalId · Dept: $dept",
            time = currentTime,
            audience = "all",
            priority = "NORMAL",
            senderName = _currentUser.value?.name ?: "Boss",
            senderRole = _currentUser.value?.role ?: "Boss",
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.insertUser(newUser)
            repository.insertNotification(notif)
            com.example.cloud.CloudDatabaseManager.syncUserToCloud(getApplication(), newUser)
            com.example.cloud.CloudDatabaseManager.publishNotificationToCloud(getApplication(), notif)
            com.example.cloud.CloudDatabaseManager.dispatchHospitalBroadcast(getApplication(), bc)
            showToast("✉️ $name registered with ID $finalId (Synced with Cloud)")
        }
    }

    fun updateOwnProfile(newName: String, newPhone: String, newEmail: String = "") {
        val current = _currentUser.value ?: return
        if (newName.isBlank()) {
            showToast("Name cannot be empty")
            return
        }
        val updated = current.copy(
            name = newName.trim(),
            phone = newPhone.trim(),
            email = newEmail.trim()
        )
        viewModelScope.launch {
            repository.insertUser(updated)
            _currentUser.value = updated
            com.example.cloud.CloudDatabaseManager.syncUserToCloud(getApplication(), updated)
            val notif = NotificationEntity(
                id = "nt_" + UUID.randomUUID().toString().take(8),
                title = "Profile updated",
                body = "Name/contact details updated for ${updated.name} (${updated.id}).",
                time = currentTime,
                audience = updated.id,
                kind = "admin"
            )
            val bc = HospitalBroadcast(
                id = "bc_" + UUID.randomUUID().toString().take(8),
                title = "👨‍⚕️ Doctor / Staff Profile Updated — ${updated.name}",
                body = "${updated.name} (${updated.role}) updated contact details${if (updated.phone.isNotBlank()) " · Phone: ${updated.phone}" else ""}.",
                time = currentTime,
                audience = "all",
                priority = "NORMAL",
                senderName = updated.name,
                senderRole = updated.role,
                timestamp = System.currentTimeMillis()
            )
            repository.insertNotification(notif)
            com.example.cloud.CloudDatabaseManager.publishNotificationToCloud(getApplication(), notif)
            com.example.cloud.CloudDatabaseManager.dispatchHospitalBroadcast(getApplication(), bc)
            showToast("✓ Profile updated successfully")
        }
    }

    fun updateOwnPassword(currentPass: String, newPass: String): Boolean {
        val current = _currentUser.value ?: return false
        if (current.pass.isNotBlank() && currentPass != current.pass) {
            showToast("⚠ Current password incorrect")
            return false
        }
        if (newPass.trim().length < 3) {
            showToast("⚠ Password must be at least 3 characters")
            return false
        }
        val updated = current.copy(pass = newPass.trim())
        viewModelScope.launch {
            repository.insertUser(updated)
            _currentUser.value = updated
            com.example.cloud.CloudDatabaseManager.syncUserToCloud(getApplication(), updated)
            val notif = NotificationEntity(
                id = "nt_" + UUID.randomUUID().toString().take(8),
                title = "Password changed",
                body = "Your account password was updated successfully.",
                time = currentTime,
                audience = current.id,
                kind = "admin"
            )
            repository.insertNotification(notif)
            com.example.cloud.CloudDatabaseManager.publishNotificationToCloud(getApplication(), notif)
        }
        showToast("✓ Password changed successfully")
        return true
    }

    fun updateStaffByBoss(
        oldId: String,
        newId: String,
        newName: String,
        newRole: UserRole,
        newDept: String,
        newSpecialty: String,
        newPhone: String,
        newEmail: String,
        newPass: String,
        newPhotoUri: String?
    ): Boolean {
        val current = _currentUser.value
        val isBoss = current?.role == "BOSS" || current?.id == "BOSS-0001"
        if (!isBoss) {
            showToast("⚠ Only Boss has permission to edit staff profiles and credentials")
            return false
        }

        val cleanNewId = newId.trim().uppercase()
        val existingStaff = users.value.firstOrNull { it.id == oldId } ?: return false

        if (cleanNewId != oldId && users.value.any { it.id == cleanNewId }) {
            showToast("⚠ User ID $cleanNewId is already assigned to another staff member")
            return false
        }

        viewModelScope.launch {
            if (cleanNewId != oldId) {
                repository.deleteUser(oldId)
                com.example.cloud.CloudDatabaseManager.deleteUserFromCloud(getApplication(), oldId)
            }

            val updatedUser = UserEntity(
                id = cleanNewId,
                pass = newPass.trim().ifBlank { "12345" },
                name = newName.trim(),
                role = newRole.name,
                dept = newDept.trim(),
                specialty = newSpecialty.trim(),
                phone = newPhone.trim(),
                email = newEmail.trim(),
                photoUri = newPhotoUri ?: existingStaff.photoUri,
                joinedDate = existingStaff.joinedDate
            )

            repository.insertUser(updatedUser)
            com.example.cloud.CloudDatabaseManager.syncUserToCloud(getApplication(), updatedUser)

            if (current.id == oldId) {
                _currentUser.value = updatedUser
            }

            val notif = NotificationEntity(
                id = "nt_" + UUID.randomUUID().toString().take(8),
                title = "Staff profile & rights updated",
                body = "Boss updated profile for $newName (ID: $cleanNewId · ${newRole.label}).",
                time = currentTime,
                audience = "all",
                kind = "admin"
            )
            val bc = HospitalBroadcast(
                id = "bc_" + UUID.randomUUID().toString().take(8),
                title = "👨‍⚕️ Doctor / Staff Updates — $newName",
                body = "Profile updated by Boss: $newName ($cleanNewId · ${newRole.label}). Dept: ${newDept.trim()}${if (newPhone.isNotBlank()) " · Phone: ${newPhone.trim()}" else ""}",
                time = currentTime,
                audience = "all",
                priority = "NORMAL",
                senderName = current?.name ?: "Boss",
                senderRole = current?.role ?: "Boss",
                timestamp = System.currentTimeMillis()
            )
            repository.insertNotification(notif)
            com.example.cloud.CloudDatabaseManager.publishNotificationToCloud(getApplication(), notif)
            com.example.cloud.CloudDatabaseManager.dispatchHospitalBroadcast(getApplication(), bc)
        }
        showToast("✓ Staff $newName ($cleanNewId) updated by Boss")
        return true
    }

    fun deleteStaff(userId: String) {
        if (userId == "BOSS-0001") {
            showToast("⚠️ Boss account cannot be deleted")
            return
        }
        viewModelScope.launch {
            repository.deleteUser(userId)
            com.example.cloud.CloudDatabaseManager.deleteUserFromCloud(getApplication(), userId)
            showToast("Staff member removed")
        }
    }

    fun updateUserPhoto(userId: String, photoUri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalUri = if (photoUri != null && photoUri.startsWith("content://")) {
                try {
                    val stream = getApplication<Application>().contentResolver.openInputStream(Uri.parse(photoUri))
                    val bmp = BitmapFactory.decodeStream(stream)
                    stream?.close()
                    if (bmp != null) {
                        val maxDim = 500
                        val scaled = if (bmp.width > maxDim || bmp.height > maxDim) {
                            val ratio = Math.min(maxDim.toFloat() / bmp.width, maxDim.toFloat() / bmp.height)
                            Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
                        } else bmp
                        val out = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
                        "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                    } else photoUri
                } catch (_: Exception) {
                    photoUri
                }
            } else photoUri

            repository.updateUserPhoto(userId, finalUri)
            val updatedUser = repository.getUserById(userId)
            if (updatedUser != null) {
                com.example.cloud.CloudDatabaseManager.syncUserToCloud(getApplication(), updatedUser)
            }
            withContext(Dispatchers.Main) {
                if (_currentUser.value?.id == userId) {
                    _currentUser.value = _currentUser.value?.copy(photoUri = finalUri)
                }
                showToast("📷 Profile photo updated & synced across all devices")
            }
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
            com.example.cloud.CloudDatabaseManager.deleteUserFromCloud(getApplication(), user.id)
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

    fun clearAllNotificationsAndUpdates() {
        viewModelScope.launch {
            repository.deleteAllNotifications()
            com.example.cloud.CloudDatabaseManager.clearAllNotificationsAndBroadcasts(getApplication())
            showToast("🧹 All notifications and updates cleared")
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.deleteNotification(id)
            com.example.cloud.CloudDatabaseManager.deleteNotification(getApplication(), id)
            showToast("Notification cleared")
        }
    }

    fun dismissBroadcast(id: String) {
        viewModelScope.launch {
            com.example.cloud.CloudDatabaseManager.dismissBroadcast(getApplication(), id)
            showToast("Alert dismissed")
        }
    }

    fun publishAppUpdateByBoss(
        versionName: String,
        releaseTitle: String,
        releaseNotes: String,
        forceUpdate: Boolean
    ) {
        val current = _currentUser.value
        val isBoss = current?.role == "BOSS" || current?.id == "BOSS-0001"
        if (!isBoss) {
            showToast("⚠️ Only Boss (BOSS-0001) can publish app updates")
            return
        }

        val newVersionCode = _installedVersionCode.value + 1
        val cleanName = versionName.ifBlank { "v1.$newVersionCode" }
        val cleanTitle = releaseTitle.ifBlank { "Hospital Auto-Sync & Feature Release" }
        val cleanNotes = releaseNotes.ifBlank { "Boss updated features and database structure. Auto-syncing all devices..." }

        com.example.cloud.CloudDatabaseManager.publishAppUpdateByBoss(
            context = getApplication(),
            versionCode = newVersionCode,
            versionName = cleanName,
            releaseTitle = cleanTitle,
            releaseNotes = cleanNotes,
            forceUpdate = forceUpdate,
            publishedBy = "${current.name} (BOSS-0001)"
        )

        _installedVersionCode.value = newVersionCode
        _installedVersionName.value = cleanName
        showToast("🚀 App update $cleanName published to all devices by Boss!")
    }

    fun applyAppUpdateAndSync() {
        if (_isUpdatingApp.value) return
        viewModelScope.launch {
            _isUpdatingApp.value = true
            showToast("⚡ Updating app modules & syncing cloud database...")
            com.example.cloud.CloudDatabaseManager.reconcileWithCloudServer(getApplication(), repository)
            kotlinx.coroutines.delay(1200)

            val update = appUpdateInfo.value
            if (update != null) {
                _installedVersionCode.value = update.versionCode
                _installedVersionName.value = update.versionName
                _lastAppliedPulseId.value = update.updatePulseId
            } else {
                _installedVersionCode.value += 1
                _installedVersionName.value = "1.${_installedVersionCode.value}"
            }

            _isUpdatingApp.value = false
            _showUpdateModal.value = false
            showToast("✓ App successfully updated to ${_installedVersionName.value}! All features and data synced.")
        }
    }

    fun dismissUpdateModal() {
        _showUpdateModal.value = false
    }

    fun triggerManualCheckForUpdates() {
        viewModelScope.launch {
            showToast("Checking for cloud app updates...")
            com.example.cloud.CloudDatabaseManager.reconcileWithCloudServer(getApplication(), repository)
            kotlinx.coroutines.delay(800)
            val update = appUpdateInfo.value
            if (update != null && (update.versionCode > _installedVersionCode.value || (update.updatePulseId.isNotBlank() && update.updatePulseId != _lastAppliedPulseId.value))) {
                _showUpdateModal.value = true
            } else {
                showToast("✓ App is on the latest version (${_installedVersionName.value})")
            }
        }
    }
}
