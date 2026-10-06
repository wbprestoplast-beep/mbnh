package com.example.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HospitalSeedData {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today: String = dateFormat.format(Date())

    val users = listOf(
        UserEntity(
            id = "BOSS-0001",
            pass = "12345",
            name = "Dr. S. K. Bose",
            role = "BOSS",
            dept = "Administration",
            specialty = "Owner · Full Access",
            phone = "+91 98300 11111",
            joinedDate = "2015-04-01"
        ),
        UserEntity(
            id = "ADM-1001",
            pass = "12345",
            name = "Rita Sharma",
            role = "ADMINISTRATOR",
            dept = "Administration",
            specialty = "Staff Management",
            phone = "+91 98300 22222",
            joinedDate = "2019-08-12"
        ),
        UserEntity(
            id = "DOC-2001",
            pass = "12345",
            name = "Dr. Arif Khan",
            role = "DOCTOR",
            dept = "Cardiology",
            specialty = "Cardiology",
            phone = "+91 98301 33331",
            joinedDate = "2020-02-10"
        ),
        UserEntity(
            id = "DOC-2002",
            pass = "12345",
            name = "Dr. Priya Mehta",
            role = "DOCTOR",
            dept = "Neurology",
            specialty = "Neurology",
            phone = "+91 98301 33332",
            joinedDate = "2021-06-01"
        ),
        UserEntity(
            id = "DOC-2003",
            pass = "12345",
            name = "Dr. J. Nandi",
            role = "DOCTOR",
            dept = "General Surgery",
            specialty = "General Surgery",
            phone = "+91 98301 33333",
            joinedDate = "2022-01-15"
        ),
        UserEntity(
            id = "NUR-3001",
            pass = "12345",
            name = "Anita Das",
            role = "NURSE",
            dept = "ICU",
            specialty = "Critical Care Nursing",
            phone = "+91 98302 44441",
            joinedDate = "2021-03-20"
        ),
        UserEntity(
            id = "NUR-3002",
            pass = "12345",
            name = "Sneha Roy",
            role = "NURSE",
            dept = "Nursing",
            specialty = "General Ward",
            phone = "+91 98302 44442",
            joinedDate = "2022-09-05"
        ),
        UserEntity(
            id = "TEC-3501",
            pass = "12345",
            name = "R. Verma",
            role = "TECHNICIAN",
            dept = "Laboratory",
            specialty = "Radiology Technician",
            phone = "+91 98303 55551",
            joinedDate = "2023-01-09"
        ),
        UserEntity(
            id = "RCP-4001",
            pass = "12345",
            name = "M. Hussain",
            role = "RECEPTIONIST",
            dept = "Front Desk",
            specialty = "Reception",
            phone = "+91 98304 66661",
            joinedDate = "2020-11-11"
        ),
        UserEntity(
            id = "ACC-4002",
            pass = "12345",
            name = "S. Gupta",
            role = "ACCOUNTANT",
            dept = "Accounts",
            specialty = "Billing & Accounts",
            phone = "+91 98304 66662",
            joinedDate = "2021-05-30"
        ),
        UserEntity(
            id = "INC-4101",
            pass = "12345",
            name = "K. Bhattacharya",
            role = "INCHARGE",
            dept = "Ward Management",
            specialty = "Nursing Incharge",
            phone = "+91 98305 77771",
            joinedDate = "2019-02-14"
        ),
        UserEntity(
            id = "CSH-4201",
            pass = "12345",
            name = "P. Saha",
            role = "CASHIER",
            dept = "Cashier",
            specialty = "Counter Cashier",
            phone = "+91 98306 88881",
            joinedDate = "2023-07-01"
        ),
        UserEntity(
            id = "MED-4301",
            pass = "12345",
            name = "D. Pal",
            role = "MEDICINE",
            dept = "Pharmacy",
            specialty = "Pharmacist",
            phone = "+91 98307 99991",
            joinedDate = "2022-04-18"
        ),
        UserEntity(
            id = "MNT-4401",
            pass = "12345",
            name = "G. Mandal",
            role = "MAINTENANCE",
            dept = "Maintenance",
            specialty = "Facility Maintenance",
            phone = "+91 98308 10101",
            joinedDate = "2020-01-02"
        )
    )

    val patients = listOf(
        PatientEntity(
            id = "PT-5001",
            name = "Ratan Dey",
            age = 58,
            gender = "M",
            ward = "ICU",
            bed = "I-201",
            doctorId = "DOC-2001",
            admittedOn = "2026-09-20",
            condition = "Acute coronary syndrome — post angioplasty",
            phone = "+91 90000 11111"
        ),
        PatientEntity(
            id = "PT-5002",
            name = "Mamata Khatun",
            age = 44,
            gender = "F",
            ward = "General Ward",
            bed = "G-102",
            doctorId = "DOC-2001",
            admittedOn = "2026-09-22",
            condition = "Hypertensive urgency — observation",
            phone = "+91 90000 22222"
        ),
        PatientEntity(
            id = "PT-5003",
            name = "Bikash Shaw",
            age = 61,
            gender = "M",
            ward = "General Ward",
            bed = "G-105",
            doctorId = "DOC-2001",
            admittedOn = "2026-09-18",
            condition = "CHF — diuretic titration",
            phone = "+91 90000 33333"
        ),
        PatientEntity(
            id = "PT-5004",
            name = "Subhash Mitra",
            age = 66,
            gender = "M",
            ward = "ICU",
            bed = "I-203",
            doctorId = "DOC-2002",
            admittedOn = "2026-09-17",
            condition = "Acute ischemic stroke (MCA territory)",
            phone = "+91 90000 44444",
            referralDoctorId = "DOC-2001",
            referralReason = "Cardiac evaluation — suspected cardio-embolic source, AF on ECG.",
            referralBy = "DOC-2002",
            referralDate = "2026-09-19"
        ),
        PatientEntity(
            id = "PT-5005",
            name = "Lakshmi Devi",
            age = 72,
            gender = "F",
            ward = "General Ward",
            bed = "G-101",
            doctorId = "DOC-2002",
            admittedOn = "2026-09-21",
            condition = "Vertebrobasilar insufficiency",
            phone = "+91 90000 55555"
        ),
        PatientEntity(
            id = "PT-5006",
            name = "Imran Ali",
            age = 35,
            gender = "M",
            ward = "General Ward",
            bed = "G-106",
            doctorId = "DOC-2003",
            admittedOn = "2026-09-23",
            condition = "Acute appendicitis — post appendectomy",
            phone = "+91 90000 66666"
        ),
        PatientEntity(
            id = "PT-5007",
            name = "Shikha Ghosh",
            age = 29,
            gender = "F",
            ward = "Private Cabins",
            bed = "C-302",
            doctorId = "DOC-2003",
            admittedOn = "2026-09-24",
            condition = "Cholelithiasis — scheduled lap chole",
            phone = "+91 90000 77777"
        ),
        PatientEntity(
            id = "PT-5008",
            name = "A. Mandal",
            age = 8,
            gender = "M",
            ward = "General Ward",
            bed = "G-108",
            doctorId = "DOC-2002",
            admittedOn = "2026-09-24",
            condition = "Febrile seizure — observation",
            phone = "+91 90000 88888"
        ),
        PatientEntity(
            id = "PT-5009",
            name = "Nirmal Bose",
            age = 70,
            gender = "M",
            ward = "ICU",
            bed = "I-202",
            doctorId = "DOC-2001",
            admittedOn = "2026-09-15",
            condition = "NSTEMI — conservative management",
            phone = "+91 90000 99999"
        ),
        PatientEntity(
            id = "PT-5010",
            name = "Rupa Sen",
            age = 52,
            gender = "F",
            ward = "Private Cabins",
            bed = "C-301",
            doctorId = "DOC-2003",
            admittedOn = "2026-09-19",
            condition = "Herniorrhaphy — recovery",
            phone = "+91 90000 12121",
            referralDoctorId = "DOC-2002",
            referralReason = "New onset confusion post-anesthesia — neurological opinion requested.",
            referralBy = "DOC-2003",
            referralDate = "2026-09-24"
        )
    )

    val documents = listOf(
        DocumentEntity(
            id = "d1",
            patientId = "PT-5001",
            title = "Rx — Dual antiplatelet therapy",
            category = "Prescription",
            date = "2026-09-20",
            docTypeOrUri = "SEED_RX",
            remarks = "Tab. Aspirin 75 + Clopidogrel 75 OD; Atorvastatin 40 HS.",
            addedBy = "DOC-2001"
        ),
        DocumentEntity(
            id = "d2",
            patientId = "PT-5001",
            title = "CBC + Lipid profile",
            category = "Lab Panel",
            date = "2026-09-22",
            docTypeOrUri = "SEED_LAB",
            remarks = "Anaemia with leucocytosis; LDL elevated.",
            addedBy = "TEC-3501"
        ),
        DocumentEntity(
            id = "d3",
            patientId = "PT-5001",
            title = "Chest X-ray PA view",
            category = "X-Ray",
            date = "2026-09-21",
            docTypeOrUri = "SEED_XRAY",
            remarks = "Mild basal congestion, review after diuretics.",
            addedBy = "TEC-3501"
        ),
        DocumentEntity(
            id = "d4",
            patientId = "PT-5002",
            title = "Rx — Antihypertensives",
            category = "Prescription",
            date = "2026-09-22",
            docTypeOrUri = "SEED_RX",
            remarks = "Tab. Telmisartan 40 OD; amlodipine 5 HS.",
            addedBy = "DOC-2001"
        ),
        DocumentEntity(
            id = "d5",
            patientId = "PT-5003",
            title = "Chest X-ray — follow up",
            category = "X-Ray",
            date = "2026-09-20",
            docTypeOrUri = "SEED_XRAY",
            remarks = "Improving congestion.",
            addedBy = "TEC-3501"
        ),
        DocumentEntity(
            id = "d6",
            patientId = "PT-5004",
            title = "MRI Brain — Axial T2",
            category = "MRI / CT Scan",
            date = "2026-09-18",
            docTypeOrUri = "SEED_MRI",
            remarks = "Acute infarct left MCA territory, ~38mm.",
            addedBy = "DOC-2002"
        ),
        DocumentEntity(
            id = "d7",
            patientId = "PT-5005",
            title = "Rx — Anti-vertigo regimen",
            category = "Prescription",
            date = "2026-09-21",
            docTypeOrUri = "SEED_RX",
            remarks = "Tab. Betahistine 16 TDS; fall precautions.",
            addedBy = "DOC-2002"
        ),
        DocumentEntity(
            id = "d8",
            patientId = "PT-5006",
            title = "Discharge planning summary (draft)",
            category = "Discharge Summary",
            date = "2026-09-25",
            docTypeOrUri = "SEED_LAB",
            remarks = "Expected discharge 27 Sep if afebrile.",
            addedBy = "DOC-2003"
        ),
        DocumentEntity(
            id = "d9",
            patientId = "PT-5008",
            title = "CBC — pediatric panel",
            category = "Lab Panel",
            date = "2026-09-24",
            docTypeOrUri = "SEED_LAB",
            remarks = "Viral picture, no leucocytosis.",
            addedBy = "TEC-3501"
        )
    )

    val clinicalNotes = listOf(
        ClinicalNoteEntity(
            id = "n1",
            patientId = "PT-5001",
            text = "Patient chest-pain free since 48h. Continue monitoring BP q4h. ECG repeat on day 5.",
            authorId = "DOC-2001",
            date = "2026-09-23"
        ),
        ClinicalNoteEntity(
            id = "n2",
            patientId = "PT-5002",
            text = "BP 178/104 on admission, reduced to 150/92 after oral therapy.",
            authorId = "DOC-2001",
            date = "2026-09-23"
        ),
        ClinicalNoteEntity(
            id = "n3",
            patientId = "PT-5004",
            text = "Motor power improving right side (3/5 → 4/5). Continue antiplatelet + statin. Referred to Dr. Khan for AF evaluation.",
            authorId = "DOC-2002",
            date = "2026-09-21"
        ),
        ClinicalNoteEntity(
            id = "n4",
            patientId = "PT-5006",
            text = "Post-op day 2, wound clean, afebrile, on oral analgesics.",
            authorId = "DOC-2003",
            date = "2026-09-25"
        )
    )

    val carePlans = listOf(
        CarePlanEntity(
            id = "p1",
            patientId = "PT-5001",
            title = "Repeat ECG + Echo (Day 5)",
            due = today,
            status = "pending"
        ),
        CarePlanEntity(
            id = "p2",
            patientId = "PT-5001",
            title = "Medication — Inj. Pantoprazole 40mg IV OD",
            due = today,
            status = "done"
        ),
        CarePlanEntity(
            id = "p3",
            patientId = "PT-5002",
            title = "BP charting every 6 hours",
            due = today,
            status = "pending"
        ),
        CarePlanEntity(
            id = "p4",
            patientId = "PT-5003",
            title = "Daily weight chart + fluid balance",
            due = today,
            status = "pending"
        ),
        CarePlanEntity(
            id = "p5",
            patientId = "PT-5004",
            title = "Physiotherapy — daily 11 AM",
            due = today,
            status = "pending"
        ),
        CarePlanEntity(
            id = "p6",
            patientId = "PT-5004",
            title = "2D Echo for embolic source",
            due = today,
            status = "pending"
        ),
        CarePlanEntity(
            id = "p7",
            patientId = "PT-5005",
            title = "Fall-risk assessment daily",
            due = today,
            status = "done"
        ),
        CarePlanEntity(
            id = "p8",
            patientId = "PT-5006",
            title = "Dressing change — alternate days",
            due = today,
            status = "pending"
        ),
        CarePlanEntity(
            id = "p9",
            patientId = "PT-5007",
            title = "Pre-op checklist + NPO from 6 AM",
            due = today,
            status = "pending"
        ),
        CarePlanEntity(
            id = "p10",
            patientId = "PT-5008",
            title = "Temperature charting q4h",
            due = today,
            status = "pending"
        ),
        CarePlanEntity(
            id = "p11",
            patientId = "PT-5009",
            title = "Troponin-T repeat 12 hourly",
            due = today,
            status = "pending"
        )
    )

    val attendances = listOf(
        AttendanceEntity(
            id = 1,
            userId = "NUR-3001",
            date = today,
            time = "07:52",
            method = "Biometric"
        ),
        AttendanceEntity(
            id = 2,
            userId = "RCP-4001",
            date = today,
            time = "08:05",
            method = "Password"
        ),
        AttendanceEntity(
            id = 3,
            userId = "DOC-2001",
            date = today,
            time = "09:10",
            method = "Biometric"
        )
    )

    val notifications = listOf(
        NotificationEntity(
            id = "nt1",
            title = "Care plan due",
            body = "PT-5001 Ratan Dey — Repeat ECG + Echo scheduled for today (Day 5).",
            time = "08:30",
            audience = "DOC-2001",
            read = false,
            kind = "task"
        ),
        NotificationEntity(
            id = "nt2",
            title = "Medication schedule",
            body = "PT-5004 S. Mitra — Physiotherapy session at 11:00 AM.",
            time = "09:05",
            audience = "NUR-3001",
            read = false,
            kind = "task"
        ),
        NotificationEntity(
            id = "nt3",
            title = "Administrative announcement",
            body = "Staff meeting on Saturday 10 AM in the conference hall. Attendance mandatory.",
            time = "Yesterday",
            audience = "all",
            read = true,
            kind = "admin"
        ),
        NotificationEntity(
            id = "nt4",
            title = "Referral received",
            body = "Dr. Mehta referred PT-5004 (S. Mitra) to you for cardiac evaluation.",
            time = "19 Sep",
            audience = "DOC-2001",
            read = true,
            kind = "refer"
        )
    )
}
