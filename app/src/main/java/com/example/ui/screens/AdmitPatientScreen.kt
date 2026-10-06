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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HospitalConstants
import com.example.model.UserRole
import com.example.ui.theme.BrandTeal
import com.example.ui.viewmodel.HospitalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdmitPatientScreen(viewModel: HospitalViewModel) {
    val patients by viewModel.patients.collectAsState()
    val users by viewModel.users.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val userRole = UserRole.fromKey(currentUser?.role ?: "NURSE")
    val isDoctor = userRole == UserRole.DOCTOR
    val doctors = users.filter { it.role.equals("DOCTOR", ignoreCase = true) }

    // Find all free beds
    val occupiedBedIds = patients.map { it.bed }.toSet()
    val freeBeds = HospitalConstants.WARDS.flatMap { ward ->
        ward.beds.filter { it !in occupiedBedIds }.map { bed -> Pair(ward.name, bed) }
    }

    var name by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("M") }
    var phone by remember { mutableStateOf("") }
    var selectedBedPair by remember { mutableStateOf(freeBeds.firstOrNull()) }
    var selectedDoctorId by remember {
        mutableStateOf(if (isDoctor) currentUser?.id ?: (doctors.firstOrNull()?.id ?: "") else (doctors.firstOrNull()?.id ?: ""))
    }
    var condition by remember { mutableStateOf("") }

    var isBedDropdownExpanded by remember { mutableStateOf(false) }
    var isDocDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Admit New Patient",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enter patient admission and clinical details",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admit_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it },
                            label = { Text("Age") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admit_age_input")
                        )

                        Column(modifier = Modifier.weight(1.4f)) {
                            Text(
                                text = "GENDER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("M" to "Male", "F" to "Female", "O" to "Other").forEach { (code, lbl) ->
                                    FilterChip(
                                        selected = gender == code,
                                        onClick = { gender = code },
                                        label = { Text(code, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Contact Phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bed Dropdown
                    Text(
                        text = "AVAILABLE BED ASSIGNMENT *",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded = isBedDropdownExpanded,
                        onExpandedChange = { isBedDropdownExpanded = !isBedDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedBedPair?.let { "${it.first} — Bed ${it.second} (Available)" } ?: "No free beds",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isBedDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isBedDropdownExpanded,
                            onDismissRequest = { isBedDropdownExpanded = false }
                        ) {
                            freeBeds.forEach { (wardName, bedCode) ->
                                DropdownMenuItem(
                                    text = { Text("$wardName — Bed $bedCode") },
                                    onClick = {
                                        selectedBedPair = Pair(wardName, bedCode)
                                        isBedDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Doctor Dropdown (if not doctor)
                    if (!isDoctor) {
                        Text(
                            text = "ASSIGNED ATTENDING DOCTOR *",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        ExposedDropdownMenuBox(
                            expanded = isDocDropdownExpanded,
                            onExpandedChange = { isDocDropdownExpanded = !isDocDropdownExpanded }
                        ) {
                            val selDoc = doctors.firstOrNull { it.id == selectedDoctorId }
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
                                doctors.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text("${d.name} — ${d.specialty}") },
                                        onClick = {
                                            selectedDoctorId = d.id
                                            isDocDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Text(
                        text = "DIAGNOSIS / CONDITION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = condition,
                        onValueChange = { condition = it },
                        placeholder = { Text("Initial clinical diagnosis & reason for admission…") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val bed = selectedBedPair?.second
                                val ward = selectedBedPair?.first
                                val age = ageText.toIntOrNull() ?: 0
                                if (name.isNotBlank() && bed != null && ward != null) {
                                    viewModel.admitPatient(
                                        name = name.trim(),
                                        age = age,
                                        gender = gender,
                                        phone = phone.trim(),
                                        ward = ward,
                                        bed = bed,
                                        doctorId = selectedDoctorId,
                                        condition = condition.trim().ifBlank { "Observation" }
                                    )
                                } else {
                                    viewModel.showToast("Name and Bed are required")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                            enabled = name.isNotBlank() && selectedBedPair != null,
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp)
                                .testTag("admit_confirm_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Admit Patient", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.navigateBack() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}
