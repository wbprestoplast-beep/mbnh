package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.UserRole
import com.example.ui.components.BiometricDialog
import com.example.ui.components.UserAvatar
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.MedGreen
import com.example.ui.viewmodel.HospitalViewModel

@Composable
fun AttendanceScreen(viewModel: HospitalViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val attendanceList by viewModel.attendance.collectAsState()
    val users by viewModel.users.collectAsState()

    val userRole = UserRole.fromKey(currentUser?.role ?: "NURSE")
    val isBoss = userRole == UserRole.BOSS
    val isDoctor = userRole == UserRole.DOCTOR || userRole == UserRole.RMO || userRole == UserRole.MEDICAL_SUPER || userRole == UserRole.RMO_INCHARGE

    val todayRecords = attendanceList.filter { it.date == viewModel.todayDate }
    val myCheckIn = todayRecords.firstOrNull { it.userId == currentUser?.id }

    var showBiometricModal by remember { mutableStateOf(false) }
    var showPasswordModal by remember { mutableStateOf(false) }
    var inputPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    val displayRecords = if (isBoss) todayRecords else todayRecords.filter { it.userId == currentUser?.id }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Today Status Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "My Attendance — Today",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (myCheckIn != null) {
                                    "✓ Checked in at ${myCheckIn.time} via ${myCheckIn.method}"
                                } else {
                                    "Not checked in yet today"
                                },
                                fontSize = 13.sp,
                                color = if (myCheckIn != null) MedGreen else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (myCheckIn != null) MedGreen.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (myCheckIn != null) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (myCheckIn != null) MedGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (myCheckIn == null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showBiometricModal = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(46.dp)
                                    .testTag("attendance_biometric_button")
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Biometric Check-in", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showPasswordModal = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Text("Via Password", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        if (isDoctor) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔒 Privacy: Doctors can only view and log their own attendance. Hospital-wide attendance roster is managed by the Administration.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Section: On Duty Today
        item {
            Text(
                text = if (isBoss) "ALL STAFF ON DUTY TODAY (${displayRecords.size})" else "MY ATTENDANCE RECORD",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.6.sp
            )
        }

        if (displayRecords.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No attendance marked yet today.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(displayRecords) { record ->
                val user = users.firstOrNull { it.id == record.userId }
                val uRole = UserRole.fromKey(user?.role ?: "NURSE")

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(name = user?.name ?: record.userId, size = 42.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user?.name ?: record.userId,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${uRole.label} · ${user?.id ?: record.userId}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = record.time,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MedGreen
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = record.method,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }

    // Biometric Modal Dialog
    if (showBiometricModal) {
        BiometricDialog(
            userName = currentUser?.name ?: "Staff",
            onSuccess = {
                viewModel.markAttendance("Biometric")
                showBiometricModal = false
            },
            onDismiss = { showBiometricModal = false }
        )
    }

    // Password Check-in Dialog
    if (showPasswordModal) {
        Dialog(onDismissRequest = { showPasswordModal = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Password Check-in",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showPasswordModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Text(
                        text = "Enter your staff password to confirm attendance",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                    )

                    OutlinedTextField(
                        value = inputPassword,
                        onValueChange = {
                            inputPassword = it
                            passwordError = false
                        },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = passwordError,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (passwordError) {
                        Text(
                            text = "❌ Incorrect password",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (inputPassword == currentUser?.pass) {
                                viewModel.markAttendance("Password")
                                showPasswordModal = false
                                inputPassword = ""
                            } else {
                                passwordError = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Verify & Check In", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
