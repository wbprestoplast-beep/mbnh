package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PictureAsPdf
import com.example.util.PdfExportUtil
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import com.example.data.CarePlanEntity
import com.example.data.ClinicalNoteEntity
import com.example.data.DocumentEntity
import com.example.model.DocCategory
import com.example.model.UserRole
import com.example.model.canChangeAttendingDoctor
import com.example.model.canDischargeAndTransfer
import com.example.model.canDoctorAddClinicalItems
import com.example.model.canAddVitalsAndClinicalItems
import com.example.model.canEditPatientDetails
import com.example.ui.components.DocumentCanvasView
import com.example.ui.components.UserAvatar
import com.example.ui.components.VoiceOutlinedTextField
import com.example.ui.components.ZoomableFullScreenImageViewer
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.MedGreen
import com.example.ui.theme.MedWarning
import com.example.ui.viewmodel.HospitalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: String,
    viewModel: HospitalViewModel
) {
    val context = LocalContext.current
    val patients by viewModel.patients.collectAsState()
    val users by viewModel.users.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val clinicalNotes by viewModel.clinicalNotes.collectAsState()
    val carePlans by viewModel.carePlans.collectAsState()
    val vitalRecords by viewModel.vitalRecords.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val patient = patients.firstOrNull { it.id == patientId }
    if (patient == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Patient record not found.")
        }
        return
    }

    val doctor = users.firstOrNull { it.id == patient.doctorId }
    val patientDocs = documents.filter { it.patientId == patient.id }
    val patientNotes = clinicalNotes.filter { it.patientId == patient.id }
    val patientPlans = carePlans.filter { it.patientId == patient.id }

    val userRole = UserRole.fromKey(currentUser?.role ?: "NURSE")
    val isDoctor = userRole == UserRole.DOCTOR || userRole == UserRole.RMO || userRole == UserRole.MEDICAL_SUPER || userRole == UserRole.RMO_INCHARGE
    val isRMO = userRole == UserRole.RMO || userRole == UserRole.RMO_INCHARGE || currentUser?.role?.contains("RMO", ignoreCase = true) == true
    val canEdit = currentUser.canEditPatientDetails()
    val canChangeDoctor = currentUser.canChangeAttendingDoctor()
    val canDischargeOrTransfer = currentUser.canDischargeAndTransfer()
    val canAddReportsAndPrescriptions = currentUser.canDoctorAddClinicalItems() || currentUser.canAddVitalsAndClinicalItems()
    val canRecordVitals = currentUser.canAddVitalsAndClinicalItems()

    var newPlanText by remember { mutableStateOf("") }
    var newNoteText by remember { mutableStateOf("") }
    var showReferModal by remember { mutableStateOf(false) }
    var showDischargeModal by remember { mutableStateOf(false) }
    var showTransferModal by remember { mutableStateOf(false) }
    var showChangeDoctorModal by remember { mutableStateOf(false) }
    var showVitalsModal by remember { mutableStateOf(false) }
    var showEditPatientModal by remember { mutableStateOf(false) }
    var dischargeSummaryInput by remember { mutableStateOf("") }
    var showAvatarZoom by remember { mutableStateOf(false) }

    // Modals for editing & deleting notes, care plans, documents
    var editingNote by remember { mutableStateOf<ClinicalNoteEntity?>(null) }
    var noteToDelete by remember { mutableStateOf<ClinicalNoteEntity?>(null) }
    var editingPlan by remember { mutableStateOf<CarePlanEntity?>(null) }
    var planToDelete by remember { mutableStateOf<CarePlanEntity?>(null) }
    var editingDoc by remember { mutableStateOf<DocumentEntity?>(null) }
    var docToDelete by remember { mutableStateOf<DocumentEntity?>(null) }

    val isDischarged = patient.status.equals("DISCHARGED", ignoreCase = true)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Patient Header Demographics Card
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.clickable { showAvatarZoom = true }) {
                            UserAvatar(name = patient.name, size = 56.dp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = patient.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (canEdit || isDoctor || isRMO || userRole == UserRole.BOSS || userRole == UserRole.ADMINISTRATOR) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { showEditPatientModal = true },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BrandTeal.copy(alpha = 0.12f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Patient Details",
                                            tint = BrandTeal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                if (isDischarged) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFDC2626).copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "🚪 DISCHARGED",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = patient.id,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (isDischarged) Color.Gray.copy(alpha = 0.15f) else BrandCyan.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isDischarged) "Left Hospital" else "${patient.ward} · Bed ${patient.bed}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDischarged) Color.Gray else BrandCyan
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )

                    DetailRow("Age / Sex", "${patient.age} years / ${patient.gender}")
                    DetailRow("Admitted on", patient.admittedOn)
                    if (isDischarged && !patient.dischargedOn.isNullOrBlank()) {
                        DetailRow("Discharged on", patient.dischargedOn)
                    }
                    if (!isRMO) {
                        DetailRow("Contact", patient.phone.ifBlank { "—" })
                    } else {
                        DetailRow("Contact", "🔒 Restricted (RMO Privacy Policy)")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Assigned Doctor",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${doctor?.name ?: "—"} (${doctor?.specialty ?: ""})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (canChangeDoctor) {
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { showChangeDoctorModal = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = BrandTeal)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reassign", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandTeal)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Clinical Condition / Diagnosis:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = patient.condition,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    if (isDischarged && !patient.dischargeSummary.isNullOrBlank()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFDC2626).copy(alpha = 0.08f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("🚪 Discharge Summary & Outcome", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                Text(patient.dischargeSummary, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }

                    // Active Referral Alert Box
                    if (!patient.referralDoctorId.isNullOrBlank()) {
                        val refDoc = users.firstOrNull { it.id == patient.referralDoctorId }
                        val refByDoc = users.firstOrNull { it.id == patient.referralBy }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MedWarning.copy(alpha = 0.08f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🔁 Active Referral to ${refDoc?.name ?: patient.referralDoctorId}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedWarning
                                )
                                Text(
                                    text = patient.referralReason ?: "",
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Text(
                                    text = "By ${refByDoc?.name ?: patient.referralBy} · ${patient.referralDate ?: ""}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // Action Buttons Row (Scan Report, Transfer Bed/Dept, Refer Doctor, Discharge / Re-admit Patient)
                    if (canEdit || canDischargeOrTransfer || canAddReportsAndPrescriptions || isDoctor || isRMO) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (canAddReportsAndPrescriptions) {
                                Button(
                                    onClick = { viewModel.openScanner(patient.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Scan", fontSize = 11.sp)
                                }
                            }

                            if (!isDischarged && canDischargeOrTransfer) {
                                Button(
                                    onClick = { showTransferModal = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1.1f)
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Transfer", fontSize = 11.sp)
                                }
                            }

                            if (isDoctor || isRMO) {
                                OutlinedButton(
                                    onClick = { showReferModal = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Refer", fontSize = 11.sp)
                                }
                            }

                            if (!isDischarged && canDischargeOrTransfer) {
                                Button(
                                    onClick = { showDischargeModal = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Discharge", fontSize = 11.sp)
                                }
                            } else if (isDischarged && canDischargeOrTransfer) {
                                Button(
                                    onClick = { viewModel.readmitPatient(patient.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MedGreen),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Re-Admit", fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔒", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Read-Only Access · Patient details & vitals editing is restricted to clinical and administrative staff.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Patient Vitals Section
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📊 CLINICAL VITALS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTeal,
                                letterSpacing = 0.6.sp
                            )
                        }
                        if (canRecordVitals) {
                            OutlinedButton(
                                onClick = { showVitalsModal = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = BrandTeal)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Update Vitals", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandTeal)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandCyan.copy(alpha = 0.08f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🌡️ Temp", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(patient.temperature ?: "98.6°F", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandTeal.copy(alpha = 0.08f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🫁 SPO2", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(patient.spo2 ?: "98%", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = BrandTeal, modifier = Modifier.padding(top = 2.dp))
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MedGreen.copy(alpha = 0.08f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("💓 Pulse", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(patient.pulse ?: "72 bpm", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MedGreen, modifier = Modifier.padding(top = 2.dp))
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🩺 BP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(patient.bloodPressure ?: "120/80", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFD97706).copy(alpha = 0.1f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🩸 CBG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(patient.cbg ?: "110 mg/dL", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD97706), modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }

                    if (!patient.vitalsUpdatedAt.isNullOrBlank()) {
                        Text(
                            text = "Latest recorded at ${patient.vitalsUpdatedAt} by ${patient.vitalsUpdatedBy ?: "Duty Staff"}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    val patientVitalsHistory = vitalRecords.filter { it.patientId == patient.id }
                    if (patientVitalsHistory.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "📜 SAVED VITALS HISTORY (${patientVitalsHistory.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            patientVitalsHistory.forEach { record ->
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${if (record.temperature.isNotBlank()) "Temp: ${record.temperature}  " else ""}${if (record.spo2.isNotBlank()) "SpO2: ${record.spo2}  " else ""}${if (record.pulse.isNotBlank()) "Pulse: ${record.pulse}  " else ""}${if (record.bloodPressure.isNotBlank()) "BP: ${record.bloodPressure}  " else ""}${if (record.cbg.isNotBlank()) "CBG: ${record.cbg}" else ""}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "by ${record.recordedBy} at ${record.recordedAt}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Documents, Reports, Prescriptions & Scans
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REPORTS, PRESCRIPTIONS & SCANS (${patientDocs.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.6.sp
                )
                if (canAddReportsAndPrescriptions) {
                    OutlinedButton(
                        onClick = { viewModel.openScanner(patient.id) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp), tint = BrandTeal)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add / Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandTeal)
                    }
                }
            }
        }

        if (patientDocs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No reports attached yet. Tap 'Scan Report' to capture prescriptions, lab panels, or scans.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(patientDocs) { doc ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .width(200.dp)
                                .clickable { viewModel.openDocumentViewer(doc) }
                        ) {
                            Column {
                                Box {
                                    DocumentCanvasView(
                                        docTypeOrUri = doc.docTypeOrUri,
                                        title = doc.title,
                                        category = doc.category,
                                        patientName = patient.name,
                                        remarks = doc.remarks,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                    )
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                            .padding(2.dp)
                                    ) {
                                        IconButton(
                                            onClick = { editingDoc = doc },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Report", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                        IconButton(
                                            onClick = { docToDelete = doc },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Report", tint = Color(0xFFFF6B6B), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }

                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = doc.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val cat = DocCategory.fromString(doc.category)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(Color(cat.badgeColorHex).copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = doc.category,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(cat.badgeColorHex)
                                            )
                                        }
                                        Text(
                                            text = doc.date,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "🔍 Tap to open / zoom",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandTeal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Care Plan & Instructions
        item {
            Text(
                text = "CARE PLAN & INSTRUCTIONS (${patientPlans.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.6.sp
            )
        }

        items(patientPlans) { plan ->
            val isDone = plan.status == "done"
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isDone) MedGreen.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable(enabled = canEdit) {
                                viewModel.toggleCarePlan(plan.id, plan.status)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDone) Icons.Default.Check else Icons.Default.Schedule,
                            contentDescription = if (isDone) "Completed" else "Pending",
                            tint = if (isDone) MedGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = plan.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = if (isDone) MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            ) else MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Due ${plan.due} · ${if (isDone) "Completed" else "Pending"}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    if (canEdit) {
                        Row {
                            IconButton(onClick = { editingPlan = plan }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Care Plan", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { planToDelete = plan }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Care Plan", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        if (canEdit || currentUser.canDoctorAddClinicalItems() || currentUser.canAddVitalsAndClinicalItems()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoiceOutlinedTextField(
                        value = newPlanText,
                        onValueChange = { newPlanText = it },
                        placeholder = { Text("Add care plan / advice / instruction…") },
                        speechPrompt = "Speak care plan instruction, doctor advice, or task...",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newPlanText.isNotBlank()) {
                                viewModel.addCarePlan(patient.id, newPlanText)
                                newPlanText = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            }
        }

        // Section: Clinical Notes
        item {
            Text(
                text = "CLINICAL NOTES (${patientNotes.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.6.sp
            )
        }

        items(patientNotes) { note ->
            val author = users.firstOrNull { it.id == note.authorId }
            val notePages = if (note.text.contains("|||PAGE_SEP|||")) {
                note.text.split("|||PAGE_SEP|||").filter { it.isNotBlank() }
            } else {
                listOf(note.text)
            }
            var activeNotePageIndex by remember(note.id) { mutableIntStateOf(0) }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (notePages.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📄 Multi-Page Note (${notePages.size} Pages)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTeal
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                notePages.indices.forEach { idx ->
                                    val isSelected = idx == activeNotePageIndex
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSelected) BrandTeal else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { activeNotePageIndex = idx }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Pg ${idx + 1}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = notePages.getOrElse(activeNotePageIndex) { notePages.firstOrNull() ?: note.text },
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (canEdit) {
                            Row {
                                IconButton(onClick = { editingNote = note }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Note", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { noteToDelete = note }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Note", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    Text(
                        text = "— ${author?.name ?: note.authorId} · ${note.date}${if (notePages.size > 1) " · Page ${activeNotePageIndex + 1} of ${notePages.size}" else ""}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        if (canEdit || currentUser.canDoctorAddClinicalItems() || currentUser.canAddVitalsAndClinicalItems()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ADD CLINICAL NOTE / DOCTOR ADVICE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    val curr = newNoteText.trim()
                                    newNoteText = if (curr.isBlank()) {
                                        "Page 1: "
                                    } else {
                                        val pageNum = curr.split("|||PAGE_SEP|||").size + 1
                                        "$curr\n|||PAGE_SEP|||Page $pageNum: "
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("+ Add Page", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        VoiceOutlinedTextField(
                            value = newNoteText,
                            onValueChange = { newNoteText = it },
                            placeholder = { Text("Clinical observations & notes (use '+ Add Page' for several pages under one heading)…") },
                            speechPrompt = "Speak clinical observation or progress notes...",
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (newNoteText.isNotBlank()) {
                                    viewModel.addClinicalNote(patient.id, newNoteText.trim())
                                    newNoteText = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save Note")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // Discharge Patient Dialog
    if (showDischargeModal) {
        Dialog(onDismissRequest = { showDischargeModal = false }) {
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
                        Text("Discharge ${patient.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        IconButton(onClick = { showDischargeModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "DISCHARGE SUMMARY & OUTCOME NOTES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    VoiceOutlinedTextField(
                        value = dischargeSummaryInput,
                        onValueChange = { dischargeSummaryInput = it },
                        placeholder = { Text("Clinical summary, medication on discharge, follow-up advice…") },
                        speechPrompt = "Speak discharge summary and instructions...",
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.dischargePatient(patient.id, dischargeSummaryInput)
                            showDischargeModal = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirm Discharge & Vacate Bed")
                    }
                }
            }
        }
    }

    // Refer Patient Dialog
    if (showReferModal) {
        val otherDoctors = users.filter { (it.role.equals("DOCTOR", ignoreCase = true) || it.role.equals("RMO", ignoreCase = true) || it.role.equals("RMO_INCHARGE", ignoreCase = true)) && it.id != currentUser?.id }
        var selectedDocId by remember { mutableStateOf(otherDoctors.firstOrNull()?.id ?: "") }
        var isDropdownExpanded by remember { mutableStateOf(false) }
        var referralReason by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showReferModal = false }) {
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
                            text = "Refer ${patient.name}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showReferModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "SELECT SPECIALIST DOCTOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded = isDropdownExpanded,
                        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                    ) {
                        val selDoc = otherDoctors.firstOrNull { it.id == selectedDocId }
                        OutlinedTextField(
                            value = selDoc?.let { "${it.name} (${it.specialty})" } ?: "Select doctor",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            otherDoctors.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text("${d.name} — ${d.specialty}") },
                                    onClick = {
                                        selectedDocId = d.id
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "REASON FOR REFERRAL / CLINICAL NOTES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    VoiceOutlinedTextField(
                        value = referralReason,
                        onValueChange = { referralReason = it },
                        placeholder = { Text("Clinical query, opinion sought, or management advice…") },
                        speechPrompt = "Speak clinical reason for referral and management advice...",
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (selectedDocId.isNotBlank() && referralReason.isNotBlank()) {
                                viewModel.referPatient(patient.id, selectedDocId, referralReason)
                                showReferModal = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        enabled = selectedDocId.isNotBlank() && referralReason.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirm Referral & Send Push Alert")
                    }
                }
            }
        }
    }

    // Reassign Attending Doctor Dialog (Admins, Incharges, Receptionists)
    if (showChangeDoctorModal) {
        val doctorsList = users.filter {
            it.role.equals("DOCTOR", ignoreCase = true) ||
            it.role.equals("RMO", ignoreCase = true) ||
            it.role.equals("MEDICAL_SUPER", ignoreCase = true) ||
            it.role.equals("RMO_INCHARGE", ignoreCase = true)
        }
        var selectedDocId by remember { mutableStateOf(patient.doctorId) }
        var reassignReason by remember { mutableStateOf("") }
        var isDocDropdownExpanded by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showChangeDoctorModal = false }) {
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
                            text = "Reassign Attending Doctor",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandTeal
                        )
                        IconButton(onClick = { showChangeDoctorModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("NEW ATTENDING DOCTOR / CONSULTANT *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded = isDocDropdownExpanded,
                        onExpandedChange = { isDocDropdownExpanded = !isDocDropdownExpanded }
                    ) {
                        val selDoc = doctorsList.firstOrNull { it.id == selectedDocId }
                        OutlinedTextField(
                            value = selDoc?.let { "${it.name} (${it.specialty})" } ?: "Select Doctor",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDocDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isDocDropdownExpanded,
                            onDismissRequest = { isDocDropdownExpanded = false }
                        ) {
                            doctorsList.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text("${d.name} — ${d.specialty}") },
                                    onClick = {
                                        selectedDocId = d.id
                                        isDocDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("REASON FOR DOCTOR REASSIGNMENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))

                    VoiceOutlinedTextField(
                        value = reassignReason,
                        onValueChange = { reassignReason = it },
                        placeholder = { Text("Clinical handoff, specialty consultation, or shift change…") },
                        speechPrompt = "Speak reason for doctor reassignment...",
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (selectedDocId.isNotBlank()) {
                                viewModel.changePatientDoctor(patient.id, selectedDocId, reassignReason.trim())
                                showChangeDoctorModal = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        enabled = selectedDocId.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirm Doctor Reassignment")
                    }
                }
            }
        }
    }

    // Transfer Patient Ward / Bed Dialog
    if (showTransferModal) {
        var selectedWard by remember { mutableStateOf(patient.ward) }
        var targetBedInput by remember { mutableStateOf(patient.bed) }
        var transferReasonInput by remember { mutableStateOf("") }
        var isWardDropdownExpanded by remember { mutableStateOf(false) }

        val wardOptions = com.example.model.HospitalConstants.WARDS.map { it.name } + listOf("Emergency", "ICU", "3rd Floor", "General Ward")

        Dialog(onDismissRequest = { showTransferModal = false }) {
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
                            text = "Transfer ${patient.name}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandCyan
                        )
                        IconButton(onClick = { showTransferModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("TARGET WARD / DEPARTMENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded = isWardDropdownExpanded,
                        onExpandedChange = { isWardDropdownExpanded = !isWardDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedWard,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isWardDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isWardDropdownExpanded,
                            onDismissRequest = { isWardDropdownExpanded = false }
                        ) {
                            wardOptions.distinct().forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w) },
                                    onClick = {
                                        selectedWard = w
                                        isWardDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("NEW BED NUMBER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))

                    VoiceOutlinedTextField(
                        value = targetBedInput,
                        onValueChange = { targetBedInput = it },
                        placeholder = { Text("e.g. 104, 202, 305A") },
                        speechPrompt = "Speak bed number...",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("REASON / TRANSFER NOTES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))

                    VoiceOutlinedTextField(
                        value = transferReasonInput,
                        onValueChange = { transferReasonInput = it },
                        placeholder = { Text("Reason for ward or bed transfer…") },
                        speechPrompt = "Speak reason for patient transfer...",
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (targetBedInput.isNotBlank()) {
                                viewModel.transferPatient(patient.id, selectedWard, targetBedInput.trim(), transferReasonInput.trim())
                                showTransferModal = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                        enabled = targetBedInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirm Transfer & Free Old Bed")
                    }
                }
            }
        }
    }

    // Update Patient Vitals Dialog
    if (showVitalsModal) {
        var tempInput by remember { mutableStateOf(patient.temperature?.replace(" °F", "")?.replace("°F", "") ?: "98.6") }
        var spo2Input by remember { mutableStateOf(patient.spo2?.replace("%", "") ?: "98") }
        var pulseInput by remember { mutableStateOf(patient.pulse?.replace(" bpm", "") ?: "72") }
        var bpInput by remember { mutableStateOf(patient.bloodPressure?.replace(" mmHg", "") ?: "120/80") }
        var cbgInput by remember { mutableStateOf(patient.cbg?.replace(" mg/dL", "")?.replace("mg/dL", "") ?: "110") }

        Dialog(onDismissRequest = { showVitalsModal = false }) {
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
                        Text("Update Vitals for ${patient.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandTeal)
                        IconButton(onClick = { showVitalsModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VoiceOutlinedTextField(
                            value = tempInput,
                            onValueChange = { tempInput = it },
                            label = { Text("Temp (°F)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        VoiceOutlinedTextField(
                            value = spo2Input,
                            onValueChange = { spo2Input = it },
                            label = { Text("SpO2 (%)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VoiceOutlinedTextField(
                            value = pulseInput,
                            onValueChange = { pulseInput = it },
                            label = { Text("Pulse (bpm)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        VoiceOutlinedTextField(
                            value = bpInput,
                            onValueChange = { bpInput = it },
                            label = { Text("BP (mmHg)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    VoiceOutlinedTextField(
                        value = cbgInput,
                        onValueChange = { cbgInput = it },
                        label = { Text("CBG (Blood Sugar mg/dL)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            viewModel.updatePatientVitals(
                                patientId = patient.id,
                                temp = tempInput.trim(),
                                spo2 = spo2Input.trim(),
                                pulse = pulseInput.trim(),
                                bp = bpInput.trim(),
                                cbg = cbgInput.trim()
                            )
                            showVitalsModal = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Vitals & Broadcast Update")
                    }
                }
            }
        }
    }

    // Edit Clinical Note Modal
    if (editingNote != null) {
        val targetNote = editingNote!!
        var noteText by remember { mutableStateOf(targetNote.text) }

        Dialog(onDismissRequest = { editingNote = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Edit Clinical Note", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    VoiceOutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        speechPrompt = "Speak updated clinical note...",
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (noteText.isNotBlank()) {
                                viewModel.updateClinicalNote(targetNote.copy(text = noteText.trim()))
                                editingNote = null
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Update Clinical Note")
                    }
                }
            }
        }
    }

    // Delete Clinical Note Confirm Dialog
    if (noteToDelete != null) {
        Dialog(onDismissRequest = { noteToDelete = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Delete Clinical Note?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Are you sure you want to delete this clinical note?", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { noteToDelete = null }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.deleteClinicalNote(noteToDelete!!.id)
                                noteToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) { Text("Delete") }
                    }
                }
            }
        }
    }

    // Edit Care Plan Modal
    if (editingPlan != null) {
        val targetPlan = editingPlan!!
        var planTitle by remember { mutableStateOf(targetPlan.title) }

        Dialog(onDismissRequest = { editingPlan = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Edit Care Plan Instruction", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    VoiceOutlinedTextField(
                        value = planTitle,
                        onValueChange = { planTitle = it },
                        speechPrompt = "Speak care plan instruction...",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (planTitle.isNotBlank()) {
                                viewModel.updateCarePlan(targetPlan.copy(title = planTitle.trim()))
                                editingPlan = null
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Update Care Plan")
                    }
                }
            }
        }
    }

    // Delete Care Plan Confirm Dialog
    if (planToDelete != null) {
        Dialog(onDismissRequest = { planToDelete = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Delete Care Plan Task?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Are you sure you want to delete '${planToDelete!!.title}'?", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { planToDelete = null }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.deleteCarePlan(planToDelete!!.id)
                                planToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) { Text("Delete") }
                    }
                }
            }
        }
    }

    // Edit Document Report Modal (from thumbnail edit icon)
    if (editingDoc != null) {
        val doc = editingDoc!!
        var editTitle by remember { mutableStateOf(doc.title) }
        var editRemarks by remember { mutableStateOf(doc.remarks) }

        Dialog(onDismissRequest = { editingDoc = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Edit Report Details", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    VoiceOutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    VoiceOutlinedTextField(
                        value = editRemarks,
                        onValueChange = { editRemarks = it },
                        label = { Text("Clinical Remarks") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.updateDocument(doc.copy(title = editTitle.ifBlank { doc.title }, remarks = editRemarks))
                            editingDoc = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }

    // Delete Document Confirm Dialog (from thumbnail delete icon)
    if (docToDelete != null) {
        Dialog(onDismissRequest = { docToDelete = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Delete Report?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Are you sure you want to delete '${docToDelete!!.title}'?", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { docToDelete = null }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.deleteDocument(docToDelete!!.id)
                                docToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) { Text("Delete") }
                    }
                }
            }
        }
    }

    // Full Screen Avatar Zoom Viewer
    if (showAvatarZoom) {
        ZoomableFullScreenImageViewer(
            title = patient.name,
            subtitle = "Patient Avatar · ${patient.id} (${patient.ward} Bed ${patient.bed})",
            onDismiss = { showAvatarZoom = false }
        ) {
            UserAvatar(name = patient.name, size = 280.dp)
        }
    }

    // Edit Patient Details Dialog
    if (showEditPatientModal) {
        EditPatientDetailsDialog(
            patient = patient,
            doctors = users.filter {
                it.role.equals("DOCTOR", ignoreCase = true) ||
                it.role.equals("RMO", ignoreCase = true) ||
                it.role.equals("MEDICAL_SUPER", ignoreCase = true) ||
                it.role.equals("RMO_INCHARGE", ignoreCase = true)
            },
            onDismiss = { showEditPatientModal = false },
            onConfirm = { name, age, gender, phone, ward, bed, condition, doctorId ->
                viewModel.updatePatientDetails(
                    patientId = patient.id,
                    newName = name,
                    newAge = age,
                    newGender = gender,
                    newPhone = phone,
                    newWard = ward,
                    newBed = bed,
                    newCondition = condition,
                    newDoctorId = doctorId
                )
                showEditPatientModal = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPatientDetailsDialog(
    patient: com.example.data.PatientEntity,
    doctors: List<com.example.data.UserEntity>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, age: Int, gender: String, phone: String, ward: String, bed: String, condition: String, doctorId: String) -> Unit
) {
    var nameInput by remember { mutableStateOf(patient.name) }
    var ageInput by remember { mutableStateOf(patient.age.toString()) }
    var genderInput by remember { mutableStateOf(patient.gender) }
    var phoneInput by remember { mutableStateOf(patient.phone) }
    var wardInput by remember { mutableStateOf(patient.ward) }
    var bedInput by remember { mutableStateOf(patient.bed) }
    var conditionInput by remember { mutableStateOf(patient.condition) }
    var selectedDoctorId by remember { mutableStateOf(patient.doctorId) }
    var isDocExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = BrandTeal, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit Patient Details",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Update demographics and assigned bed for Patient ID: ${patient.id}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                VoiceOutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Patient Full Name *") },
                    speechPrompt = "Speak patient full name...",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VoiceOutlinedTextField(
                        value = ageInput,
                        onValueChange = { ageInput = it },
                        label = { Text("Age (Years)") },
                        speechPrompt = "Speak patient age...",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sex / Gender", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 2.dp)) {
                            listOf("M", "F", "Other").forEach { g ->
                                FilterChip(
                                    selected = genderInput.equals(g, ignoreCase = true),
                                    onClick = { genderInput = g },
                                    label = { Text(g, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                VoiceOutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Contact Phone") },
                    speechPrompt = "Speak contact phone number...",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VoiceOutlinedTextField(
                        value = wardInput,
                        onValueChange = { wardInput = it },
                        label = { Text("Ward Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    VoiceOutlinedTextField(
                        value = bedInput,
                        onValueChange = { bedInput = it },
                        label = { Text("Bed No.") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                VoiceOutlinedTextField(
                    value = conditionInput,
                    onValueChange = { conditionInput = it },
                    label = { Text("Clinical Condition / Diagnosis") },
                    speechPrompt = "Speak clinical condition...",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text("Attending Specialist Doctor", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val currentDocName = doctors.firstOrNull { it.id == selectedDoctorId }?.let { "${it.name} (${it.specialty})" } ?: "Select Doctor"
                ExposedDropdownMenuBox(
                    expanded = isDocExpanded,
                    onExpandedChange = { isDocExpanded = !isDocExpanded }
                ) {
                    OutlinedTextField(
                        value = currentDocName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDocExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isDocExpanded,
                        onDismissRequest = { isDocExpanded = false }
                    ) {
                        doctors.forEach { doc ->
                            DropdownMenuItem(
                                text = { Text("${doc.name} · ${doc.specialty}") },
                                onClick = {
                                    selectedDoctorId = doc.id
                                    isDocExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                onConfirm(
                                    nameInput.trim(),
                                    ageInput.toIntOrNull() ?: patient.age,
                                    genderInput,
                                    phoneInput.trim(),
                                    wardInput.trim(),
                                    bedInput.trim(),
                                    conditionInput.trim(),
                                    selectedDoctorId
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        enabled = nameInput.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Details", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
