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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppScreen
import com.example.model.UserRole
import com.example.ui.components.DocumentCanvasView
import com.example.ui.components.UserAvatar
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.MedWarning
import com.example.ui.viewmodel.HospitalViewModel

@Composable
fun PatientsScreen(viewModel: HospitalViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val patients by viewModel.patients.collectAsState()
    val users by viewModel.users.collectAsState()
    val documents by viewModel.documents.collectAsState()

    val userRole = UserRole.fromKey(currentUser?.role ?: "NURSE")
    val isDoctor = userRole == UserRole.DOCTOR || userRole == UserRole.RMO || userRole == UserRole.MEDICAL_SUPER || userRole == UserRole.RMO_INCHARGE
    val isBoss = userRole == UserRole.BOSS
    val isClinical = listOf(UserRole.NURSE, UserRole.TECHNICIAN, UserRole.INCHARGE).contains(userRole)

    var searchQuery by remember { mutableStateOf("") }
    var doctorTab by remember { mutableStateOf("mine") } // "mine" or "referred"
    var selectedDoctorFilter by remember { mutableStateOf("all") }

    val doctors = users.filter {
        it.role.equals("DOCTOR", ignoreCase = true) ||
        it.role.equals("RMO", ignoreCase = true) ||
        it.role.equals("MEDICAL_SUPER", ignoreCase = true) ||
        it.role.equals("RMO_INCHARGE", ignoreCase = true)
    }

    val filteredPatients = patients.filter { patient ->
        val assignedDoctor = users.firstOrNull { it.id == patient.doctorId }

        // Role-based visibility
        val roleAllowed = if (isDoctor) {
            if (doctorTab == "mine") {
                patient.doctorId == currentUser?.id
            } else {
                patient.referralDoctorId == currentUser?.id
            }
        } else {
            if (selectedDoctorFilter == "all") true else patient.doctorId == selectedDoctorFilter
        }

        // Search filter
        val searchAllowed = searchQuery.isBlank() || (
            patient.name.contains(searchQuery, ignoreCase = true) ||
            patient.id.contains(searchQuery, ignoreCase = true) ||
            patient.condition.contains(searchQuery, ignoreCase = true) ||
            patient.bed.contains(searchQuery, ignoreCase = true) ||
            (assignedDoctor?.name?.contains(searchQuery, ignoreCase = true) == true) ||
            (assignedDoctor?.dept?.contains(searchQuery, ignoreCase = true) == true)
        )

        roleAllowed && searchAllowed
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Input
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, ID, department or doctor…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("patient_search_input")
                )
            }

            // Tabs / Filters
            item {
                if (isDoctor) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = doctorTab == "mine",
                            onClick = { doctorTab = "mine" },
                            label = { Text("My Patients") },
                            shape = RoundedCornerShape(999.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandCyan.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        FilterChip(
                            selected = doctorTab == "referred",
                            onClick = { doctorTab = "referred" },
                            label = { Text("🔁 Referred to Me") },
                            shape = RoundedCornerShape(999.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandCyan.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                } else if (isBoss) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedDoctorFilter == "all",
                                onClick = { selectedDoctorFilter = "all" },
                                label = { Text("All Doctors") },
                                shape = RoundedCornerShape(999.dp)
                            )
                        }
                        items(doctors) { doc ->
                            FilterChip(
                                selected = selectedDoctorFilter == doc.id,
                                onClick = { selectedDoctorFilter = doc.id },
                                label = { Text(doc.name.replace("Dr. ", "")) },
                                shape = RoundedCornerShape(999.dp)
                            )
                        }
                    }
                }
            }

            // Patient List Items
            if (filteredPatients.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No patients match the current criteria.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredPatients) { patient ->
                    val doc = users.firstOrNull { it.id == patient.doctorId }
                    val patientDocs = documents.filter { it.patientId == patient.id }
                    val isReferredToMe = isDoctor && patient.referralDoctorId == currentUser?.id && patient.doctorId != currentUser?.id

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openPatientDetail(patient.id) }
                            .testTag("patient_card_${patient.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                UserAvatar(name = patient.name, size = 46.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = patient.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = patient.id,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isReferredToMe) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(MedWarning.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "🔁 Referred",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MedWarning
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "${patient.age}y / ${patient.gender} · ${patient.ward} · Bed ${patient.bed}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )

                                    Text(
                                        text = patient.condition,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )

                                    Text(
                                        text = "Under ${doc?.name ?: "Doctor"} · ${doc?.specialty ?: ""}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.openScanner(patient.id) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(BrandTeal.copy(alpha = 0.12f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Scan Document",
                                        tint = BrandTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Document Thumbnails
                            if (patientDocs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    patientDocs.take(4).forEach { d ->
                                        DocumentCanvasView(
                                            docTypeOrUri = d.docTypeOrUri,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { viewModel.openDocumentViewer(d) }
                                        )
                                    }
                                    if (patientDocs.size > 4) {
                                        Text(
                                            text = "+${patientDocs.size - 4}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Action Button to Admit Patient
        if (isBoss || isDoctor || isClinical) {
            FloatingActionButton(
                onClick = { viewModel.pushScreen(AppScreen.ADMIT_PATIENT) },
                containerColor = BrandTeal,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 20.dp)
                    .testTag("fab_admit_patient")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Admit Patient")
            }
        }
    }
}
