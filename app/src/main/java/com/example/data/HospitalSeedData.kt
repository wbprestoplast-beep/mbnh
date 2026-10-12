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
            name = "BOSS",
            role = "BOSS",
            dept = "Administration",
            specialty = "Owner · Full Access",
            phone = "+91 98300 11111",
            joinedDate = "2015-04-01"
        ),
        UserEntity(
            id = "INC-4101",
            pass = "12345",
            name = "Ayendrila Roy",
            role = "INCHARGE",
            dept = "Ward Management",
            specialty = "Nursing Incharge",
            phone = "+91 98305 77771",
            joinedDate = "2019-02-14"
        ),
        UserEntity(
            id = "MED-4301",
            pass = "12345",
            name = "Susanta Ghosh",
            role = "MEDICINE",
            dept = "Pharmacy",
            specialty = "Pharmacist",
            phone = "+91 98307 99991",
            joinedDate = "2022-04-18"
        )
    )

    val patients = listOf(
        PatientEntity(
            id = "PT-5001",
            name = "Ratan Dey",
            age = 58,
            gender = "M",
            ward = "ICU",
            bed = "101",
            doctorId = "BOSS-0001",
            admittedOn = "2026-09-20",
            condition = "Acute coronary syndrome — post angioplasty",
            phone = "+91 90000 11111"
        ),
        PatientEntity(
            id = "PT-5002",
            name = "Mamata Khatun",
            age = 44,
            gender = "F",
            ward = "Female Ward",
            bed = "214",
            doctorId = "BOSS-0001",
            admittedOn = "2026-09-22",
            condition = "Hypertensive urgency — observation",
            phone = "+91 90000 22222"
        ),
        PatientEntity(
            id = "PT-5003",
            name = "Bikash Shaw",
            age = 61,
            gender = "M",
            ward = "Male Ward",
            bed = "201",
            doctorId = "BOSS-0001",
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
            bed = "102",
            doctorId = "BOSS-0001",
            admittedOn = "2026-09-17",
            condition = "Acute ischemic stroke (MCA territory)",
            phone = "+91 90000 44444",
            referralDoctorId = "BOSS-0001",
            referralReason = "Cardiac evaluation — suspected cardio-embolic source, AF on ECG.",
            referralBy = "BOSS-0001",
            referralDate = "2026-09-19"
        ),
        PatientEntity(
            id = "PT-5005",
            name = "Lakshmi Devi",
            age = 72,
            gender = "F",
            ward = "Female Ward",
            bed = "215",
            doctorId = "BOSS-0001",
            admittedOn = "2026-09-21",
            condition = "Vertebrobasilar insufficiency",
            phone = "+91 90000 55555"
        ),
        PatientEntity(
            id = "PT-5006",
            name = "Imran Ali",
            age = 35,
            gender = "M",
            ward = "Male Ward",
            bed = "202",
            doctorId = "BOSS-0001",
            admittedOn = "2026-09-23",
            condition = "Acute appendicitis — post appendectomy",
            phone = "+91 90000 66666"
        ),
        PatientEntity(
            id = "PT-5007",
            name = "Shikha Ghosh",
            age = 29,
            gender = "F",
            ward = "Female Ward",
            bed = "216",
            doctorId = "BOSS-0001",
            admittedOn = "2026-09-24",
            condition = "Cholelithiasis — scheduled lap chole",
            phone = "+91 90000 77777"
        ),
        PatientEntity(
            id = "PT-5008",
            name = "A. Mandal",
            age = 8,
            gender = "M",
            ward = "3rd Floor",
            bed = "301",
            doctorId = "BOSS-0001",
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
            bed = "103",
            doctorId = "BOSS-0001",
            admittedOn = "2026-09-15",
            condition = "NSTEMI — conservative management",
            phone = "+91 90000 99999"
        ),
        PatientEntity(
            id = "PT-5010",
            name = "Rupa Sen",
            age = 52,
            gender = "F",
            ward = "3rd Floor",
            bed = "302A",
            doctorId = "BOSS-0001",
            admittedOn = "2026-09-19",
            condition = "Herniorrhaphy — recovery",
            phone = "+91 90000 12121",
            referralDoctorId = "BOSS-0001",
            referralReason = "New onset confusion post-anesthesia — neurological opinion requested.",
            referralBy = "BOSS-0001",
            referralDate = "2026-09-24"
        ),
        PatientEntity(
            id = "PT-5011",
            name = "Sunita Ghosh",
            age = 48,
            gender = "F",
            ward = "Female Ward",
            bed = "217",
            doctorId = "BOSS-0001",
            admittedOn = "2026-09-10",
            condition = "Acute Gastroenteritis — resolved",
            phone = "+91 90000 13131",
            status = "DISCHARGED",
            dischargedOn = "2026-10-05 11:30",
            dischargeSummary = "Patient rehydrated and asymptomatic. Discharged with oral probiotics & dietary advice."
        ),
        PatientEntity(
            id = "PT-5012",
            name = "Anil Kapoor",
            age = 63,
            gender = "M",
            ward = "Male Ward",
            bed = "205",
            doctorId = "BOSS-0001",
            admittedOn = "2026-09-12",
            condition = "Left inguinal hernia repair",
            phone = "+91 90000 14141",
            status = "DISCHARGED",
            dischargedOn = "2026-10-06 16:00",
            dischargeSummary = "Wound healing clean. Sutures intact. Discharged with analgesic cover and follow-up in 7 days."
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
            addedBy = "BOSS-0001"
        ),
        DocumentEntity(
            id = "d2",
            patientId = "PT-5001",
            title = "CBC + Lipid profile",
            category = "Lab Panel",
            date = "2026-09-22",
            docTypeOrUri = "SEED_LAB",
            remarks = "Anaemia with leucocytosis; LDL elevated.",
            addedBy = "BOSS-0001"
        ),
        DocumentEntity(
            id = "d3",
            patientId = "PT-5001",
            title = "Chest X-ray PA view",
            category = "X-Ray",
            date = "2026-09-21",
            docTypeOrUri = "SEED_XRAY",
            remarks = "Mild basal congestion, review after diuretics.",
            addedBy = "BOSS-0001"
        ),
        DocumentEntity(
            id = "d4",
            patientId = "PT-5002",
            title = "Rx — Antihypertensives",
            category = "Prescription",
            date = "2026-09-22",
            docTypeOrUri = "SEED_RX",
            remarks = "Tab. Telmisartan 40 OD; amlodipine 5 HS.",
            addedBy = "BOSS-0001"
        ),
        DocumentEntity(
            id = "d5",
            patientId = "PT-5003",
            title = "Chest X-ray — follow up",
            category = "X-Ray",
            date = "2026-09-20",
            docTypeOrUri = "SEED_XRAY",
            remarks = "Improving congestion.",
            addedBy = "BOSS-0001"
        ),
        DocumentEntity(
            id = "d6",
            patientId = "PT-5004",
            title = "MRI Brain — Axial T2",
            category = "MRI / CT Scan",
            date = "2026-09-18",
            docTypeOrUri = "SEED_MRI",
            remarks = "Acute infarct left MCA territory, ~38mm.",
            addedBy = "BOSS-0001"
        ),
        DocumentEntity(
            id = "d7",
            patientId = "PT-5005",
            title = "Rx — Anti-vertigo regimen",
            category = "Prescription",
            date = "2026-09-21",
            docTypeOrUri = "SEED_RX",
            remarks = "Tab. Betahistine 16 TDS; fall precautions.",
            addedBy = "BOSS-0001"
        ),
        DocumentEntity(
            id = "d8",
            patientId = "PT-5006",
            title = "Discharge planning summary (draft)",
            category = "Discharge Summary",
            date = "2026-09-25",
            docTypeOrUri = "SEED_LAB",
            remarks = "Expected discharge 27 Sep if afebrile.",
            addedBy = "BOSS-0001"
        ),
        DocumentEntity(
            id = "d9",
            patientId = "PT-5008",
            title = "CBC — pediatric panel",
            category = "Lab Panel",
            date = "2026-09-24",
            docTypeOrUri = "SEED_LAB",
            remarks = "Viral picture, no leucocytosis.",
            addedBy = "BOSS-0001"
        )
    )

    val clinicalNotes = listOf(
        ClinicalNoteEntity(
            id = "n1",
            patientId = "PT-5001",
            text = "Patient chest-pain free since 48h. Continue monitoring BP q4h. ECG repeat on day 5.",
            authorId = "BOSS-0001",
            date = "2026-09-23"
        ),
        ClinicalNoteEntity(
            id = "n2",
            patientId = "PT-5002",
            text = "BP 178/104 on admission, reduced to 150/92 after oral therapy.",
            authorId = "BOSS-0001",
            date = "2026-09-23"
        ),
        ClinicalNoteEntity(
            id = "n3",
            patientId = "PT-5004",
            text = "Motor power improving right side (3/5 → 4/5). Continue antiplatelet + statin. Referred to BOSS for AF evaluation.",
            authorId = "BOSS-0001",
            date = "2026-09-21"
        ),
        ClinicalNoteEntity(
            id = "n4",
            patientId = "PT-5006",
            text = "Post-op day 2, wound clean, afebrile, on oral analgesics.",
            authorId = "BOSS-0001",
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
            userId = "BOSS-0001",
            date = today,
            time = "07:52",
            method = "Biometric"
        ),
        AttendanceEntity(
            id = 2,
            userId = "INC-4101",
            date = today,
            time = "08:05",
            method = "Password"
        ),
        AttendanceEntity(
            id = 3,
            userId = "MED-4301",
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
            audience = "BOSS-0001",
            read = false,
            kind = "task"
        ),
        NotificationEntity(
            id = "nt2",
            title = "Medication schedule",
            body = "PT-5004 S. Mitra — Physiotherapy session at 11:00 AM.",
            time = "09:05",
            audience = "BOSS-0001",
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
            body = "Referred PT-5004 (S. Mitra) to BOSS for cardiac evaluation.",
            time = "19 Sep",
            audience = "BOSS-0001",
            read = true,
            kind = "refer"
        )
    )
}
