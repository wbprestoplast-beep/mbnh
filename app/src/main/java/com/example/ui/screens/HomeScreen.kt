package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppScreen
import com.example.model.HospitalConstants
import com.example.model.UserRole
import com.example.model.canViewAllStaff
import com.example.ui.components.UserAvatar
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.MedGreen
import com.example.ui.theme.MedWarning
import com.example.ui.viewmodel.HospitalViewModel

@Composable
fun HomeScreen(viewModel: HospitalViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val patients by viewModel.patients.collectAsState()
    val users by viewModel.users.collectAsState()
    val carePlans by viewModel.carePlans.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val latestBroadcast by viewModel.latestBroadcast.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val isPlayingAudio by viewModel.isPlayingAudio.collectAsState()
    val currentlyPlayingAudioId by viewModel.currentlyPlayingAudioId.collectAsState()

    var showProfilePhotoModal by remember { mutableStateOf(false) }

    val userRole = UserRole.fromKey(currentUser?.role ?: "NURSE")
    val isBoss = userRole == UserRole.BOSS
    val isAttendingDoctorOnly = userRole == UserRole.DOCTOR
    val isRMO = userRole == UserRole.RMO || userRole == UserRole.RMO_INCHARGE
    val isDoctor = isAttendingDoctorOnly || isRMO || userRole == UserRole.MEDICAL_SUPER
    val isStaffAdmin = isBoss || userRole == UserRole.ADMINISTRATOR
    val canViewStaffRoster = currentUser.canViewAllStaff()
    val isClinical = listOf(UserRole.NURSE, UserRole.TECHNICIAN, UserRole.INCHARGE).contains(userRole)

    val totalBeds = HospitalConstants.WARDS.sumOf { it.beds.size }
    val occupiedBeds = patients.count { !it.status.equals("DISCHARGED", ignoreCase = true) }
    val staffOnDuty = attendance.filter { it.date == viewModel.todayDate }.map { it.userId }.distinct().size
    val myCheckIn = attendance.firstOrNull { it.userId == currentUser?.id && it.date == viewModel.todayDate }

    // Visible patients filter: attending specialist doctors see assigned/referred; RMOs and staff see ALL active patients
    val visiblePatients = if (isAttendingDoctorOnly) {
        patients.filter { !it.status.equals("DISCHARGED", ignoreCase = true) && (it.doctorId == currentUser?.id || it.referralDoctorId == currentUser?.id) }
    } else {
        patients.filter { !it.status.equals("DISCHARGED", ignoreCase = true) }
    }

    val pendingTasks = visiblePatients.flatMap { p ->
        carePlans.filter { it.patientId == p.id && it.status == "pending" }
            .map { plan -> Pair(plan, p) }
    }.take(5)

    val doctors = users.filter { it.role.equals("DOCTOR", ignoreCase = true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // User Profile Header Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { showProfilePhotoModal = true }
                    ) {
                        UserAvatar(
                            name = currentUser?.name ?: "User",
                            photoUri = currentUser?.photoUri,
                            size = 54.dp
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(BrandTeal)
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Take Selfie or Change Photo",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.name ?: "Hospital Staff",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (isBoss) MedWarning.copy(alpha = 0.15f)
                                        else BrandTeal.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isBoss) "👑 BOSS · Full Access" else userRole.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBoss) MedWarning else BrandTeal
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = currentUser?.id ?: "",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.OutlinedButton(
                            onClick = { showProfilePhotoModal = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("home_take_selfie_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(13.dp), tint = BrandTeal)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("🤳 Take Selfie / Upload Photo", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = BrandTeal)
                        }
                    }
                }
            }
        }

        // Live Emergency & Multi-Phone Hospital Broadcast Ticker
        latestBroadcast?.let { bc ->
            val docPatientNames = patients.filter { it.doctorId == currentUser?.id || it.referralDoctorId == currentUser?.id }
                .map { it.name.lowercase().trim() }
            val docPatientBeds = patients.filter { it.doctorId == currentUser?.id || it.referralDoctorId == currentUser?.id }
                .map { it.bed.lowercase().trim() }
                .filter { it.isNotBlank() }

            val shouldShow = if (isAttendingDoctorOnly) {
                val targetedToMe = bc.audience == currentUser?.id
                val titleLower = bc.title.lowercase()
                val bodyLower = bc.body.lowercase()
                val matchesPatient = docPatientNames.any { pName ->
                    pName.isNotBlank() && (titleLower.contains(pName) || bodyLower.contains(pName))
                }
                val matchesBed = docPatientBeds.any { bed ->
                    titleLower.contains(bed) || bodyLower.contains(bed)
                }
                targetedToMe || matchesPatient || matchesBed
            } else {
                bc.audience == "all" || bc.audience == currentUser?.id || bc.audience.equals(currentUser?.role, ignoreCase = true) || currentUser?.role?.contains("RMO", ignoreCase = true) == true
            }

            if (shouldShow) {
                item {
                val isEmergency = bc.priority.equals("critical", ignoreCase = true)
                val isAppUpdate = bc.priority.equals("app_update", ignoreCase = true)
                val bannerColor = if (isEmergency) Color(0xFFDC2626)
                    else if (isAppUpdate) Color(0xFF0284C7)
                    else BrandTeal

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = bannerColor.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.pushScreen(AppScreen.NOTIFICATIONS) }
                        .testTag("home_live_broadcast_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bannerColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isEmergency) Icons.Default.Warning else if (isAppUpdate) Icons.Default.CloudSync else Icons.Default.Campaign,
                                contentDescription = null,
                                tint = bannerColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isEmergency) "🚨 CRITICAL EMERGENCY ALERT" else if (isAppUpdate) "🚀 APP & SYSTEM UPDATE" else "📢 HOSPITAL BROADCAST",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = bannerColor
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = bc.time,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { viewModel.dismissBroadcast(bc.id) },
                                        modifier = Modifier
                                            .size(24.dp)
                                            .testTag("home_dismiss_broadcast_${bc.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss Alert",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = bc.title,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = "${bc.body} · by ${bc.senderName}",
                                fontSize = 11.5.sp,
                                maxLines = 2,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            if (bc.voiceNoteBase64 != null) {
                                val isPlayingThis = isPlayingAudio && currentlyPlayingAudioId == bc.id
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        if (isPlayingThis) {
                                            viewModel.stopVoiceMessage()
                                        } else {
                                            viewModel.playVoiceMessage(bc.voiceNoteBase64, bc.id)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .testTag("listen_broadcast_voice_button")
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingThis) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = bannerColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isPlayingThis) "Stop Voice" else "▶ Hear Voice Message (${if (bc.voiceDurationSec > 0) "${bc.voiceDurationSec}s" else "Audio"})",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = bannerColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // Section: Overview
        item {
            Text(
                text = "OVERVIEW",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.6.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "BEDS OCCUPIED",
                    value = "$occupiedBeds/$totalBeds",
                    valueColor = BrandTeal,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_beds_card"),
                    onClick = { viewModel.navigateTo(AppScreen.BEDS) }
                )
                StatCard(
                    title = if (isDoctor) "MY PATIENTS" else "PATIENTS",
                    value = "${visiblePatients.count { !it.status.equals("DISCHARGED", ignoreCase = true) }}",
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_patients_card"),
                    onClick = { viewModel.navigateTo(AppScreen.PATIENTS) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "OPEN TASKS",
                    value = "${pendingTasks.size}",
                    valueColor = MedWarning,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_tasks_card"),
                    onClick = { viewModel.navigateTo(AppScreen.PLANNINGS) }
                )
                StatCard(
                    title = "BROADCASTS & UPDATES",
                    value = "${notifications.size}",
                    valueColor = BrandTeal,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_notifications_card"),
                    onClick = { viewModel.navigateTo(AppScreen.NOTIFICATIONS) }
                )
            }
        }

        // Section: Quick Actions
        item {
            Text(
                text = "QUICK ACTIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.6.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.DocumentScanner,
                    label = "Scan Report",
                    color = BrandTeal,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.openScanner() }
                )
                val canAdmit = isBoss || isDoctor || isClinical || userRole == UserRole.RECEPTIONIST
                if (canAdmit) {
                    QuickActionButton(
                        icon = Icons.Default.PersonAdd,
                        label = "Admit Patient",
                        color = MedGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.pushScreen(AppScreen.ADMIT_PATIENT) }
                    )
                }
                if (isDoctor) {
                    QuickActionButton(
                        icon = Icons.Default.SwapHoriz,
                        label = "Referrals",
                        color = MedWarning,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.PATIENTS) }
                    )
                }
                if (canViewStaffRoster) {
                    QuickActionButton(
                        icon = Icons.Default.People,
                        label = if (isBoss) "Admin Portal" else if (isStaffAdmin) "Staff Admin" else "Staff & Doctors",
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.pushScreen(AppScreen.ADMIN) }
                    )
                }
                QuickActionButton(
                    icon = Icons.Default.Campaign,
                    label = "Broadcasts",
                    color = BrandTeal,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.NOTIFICATIONS) }
                )
            }
        }

        // Section: Beds Occupied by Doctor
        item {
            Text(
                text = if (isDoctor) "BEDS UNDER YOU" else "BEDS OCCUPIED — BY DOCTOR",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.6.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val displayedDoctors = if (isDoctor) {
                        doctors.filter { it.id == currentUser?.id }
                    } else {
                        doctors
                    }
                    val maxCount = maxOf(1, displayedDoctors.maxOfOrNull { d ->
                        patients.count { it.doctorId == d.id && !it.status.equals("DISCHARGED", ignoreCase = true) }
                    } ?: 1)

                    displayedDoctors.forEachIndexed { index, doc ->
                        val count = patients.count { it.doctorId == doc.id && !it.status.equals("DISCHARGED", ignoreCase = true) }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${doc.name} · ${doc.specialty}",
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp, bottom = 4.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(BrandCyan.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "$count bed${if (count != 1) "s" else ""}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandCyan,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { count.toFloat() / maxCount.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = BrandCyan,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                        if (index < displayedDoctors.size - 1) {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }
            }
        }

        // Section: Upcoming Care Tasks
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UPCOMING CARE TASKS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.6.sp
                )
                Text(
                    text = "View All",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTeal,
                    modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.PLANNINGS) }
                )
            }
        }

        if (pendingTasks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No pending care tasks 🎉",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        } else {
            items(pendingTasks) { (task, patient) ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_item_${task.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${patient.name} · Bed ${patient.bed} · Due ${task.due}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.toggleCarePlan(task.id, task.status) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Done", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showProfilePhotoModal && currentUser != null) {
        com.example.ui.components.ProfilePictureModal(
            targetUserId = currentUser!!.id,
            userName = currentUser!!.name,
            currentPhotoUri = currentUser!!.photoUri,
            viewModel = viewModel,
            onDismiss = { showProfilePhotoModal = false }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.4.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
