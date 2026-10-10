package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.ProfilePictureModal
import com.example.ui.components.VoiceOutlinedTextField
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
import com.example.model.canViewAllStaff
import com.example.ui.components.UserAvatar
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.MedWarning
import com.example.ui.viewmodel.HospitalViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminPortalScreen(viewModel: HospitalViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val users by viewModel.users.collectAsState()

    val userRole = UserRole.fromKey(currentUser?.role ?: "ADMINISTRATOR")
    val isBoss = userRole == UserRole.BOSS || currentUser?.id == "BOSS-0001"
    val isAdministrator = userRole == UserRole.ADMINISTRATOR || currentUser?.role?.contains("ADMIN", ignoreCase = true) == true
    val canManageStaff = isBoss || isAdministrator

    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("ALL") }

    var showAddModal by remember { mutableStateOf(false) }
    var modalInitialRole by remember { mutableStateOf(UserRole.DOCTOR) }
    var deleteCandidate by remember { mutableStateOf<UserEntity?>(null) }
    var pendingPhotoUser by remember { mutableStateOf<UserEntity?>(null) }

    var bossEditingUser by remember { mutableStateOf<UserEntity?>(null) }
    var showBossVaultModal by remember { mutableStateOf(false) }
    var showBossUpdatePublisherModal by remember { mutableStateOf(false) }
    val installedVersionName by viewModel.installedVersionName.collectAsState()

    val filteredStaff = users.filter { u ->
        val matchesCategory = when (selectedRoleFilter) {
            "ALL" -> true
            "DOCTOR" -> listOf("DOCTOR", "RMO", "MEDICAL_SUPER", "RMO_INCHARGE").contains(u.role)
            "CLINICAL" -> listOf("NURSE", "TECHNICIAN", "INCHARGE", "RMO", "RMO_INCHARGE").contains(u.role)
            else -> u.role.equals(selectedRoleFilter, ignoreCase = true)
        }
        val matchesSearch = searchQuery.isBlank() || (
            u.name.contains(searchQuery, ignoreCase = true) ||
            u.id.contains(searchQuery, ignoreCase = true) ||
            u.dept.contains(searchQuery, ignoreCase = true) ||
            u.specialty.contains(searchQuery, ignoreCase = true) ||
            u.phone.contains(searchQuery, ignoreCase = true)
        )
        matchesCategory && matchesSearch
    }

    val doctorCount = users.count {
        it.role.equals("DOCTOR", ignoreCase = true) ||
        it.role.equals("RMO", ignoreCase = true) ||
        it.role.equals("MEDICAL_SUPER", ignoreCase = true) ||
        it.role.equals("RMO_INCHARGE", ignoreCase = true)
    }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isBoss) "👑 Boss Staff Management" else if (canManageStaff) "Administrator Portal" else "Hospital Staff & Doctors Roster",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isBoss) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 100.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MedWarning.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "FULL ACCESS",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedWarning,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isBoss) "Boss ID authority: view all passwords & IDs, edit staff profiles, photos, numbers, and access rights."
                        else if (canManageStaff) "Manage hospital staff members, specialist doctors, and departments. User IDs set by Boss & Admin only."
                        else "Directory of hospital specialists, doctors, and clinical staff. Tap on contact or phone button to call directly.",
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

                    if (canManageStaff) {
                        Spacer(modifier = Modifier.height(14.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    modalInitialRole = UserRole.DOCTOR
                                    showAddModal = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 140.dp)
                                    .height(44.dp)
                                    .testTag("admin_add_doctor_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Add Doctor",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
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
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 165.dp)
                                    .height(44.dp)
                                    .testTag("admin_add_staff_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Add Staff / Nurse",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // Boss Exclusive Controls
                    if (isBoss) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showBossVaultModal = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MedWarning,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("👑 View All Staff Passwords & IDs Vault", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showBossUpdatePublisherModal = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandTeal,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("boss_publish_update_btn")
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🚀 Boss Control: Publish App Update to All Devices", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }


                    }
                }
            }
        }

        // Search Bar
        item {
            VoiceOutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search name, user ID, phone, dept or specialty…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                customTrailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                speechPrompt = "Speak staff name, user ID, phone, department, or specialty to search...",
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_staff_search")
            )
        }

        // Filter Pills
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf(
                    "ALL" to "All Staff (${users.size})",
                    "DOCTOR" to "Doctors ($doctorCount)",
                    "RMO" to "RMO (${users.count { it.role.equals("RMO", ignoreCase = true) }})",
                    "MEDICAL_SUPER" to "Med Super (${users.count { it.role.equals("MEDICAL_SUPER", ignoreCase = true) }})",
                    "RMO_INCHARGE" to "RMO Incharge (${users.count { it.role.equals("RMO_INCHARGE", ignoreCase = true) }})",
                    "NURSE" to "Nurses ($nurseCount)",
                    "ADMINISTRATOR" to "Admins ($adminCount)",
                    "CLINICAL" to "All Clinical"
                )
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = selectedRoleFilter == key,
                        onClick = { selectedRoleFilter = key },
                        label = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 38.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandCyan.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // Staff Items List
        items(filteredStaff) { staff ->
            val sRole = UserRole.fromKey(staff.role)
            val canDeleteThis = canManageStaff && staff.id != currentUser?.id && staff.role != "BOSS"

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("staff_card_${staff.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(name = staff.name, photoUri = staff.photoUri, size = 48.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = staff.name,
                                    fontSize = 15.sp,
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
                                text = "ID: ${staff.id} · ${sRole.label} · ${staff.dept.ifBlank { staff.specialty }}",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandTeal,
                                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                            )
                            if (staff.phone.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${staff.phone.trim()}"))
                                            context.startActivity(intent)
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Call ${staff.name}",
                                        tint = BrandTeal,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = staff.phone,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandTeal
                                    )
                                }
                            }
                        }

                        // Direct Call Phone Button
                        if (staff.phone.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${staff.phone.trim()}"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(com.example.ui.theme.MedGreen.copy(alpha = 0.15f))
                                    .testTag("admin_call_button_${staff.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Direct call ${staff.name}",
                                    tint = com.example.ui.theme.MedGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Photo quick update button (Take selfie / upload photo)
                        IconButton(
                            onClick = { pendingPhotoUser = staff },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Take selfie or upload photo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Boss Edit Rights & Profile Button
                        if (isBoss) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { bossEditingUser = staff },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MedWarning.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit staff rights and profile",
                                    tint = MedWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
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

                    // Boss Password Visibility & Credentials Row
                    if (isBoss) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = MedWarning, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Password: ",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = staff.pass,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Text(
                                    text = "Tap ✏️ to edit profile & rights",
                                    fontSize = 10.sp,
                                    color = MedWarning,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { bossEditingUser = staff }
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

    // Add Member Dialog (Supports Custom User ID set by Boss & Admin)
    if (showAddModal) {
        AddMemberDialog(
            initialRole = modalInitialRole,
            existingUsers = users,
            onDismiss = { showAddModal = false },
            onConfirm = { name, role, dept, spec, phone, email, photo, pass, customId ->
                viewModel.addStaff(name, role, dept, spec, phone, email, photo, pass, customId)
                showAddModal = false
            }
        )
    }

    // Boss Edit Staff Member Dialog
    if (bossEditingUser != null) {
        val targetStaff = bossEditingUser!!
        BossEditStaffDialog(
            staff = targetStaff,
            onDismiss = { bossEditingUser = null },
            onUpdatePhotoClick = { pendingPhotoUser = targetStaff },
            onConfirm = { oldId, newId, name, role, dept, spec, phone, email, pass, photoUri ->
                viewModel.updateStaffByBoss(oldId, newId, name, role, dept, spec, phone, email, pass, photoUri)
                bossEditingUser = null
            }
        )
    }

    // Boss Master Passwords Vault Dialog
    if (showBossVaultModal) {
        BossMasterVaultDialog(
            users = users,
            onDismiss = { showBossVaultModal = false },
            onEditStaff = { staff ->
                showBossVaultModal = false
                bossEditingUser = staff
            }
        )
    }

    // Boss App Update Publisher Dialog
    if (showBossUpdatePublisherModal) {
        BossUpdatePublisherDialog(
            installedVersionName = installedVersionName,
            onDismiss = { showBossUpdatePublisherModal = false },
            onPublish = { vName, title, notes, force ->
                viewModel.publishAppUpdateByBoss(
                    versionName = vName,
                    releaseTitle = title,
                    releaseNotes = notes,
                    forceUpdate = force
                )
            }
        )
    }

    // Selfie & Profile Photo Modal for Staff Members
    if (pendingPhotoUser != null) {
        ProfilePictureModal(
            targetUserId = pendingPhotoUser!!.id,
            userName = pendingPhotoUser!!.name,
            currentPhotoUri = pendingPhotoUser!!.photoUri,
            viewModel = viewModel,
            onDismiss = { pendingPhotoUser = null }
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
                        text = "Remove Staff Member?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Are you sure you want to remove ${staffToDelete.name} (${staffToDelete.id}) from the hospital staff registry?\nTheir access and attendance records will be removed.",
                        fontSize = 13.sp,
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
                            Text("Remove", fontWeight = FontWeight.Bold)
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

/**
 * Dialog to add a new member. User ID can be custom-set by Boss and Admin or automatically generated.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberDialog(
    initialRole: UserRole,
    existingUsers: List<UserEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, UserRole, String, String, String, String, String?, String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(initialRole) }
    var selectedSpecialty by remember { mutableStateOf(HospitalConstants.SPECIALTIES.first()) }
    var selectedDept by remember { mutableStateOf(HospitalConstants.DEPARTMENTS.first()) }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var tempPass by remember { mutableStateOf("12345") }
    var customIdInput by remember { mutableStateOf("") }

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
                VoiceOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    speechPrompt = "Speak staff member's full name...",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                // Custom User ID field (set by Boss and Admin only)
                VoiceOutlinedTextField(
                    value = customIdInput,
                    onValueChange = { customIdInput = it },
                    label = { Text("Custom User ID (Set by Admin/Boss)") },
                    placeholder = { Text("Leave empty for auto: ${selectedRole.prefix}-XXXX") },
                    supportingText = { Text("User ID is exclusively provisioned by Admin & Boss", fontSize = 10.5.sp) },
                    speechPrompt = "Speak custom user ID or prefix...",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text("STAFF ROLE & ACCESS RIGHTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                val isMedicalRole = selectedRole in listOf(UserRole.DOCTOR, UserRole.RMO, UserRole.MEDICAL_SUPER, UserRole.RMO_INCHARGE)
                if (isMedicalRole) {
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
                VoiceOutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Contact Phone Number") },
                    speechPrompt = "Speak contact phone number...",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                VoiceOutlinedTextField(
                    value = tempPass,
                    onValueChange = { tempPass = it },
                    label = { Text("Initial Password") },
                    speechPrompt = "Speak initial password...",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val dept = if (isMedicalRole) selectedSpecialty else selectedDept
                            val spec = if (isMedicalRole) selectedSpecialty else selectedDept
                            val customId = customIdInput.trim().ifBlank { null }
                            onConfirm(name.trim(), selectedRole, dept, spec, phone.trim(), email.trim(), photoUri, tempPass.trim(), customId)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Register & Save Account", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Boss Exclusive Staff Profile, Rights, and Password Editor.
 * Boss can view and edit any user's ID, password, name, picture, phone, role, and rights.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BossEditStaffDialog(
    staff: UserEntity,
    onDismiss: () -> Unit,
    onUpdatePhotoClick: () -> Unit = {},
    onConfirm: (oldId: String, newId: String, name: String, role: UserRole, dept: String, spec: String, phone: String, email: String, pass: String, photoUri: String?) -> Unit
) {
    var idInput by remember { mutableStateOf(staff.id) }
    var nameInput by remember { mutableStateOf(staff.name) }
    var selectedRole by remember { mutableStateOf(UserRole.fromKey(staff.role)) }
    var deptInput by remember { mutableStateOf(staff.dept) }
    var specialtyInput by remember { mutableStateOf(staff.specialty) }
    var phoneInput by remember { mutableStateOf(staff.phone) }
    var emailInput by remember { mutableStateOf(staff.email) }
    var passInput by remember { mutableStateOf(staff.pass) }
    var photoUri by remember(staff.photoUri) { mutableStateOf(staff.photoUri) }
    var showPass by remember { mutableStateOf(true) }

    var isRoleExpanded by remember { mutableStateOf(false) }

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
                    Column {
                        Text(
                            text = "👑 Boss Staff Editor",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedWarning
                        )
                        Text(
                            text = "Full authority over ID, profile, password & access rights",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(name = nameInput.ifBlank { "?" }, photoUri = photoUri, size = 52.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onUpdatePhotoClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Selfie / Change Picture", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                // Boss can edit User ID
                VoiceOutlinedTextField(
                    value = idInput,
                    onValueChange = { idInput = it },
                    label = { Text("User ID * (Boss Authority)") },
                    supportingText = { Text("Only Boss can change user IDs", fontSize = 10.5.sp, color = MedWarning) },
                    speechPrompt = "Speak staff User ID...",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                VoiceOutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Full Name *") },
                    speechPrompt = "Speak staff full name...",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                // Boss can view and edit Password
                VoiceOutlinedTextField(
                    value = passInput,
                    onValueChange = { passInput = it },
                    label = { Text("Password * (Plain View / Boss Access)") },
                    customTrailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(
                                imageVector = if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (showPass) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    speechPrompt = "Speak password...",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text("ROLE & RIGHTS (BOSS MODIFIABLE)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MedWarning)
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
                        UserRole.entries.forEach { r ->
                            DropdownMenuItem(text = { Text(r.label) }, onClick = { selectedRole = r; isRoleExpanded = false })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                VoiceOutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Phone Number") },
                    speechPrompt = "Speak phone number...",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                VoiceOutlinedTextField(
                    value = deptInput,
                    onValueChange = { deptInput = it },
                    label = { Text("Department") },
                    speechPrompt = "Speak department name...",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                VoiceOutlinedTextField(
                    value = specialtyInput,
                    onValueChange = { specialtyInput = it },
                    label = { Text("Clinical Specialty") },
                    speechPrompt = "Speak clinical specialty...",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (idInput.isNotBlank() && nameInput.isNotBlank() && passInput.isNotBlank()) {
                                onConfirm(
                                    staff.id,
                                    idInput.trim().uppercase(),
                                    nameInput.trim(),
                                    selectedRole,
                                    deptInput.trim(),
                                    specialtyInput.trim(),
                                    phoneInput.trim(),
                                    emailInput.trim(),
                                    passInput.trim(),
                                    photoUri
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedWarning),
                        enabled = idInput.isNotBlank() && nameInput.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
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

/**
 * Boss Exclusive Master Password & Credentials Vault.
 * Lists all registered users with their user IDs, plain passwords, roles, and contact numbers.
 */
@Composable
fun BossMasterVaultDialog(
    users: List<UserEntity>,
    onDismiss: () -> Unit,
    onEditStaff: (UserEntity) -> Unit
) {
    var vaultSearch by remember { mutableStateOf("") }
    val filtered = users.filter {
        vaultSearch.isBlank() ||
        it.name.contains(vaultSearch, ignoreCase = true) ||
        it.id.contains(vaultSearch, ignoreCase = true) ||
        it.role.contains(vaultSearch, ignoreCase = true) ||
        it.phone.contains(vaultSearch, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .height(580.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "👑 Master Passwords & ID Vault",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedWarning
                        )
                        Text(
                            text = "Confidential: Boss Access to all ${users.size} staff credentials",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                VoiceOutlinedTextField(
                    value = vaultSearch,
                    onValueChange = { vaultSearch = it },
                    placeholder = { Text("Filter staff ID or name…", fontSize = 12.sp) },
                    customTrailingIcon = {
                        if (vaultSearch.isNotEmpty()) {
                            IconButton(onClick = { vaultSearch = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    speechPrompt = "Speak staff name, user ID or role to filter...",
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { user ->
                        val r = UserRole.fromKey(user.role)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(name = user.name, photoUri = user.photoUri, size = 36.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${user.id} · ${r.label}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandTeal
                                    )
                                    if (user.phone.isNotBlank()) {
                                        val ctx = LocalContext.current
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${user.phone.trim()}"))
                                                ctx.startActivity(intent)
                                            }
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = BrandTeal, modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(text = user.phone, fontSize = 10.5.sp, color = BrandTeal, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Password box
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MedWarning.copy(alpha = 0.18f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "🔑 ${user.pass}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { onEditStaff(user) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MedWarning, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Vault", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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

@Composable
fun BossUpdatePublisherDialog(
    installedVersionName: String,
    onDismiss: () -> Unit,
    onPublish: (versionName: String, title: String, notes: String, force: Boolean) -> Unit
) {
    var versionName by remember { mutableStateOf("v1.2") }
    var releaseTitle by remember { mutableStateOf("Multi-Device Auto-Sync & Feature Upgrade") }
    var releaseNotes by remember { mutableStateOf("Boss updated hospital modules. All staff devices auto-syncing in real-time.") }
    var forceUpdate by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "👑 Boss App Auto-Update Publisher",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedWarning
                        )
                        Text(
                            text = "Current: $installedVersionName · Auto-updates all staff devices",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                VoiceOutlinedTextField(
                    value = versionName,
                    onValueChange = { versionName = it },
                    label = { Text("Version Label (e.g. v1.2)") },
                    speechPrompt = "Speak version label...",
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                VoiceOutlinedTextField(
                    value = releaseTitle,
                    onValueChange = { releaseTitle = it },
                    label = { Text("Release Title") },
                    speechPrompt = "Speak release title...",
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                VoiceOutlinedTextField(
                    value = releaseNotes,
                    onValueChange = { releaseNotes = it },
                    label = { Text("Release Notes & Features") },
                    speechPrompt = "Speak release notes...",
                    minLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { forceUpdate = !forceUpdate }
                        .padding(12.dp)
                ) {
                    androidx.compose.material3.Switch(
                        checked = forceUpdate,
                        onCheckedChange = { forceUpdate = it }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Force Instant Auto-Update on All Devices",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "All staff devices update data & features automatically.",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onPublish(versionName.trim(), releaseTitle.trim(), releaseNotes.trim(), forceUpdate)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("🚀 Publish Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

