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
    val allNotifications: Flow<List<NotificationEntity>> = dao.getAllNotifications()

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        AppDatabase.seedDatabase(dao)
    }

    suspend fun getUserById(id: String): UserEntity? = withContext(Dispatchers.IO) {
        dao.getUserById(id)
    }

    suspend fun insertUser(user: UserEntity) = withContext(Dispatchers.IO) {
        dao.insertUser(user)
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

    suspend fun insertPatient(patient: PatientEntity) = withContext(Dispatchers.IO) {
        dao.insertPatient(patient)
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

    suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        dao.deleteDocument(id)
    }

    suspend fun insertClinicalNote(note: ClinicalNoteEntity) = withContext(Dispatchers.IO) {
        dao.insertClinicalNote(note)
    }

    suspend fun insertCarePlan(plan: CarePlanEntity) = withContext(Dispatchers.IO) {
        dao.insertCarePlan(plan)
    }

    suspend fun updateCarePlanStatus(id: String, status: String) = withContext(Dispatchers.IO) {
        dao.updateCarePlanStatus(id, status)
    }

    suspend fun insertAttendance(attendance: AttendanceEntity) = withContext(Dispatchers.IO) {
        dao.insertAttendance(attendance)
    }

    suspend fun insertNotification(notification: NotificationEntity) = withContext(Dispatchers.IO) {
        dao.insertNotification(notification)
    }

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        dao.markAllNotificationsAsRead()
    }

    suspend fun deleteAllNotifications() = withContext(Dispatchers.IO) {
        dao.deleteAllNotifications()
    }
}
