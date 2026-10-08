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
            return entries.firstOrNull { it.name.equals(key, ignoreCase = true) || it.label.equals(key, ignoreCase = true) } ?: NURSE
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
        "Cardiology", "Neurology", "General Surgery", "Orthopedics",
        "Pediatrics", "ICU / Critical Care", "General Medicine",
        "Radiology", "Anesthesiology", "Emergency Medicine", "Gynecology"
    )

    val DEPARTMENTS = listOf(
        "Front Desk", "Accounts", "Pharmacy", "Maintenance",
        "Nursing", "Administration", "Cashier", "ICU", "Emergency",
        "Laboratory", "Ward Management"
    )
}
