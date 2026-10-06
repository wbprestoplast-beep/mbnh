package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HospitalDao {

    // Users
    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: String)

    @Query("UPDATE users SET photoUri = :photoUri WHERE id = :id")
    suspend fun updateUserPhoto(id: String, photoUri: String?)

    @Query("UPDATE users SET pass = :newPass WHERE id = :id")
    suspend fun updateUserPassword(id: String, newPass: String)

    @Query("UPDATE users SET pass = '12345'")
    suspend fun resetAllUsersPasswordToDefault()

    // Patients
    @Query("SELECT * FROM patients ORDER BY id ASC")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE id = :id LIMIT 1")
    suspend fun getPatientById(id: String): PatientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatients(patients: List<PatientEntity>)

    @Update
    suspend fun updatePatient(patient: PatientEntity)

    @Query("DELETE FROM patients WHERE id = :id")
    suspend fun deletePatient(id: String)

    // Documents
    @Query("SELECT * FROM documents ORDER BY date DESC, id DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE patientId = :patientId ORDER BY date DESC")
    fun getDocumentsByPatient(patientId: String): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocumentEntity>)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocument(id: String)

    // Clinical Notes
    @Query("SELECT * FROM clinical_notes ORDER BY date DESC, id DESC")
    fun getAllClinicalNotes(): Flow<List<ClinicalNoteEntity>>

    @Query("SELECT * FROM clinical_notes WHERE patientId = :patientId ORDER BY date DESC")
    fun getNotesByPatient(patientId: String): Flow<List<ClinicalNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClinicalNote(note: ClinicalNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClinicalNotes(notes: List<ClinicalNoteEntity>)

    // Care Plans
    @Query("SELECT * FROM care_plans ORDER BY due ASC, id ASC")
    fun getAllCarePlans(): Flow<List<CarePlanEntity>>

    @Query("SELECT * FROM care_plans WHERE patientId = :patientId ORDER BY due ASC")
    fun getCarePlansByPatient(patientId: String): Flow<List<CarePlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarePlan(plan: CarePlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarePlans(plans: List<CarePlanEntity>)

    @Query("UPDATE care_plans SET status = :status WHERE id = :id")
    suspend fun updateCarePlanStatus(id: String, status: String)

    // Attendance
    @Query("SELECT * FROM attendance ORDER BY date DESC, time DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE date = :date ORDER BY time DESC")
    fun getAttendanceByDate(date: String): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendances(attendances: List<AttendanceEntity>)

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY id DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET read = 1")
    suspend fun markAllNotificationsAsRead()

    // Counts for initialization check
    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUsersCount(): Int
}
