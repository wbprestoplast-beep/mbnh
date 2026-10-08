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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    val userRole = UserRole.fromKey(currentUser?.role ?: "NURSE")
    val isBoss = userRole == UserRole.BOSS
    val isDoctor = userRole == UserRole.DOCTOR || userRole == UserRole.RMO || userRole == UserRole.MEDICAL_SUPER || userRole == UserRole.RMO_INCHARGE
    val isStaffAdmin = isBoss || userRole == UserRole.ADMINISTRATOR
    val isClinical = listOf(UserRole.NURSE, UserRole.TECHNICIAN, UserRole.INCHARGE).contains(userRole)

    val totalBeds = HospitalConstants.WARDS.sumOf { it.beds.size }
    val occupiedBeds = patients.size
    val staffOnDuty = attendance.filter { it.date == viewModel.todayDate }.size
    val myCheckIn = attendance.firstOrNull { it.userId == currentUser?.id && it.date == viewModel.todayDate }

    // Visible patients filter: doctors see only their assigned patients or referred patients
    val visiblePatients = if (isDoctor) {
        patients.filter { it.doctorId == currentUser?.id || it.referralDoctorId == currentUser?.id }
    } else {
        patients
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
                    UserAvatar(
                        name = currentUser?.name ?: "User",
                        photoUri = currentUser?.photoUri,
                        size = 54.dp
                    )
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
                    }
                }
            }
        }

        // Live Emergency & Multi-Phone Hospital Broadcast Ticker
        latestBroadcast?.let { bc ->
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
                                Text(
                                    text = bc.time,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                    value = "${visiblePatients.size}",
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
                    title = if (isStaffAdmin) "STAFF ON DUTY" else "MY ATTENDANCE",
                    value = if (isStaffAdmin) "$staffOnDuty" else (myCheckIn?.time ?: "Not marked"),
                    valueColor = if (isStaffAdmin || myCheckIn != null) MedGreen else MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_attendance_card"),
                    onClick = { viewModel.navigateTo(AppScreen.ATTENDANCE) }
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
                if (isBoss || isDoctor || isClinical) {
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
                if (isStaffAdmin) {
                    QuickActionButton(
                        icon = Icons.Default.People,
                        label = if (isBoss) "Admin Portal" else "Staff Directory",
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.pushScreen(AppScreen.ADMIN) }
                    )
                }
                if (!isStaffAdmin) {
                    QuickActionButton(
                        icon = Icons.Default.Fingerprint,
                        label = "Attendance",
                        color = MedGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.ATTENDANCE) }
                    )
                }
                QuickActionButton(
                    icon = Icons.Default.Campaign,
                    label = "Alerts",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.pushScreen(AppScreen.NOTIFICATIONS) }
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
                        patients.count { it.doctorId == d.id }
                    } ?: 1)

                    displayedDoctors.forEachIndexed { index, doc ->
                        val count = patients.count { it.doctorId == doc.id }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${doc.name} · ${doc.specialty}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(BrandCyan.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$count bed${if (count != 1) "s" else ""}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandCyan
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
