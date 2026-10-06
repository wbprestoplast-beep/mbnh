# MB Nursing Home Pvt. Ltd. — Hospital Management System (HMS)

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Cloud Firestore](https://img.shields.io/badge/Database-Cloud%20Firestore%20%2B%20Room-FFCA28.svg?style=flat&logo=firebase)](https://firebase.google.com/)
[![Firebase Cloud Messaging](https://img.shields.io/badge/Push-FCM%20%2B%20Service%20Worker-FF6F00.svg?style=flat&logo=firebase)](https://firebase.google.com/docs/cloud-messaging)

A comprehensive, production-grade **Hospital Management System (HMS)** designed for **MB Nursing Home Pvt. Ltd.** (12/3 M.G. Road, Kolkata). Built with modern Android native architecture (Kotlin + Jetpack Compose Material 3), offline-first Room database, real-time Cloud Firestore synchronization, and instant multi-phone heads-up push alerting. Includes a companion Progressive Web App (PWA) with a background Service Worker and HTTPS hosting.

---

## 🌟 Key Features

### 1. Multi-Phone Real-Time Sync & Instant Alert Dispatch
- **Instant Cloud Database Sync**: Changes made to patients, beds, care tasks, or referrals are automatically synchronized across all connected devices and web clients via Cloud Firestore.
- **Dual Notification Channels**:
  - `hospital_critical_alerts`: Urgent heads-up alarms with high-intensity vibration patterns for Code Blue, vital drops, and emergency ICU alerts.
  - `hospital_alerts`: Routine clinical notifications, doctor referrals, and care task reminders.
- **Instant Multi-Phone Dispatch Center**:
  - Authorized staff (Boss, Doctors, Administrators, Incharge) can broadcast alerts to all phones simultaneously.
  - One-tap preset templates:
    - 🚨 *Code Blue — Emergency Resuscitation Team to ICU Bed 201*
    - 🚀 *App & System Update v2.5 — Real-time multi-phone cloud sync active*
    - ⚡ *Critical Vitals Drop — Urgent doctor review requested*
    - 🛏 *Emergency Bed Prepared — Bed sanitized for immediate trauma admission*
    - 📢 *Shift Handover Meeting — Staff handover commencing*
  - Priority levels: `CRITICAL`, `APP_UPDATE`, `HIGH`, `NORMAL`.
  - Target audience filtering: *All Phones & Web*, *Specialist Doctors*, *Nursing & ICU*.

### 2. Patient Admissions & Ward Bed Management
- **Admissions**: Quick admission workflow with validation, condition notes, contact details, and assigned doctor.
- **Ward Management**: Live bed occupancy visualization across **General Ward (8 beds)**, **ICU (6 beds)**, and **Private Cabins (4 beds)**.
- **Doctor Referrals**: Internal inter-departmental patient referrals with instant doctor push notification alerts.

### 3. Medical Document Scanner & Image Enhancer
- **Camera & Photo Picker**: Built-in document capture and visual media picker.
- **Enhancement Filters**: High-contrast black & white, grayscale doc enhancement, and edge-boost presets.
- **Categorization**: Prescriptions, Lab Panels, X-Rays, MRI/CT Scans, Discharge Summaries, and Clinical Reports.

### 4. Care Plans & Clinical Checklists
- Patient-specific daily care plans and clinical milestones.
- Real-time check-off synced with both the local SQLite ledger and Cloud Firestore.

### 5. Staff Administration & Biometric Check-In
- **11 Role Personas**: Boss (Full Admin), Doctor, Nurse, Technician, Administrator, Receptionist, Accountant, Incharge, Cashier, Pharmacy/Medicine, and Maintenance.
- **Attendance**: Biometric fingerprint and facial check-in simulation with instant arrival notifications sent to administration.

### 6. Web Portal with Service Worker & HTTPS Hosting
- **Location**: `/web` directory (`index.html`, `firebase-messaging-sw.js`, `manifest.json`, `icon.svg`).
- **Background Push Service Worker**: Receives push notifications even when the web application or browser tab is closed.
- **Live Firestore Mirror**: Shared bed status, real-time alerts feed, and push broadcast dispatcher.
- **Firebase Hosting Configuration**: `firebase.json` pre-configured with HTTPS caching headers and `Service-Worker-Allowed: /`.

---

## 🛠 Tech Stack & Architecture

- **Architecture**: Clean MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF).
- **UI Framework**: 100% Jetpack Compose with Material Design 3 (M3) theming.
- **Local Persistence**: Android Room Database (`AppDatabase`, `HospitalDao`, SQLite).
- **Cloud Backend**: Google Cloud Firestore (`hospital_patients`, `hospital_notifications`, `hospital_broadcasts`, `hospital_care_plans`, `hospital_attendance`).
- **Push Messaging**: Firebase Cloud Messaging (FCM) + Android `NotificationManagerCompat`.
- **Web Client**: HTML5, Vanilla JavaScript, Firebase Web SDK v10 (Compat), Web Push API, Service Worker.
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`) with Version Catalog (`libs.versions.toml`).

---

## 🚀 How to Run & Build

### Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK 35 (compileSdk 35, minSdk 24)
- JDK 17 or JDK 21

### Building with Gradle
```bash
# Build Debug APK
gradle :app:assembleDebug

# Run Unit Tests
gradle :app:testDebugUnitTest
```

### Output APK
The compiled debug APK is located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🌐 Deploying Web Portal to Firebase Hosting

To deploy the companion web portal with HTTPS hosting:
```bash
# Install Firebase CLI if not already installed
npm install -g firebase-tools

# Login to Firebase
firebase login

# Deploy hosting
firebase deploy --only hosting
```

---

## 📄 License
MB Nursing Home Pvt. Ltd. — All rights reserved.
