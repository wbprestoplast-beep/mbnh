package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class HospitalRepository(private val dao: HospitalDao) {

    val allUsers: Flow<List<UserEntity>> = dao.getAllUsers()
    val allPatients: Flow<List<PatientEntity>> = dao.getAllPatients()
    val allDocuments: Flow<List<DocumentEntity>> = dao.getAllDocuments()
    val allClinicalNotes: Flow<List<ClinicalNoteEntity>> = dao.getAllClinicalNotes()
    val allCarePlans: Flow<List<CarePlanEntity>> = dao.getAllCarePlans()
    val allAttendance: Flow<List<AttendanceEntity>> = dao.getAllAttendance()
    val allVitalRecords: Flow<List<VitalRecordEntity>> = dao.getAllVitalRecords()
    val allNotifications: Flow<List<NotificationEntity>> = dao.getAllNotifications()

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        AppDatabase.seedDatabase(dao)
    }

    suspend fun getUserById(id: String): UserEntity? = withContext(Dispatchers.IO) {
        dao.getUserById(id)
    }

    suspend fun getUserByCleanId(cleanId: String): UserEntity? = withContext(Dispatchers.IO) {
        dao.getUserByCleanId(cleanId) ?: dao.getUserById(cleanId)
    }

    suspend fun getAllUsersDirect(): List<UserEntity> = withContext(Dispatchers.IO) {
        dao.getAllUsersDirect()
    }

    suspend fun insertUser(user: UserEntity) = withContext(Dispatchers.IO) {
        dao.insertUser(user)
    }

    suspend fun insertUsers(users: List<UserEntity>) = withContext(Dispatchers.IO) {
        dao.insertUsers(users)
    }

    suspend fun deleteUser(id: String) = withContext(Dispatchers.IO) {
        dao.deleteUser(id)
    }

    suspend fun updateUserPhoto(id: String, photoUri: String?) = withContext(Dispatchers.IO) {
        dao.updateUserPhoto(id, photoUri)
    }

    suspend fun updateUserPassword(id: String, newPass: String) = withContext(Dispatchers.IO) {
        dao.updateUserPassword(id, newPass)
    }

    suspend fun resetAllUsersPasswordToDefault() = withContext(Dispatchers.IO) {
        dao.resetAllUsersPasswordToDefault()
    }

    suspend fun getAllPatientsDirect(): List<PatientEntity> = withContext(Dispatchers.IO) {
        dao.getAllPatientsDirect()
    }

    suspend fun insertPatient(patient: PatientEntity) = withContext(Dispatchers.IO) {
        dao.insertPatient(patient)
    }

    suspend fun insertPatients(patients: List<PatientEntity>) = withContext(Dispatchers.IO) {
        dao.insertPatients(patients)
    }

    suspend fun updatePatient(patient: PatientEntity) = withContext(Dispatchers.IO) {
        dao.updatePatient(patient)
    }

    suspend fun deletePatient(id: String) = withContext(Dispatchers.IO) {
        dao.deletePatient(id)
    }

    suspend fun insertDocument(document: DocumentEntity) = withContext(Dispatchers.IO) {
        dao.insertDocument(document)
    }

    suspend fun insertDocuments(documents: List<DocumentEntity>) = withContext(Dispatchers.IO) {
        dao.insertDocuments(documents)
    }

    suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        dao.deleteDocument(id)
    }

    suspend fun insertClinicalNote(note: ClinicalNoteEntity) = withContext(Dispatchers.IO) {
        dao.insertClinicalNote(note)
    }

    suspend fun insertClinicalNotes(notes: List<ClinicalNoteEntity>) = withContext(Dispatchers.IO) {
        dao.insertClinicalNotes(notes)
    }

    suspend fun deleteClinicalNote(id: String) = withContext(Dispatchers.IO) {
        dao.deleteClinicalNote(id)
    }

    suspend fun insertCarePlan(plan: CarePlanEntity) = withContext(Dispatchers.IO) {
        dao.insertCarePlan(plan)
    }

    suspend fun insertCarePlans(plans: List<CarePlanEntity>) = withContext(Dispatchers.IO) {
        dao.insertCarePlans(plans)
    }

    suspend fun updateCarePlanStatus(id: String, status: String) = withContext(Dispatchers.IO) {
        dao.updateCarePlanStatus(id, status)
    }

    suspend fun deleteCarePlan(id: String) = withContext(Dispatchers.IO) {
        dao.deleteCarePlan(id)
    }

    suspend fun insertAttendance(attendance: AttendanceEntity) = withContext(Dispatchers.IO) {
        val existing = dao.getAttendanceByUserAndDate(attendance.userId, attendance.date)
        if (existing != null) {
            dao.insertAttendance(attendance.copy(id = existing.id))
        } else {
            dao.insertAttendance(attendance)
        }
    }

    suspend fun insertAttendances(attendances: List<AttendanceEntity>) = withContext(Dispatchers.IO) {
        attendances.forEach { insertAttendance(it) }
    }

    suspend fun insertVitalRecord(record: VitalRecordEntity) = withContext(Dispatchers.IO) {
        dao.insertVitalRecord(record)
    }

    suspend fun deleteVitalRecord(id: String) = withContext(Dispatchers.IO) {
        dao.deleteVitalRecord(id)
    }

    suspend fun insertNotification(notification: NotificationEntity) = withContext(Dispatchers.IO) {
        dao.insertNotification(notification)
    }

    suspend fun insertNotifications(notifications: List<NotificationEntity>) = withContext(Dispatchers.IO) {
        dao.insertNotifications(notifications)
    }

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        dao.markAllNotificationsAsRead()
    }

    suspend fun deleteAllNotifications() = withContext(Dispatchers.IO) {
        dao.deleteAllNotifications()
    }

    suspend fun deleteNotification(id: String) = withContext(Dispatchers.IO) {
        dao.deleteNotification(id)
    }

    suspend fun getAllPatientIds(): List<String> = withContext(Dispatchers.IO) { dao.getAllPatientIds() }
    suspend fun getAllUserIds(): List<String> = withContext(Dispatchers.IO) { dao.getAllUserIds() }
    suspend fun getAllDocumentIds(): List<String> = withContext(Dispatchers.IO) { dao.getAllDocumentIds() }
    suspend fun getAllClinicalNoteIds(): List<String> = withContext(Dispatchers.IO) { dao.getAllClinicalNoteIds() }
    suspend fun getAllCarePlanIds(): List<String> = withContext(Dispatchers.IO) { dao.getAllCarePlanIds() }
    suspend fun getAllVitalRecordIds(): List<String> = withContext(Dispatchers.IO) { dao.getAllVitalRecordIds() }

    suspend fun getAllDocumentsDirect(): List<DocumentEntity> = withContext(Dispatchers.IO) { dao.getAllDocumentsDirect() }
    suspend fun getAllClinicalNotesDirect(): List<ClinicalNoteEntity> = withContext(Dispatchers.IO) { dao.getAllClinicalNotesDirect() }
    suspend fun getAllCarePlansDirect(): List<CarePlanEntity> = withContext(Dispatchers.IO) { dao.getAllCarePlansDirect() }
    suspend fun getAllVitalRecordsDirect(): List<VitalRecordEntity> = withContext(Dispatchers.IO) { dao.getAllVitalRecordsDirect() }
    suspend fun getAllAttendanceDirect(): List<AttendanceEntity> = withContext(Dispatchers.IO) { dao.getAllAttendanceDirect() }
}
