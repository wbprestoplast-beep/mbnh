package com.example.model

enum class UserRole(val label: String, val prefix: String, val colorHex: Long) {
    BOSS("Boss", "BOSS", 0xFFD97706),
    DOCTOR("Doctor", "DOC", 0xFF0E7490),
    RMO("RMO", "RMO", 0xFF0891B2),
    MEDICAL_SUPER("Medical Super", "MSU", 0xFF059669),
    RMO_INCHARGE("RMO Incharge", "RMI", 0xFF0284C7),
    NURSE("Nurse", "NUR", 0xFF7C3AED),
    TECHNICIAN("Technician", "TEC", 0xFF0369A1),
    ADMINISTRATOR("Administrator", "ADM", 0xFFB45309),
    RECEPTIONIST("Receptionist", "RCP", 0xFFBE185D),
    ACCOUNTANT("Accountant", "ACC", 0xFF15803D),
    INCHARGE("Incharge", "INC", 0xFF0F766E),
    CASHIER("Cashier", "CSH", 0xFFA16207),
    MEDICINE("Medicine / Pharmacy", "MED", 0xFFDC2626),
    MAINTENANCE("Maintenance", "MNT", 0xFF475569);

    companion object {
        fun fromKey(key: String): UserRole {
            val clean = key.trim().uppercase()
            if (clean == "BOSS" || clean.contains("BOSS") || clean.contains("OWNER") || clean.contains("DIRECTOR") || clean == "BOSS-0001") return BOSS
            if (clean == "ADMINISTRATOR" || clean == "ADMIN" || clean.contains("ADMIN")) return ADMINISTRATOR
            if (clean == "DOCTOR" || clean == "DOC" || clean.contains("DOCTOR") || clean.contains("CARDIOL") || clean.contains("NEUROL") || clean.contains("SURG")) return DOCTOR
            if (clean == "MEDICAL_SUPER" || clean == "MSU" || clean.contains("SUPER")) return MEDICAL_SUPER
            if (clean == "RMO_INCHARGE" || clean == "RMI" || (clean.contains("INCHARGE") && clean.contains("RMO"))) return RMO_INCHARGE
            if (clean == "RMO" || clean.contains("RESIDENT")) return RMO
            if (clean == "NURSE" || clean == "NUR" || clean.contains("NURS")) return NURSE
            if (clean == "TECHNICIAN" || clean == "TEC" || clean.contains("TECH") || clean.contains("LAB") || clean.contains("RADIO")) return TECHNICIAN
            if (clean == "RECEPTIONIST" || clean == "RCP" || clean.contains("RECEPT") || clean.contains("FRONT")) return RECEPTIONIST
            if (clean == "ACCOUNTANT" || clean == "ACC" || clean.contains("ACCOUNT") || clean.contains("BILL")) return ACCOUNTANT
            if (clean == "INCHARGE" || clean == "INC" || clean.contains("INCHARGE")) return INCHARGE
            if (clean == "CASHIER" || clean == "CSH" || clean.contains("CASH")) return CASHIER
            if (clean == "MEDICINE" || clean == "MED" || clean.contains("PHARM") || clean.contains("MEDIC")) return MEDICINE
            if (clean == "MAINTENANCE" || clean == "MNT" || clean.contains("MAINT") || clean.contains("FACIL")) return MAINTENANCE

            return entries.firstOrNull { 
                it.name.equals(key, ignoreCase = true) || 
                it.label.equals(key, ignoreCase = true) ||
                it.prefix.equals(key, ignoreCase = true)
            } ?: NURSE
        }
    }
}

enum class DocCategory(val label: String, val badgeColorHex: Long) {
    PRESCRIPTION("Prescription", 0xFF0284C7),
    LAB_PANEL("Lab Panel", 0xFF16A34A),
    X_RAY("X-Ray", 0xFF475569),
    MRI_CT("MRI / CT Scan", 0xFFD97706),
    DISCHARGE_SUMMARY("Discharge Summary", 0xFFDC2626),
    OTHER_REPORT("Other Report", 0xFF64748B);

    companion object {
        fun fromString(str: String): DocCategory {
            return entries.firstOrNull { it.label.equals(str, ignoreCase = true) || it.name.equals(str, ignoreCase = true) } ?: OTHER_REPORT
        }
    }
}

enum class DocumentFilter(val label: String) {
    ORIGINAL("Original"),
    BW_CONTRAST("High-Contrast B&W"),
    GRAYSCALE("Doc Grayscale"),
    BOOST("Document Boost")
}

enum class AppScreen {
    LOGIN,
    HOME,
    PATIENTS,
    PATIENT_DETAIL,
    ADMIT_PATIENT,
    BEDS,
    ATTENDANCE,
    PLANNINGS,
    ADMIN,
    NOTIFICATIONS,
    SETTINGS
}

data class WardInfo(
    val name: String,
    val beds: List<String>
)

enum class BroadcastPriority(val label: String, val badgeColorHex: Long) {
    CRITICAL("🚨 Critical / Emergency", 0xFFDC2626),
    HIGH("⚠️ High Priority Clinical", 0xFFD97706),
    APP_UPDATE("🚀 App & System Update", 0xFF0284C7),
    NORMAL("📢 General Announcement", 0xFF0E7490)
}

data class HospitalBroadcast(
    val id: String,
    val title: String,
    val body: String,
    val priority: String,
    val senderName: String,
    val senderRole: String,
    val time: String,
    val audience: String,
    val timestamp: Long = System.currentTimeMillis(),
    val voiceNoteBase64: String? = null,
    val voiceDurationSec: Int = 0
)

object HospitalConstants {
    val WARDS = listOf(
        WardInfo(
            "ICU",
            (101..108).map { "$it" }
        ),
        WardInfo(
            "Male Ward",
            (201..211).map { "$it" }
        ),
        WardInfo(
            "Female Ward",
            (212..224).map { "$it" }
        ),
        WardInfo(
            "3rd Floor",
            listOf(
                "301", "302A", "302B", "302C", "302D",
                "303", "304", "305", "306",
                "307A", "307B", "308",
                "309A", "309B", "309C",
                "310A", "310B"
            )
        )
    )

    val SPECIALTIES = listOf(
        "Anesthesiologist",
        "Cardiologist",
        "Dermatologist",
        "Endocrinologist",
        "Gastroenterologist",
        "General Surgeon",
        "Gynecologist & Obstetrician",
        "Hematologist",
        "Immunologist",
        "Infectious Disease Specialist",
        "Nephrologist",
        "Neurologist",
        "Neurosurgeon",
        "Ophthalmologist",
        "Otolaryngologist (ENT)",
        "Physical Medicine and Rehabilitation (Physiatrist)",
        "Plastic / Reconstructive Surgeon",
        "Pre-Anesthetic Check-up (PAC)",
        "Psychiatrist",
        "Pulmonologist",
        "Reproductive Endocrinologist",
        "Rheumatologist",
        "General Medicine / Internal Medicine",
        "Orthopedics",
        "Pediatrician",
        "ICU / Critical Care",
        "Emergency Medicine",
        "Oncology",
        "Urology",
        "Radiology",
        "Pathology"
    )

    val DEPARTMENTS = listOf(
        "Front Desk", "Accounts", "Pharmacy", "Maintenance",
        "Nursing", "Administration", "Cashier", "ICU", "Emergency",
        "Laboratory", "Ward Management"
    )
}

fun com.example.data.UserEntity?.canEditPatientDetails(): Boolean {
    if (this == null) return false
    val r = role.uppercase()
    return r == "BOSS" ||
           r == "ADMINISTRATOR" ||
           r == "ADMIN" ||
           r == "INCHARGE" ||
           r == "RMO_INCHARGE" ||
           r == "RMO" ||
           r == "MEDICAL_SUPER" ||
           r.contains("ADMIN") ||
           r.contains("INCHARGE") ||
           r.contains("RMO")
}

fun com.example.data.UserEntity?.canChangeAttendingDoctor(): Boolean {
    if (this == null) return false
    val r = role.uppercase()
    return r == "BOSS" ||
           r == "ADMINISTRATOR" ||
           r == "ADMIN" ||
           r == "INCHARGE" ||
           r == "RMO_INCHARGE" ||
           r == "RECEPTIONIST" ||
           r.contains("ADMIN") ||
           r.contains("INCHARGE") ||
           r.contains("RECEPT")
}

fun com.example.data.UserEntity?.canDischargeAndTransfer(): Boolean {
    if (this == null) return false
    val r = role.uppercase()
    return r == "BOSS" ||
           r == "ADMINISTRATOR" ||
           r == "ADMIN" ||
           r == "INCHARGE" ||
           r == "RMO_INCHARGE" ||
           r == "RMO" ||
           r == "MEDICAL_SUPER" ||
           r == "RECEPTIONIST" ||
           r.contains("ADMIN") ||
           r.contains("INCHARGE") ||
           r.contains("RMO") ||
           r.contains("RECEPT")
}

fun com.example.data.UserEntity?.canViewAllStaff(): Boolean {
    if (this == null) return false
    val r = role.uppercase()
    return r == "BOSS" ||
           r == "ADMINISTRATOR" ||
           r == "ADMIN" ||
           r == "RECEPTIONIST" ||
           r == "MAINTENANCE" ||
           r == "ACCOUNTANT" ||
           r == "ACCOUNTS" ||
           r == "CASHIER" ||
           r == "RMO" ||
           r == "RMO_INCHARGE" ||
           r == "DOCTOR" ||
           r == "MEDICAL_SUPER" ||
           r == "INCHARGE" ||
           r.contains("ADMIN") ||
           r.contains("RECEPT") ||
           r.contains("MAINT") ||
           r.contains("ACC") ||
           r.contains("RMO")
}

fun com.example.data.UserEntity?.canDoctorAddClinicalItems(): Boolean {
    if (this == null) return false
    val r = role.uppercase()
    return r == "DOCTOR" ||
           r == "RMO" ||
           r == "RMO_INCHARGE" ||
           r == "MEDICAL_SUPER" ||
           r == "BOSS" ||
           r.contains("DOC") ||
           r.contains("RMO")
}

fun com.example.data.UserEntity?.canAddVitalsAndClinicalItems(): Boolean {
    if (this == null) return false
    val r = role.uppercase()
    return r == "NURSE" ||
           r == "BOSS" ||
           r == "ADMINISTRATOR" ||
           r == "ADMIN" ||
           r == "INCHARGE" ||
           r == "RMO_INCHARGE" ||
           r == "RMO" ||
           r == "DOCTOR" ||
           r == "MEDICAL_SUPER" ||
           r.contains("NURS") ||
           r.contains("ADMIN") ||
           r.contains("INCHARGE") ||
           r.contains("RMO") ||
           r.contains("DOC")
}

data class AppUpdateInfo(
    val versionCode: Int = 1,
    val versionName: String = "1.0",
    val releaseTitle: String = "App & Feature Release",
    val releaseNotes: String = "All hospital modules and cloud database sync verified.",
    val forceUpdate: Boolean = false,
    val publishedBy: String = "BOSS-0001 (Boss)",
    val publishedAt: String = "",
    val updatePulseId: String = ""
)

