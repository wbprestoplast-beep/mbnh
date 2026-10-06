package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val pass: String,
    val name: String,
    val role: String,
    val dept: String,
    val specialty: String,
    val phone: String,
    val email: String = "",
    val photoUri: String? = null,
    val joinedDate: String
)

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey val id: String,
    val name: String,
    val age: Int,
    val gender: String,
    val ward: String,
    val bed: String,
    val doctorId: String,
    val admittedOn: String,
    val condition: String,
    val phone: String,
    val referralDoctorId: String? = null,
    val referralReason: String? = null,
    val referralBy: String? = null,
    val referralDate: String? = null
)

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val title: String,
    val category: String,
    val date: String,
    val docTypeOrUri: String, // e.g. "SEED_RX", "SEED_LAB", "SEED_XRAY", "SEED_MRI", or file URI / base64
    val filterApplied: String = "ORIGINAL",
    val remarks: String = "",
    val addedBy: String
)

@Entity(tableName = "clinical_notes")
data class ClinicalNoteEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val text: String,
    val authorId: String,
    val date: String
)

@Entity(tableName = "care_plans")
data class CarePlanEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val title: String,
    val due: String,
    val status: String // "pending" or "done"
)

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val date: String,
    val time: String,
    val method: String // "Biometric" or "Password"
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val time: String,
    val audience: String, // "all" or userId
    val read: Boolean = false,
    val kind: String // "task", "refer", "doc", "attendance", "admin"
)
