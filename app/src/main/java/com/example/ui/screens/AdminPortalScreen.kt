package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.UserEntity
import com.example.model.HospitalConstants
import com.example.model.UserRole
import com.example.ui.components.UserAvatar
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.MedWarning
import com.example.ui.viewmodel.HospitalViewModel
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalScreen(viewModel: HospitalViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val users by viewModel.users.collectAsState()

    val userRole = UserRole.fromKey(currentUser?.role ?: "ADMINISTRATOR")
    val isBoss = userRole == UserRole.BOSS

    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("ALL") }

    var showAddModal by remember { mutableStateOf(false) }
    var modalInitialRole by remember { mutableStateOf(UserRole.DOCTOR) }
    var deleteCandidate by remember { mutableStateOf<UserEntity?>(null) }
    var pendingPhotoUser by remember { mutableStateOf<UserEntity?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && pendingPhotoUser != null) {
            viewModel.updateUserPhoto(pendingPhotoUser!!.id, uri.toString())
            pendingPhotoUser = null
        }
    }

    val filteredStaff = users.filter { u ->
        val matchesCategory = when (selectedRoleFilter) {
            "ALL" -> true
            "CLINICAL" -> listOf("NURSE", "TECHNICIAN", "INCHARGE").contains(u.role)
            else -> u.role.equals(selectedRoleFilter, ignoreCase = true)
        }
        val matchesSearch = searchQuery.isBlank() || (
            u.name.contains(searchQuery, ignoreCase = true) ||
            u.id.contains(searchQuery, ignoreCase = true) ||
            u.dept.contains(searchQuery, ignoreCase = true) ||
            u.specialty.contains(searchQuery, ignoreCase = true)
        )
        matchesCategory && matchesSearch
    }

    val doctorCount = users.count { it.role.equals("DOCTOR", ignoreCase = true) }
    val nurseCount = users.count { it.role.equals("NURSE", ignoreCase = true) }
    val adminCount = users.count { it.role.equals("ADMINISTRATOR", ignoreCase = true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Control Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isBoss) "👑 Boss Control — Administrator Portal" else "Administrator Portal",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isBoss) "Full rights: add / remove hospital members, update photos & credentials."
                        else "Manage hospital staff members and specialist doctors.",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeadcountStat("TOTAL", "${users.size}", BrandTeal, Modifier.weight(1f))
                        HeadcountStat("DOCTORS", "$doctorCount", BrandCyan, Modifier.weight(1f))
                        HeadcountStat("NURSES", "$nurseCount", Color(0xFF7C3AED), Modifier.weight(1f))
                        HeadcountStat("ADMINS", "$adminCount", MedWarning, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                modalInitialRole = UserRole.DOCTOR
                                showAddModal = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("admin_add_doctor_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Doctor", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                modalInitialRole = UserRole.NURSE
                                showAddModal = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("admin_add_staff_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Staff / Nurse", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Search
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search name, user ID, dept or specialty…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_staff_search")
            )
        }

        // Filter chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "ALL" to "All",
                    "DOCTOR" to "Doctors",
                    "NURSE" to "Nurses",
                    "CLINICAL" to "Technicians & Ward",
                    "ADMINISTRATOR" to "Admins",
                    "RECEPTIONIST" to "Reception",
                    "ACCOUNTANT" to "Accounts",
                    "MEDICINE" to "Pharmacy",
                    "MAINTENANCE" to "Maintenance"
                ).forEach { (code, label) ->
                    item {
                        FilterChip(
                            selected = selectedRoleFilter == code,
                            onClick = { selectedRoleFilter = code },
                            label = { Text(label, fontSize = 12.sp) },
                            shape = RoundedCornerShape(999.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandCyan.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }

        // Staff Items
        items(filteredStaff) { staff ->
            val sRole = UserRole.fromKey(staff.role)
            val canDeleteThis = staff.id != currentUser?.id && staff.role != "BOSS"

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("staff_card_${staff.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(name = staff.name, photoUri = staff.photoUri, size = 44.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = staff.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (staff.role == "BOSS") {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(MedWarning.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "👑 BOSS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MedWarning
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${staff.id} · ${sRole.label} · ${staff.dept.ifBlank { staff.specialty }}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                        Text(
                            text = "Temp pass: ${staff.pass} ${if (staff.phone.isNotBlank()) "· " + staff.phone else ""}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }

                    // Photo update button
                    IconButton(
                        onClick = {
                            pendingPhotoUser = staff
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Update photo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Delete button
                    if (canDeleteThis) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { deleteCandidate = staff },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }

    // Add Member Dialog
    if (showAddModal) {
        AddMemberDialog(
            initialRole = modalInitialRole,
            existingUsers = users,
            onDismiss = { showAddModal = false },
            onConfirm = { name, role, dept, spec, phone, email, photo, pass ->
                viewModel.addStaff(name, role, dept, spec, phone, email, photo, pass)
                showAddModal = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (deleteCandidate != null) {
        val staffToDelete = deleteCandidate!!
        Dialog(onDismissRequest = { deleteCandidate = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "⚠ Delete Staff Member",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Remove ${staffToDelete.name} (${staffToDelete.id}, ${UserRole.fromKey(staffToDelete.role).label}) from MB Nursing Home? Their credentials and access will be revoked immediately.",
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.deleteStaff(staffToDelete.id)
                                deleteCandidate = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete Member")
                        }
                        OutlinedButton(
                            onClick = { deleteCandidate = null },
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberDialog(
    initialRole: UserRole,
    existingUsers: List<UserEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, UserRole, String, String, String, String, String?, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(initialRole) }
    var selectedSpecialty by remember { mutableStateOf(HospitalConstants.SPECIALTIES.first()) }
    var selectedDept by remember { mutableStateOf(HospitalConstants.DEPARTMENTS.first()) }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var tempPass by remember { mutableStateOf("12345") }

    var isRoleExpanded by remember { mutableStateOf(false) }
    var isDeptExpanded by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) photoUri = uri.toString()
    }

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
                    Text(
                        text = if (selectedRole == UserRole.DOCTOR) "Register New Doctor" else "Register New Staff",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(name = name.ifBlank { "?" }, photoUri = photoUri, size = 52.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Photo", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("STAFF ROLE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ExposedDropdownMenuBox(
                    expanded = isRoleExpanded,
                    onExpandedChange = { isRoleExpanded = !isRoleExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedRole.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRoleExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = isRoleExpanded, onDismissRequest = { isRoleExpanded = false }) {
                        UserRole.entries.filter { it != UserRole.BOSS }.forEach { r ->
                            DropdownMenuItem(text = { Text(r.label) }, onClick = { selectedRole = r; isRoleExpanded = false })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                if (selectedRole == UserRole.DOCTOR) {
                    Text("CLINICAL SPECIALTY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ExposedDropdownMenuBox(
                        expanded = isDeptExpanded,
                        onExpandedChange = { isDeptExpanded = !isDeptExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedSpecialty,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDeptExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = isDeptExpanded, onDismissRequest = { isDeptExpanded = false }) {
                            HospitalConstants.SPECIALTIES.forEach { s ->
                                DropdownMenuItem(text = { Text(s) }, onClick = { selectedSpecialty = s; isDeptExpanded = false })
                            }
                        }
                    }
                } else {
                    Text("DEPARTMENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ExposedDropdownMenuBox(
                        expanded = isDeptExpanded,
                        onExpandedChange = { isDeptExpanded = !isDeptExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedDept,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDeptExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = isDeptExpanded, onDismissRequest = { isDeptExpanded = false }) {
                            HospitalConstants.DEPARTMENTS.forEach { d ->
                                DropdownMenuItem(text = { Text(d) }, onClick = { selectedDept = d; isDeptExpanded = false })
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("LOGIN CREDENTIALS PREVIEW", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "Auto ID: ${selectedRole.prefix}-XXXX · Temp Password: $tempPass",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandTeal,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val dept = if (selectedRole == UserRole.DOCTOR) selectedSpecialty else selectedDept
                            val spec = if (selectedRole == UserRole.DOCTOR) selectedSpecialty else selectedDept
                            onConfirm(name.trim(), selectedRole, dept, spec, phone.trim(), email.trim(), photoUri, tempPass)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Invite & Create Account", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HeadcountStat(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
        }
    }
}
